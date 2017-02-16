#!/usr/bin/env python
import sys, rospy, tf, json
from gazebo_msgs.srv import *
from geometry_msgs.msg import *
from copy import deepcopy
import os.path
import uriString
import settings
import time

from std_msgs.msg import String
ackList = False;
ackDel = False;
ackExport = False;
ackAdd = False;
ackLoad = False;
version = 0.3;

count = 0;

commands = rospy.Publisher('extern_commands', String, queue_size=10)

def showHelp():
	print("usage: my_node.py")
	print("\n list all \n export filenameWithoutExt")
	print("\n add Chair,uri,poseChair,lexChair \n add Chair,default")
	print("\n del instanceUri\n del all")
	print("\n load filenameWithoutExt")

def query_coordinator(mode,option):
	global count
	global ackList
	global ackDel
	global ackExport
	global ackAdd
	global ackLoad

	rospy.Subscriber("extern_commands", String, callback)
	time.sleep(1)

	rate = rospy.Rate(0.2) 
	while not rospy.is_shutdown():
		if mode == uriString.HELP:
			showHelp()
			rospy.signal_shutdown("")	
		if mode == uriString.LIST_REF_HEADER:
			if not ackList:
				print("Sendind list request");
				hello_str = uriString.LIST_HEADER+uriString.SEPARATOR+"T" 
				rospy.loginfo(hello_str);
				commands.publish(hello_str);   
				#count+=1
				#if count == 2:
				#	ackList = True
		elif mode == uriString.DEL_HEADER:
			if not ackDel:
				if option == uriString.ALL:					
					print("Sendind Delete ALL request");
				else:
					print("Sendind Delete Instance request");
				hello_str = uriString.DEL_HEADER+uriString.SEPARATOR+option+uriString.SEPARATOR+"T" 
				rospy.loginfo(hello_str);
				commands.publish(hello_str);   
		elif mode == uriString.EXPORT_EXT_HEADER:
			if not ackExport:
				print("Sendind Export request");
				hello_str = uriString.EXPORT_HEADER+uriString.SEPARATOR+option+uriString.SEPARATOR+"T" 
				rospy.loginfo(hello_str);
				commands.publish(hello_str);  
		elif mode == uriString.LOAD_HEADER:
			if not ackLoad:
				print("Sendind Load request");
				hello_str = uriString.LOAD_HEADER+uriString.SEPARATOR+option+uriString.SEPARATOR+"T" 
				rospy.loginfo(hello_str);
				commands.publish(hello_str);   
		elif mode == uriString.ADD_RQ_HEADER:		
			if not ackAdd:
				
				info = option.split(",")
				
				if info[0] == uriString.class_to_add:
					if (len(info)<3):
						if info[1] == uriString.defaultConfig:
							print("\nrequested default config")
							add_str = "add\t"+uriString.class_to_add+"\t"+uriString.uri_to_add+"\t"+uriString.pose_to_add + "\t"+ uriString.lex_ref_to_add +"\tT" 

							print("Sent add request");
							#rospy.loginfo(add_str);
							print(add_str)
							commands.publish(add_str);
						else:
							print("\nWrong request format. Try: add Chair,uri,posx,posy,pos1-z,lex OR Chair,default")
					else:
						uriX = info[1]
						poseX = info[2]
						poseY = info[3]
						poseZ = info[4]
						lexX = info[5]
						poseToAdd = poseX+","+poseY+","+poseZ
						add_str = "add\t"+uriString.class_to_add+"\t"+uriX+"\t"+poseToAdd + "\t"+ lexX +"\tT" 

						print("Sent add request");
						#rospy.loginfo(add_str);
						print(add_str)
						commands.publish(add_str); 
				else:
					print("\nClass not implemented. Try \"add Chair,default\"")


				#commands.publish(add_str);   a
		rate.sleep()



def callback(data):
    global ackList
    global ackDel
    global ackExport
    global ackLoad
    global ackAdd

    prop = data.data.split("\t")
    if (len(prop)>1):

        # Received response from SemanticMap Node 
        # Initial sync with knowledge base
        if (prop[0] == uriString.LIST_ACK_HEADER):
        	ackList = True;
        	print("\nAcknowledgement received")   
        if (prop[0] == uriString.LIST_OBJ_ACK_HEADER):        	
        	print("uri:\t" + prop[1]);
        	print("\nclass:\t" + prop[2]);
        	print("\npose:\t" + prop[3]);
        	print("\nlexical reference:\t" + prop[4]);
        	print("\n\n");
        if (prop[0] == uriString.DEL_OBJ_ACK_HEADER):
        	ackDel = True
        	if prop[1] == uriString.TERMINATOR:
        		print("\nInstance Deleted successfully")
        	elif prop[1] == uriString.ERROR:        		
        		print("\nIntance Not Found")
        if (prop[0] == uriString.ADD_ACK_HEADER):
    		ackAdd = True	;
    		print("\nObject added successfully")
        if (prop[0] == uriString.LOAD_ACK_HEADER):
    		ackLoad = True	;
    		print("\nAbox loaded successfully")
        if (prop[0] == uriString.EXPORT_ACK_HEADER):
        	ackExport = True
        	errorExport = prop[1] 
        	if errorExport == "1":
        		print("\nExport PROBLEM")
        	else:
        		print("\nAck received. Ontology successfully exported. Have a good one!")

def my_node(myArg1,myArg2):
	rospy.init_node("extern_interface")
	try:
		query_coordinator(myArg1,myArg2)
	except rospy.ROSInterruptException:
		print("exception")


if __name__=="__main__":
    if len(sys.argv) < 3:
        print("usage: my_node.py del URI \n list all \n export filenameWithoutExt \n add Chair,uri,poseChair,lexChair \n add Chair,default")
    else:
    	print("Welcome\nv " + str(version))
        my_node(sys.argv[1], sys.argv[2])

	
