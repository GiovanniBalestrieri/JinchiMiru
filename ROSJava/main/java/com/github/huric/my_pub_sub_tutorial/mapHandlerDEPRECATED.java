/*
 * Copyright (C) 2014 userk.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.rosjava.huric.my_pub_sub_tutorial;



import java.util.Iterator;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.PrintUtil;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.ontology.Individual;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.reasoner.ValidityReport.Report;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.commons.logging.Log;
import org.ros.message.MessageListener;


import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

// Adding instances hashmap
import java.util.ArrayList; 
import java.util.HashMap; 
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
/**
 * A simple {@link Publisher} {@link NodeMain}.
 */
public class mapHandlerDEPRECATED extends AbstractNodeMain {
	OntModel abox, tbox, infModel;
	OntClass class1, class2,class3;
	boolean isFurniture;
	boolean isClothes;
	boolean isBook;
	boolean isDrink;
	int sequenceNumber = 0;
	boolean debugInstancesHashMaps = true; 
	String TERMINATOR = "T";
	String SEPARATOR = "\t";
	String HEADER = "a";
	static int NUM_PROPS = 4; //Class, URI, Pose, LexicalReference

	final String JENA_PATH = "/home/userk/catkin_ws/rosjava/src/huric/my_pub_sub_tutorial/src/main/java/com/github/huric/my_pub_sub_tutorial/";
	final String SOURCE = "http://www.semanticweb.org/ontologies/2016/1/";
	final String test_file = "ontology.owl";
	final String fileName = "a_box_mod.owl";
	final String OWL = ".owl";
	final String TBOX_FILE = "semantic_mapping_domain_model";
	final String ABOX_FILE = "semantic_map1";
	final String absoluteFileName = JENA_PATH + fileName;
	final String NS = SOURCE + TBOX_FILE + "#";

	
	final String ALTERNATIVE_REF = "hasAlternativeReference"; 
	final String PREF_REF = "hasPreferredReference";
	final String AFFORDANCE = "hasAffordance"; 
	final String POSITION = "hasPosition"; 
	final String COORD_X = "float_coordinates_x";
	final String COORD_Y = "float_coordinates_y";
	final String COORD_Z = "float_coordinates_z";
	final String LEXICAL = "lexicalReference";
	final String FURNITURE = "Furniture";
	final String CLOTHES = "Clothes";
	final String DRINK = "Drink";
	final String BOOK = "Book";


	OntProperty affordance;
	OntProperty position;
	OntProperty alternativeReference;
	OntProperty prefReference;

	HashMap<String, HashMap<String,String[]>> instancesHashMaps = new HashMap<>();
	HashMap<String, String[]> furnitures = new HashMap<>();
	HashMap<String, String[]> drinks = new HashMap<>();
	HashMap<String, String[]> clothes = new HashMap<>();
	HashMap<String, String[]> books = new HashMap<>();

	@Override
	public GraphName getDefaultNodeName() {
		return GraphName.of("rosjava/talker");
	}

	public void printStatements(Model m, Resource s, Property p, Resource o) {
		for (StmtIterator i = m.listStatements(s,p,o); i.hasNext(); ) {
			Statement stmt = i.nextStatement();
			System.out.println(" - " + PrintUtil.print(stmt));
		}
	}

	public void printAllProperties(Individual thisInstance){
		for (StmtIterator j = thisInstance.listProperties(); j.hasNext(); ) {
			Statement s = j.next();
			System.out.print( "\t" + s.getPredicate().getLocalName() + " -> " );
			if (s.getObject().isLiteral()) {
				System.out.println( s.getLiteral().getLexicalForm() );
			} else {
				System.out.println( s.getObject() );
			}
		}
	}

	public void resetCategories(){
		isFurniture = false;
		isBook = false;
		isClothes = false;
		isDrink = false;

	}

	public void resetHashmaps(){

		// Clean hashmaps
		Set set = instancesHashMaps.entrySet();
		Iterator iterator = set.iterator();
		while(iterator.hasNext()) {
			Map.Entry mentry = (Map.Entry)iterator.next();
			if (debugInstancesHashMaps) {
				System.out.print("key is: "+ mentry.getKey() + " & Value is: ");
				System.out.println(mentry.getValue());
				System.out.print("Removing");
				
			}
		}
	}


	@Override
	public void onStart(final ConnectedNode connectedNode) {

		System.out.println("\n\n---- [ Welcome to SemanticMapInterface ] ----\n\n");
		resetHashmaps();


		final Publisher<std_msgs.String> publisher =
		connectedNode.newPublisher("huric_jena", std_msgs.String._TYPE);
	    // This CancellableLoop will be canceled automatically when the node shuts
	    // down.		
		connectedNode.executeCancellableLoop(new CancellableLoop() {
			private int sequenceNumber;

			@Override
			protected void setup() {
			}

			@Override
			protected void loop() throws InterruptedException {
		  		
		  		std_msgs.String str = publisher.newMessage();
		  		str.setData("TEST " + sequenceNumber);
		  		publisher.publish(str);
		  		sequenceNumber++;
		  		
		  		Thread.sleep(5000);
		  	}
		  });
		
		final Log log = connectedNode.getLog();


		boolean verbose = false;
		boolean debug = false;
		boolean debugInner = false;


	    /**
	      * Importing Tbox
	      */
	    
	    tbox = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
	    OntDocumentManager dm_tbox = tbox.getDocumentManager();
	    dm_tbox.addAltEntry(SOURCE+TBOX_FILE,"file:" + JENA_PATH + TBOX_FILE + OWL );
	    tbox.read(SOURCE+TBOX_FILE,"RDF/XML");

	    /**
	      * Importing Abox
	      */

	    abox = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM);
	    OntDocumentManager dma = abox.getDocumentManager();
	    dma.addAltEntry( SOURCE + ABOX_FILE , "file:" + JENA_PATH + ABOX_FILE + OWL );
	    abox.read(SOURCE + ABOX_FILE,"RDF/XML");

	    /** 
	     * Instead of creating an inference ontology model
	     * based on only the tBox, we will create a specialized 
	     * reasoner on tbox and apply inferences on abox.
	     * Thus, instead of:
	     * OntModel inf = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF, base);
	     * 
	     * Use: 
	     *
	     * Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
	     * reasoner = reasoner.bindSchema(tbox);
	     * OntModelSpec ontModelSpec=OntModelSpec.OWL_MEM_MICRO_RULE_INF;
	     * ontModelSpec.setReasoner(reasoner);
	     * InfModel infmodel = ModelFactory.createInfModel(reasoner,abox);
	     * 
	     * This should be equivalent to creating ontModel with Reasoner capabilities
	     * form aBox wth include inside
		 */

	    infModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF, abox);

	    System.out.println("\n\n-- All you know about an instance --\n\n");

	    affordance = infModel.getOntProperty(NS+AFFORDANCE);
	    position = infModel.getOntProperty(NS+POSITION);
	    alternativeReference = infModel.getOntProperty(NS+ALTERNATIVE_REF);
	    prefReference = infModel.getOntProperty(NS+PREF_REF);

		// Get chair class
	    //OntClass chair = tbox.getOntClass(NS + "Chair");

	    System.out.println("\n\n---- List all classes of ABox ----\n\n");
	    ExtendedIterator classes = infModel.listClasses(); 

	    while (classes.hasNext()) {
			// Reset categories flag
	    	resetCategories();

	    	OntClass thisClass = (OntClass) classes.next();
	    	ExtendedIterator superclasses = thisClass.listSuperClasses();

	    	if (debug)
	    		System.out.println("\nAnalyzing: " + thisClass.getURI() + " ...\n\n");

	        //  Detects whether the class is of a type
	    	while (superclasses.hasNext()) {
	    		OntClass c = (OntClass) superclasses.next();
	    		if (!c.isAnon()){
	    			if (c.getURI().equals(NS+FURNITURE)){
	    	    		// Toggle furniture flag
	    				if (debug)
	    					System.out.println("\t[ FURNITURE ]");
	    				isFurniture = true;
	    			} else if (c.getURI().equals(NS+CLOTHES)){
	    	    		// Toggle furniture flag
	    				if (debug)
	    					System.out.println("\t[ CLOTHES ]");
	    				isClothes = true;
	    			} else if (c.getURI().equals(NS+BOOK)){
	    	    		// Toggle furniture flag
	    				if (debug)
	    					System.out.println("\t[ BOOK ]");
	    				isBook = true;
	    			} else if (c.getURI().equals(NS+DRINK)){
	    	    		// Toggle furniture flag
	    				if (debug)
	    					System.out.println("\t[ DRINK ]");
	    				isDrink = true;
	    			}
	    		}
	    	}

	    	ExtendedIterator instances = thisClass.listInstances();
	    	while (instances.hasNext()) {
	    		Individual thisInstance = (Individual) instances.next();

	    		if (isFurniture){
	    			String uriInstance = thisInstance.getURI();
	    			String pose ="";
	    			String refs ="";
	    			String[] props = new String[NUM_PROPS];

	    			System.out.format(" Object: %s\n", uriInstance); 
	    			System.out.format(" Class: %s", thisClass); 
	    			
	    			if (verbose){
	    				System.out.println("\n\n\n -------------------- \n\n\n");
	    				System.out.println("\n\tFound Furniture: " + thisInstance.toString());
	    			}
	    			float cx=-1000,cy=-1000,cz=-1000;

    				// Pick just position property	
	    			if (thisInstance.hasProperty(position)){
	    				Statement s = thisInstance.getProperty(position);

	    				if (debugInner){
	    					System.out.print( "\t\t\t\tgetPredicate:\t" + s.getPredicate().getLocalName() + " -> " );
	    					System.out.println( s.getObject().toString() + "\n");
	    				}

						// Looking for a position
	    				Individual pos = infModel.getIndividual(s.getObject().toString());  
	    				for (StmtIterator j = pos.listProperties(); j.hasNext(); ) {
	    					Statement coord = j.next();
	    					if (coord.getObject().isLiteral()) {
	    						if (coord.getPredicate().getLocalName().equals(COORD_X)){
	    							cx = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						} else if (coord.getPredicate().getLocalName().equals(COORD_Y)) {
	    							cy = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						} else if (coord.getPredicate().getLocalName().equals(COORD_Z)) {
	    							cz = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						}         
	    					} 
	    				}
	    				System.out.format(" Pose:  ( %f, %f , %f )\n",cx,cy,cz); 
	    				pose = cx+","+cy+","+cz;
	    			}

		    		// Pick the preferred reference
	    			if (thisInstance.hasProperty(prefReference)){
	    				Statement s = thisInstance.getProperty(prefReference);
	    				String lexiRef = "";

	    				if (debugInner){
	    					System.out.print( "\t\t\t\tgetPredicate:\t" + s.getPredicate().getLocalName() + " -> " );
	    					System.out.println( s.getObject().toString() + "\n");
	    				}

						// Looking for a position
	    				Individual alt_refs = infModel.getIndividual(s.getObject().toString());  
	    				for (StmtIterator j = alt_refs.listProperties(); j.hasNext(); ) {
	    					Statement ref = j.next();
	    					if (ref.getObject().isLiteral()) {
	    						if (debug)
	    							System.out.print("\t\t\t"+ref.getPredicate().getLocalName()+"\n\n");
	    						if (ref.getPredicate().getLocalName().equals(LEXICAL)){
	    							lexiRef = (ref.getLiteral().getLexicalForm());
	    						}        
	    					} 
	    				}
	    				System.out.format(" Reference: %s\n\n",lexiRef); 
	    				refs = lexiRef;
	    			}
	    			props[0] = thisClass.toString();
	    			props[1] = uriInstance;
	    			props[2] = pose;
	    			props[3] = refs;

	    			furnitures.put(uriInstance,props);

	    		} else if (isBook) {
	    			String uriInstance = thisInstance.getURI();
	    			String pose ="";;
	    			String refs ="";
	    			String[] props = new String[NUM_PROPS];

	    			System.out.format(" Object: %s\n",uriInstance); 

	    			if (verbose){
	    				System.out.println("\n\n\n -------------------- \n\n\n");
	    				System.out.println("\n\tFound Book: " + thisInstance.toString());
	    			}
	    			float cx=-1000,cy=-1000,cz=-1000;

    				// Pick just position property	
	    			if (thisInstance.hasProperty(position)){
	    				Statement s = thisInstance.getProperty(position);

	    				if (debugInner){
	    					System.out.print( "\t\t\t\tgetPredicate:\t" + s.getPredicate().getLocalName() + " -> " );
	    					System.out.println( s.getObject().toString() + "\n");
	    				}

						// Looking for a position
	    				Individual pos = infModel.getIndividual(s.getObject().toString());  
	    				for (StmtIterator j = pos.listProperties(); j.hasNext(); ) {
	    					Statement coord = j.next();
	    					if (coord.getObject().isLiteral()) {
	    						if (coord.getPredicate().getLocalName().equals(COORD_X)){
	    							cx = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						} else if (coord.getPredicate().getLocalName().equals(COORD_Y)) {
	    							cy = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						} else if (coord.getPredicate().getLocalName().equals(COORD_Z)) {
	    							cz = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						}         
	    					} 
	    				}
	    				System.out.format(" Pose:  ( %f, %f , %f )\n",cx,cy,cz); 
	    				pose = cx+","+cy+","+cz;
	    			}

		    		// Pick the preferred reference
	    			if (thisInstance.hasProperty(prefReference)){
	    				Statement s = thisInstance.getProperty(prefReference);
	    				String lexiRef = "";

	    				if (debugInner){
	    					System.out.print( "\t\t\t\tgetPredicate:\t" + s.getPredicate().getLocalName() + " -> " );
	    					System.out.println( s.getObject().toString() + "\n");
	    				}

						// Looking for a position
	    				Individual alt_refs = infModel.getIndividual(s.getObject().toString());  
	    				for (StmtIterator j = alt_refs.listProperties(); j.hasNext(); ) {
	    					Statement ref = j.next();
	    					if (ref.getObject().isLiteral()) {
	    						if (debug)
	    							System.out.print("\t\t\t"+ref.getPredicate().getLocalName()+"\n\n");
	    						if (ref.getPredicate().getLocalName().equals(LEXICAL)){
	    							lexiRef = (ref.getLiteral().getLexicalForm());
	    						}        
	    					} 
	    				}
	    				System.out.format(" Reference: %s\n\n",lexiRef); 
	    				refs = lexiRef;
	    			}
	    			props[0] = thisClass.toString();
	    			props[1] = uriInstance;
	    			props[2] = pose;
	    			props[3] = refs;

	    			books.put(uriInstance,props);
	    		} else if (isClothes) {
	    			String uriInstance = thisInstance.getURI();
	    			String pose ="";;
	    			String refs ="";
	    			String[] props = new String[NUM_PROPS];

	    			System.out.format(" Object: %s\n", thisInstance.getURI()); 
		    		// Loop throught other type of instances
	    			if (verbose){
	    				System.out.println("\n\n\n -------------------- \n\n\n");
	    				System.out.println("\n\tFound Clothes: " + thisInstance.toString());
	    			}

	    			float cx=-1000,cy=-1000,cz=-1000;

    				// Pick just position property	
	    			if (thisInstance.hasProperty(position)){
	    				Statement s = thisInstance.getProperty(position);

	    				if (debugInner){
	    					System.out.print( "\t\t\t\tgetPredicate:\t" + s.getPredicate().getLocalName() + " -> " );
	    					System.out.println( s.getObject().toString() + "\n");
	    				}

						// Looking for a position
	    				Individual pos = infModel.getIndividual(s.getObject().toString());  
	    				for (StmtIterator j = pos.listProperties(); j.hasNext(); ) {
	    					Statement coord = j.next();
	    					if (coord.getObject().isLiteral()) {
	    						if (coord.getPredicate().getLocalName().equals(COORD_X)){
	    							cx = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						} else if (coord.getPredicate().getLocalName().equals(COORD_Y)) {
	    							cy = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						} else if (coord.getPredicate().getLocalName().equals(COORD_Z)) {
	    							cz = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						}         
	    					} 
	    				}
	    				System.out.format(" Pose:  ( %f, %f , %f )\n",cx,cy,cz); 
	    				pose = cx+","+cy+","+cz;
	    			}

		    		// Pick the preferred reference
	    			if (thisInstance.hasProperty(prefReference)){
	    				Statement s = thisInstance.getProperty(prefReference);
	    				String lexiRef = "";

	    				if (debugInner){
	    					System.out.print( "\t\t\t\tgetPredicate:\t" + s.getPredicate().getLocalName() + " -> " );
	    					System.out.println( s.getObject().toString() + "\n");
	    				}

						// Looking for a position
	    				Individual alt_refs = infModel.getIndividual(s.getObject().toString());  
	    				for (StmtIterator j = alt_refs.listProperties(); j.hasNext(); ) {
	    					Statement ref = j.next();
	    					if (ref.getObject().isLiteral()) {
	    						if (debug)
	    							System.out.print("\t\t\t"+ref.getPredicate().getLocalName()+"\n\n");
	    						if (ref.getPredicate().getLocalName().equals(LEXICAL)){
	    							lexiRef = (ref.getLiteral().getLexicalForm());
	    						}        
	    					} 
	    				}
	    				System.out.format(" Reference: %s\n\n",lexiRef); 
	    				refs = lexiRef;
	    			}
	    			props[0] = thisClass.toString();
	    			props[1] = uriInstance;
	    			props[2] = pose;
	    			props[3] = refs;

	    			clothes.put(uriInstance,props);

	    		} else if (isDrink) {
	    			String uriInstance = thisInstance.getURI();
	    			String pose ="";
	    			String refs ="";
	    			String[] props = new String[NUM_PROPS];

	    			System.out.format(" Object: %s\n", uriInstance); 
		    		// Loop throught other type of instances
	    			if (verbose){
	    				System.out.println("\n\n\n -------------------- \n\n\n");
	    				System.out.println("\n\tFound Drink: " + thisInstance.toString());
	    			}

	    			float cx=-1000,cy=-1000,cz=-1000;

    				// Pick just position property	
	    			if (thisInstance.hasProperty(position)){
	    				Statement s = thisInstance.getProperty(position);

	    				if (debugInner){
	    					System.out.print( "\t\t\t\tgetPredicate:\t" + s.getPredicate().getLocalName() + " -> " );
	    					System.out.println( s.getObject().toString() + "\n");
	    				}

						// Looking for a position
	    				Individual pos = infModel.getIndividual(s.getObject().toString());  
	    				for (StmtIterator j = pos.listProperties(); j.hasNext(); ) {
	    					Statement coord = j.next();
	    					if (coord.getObject().isLiteral()) {
	    						if (coord.getPredicate().getLocalName().equals(COORD_X)){
	    							cx = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						} else if (coord.getPredicate().getLocalName().equals(COORD_Y)) {
	    							cy = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						} else if (coord.getPredicate().getLocalName().equals(COORD_Z)) {
	    							cz = Float.parseFloat(coord.getLiteral().getLexicalForm());
	    						}         
	    					} 
	    				}
	    				System.out.format(" Pose:  ( %f, %f , %f )\n",cx,cy,cz); 
	    				pose = cx+","+cy+","+cz;
	    			}


		    		// Pick the preferred reference
	    			if (thisInstance.hasProperty(prefReference)){
	    				Statement s = thisInstance.getProperty(prefReference);
	    				String lexiRef = "";

	    				if (debugInner){
	    					System.out.print( "\t\t\t\tgetPredicate:\t" + s.getPredicate().getLocalName() + " -> " );
	    					System.out.println( s.getObject().toString() + "\n");
	    				}

						// Looking for a position
	    				Individual alt_refs = infModel.getIndividual(s.getObject().toString());  
	    				for (StmtIterator j = alt_refs.listProperties(); j.hasNext(); ) {
	    					Statement ref = j.next();
	    					if (ref.getObject().isLiteral()) {
	    						if (debug)
	    							System.out.print("\t\t\t"+ref.getPredicate().getLocalName()+"\n\n");
	    						if (ref.getPredicate().getLocalName().equals(LEXICAL)){
	    							lexiRef = (ref.getLiteral().getLexicalForm());
	    						}        
	    					} 
	    				}
	    				System.out.format(" Reference: %s\n\n",lexiRef); 
	    				refs = lexiRef;
	    			}
	    			props[0] = thisClass.toString();
	    			props[1] = uriInstance;
	    			props[2] = pose;
	    			props[3] = refs;

	    			drinks.put(uriInstance,props);

	    		}
	    	}

	    	
	    	instancesHashMaps.put("book",books);
	    	instancesHashMaps.put("drink",drinks);
	    	instancesHashMaps.put("furniture",furnitures);
	    	instancesHashMaps.put("clothes",clothes);

	    }





	    System.out.println("\n\n---- [ Creating a Subscriber to bridge] ----\n\n");

	    Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber("requests", std_msgs.String._TYPE);
	    subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
	    	@Override

	    	public void onNewMessage(std_msgs.String message) {
	    		log.info("Received from bridge: \"" + message.getData() + "\"");
	    		log.info("Sending intances ... ");


	    		String[] mess;
	    		mess = message.getData().split("\t");
	    		// Initialization
	    		if (mess[0].equals("s")) {
		    		// TODO if message.getData == "all" -> getAllInstances
					//                         ==  "export" 
					// "up\turi\tclass\tpose\tlexical"
					// "add\turi\tclass\tpose\tlexical"


		    		Set set = instancesHashMaps.entrySet();
		    		Iterator iterator = set.iterator();
		    		while(iterator.hasNext()) {

			    		String uri_i = "";
			    		String class_i = "";
			    		String pose_i = "";
			    		String ref_i = "";

		    			Map.Entry mentry = (Map.Entry)iterator.next();
		    			String hashm = (String) mentry.getKey();
		    			System.out.print("\t"+ hashm + "\n");
		    			HashMap<String,String[]> tmp = (HashMap<String,String[]>)  mentry.getValue();

		    			Set set1 = tmp.entrySet();
		    			Iterator iterator1 = set1.iterator();

		    			while(iterator1.hasNext()) {
		    				Map.Entry mentry1 = (Map.Entry)iterator1.next();
		    				uri_i = (String) mentry1.getKey();
		    				System.out.print("URI: "+ uri_i + " \n ");

		    				String[] a = (String[]) mentry1.getValue();
		    				for (int i = 0; i<a.length ; i++) {
		    					if (i==0) 
		    						class_i = a[i];
		    					else if (i==1)
		    						uri_i = a[i];
		    					else if (i==2)
		    						pose_i = a[i];
		    					else if (i==3)
		    						ref_i = a[i];
		    					
		    				}
		    				std_msgs.String str = publisher.newMessage();
							String msgToSend = HEADER+SEPARATOR+class_i +SEPARATOR+uri_i+SEPARATOR+pose_i+SEPARATOR+ref_i+SEPARATOR+TERMINATOR;
							str.setData(msgToSend);
			    			publisher.publish(str);
		    			}
						//String msgToSend = class +","+
		    		}
	    		}
    		

	    		std_msgs.String str = publisher.newMessage();
	    		str.setData("publishing " + sequenceNumber);
	    		publisher.publish(str);
	    		sequenceNumber++;

		        System.out.println("Received from TalkerExpert: \"" + message.getData() + "\"");
	    	}
	    });

	    System.out.println("\n\n---- [ The End ] ----\n\n");
	}

    
    /** 
      * Consistency Check. Returns true if passed
	  */
	private boolean performConsistencyCheckWith(InfModel inf) {
		boolean res = false;
	    System.out.println("\n\n---- Consistency Check ----\n\n");

	    ValidityReport validity = inf.validate();
	    if (validity.isValid()) {
	    	System.out.println("Consistency Check:\n Passed\n");
	    	res = true;
	    } else {
	    	System.out.println("Consistency Check:\n Conflicts\n");
	    	for (Iterator i = validity.getReports(); i.hasNext(); ) {
	    		System.out.println(" - " + i.next());
	    	}
	    }
	    return res;
	}

	/**
	  * Handles AddEntityRequests from orchestrator:
	  *	Creates a new instance, adds properties to it and perform consistency check
	  * If test is passed, leaves the newly created instance, otherwise deletes it
	  */
	private boolean handleAddEntityRequest(OntModel abox, String ontClass, String uriInstance, String pose, String lexicalReference){
		boolean res = false;


		// Create instance
	    class1 = abox.getOntClass(NS+ontClass);
		
		if (class1 == null)
			return false;
		else {
			Individual i1 = abox.createIndividual(NS+uriInstance,class1);
		    prefReference = abox.getOntProperty(NS+PREF_REF);
			i1.addProperty(prefReference,abox.createDatatypeProperty(NS + lexicalReference));
			/*
				// Pick just position property	
    			if (thisInstance.hasProperty(position)){
    				Statement s = thisInstance.getProperty(position);
			*/
		}
		return res;
	}



	/**
	  * Handles updateEntityRequests from orchestrator:
	  *	Looks for an instance with uri 'uriInstance', if the instance does not exist returns flase.
	  * If there is match, looks for the properties Pose and LexicalReference and updates it. 
	  * A consistency if invoked. If the test is passed, true is returned. Otherwise False.
	  */
	private boolean handleUpdateEntityRequest(OntModel abox,String ontCass, String uriInstance, String pose, String lexicalReference) {
		boolean res = false;

		
		
		return res;
	}

	/**
	  * Handles removeEntityRequests from orchestrator:
	  *	Looks for an instance with uri 'uriInstance', if the instance does not exist returns flase.
	  * If there is match, it deletes it and returns true. 
	  * 
	  * Attention : What happens if an instance of a master concept relating two entities is deleted?
	  * 			No one should be able to alter its knowledge directly.
	  *	
	  * 			Maybe, add a negative weight to the instance or a tag
	  */
	private boolean handleRemoveEntityRequest(OntModel abox, String uriInstance) {
		boolean res = false;

		
		
		return res;
	}

	/**
	  * Handles getAllEntityRequests from orchestrator:
	  *	Loops through all classes. Finds all instances and writes them on a topic
	  *
	  */
	private boolean handleGetAllEntityRequest(OntModel abox,String ontCass) {
		boolean res = false;

		
		
		return res;
	}
	

    /** 
      * Exports Ontology to a file with provided name and path.
	  */
	private boolean exportOntology(InfModel inf, String absoluteFileName, String fileName){
		    /* Consistency Check */
		boolean res = false;
	    System.out.println("\n\n---- Export Requested : preliminary consistency Check ----\n\n");

	    FileWriter out = null;

	    ValidityReport validity = inf.validate();
	    if (validity.isValid()) {
	    	System.out.println("Consistency Check:\n Passed\n");
	    	System.out.println("Writing to file:\n\t"+fileName);
	    	try{
	    		out = new FileWriter(absoluteFileName);
	    		inf.write(out,"RDF/XML");
	    		res = true;
	    	}
	    	catch (IOException a){
	    		System.out.println(" Occhio");
	    	}
	    	finally{
	    		if (out!=null){
	    			try {out.close();} catch(IOException ex) {}
	    		}
	    	}
	    } else {
	    	System.out.println("Consistency Check:\n Conflicts\n");
	    	for (Iterator i = validity.getReports(); i.hasNext(); ) {
	    		System.out.println(" - " + i.next());
	    	}
	    }
	    return res;
	}
}
