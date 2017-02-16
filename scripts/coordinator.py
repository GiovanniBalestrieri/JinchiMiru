#!/usr/bin/env python
# license removed for brevity
import sys, tf
import rospy, math
import numpy as np
import time
from std_msgs.msg import String
from gazebo_msgs.srv import *
from geometry_msgs.msg import *
import uriString
import settings, json


# KnowledgeBase - bridge syncronization
sync = False
classInfoTag = ""
coordInfoTag = ""
lexInfoTag = ""
errorInfoTag = ""
uriInfoTag = ""
workingOnRequest = False

# Prepare list for spwaned instances
instances = []
instancesDict = {}
# Flag to send report to Ontology_Handler node
fire = False
threshold = 0.5

rospy.wait_for_service("gazebo/delete_model")
rospy.wait_for_service("gazebo/spawn_sdf_model")
rospy.wait_for_service("gazebo/get_model_state")

time.sleep(1)

gms = rospy.ServiceProxy('/gazebo/get_model_state', GetModelState)
delete_model = rospy.ServiceProxy("gazebo/delete_model", DeleteModel)
spawn_model = rospy.ServiceProxy("gazebo/spawn_sdf_model", SpawnModel)

unit = 0.05

with open("/home/heavylab/.gazebo/models/coke_can/model.sdf", "r") as f:
    coke_xml = f.read();
with open("/home/heavylab/.gazebo/models/beer/model.sdf", "r") as f:
    beer_xml = f.read();
with open("/home/heavylab/.gazebo/models/table/model.sdf", "r") as f:
    table_xml = f.read();
with open("/home/heavylab/.gazebo/models/chair/model.sdf","r") as f:
    chair_xml = f.read();


bridge = rospy.Publisher('bridge', String, queue_size=10)
extern = rospy.Publisher('extern_commands', String, queue_size=10)

def talker():
    global workingOnRequest
    # Subscribe to mapHandler Topic
    rospy.Subscriber("huric_jena", String, callback)
    # Subscribe to Externel Commands Topic
    rospy.Subscriber("extern_commands", String, callbackCommands)
    rospy.init_node('Coordinator', anonymous=True)
    print(uriString.COORDINATOR + "Welcome on board");
    rate = rospy.Rate(0.2) # 2/10sec
    time.sleep(5)
    while not rospy.is_shutdown():
        # Initialization: Request Entities of type Furniture
        if (sync==False):
            hello_str = "i\tgetInstances\tT"
            print(uriString.COORDINATOR + "Initialization: Sending request to Semantic Map");
            #rospy.loginfo(hello_str);
            bridge.publish(hello_str);            
            
        elif (not workingOnRequest):        
            if settings.finalVerbosity:
                print("\n"+uriString.COORDINATOR+ "Sync ...\n")
            # Looking for manual position updates using Gazebo
            if (settings.semananticMapVerbosityMax):
                print("\n\n"+uriString.COORDINATOR+"POST\n\n");
                print(""+uriString.COORDINATOR+"Instances:\n")
                print(instances)
                print(""+uriString.COORDINATOR+"Dictionary:\n")
                print(instancesDict)

            #print("Looking for updates. Instances:" + str(len(instances)))
            # Test for manual Changes:
            for x in instances:

                if x in instancesDict: coords = instancesDict[x][uriString.TAG_COORD_FULL]

                if (settings.updateCheckVerbosityMax):
                    print("\n\n\n\n"+uriString.COORDINATOR+"DICTIONARY:")
                    for key,values in instancesDict[x].iteritems():
                        print "%s: %s" % (key, values)
                    print("\n\n\n"+uriString.COORDINATOR+"\STOP\n\n")

                model_req = gms(str(x.split("#")[1]),"world")

                pos = model_req.pose.position
                pos.x = float("{0:.1f}".format(pos.x))
                pos.y = float("{0:.1f}".format(pos.y))
                pos.z = float("{0:.1f}".format(pos.z))

                poseToUpdate = str(pos.x)+","+str(pos.y)+","+str(pos.z)

                if isGreaterThatNorm(pos,coords,threshold,x):
                    update_rqt = uriString.UPDATE_HEADER+uriString.SEPARATOR+x+uriString.SEPARATOR+instancesDict[x][uriString.TAG_TYPE_FULL]+uriString.SEPARATOR+poseToUpdate+uriString.SEPARATOR+instancesDict[x][uriString.TAG_LEX] + uriString.SEPARATOR+uriString.TERMINATOR;
                    #print("UPDATE requested semantic Map");
                    #rospy.loginfo(update_rqt);
                    bridge.publish(update_rqt);   

        rate.sleep()

def isGreaterThatNorm(positionGazebo,positionReal,normThreshold,uriInstance):
    coords = positionReal.split(",")
    if coords[0] == "":
            coords[0] = "0.0"
            print("\n\n\n"+uriString.COORDINATOR+"WARNING! reading force 0 "+ uriInstance + "\n\n")
    if coords[1] == "":
            coords[1] = "0.0"
            print("\n\n\n"+uriString.COORDINATOR+"WARNING! reading force 0 "+ uriInstance + "\n\n")
    if coords[2] == "":
            coords[2] = "0.0"
            print("\n\n\n"+uriString.COORDINATOR+"WARNING! reading force 0 "+ uriInstance + "\n\n")
    past = np.array([float(coords[0]),float(coords[1]),float(coords[2])])

    x = float("{0:.1f}".format(positionGazebo.x))
    y = float("{0:.1f}".format(positionGazebo.y))
    z = float("{0:.1f}".format(positionGazebo.z))
    cur = np.array([x,y,z]).astype(float)
    dif = past - cur
    delta = np.linalg.norm(dif)
    if (delta > normThreshold):
        # KB Update required
        print(""+uriString.COORDINATOR+"Sync model of " + uriInstance + " : DeltaPosition: "+ str(delta) +  "\n")
        fire = True
        poseToUpdate = str(x)+","+str(y)+","+str(z)
        if settings.verboseNormComputation:
            print(poseToUpdate)

        return True

def spawn(name,coords,xml):
    ## TODO check whether instance already present in gazebo
    # get name from second property
    # retrieve coords

    # init orientation steady
    roll = 0
    pitch = 0
    yaw = 0 

    coordsInfo = coords.split(",")  
    # Check if 3D info
    if (len(coordsInfo)==3):

        
        if name in instances:
        #if (model_req.success):
            # Object is present. Check position and respawn

             # Check if Model has already been spawned
            print("\n\n\t"+uriString.COORDINATOR+"Checking model in gazebo\n");
            model_req = gms(str(name.split("#")[1]),"world")
            print("\n\n\t"+uriString.COORDINATOR+"\tTada\t"+str(model_req.success)+"\n");
            pos = model_req.pose.position
            if isGreaterThatNorm(pos,coords,threshold,name):
                # Position is different. Delete and respawn
                #delete_model(name)

                orient = Quaternion(*tf.transformations.quaternion_from_euler(float(roll),float(pitch),float(yaw)))
                pose = Pose(Point(float(coordsInfo[0]),float(coordsInfo[1]),float(coordsInfo[2])), orient)
                print spawn_model(name.split("#")[1], xml, "", pose, "world")

        else:

            print("\n\n\t"+uriString.COORDINATOR+" Not present!\n")
            orient = Quaternion(*tf.transformations.quaternion_from_euler(float(roll),float(pitch),float(yaw)))
            pose = Pose(Point(float(coordsInfo[0]),float(coordsInfo[1]),float(coordsInfo[2])), orient)
            print spawn_model(name.split("#")[1], xml, "", pose, "world")
            #print("UPDATE requested semantic Map");
            #rospy.loginfo(update_rqt);
            #bridge.publish(update_rqt);  
    else:
        print("\n\n\t"+uriString.COORDINATOR+"WARNING. Requested to spawn a model with an invalid pose information\n\n");

'''
    Callback function for /huric_jena topic
    
    Commands supported: 
        -> initial Sync  :   "i\tclass\turi\t"x,y,z"\tref\tz"
            -> spawn 3d models
            -> save into instancesDict all known instances

        -> getEntities  :   "o\t\tz"
        -> AddEntities  :   "o\ta\ttype\tcoordz\tcoordy\tcoordz\tlabel\tz"
        -> updateEntities : "o\tu\ttype\turi\tcoordz\tcoordy\tcoordz\tlabel\tz"
        -> Export Ontology : "o\te\tfilename\tpath\tz"
        -> Delete : "o\td\ttype\turi\tz"
'''
def callback(data):
    global sync
    global errorInfoTag
    global coordInfoTag
    global lexInfoTag
    global uriInfoTag
    global classInfoTag
    global workingOnRequest

    prop = data.data.split("\t")
    if (len(prop)>3):

        # Received response from SemanticMap Node 
        # Initial sync with knowledge base
        if (prop[0]==uriString.INIT_HEADER or prop[0]==uriString.DEL_ACK_HEADER or prop[0] == "u" or prop[0] == "addack" or prop[0] == "getack" or prop[0] == uriString.LIST_ACK_HEADER or prop[0] == uriString.EXPORT_ACK_HEADER or prop[0]==uriString.LOAD_ACK_HEADER):
            workingOnRequest = True

            if (prop[0]==uriString.INIT_HEADER):
                print("\n"+uriString.COORDINATOR+"Received initial instances request\n\n")
            elif (prop[0]==uriString.UPDATE_HEADER):
                print("\n"+uriString.COORDINATOR+"Received update confirmation\n\n")
            elif (prop[0]==uriString.ADD_ACK_HEADER):
                print("\n"+uriString.COORDINATOR+"Received ack from SemanticMap handler \n\n")
                print("\n"+uriString.COORDINATOR+"Sending add_ack to External topic \n\n")

                ack_rqt = uriString.ADD_ACK_HEADER + "\t" + "T"
                extern.publish(ack_rqt);
            elif (prop[0]==uriString.LOAD_ACK_HEADER):
                print("\n"+uriString.COORDINATOR+"Received ack from SemanticMap handler \n\n")
                print("\n"+uriString.COORDINATOR+"Sending load_ack to External topic. Requesting Sync \n\n")
                sync = False;
                ack_rqt = uriString.LOAD_ACK_HEADER + "\t" + "T"
                extern.publish(ack_rqt);
            elif (prop[0]==uriString.GET_ACK_HEADER):
                print("\n"+uriString.COORDINATOR+"Received ack from SemanticMap handler \n\n")
                print("\n"+uriString.COORDINATOR+"Sending get_ack to External topic \n\n")



                ack_rqt = uriString.GET_ACK_HEADER + "\t" + "T"
                extern.publish(ack_rqt);
            elif (prop[0]==uriString.LIST_ACK_HEADER):
                print("\n"+uriString.COORDINATOR+"Received ack from SemanticMap handler \n\n")
                print("\n"+uriString.COORDINATOR+"Sending INSTANCES to External topic \n\n")


                print("\nJson\n")
                json.dump(instancesDict,sys.stdout)

                ack_rqt = uriString.LIST_ACK_HEADER + "\t" + "T"
                extern.publish(ack_rqt);

            
            if (settings.topicVerbosityMax):
                print(""+uriString.COORDINATOR+"Received:\t" +data.data+"\n\n")

            # Get error
            errorInfoTag = prop[1].split(",")[0]
            errorInstance = prop[1].split(",")[1]

            if errorInstance == "1":
                print("\n"+uriString.COORDINATOR+"ERROR from Semantic Map\n\n")


            # Handle export ack with error
            if (prop[0]==uriString.EXPORT_ACK_HEADER):

                if (settings.topicVerbosityMax):
                    print("\n"+uriString.COORDINATOR+"Received ack from SemanticMap handler \n\n")

                ack_rqt = uriString.EXPORT_ACK_HEADER + "\t" + errorInstance +  "T"
                extern.publish(ack_rqt);

            # Retrieve Class URI
            classInfoTag = prop[2].split(",")[0]
            classInstance = prop[2].split(",")[1]

            # Retrieve UriInstance
            uriInfoTag = prop[3].split(",")[0]
            uriInstance = prop[3].split(",")[1]

            if (prop[0]==uriString.DEL_ACK_HEADER):

                if (settings.topicVerbosityMax):
                    print("\n"+uriString.COORDINATOR+"Received ack from SemanticMap handler.\n"+ uriInstance + "\tDeleted \n\n")
                
                if uriInstance == uriString.ALL:
                    print("\n"+uriString.COORDINATOR+" Removing ALL instances from list of instances.")

                    if (settings.topicVerbosityMax):
                        print("\n\nTEST PRE\n"+ str(instances))
                        print("\nDICT:\n"+ str(instancesDict) + "\n\n")

                    instancesDict.clear()

                    if (settings.topicVerbosityMax):
                        print("\nremoved dictionary\n\n")
                    for x in instances: 
                        delete_model(str(x.split("#")[1]))
                        time.sleep(1)

                        if (settings.topicVerbosityMax):
                            print("\nDeleting 3D model: "+ str(x.split("#")[1]) + "\n\n")

                    del instances[:]
                    if (settings.topicVerbosityMax):
                        print("\n\nTEST POST\n"+ str(instances)+"\nDICT:\n"+ str(instancesDict) + "\n\n")


                else:
                    if uriInstance in instances:
                        instances.remove(uriInstance)

                    if (settings.topicVerbosityMax):
                        print("\n"+uriString.COORDINATOR+" Removing ONE instance from Dictionary of instances")
                        print("\n"+uriString.COORDINATOR+" Removed:\t" + uriInstance)
                        print(uriInstance.split("#")[1])

                    model_req = gms(str(uriInstance.split("#")[1]),"world");
                    print("\n"+str(model_req.success))

                    delete_model(str(uriInstance.split("#")[1]))
                    if uriInstance in instancesDict: 
                        del instancesDict[uriInstance]

                    if (settings.topicVerbosityMax):
                        print("\n"+uriString.COORDINATOR+" deleting obj")
                        print("\n"+uriString.COORDINATOR+" Gazebo obj deleted")

                ack_rqt = uriString.DEL_OBJ_ACK_HEADER + "\t" + uriString.TERMINATOR
                extern.publish(ack_rqt);



            # Retrieve Coordinates of Instance
            coordInfo = prop[4].split(",")
            if (len(coordInfo)==4):
                coordInfoTag = prop[4].split(",")[0]
                coordxInstance = prop[4].split(",")[1]
                coordyInstance = prop[4].split(",")[2]
                coordzInstance = prop[4].split(",")[3]
                coordsInstance = coordxInstance+","+coordyInstance+","+coordzInstance

                # Retrieve Preferred Lexical Reference
                lexInfoTag = prop[5].split(",")[0]
                lexInstance = prop[5].split(",")[1]


                # Gazebo doesn't contain the instance. Spawn it
                if (classInstance == uriString.chair):  
                    spawn(uriInstance,coordsInstance,chair_xml)

                if (classInstance == uriString.beer): 
                    spawn(uriInstance,coordsInstance,beer_xml) 

                if (classInstance == uriString.coke): 
                    spawn(uriInstance,coordsInstance,coke_xml) 

                if (classInstance == uriString.table): 
                    spawn(uriInstance,coordsInstance,table_xml) 

                if uriInstance not in instances:
                    print(""+uriString.COORDINATOR+"There is a new instance: \n" + uriInstance)
                    instances.append(uriInstance);

                if (settings.semananticMapVerbosityMax):
                    print(classInfoTag + " -> " + classInstance)
                    print(uriInfoTag + "  -> " + uriInstance)
                    print(coordInfoTag + " :  " +coordsInstance)
                    print(lexInfoTag + "  -> " + lexInstance)

                if (prop[0]==uriString.LIST_ACK_HEADER):

                    ack_rqt = uriString.LIST_OBJ_ACK_HEADER+uriString.SEPARATOR + uriInstance +uriString.SEPARATOR + classInstance +uriString.SEPARATOR + coordsInstance +uriString.SEPARATOR + lexInstance +uriString.SEPARATOR + uriString.TERMINATOR
                    extern.publish(ack_rqt);

                # Add class, position and lexical ref info to Dict    
                instancesDict[uriInstance] = {}
                instancesDict[uriInstance][uriString.TAG_LEX] = lexInstance
                instancesDict[uriInstance][uriString.TAG_COORD_FULL] = coordsInstance
                instancesDict[uriInstance][uriString.TAG_COORD] = {}
                instancesDict[uriInstance][uriString.TAG_COORD][uriString.TAG_COORD_X] = coordxInstance
                instancesDict[uriInstance][uriString.TAG_COORD][uriString.TAG_COORD_Y] = coordyInstance
                instancesDict[uriInstance][uriString.TAG_COORD][uriString.TAG_COORD_Z] = coordzInstance
                instancesDict[uriInstance][uriString.TAG_ATOM] = str(uriInstance.split("#")[1])
                instancesDict[uriInstance][uriString.TAG_TYPE_FULL] = str(classInstance)
                instancesDict[uriInstance][uriString.TAG_TYPE] = str(classInstance.split("#")[1])


                if (settings.semananticMapVerbosityMax):
                    print("\n"+instancesDict+"\n")

                # Set flag after first instance's info received
                if (prop[0]=="i"):
                    sync = True;  
            workingOnRequest = False;  

        elif (prop[0] == uriString.DEL_ACK_HEADER):

            ack_rqt = uriString.DEL_OBJ_ACK_HEADER+uriString.SEPARATOR + "" +uriString.SEPARATOR +"" +uriString.SEPARATOR + "" +uriString.SEPARATOR + "" +uriString.SEPARATOR + uriString.TERMINATOR
            extern.publish(ack_rqt);
        



'''
    Callback function for /extern_commands topic
    
    Commands supported: 
        -> getEntities  :   "o\tg\tz"
        -> AddEntities  :   "o\ta\ttype\tcoordz\tcoordy\tcoordz\tlabel\tz"
        -> updateEntities : "o\tu\ttype\turi\tcoordz\tcoordy\tcoordz\tlabel\tz"
        -> Export Ontology : "o\te\tfilename\tpath\tz"
        -> Delete : "o\td\turi\tz"
'''
def callbackCommands(data):
    mess = data.data.split("\t")

    print(data.data+"\n\n")

    if (len(mess)>1):
        # ADD
        if (mess[0]==uriString.ADD_BY_REF_HEADER):

            lexReferenceInstance = mess[4];
            posNewInstance = mess[3];
            classInstance = mess[1];
            uriInstance = mess[2];
            
            add_rqt = uriString.ADD_BY_REF_HEADER+uriString.SEPARATOR+classInstance+uriString.SEPARATOR+uriInstance+uriString.SEPARATOR+posNewInstance+uriString.SEPARATOR+lexReferenceInstance+ uriString.SEPARATOR+uriString.TERMINATOR;

            if (settings.topicVerbosityMax):
                print(""+uriString.COORDINATOR+"ADD requested received from external node. Forwarding to Semantic Map");
            #rospy.loginfo(update_rqt);
            bridge.publish(add_rqt);

        # LIST
        if (mess[0]==uriString.LIST_HEADER):

            list_rqt = uriString.LIST_HEADER+uriString.SEPARATOR+"getInstances"+ uriString.SEPARATOR+uriString.TERMINATOR
            if (settings.topicVerbosityMax):
                print(""+uriString.COORDINATOR+"LIST requested received from external node. Forwarding to Semantic Map");
            #rospy.loginfo(list_rqt);
            bridge.publish(list_rqt);   
        
        # DEL
        if (mess[0]==uriString.DEL_HEADER):
			x = mess[1];

			if (settings.topicVerbosityMax):
				print("\n"+uriString.COORDINATOR+"Handling delete request [ "+ x + " ]\n")

            # Send delete instance request
			if x != uriString.ALL:
				model_req = gms(str(x.split("#")[1]),"world");
				print("\n"+str(model_req.success))
				print("\n"+str(model_req.pose))
				if (model_req.success):
					if (settings.topicVerbosityMax):
						print("\n"+uriString.COORDINATOR+"\t Sending delete request to SemanticMapInterface\n")
					ack_rqt = uriString.DEL_HEADER + uriString.SEPARATOR+ x+ uriString.SEPARATOR+uriString.TERMINATOR
					bridge.publish(ack_rqt);
				else:            	
					if (settings.topicVerbosityMax):
						print("\n"+uriString.COORDINATOR+"Instance not present in scene\n")
					ack_rqt = uriString.DEL_OBJ_ACK_HEADER + uriString.SEPARATOR + uriString.ERROR + uriString.SEPARATOR + uriString.TERMINATOR
					extern.publish(ack_rqt);
			else:
				if (settings.topicVerbosityMax):
					print("\n"+uriString.COORDINATOR+"\t Sending delete request to SemanticMapInterface\n")
				ack_rqt = uriString.DEL_HEADER + uriString.SEPARATOR+ x+ uriString.SEPARATOR+uriString.TERMINATOR
				bridge.publish(ack_rqt);


        # EXPORT
        if (mess[0]==uriString.EXPORT_HEADER):
            filename = mess[1] 
            list_rqt = uriString.EXPORT_HEADER+uriString.SEPARATOR+filename+ uriString.SEPARATOR+uriString.TERMINATOR
            if (settings.topicVerbosityMax):
                print(""+uriString.COORDINATOR+"EXPORT requested received from external node. Forwarding to Semantic Map");
            #rospy.loginfo(list_rqt);
            bridge.publish(list_rqt);  


        # LOAD ONTOLOGY @ runtime
        if (mess[0]==uriString.LOAD_HEADER):
            filename = mess[1] 
            list_rqt = uriString.LOAD_HEADER+uriString.SEPARATOR+filename+ uriString.SEPARATOR+uriString.TERMINATOR
            if (settings.topicVerbosityMax):
                print(""+uriString.COORDINATOR+"LOAD ONTOLOGY requested. Forwarding to Semantic Map");
            #rospy.loginfo(list_rqt);
            bridge.publish(list_rqt);   

                
if __name__ == '__main__':
    try:
        talker()
    except rospy.ROSInterruptException:
        pass
