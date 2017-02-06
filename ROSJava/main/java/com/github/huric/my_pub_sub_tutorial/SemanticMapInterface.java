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



import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Calendar;

import org.apache.jena.util.PrintUtil;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.OWL;
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
public class SemanticMapInterface extends AbstractNodeMain {
	OntModel abox, tbox, infModel;
	OntClass class1, class2,class3;
	boolean isFurniture;
	boolean isClothes;
	boolean isBook;
	boolean isDrink;
	int sequenceNumber = 0;
	boolean debugInstancesHashMaps = false; 
	boolean topicVerbosity = true;
	boolean instanceVerbosity = true;
	static boolean sparqlVerbosity = true;

	int INSTANCE_ALREADY_PRESENT = 2;
	int INSTANCE_ADDED = 1;
	int NO_CLASS_FOUND = 0;

	static String NODE_NAME = " [ SemanticMapInterface ] ";

	String TERMINATOR = "T";
	String SEPARATOR = "\t";
	String HEADER = "a";
	String INIT_HEADER = "i";
	String UPDATE_HEADER = "u";
	String EXPORT_HEADER = "e";
	String EXPORT_ACK_HEADER = "exportack";
	String LIST_HEADER = "g";
	String LIST_ACK_HEADER = "listack";
	String ADD_HEADER = "add";
	String ADD_ACK_HEADER = "addack";
	String DELETE_HEADER = "del";
	String DELETE_ACK_HEADER = "delack";
	
	static int NUM_PROPS = 4; //Class, URI, Pose, LexicalReference


	static String JENA_PATH = "/home/userk/catkin_ws/rosjava/src/huric/my_pub_sub_tutorial/src/main/java/com/github/huric/my_pub_sub_tutorial/";
	final static String SOURCE = "http://www.semanticweb.org/ontologies/2016/1/";
	final static String test_file = "ontology.owl";
	final static String fileName = "a_box_mod.owl";
	final static String OWL_EXTENION = ".owl";
	final static String TBOX_FILE = "semantic_mapping_domain_model";

	//final static String ABOX_FILE = "exported1485545578996";
	final static String ABOX_FILE = "semantic_map1";
	final static String absoluteFileName = JENA_PATH + fileName;
	final static String NS = SOURCE + TBOX_FILE + "#";

	
	final static String CLASS_INSTANCE = "class";
	final static String ERROR = "error";
	final static String URI = "uri";
	final static String ALTERNATIVE_REF = "hasAlternativeReference"; 
	final static String PREF_REF = "hasPreferredReference";
	final static String AFFORDANCE = "hasAffordance"; 
	final static String POSITION = "hasPosition"; 
	final static String COORD_X = "float_coordinates_x";
	final static String COORD_Y = "float_coordinates_y";
	final static String COORD_Z = "float_coordinates_z";
	final static String LEXICAL = "lexicalReference";
	final static String FURNITURE = "Furniture";
	final static String CHAIR = "Chair";
	final static String BEER = "Beer";
	final static String CLOTHES = "Clothes";
	final static String DRINK = "Drink";
	final static String BOOK = "Book";
	final static String PREF_REF_CLASS = "PreferredReference";
	final static String COORDINATES_CLASS = "Coordinates";
	
	static final String DOMAIN_MODEL_NS = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model";
	static final String SEMANTIC_MAP_NS = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_1";

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
	    dm_tbox.addAltEntry(SOURCE+TBOX_FILE,"file:" + JENA_PATH + TBOX_FILE + OWL_EXTENION );
	    tbox.read(SOURCE+TBOX_FILE,"RDF/XML");

	    System.out.println("\n\n" + NODE_NAME + "Importing Tbox ... OK " +"\n\n");

	    /**
	      * Importing Abox
	      */

	    abox = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM);
	    OntDocumentManager dma = abox.getDocumentManager();
	    dma.addAltEntry( SOURCE + ABOX_FILE , "file:" + JENA_PATH + ABOX_FILE + OWL_EXTENION );
	    abox.read(SOURCE + ABOX_FILE,"RDF/XML");
	    System.out.println("\n\n" + NODE_NAME + "Importing Abox ... OK " +"\n\n");



	    /**
	      * Invoke Resoner and Infer statements
	      */

	    infModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF, abox);

	    affordance = infModel.getOntProperty(NS+AFFORDANCE);
	    position = infModel.getOntProperty(NS+POSITION);
	    alternativeReference = infModel.getOntProperty(NS+ALTERNATIVE_REF);
	    prefReference = infModel.getOntProperty(NS+PREF_REF);

	    System.out.println("\n\n"+ NODE_NAME + "\t---- [ Creating a Subscriber to bridge ] ----\n\n");

	    Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber("bridge", std_msgs.String._TYPE);
	    subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
	    	@Override

	    	public void onNewMessage(std_msgs.String message) {

	    		//if (topicVerbosity)
	    		//	log.info(""+ NODE_NAME + "\tReceived from bridge: \"" + message.getData() + "\"");
	    			
	    		System.out.println("\n\n"+ NODE_NAME + "\t---- " + message.getData()+" ----\n\n");
	    		String[] mess = message.getData().split("\t");
	     		System.out.println("\n\n"+ NODE_NAME + "\t---- 2 ----\n\n");
	
	    		String error = "0";
	    		// init response strings
	    		String uri_i = "";
	    		String class_i = "";
	    		String pose_i = "";
	    		String pose_x = "";
	    		String pose_y = "";
	    		String pose_z = "";
	    		String ref_i = "";			
	  			
	  			  		// Initialization
	    		if (mess[0].equals(INIT_HEADER) || mess[0].equals(LIST_HEADER)) {
			
				    List<HashMap<String, HashMap<String, String>>> furnitureInstances;

				    furnitureInstances = getAllFurnitureInstances(abox);
					
					for (int i = 0; i<furnitureInstances.size(); i++) {
						HashMap<String, HashMap<String, String>> sub = furnitureInstances.get(i);
						for (Entry<String, HashMap<String, String>> e : sub.entrySet()) {
						    String key = e.getKey();
						    if (instanceVerbosity)
								System.out.println(""+ NODE_NAME + "\turi: " + key);
							uri_i = key;
						    
							HashMap<String, String> value = e.getValue();

							for (Entry<String, String> f : value.entrySet()) {
							    String keyProp = f.getKey();
							    String valueProp = f.getValue();

								if (f.getKey().equals(PREF_REF)) {
									ref_i = f.getValue();
								} else if (f.getKey().equals(COORD_X)) {
									pose_x = f.getValue();
								} else if (f.getKey().equals(COORD_Y)) {
									pose_y = f.getValue();
								} else if (f.getKey().equals(COORD_Z)) {
									pose_z = f.getValue();
								} else if (f.getKey().equals(CLASS_INSTANCE)) {
									class_i = f.getValue();
								}
								pose_i = pose_x+","+pose_y+","+pose_z;
							}

							// Publish data
							std_msgs.String str = publisher.newMessage();
							String msgToSend = "";
							if ( mess[0].equals(INIT_HEADER) ) {
								// i 	error,0 	class,classChairUrI 		uri,URIchair 	coordinates,x,y,z 	lexical,sediola		z
								msgToSend = INIT_HEADER+SEPARATOR+ERROR+","+error+SEPARATOR+CLASS_INSTANCE+","+class_i+SEPARATOR+URI+","+uri_i+SEPARATOR+COORDINATES_CLASS+","+pose_i+SEPARATOR+PREF_REF+","+ref_i+SEPARATOR+TERMINATOR;
							} else if (mess[0].equals(LIST_HEADER) ) {
								msgToSend = LIST_ACK_HEADER+SEPARATOR+ERROR+","+error+SEPARATOR+CLASS_INSTANCE+","+class_i+SEPARATOR+URI+","+uri_i+SEPARATOR+COORDINATES_CLASS+","+pose_i+SEPARATOR+PREF_REF+","+ref_i+SEPARATOR+TERMINATOR;
							}

							if (topicVerbosity)
								System.out.print("\n"+ NODE_NAME + "\tPreparing to send: " + msgToSend+"\n\n");

							str.setData(msgToSend);
			    			publisher.publish(str);

						}
					}
	    		} else if (mess[0].equals(UPDATE_HEADER)) {
	    			// Update request Received
	    			System.out.println("\n\n"+ NODE_NAME + "\tUpdate Requested for " + mess[1] +"\n");

	    			List<HashMap<String, HashMap<String, String>>> furnitureInstances;

	    			String pose_ind[] = mess[3].split(",");
	    			OntClass class_ith = abox.getOntClass(mess[2]);
	    			if (class_ith == null ) {
	    				error = "1";
						System.out.print(""+ NODE_NAME + "\tInvalid class instance");
	    			} else {
	    				// TODO need to get lexical ref
						if (handleUpdateEntityRequest(abox,class_ith,mess[1],pose_ind[0],pose_ind[1],pose_ind[2],mess[4])) {
							// Positive result

							furnitureInstances = getAllFurnitureInstances(abox);
							
							
							//System.out.print("\n\nListing Furniture:\n");
							for (int i = 0; i<furnitureInstances.size(); i++) {
								HashMap<String, HashMap<String, String>> sub = furnitureInstances.get(i);
								for (Entry<String, HashMap<String, String>> e : sub.entrySet()) {
								    String key = e.getKey();

						    		if (instanceVerbosity)
										System.out.println(""+ NODE_NAME + "\turi: " + key);
									uri_i = key;
								    
									HashMap<String, String> value = e.getValue();

									for (Entry<String, String> f : value.entrySet()) {
									    String keyProp = f.getKey();
									    String valueProp = f.getValue();

										if (f.getKey().equals(PREF_REF)) {
											ref_i = f.getValue();
										} else if (f.getKey().equals(COORD_X)) {
											pose_x = f.getValue();
										} else if (f.getKey().equals(COORD_Y)) {
											pose_y = f.getValue();
										} else if (f.getKey().equals(COORD_Z)) {
											pose_z = f.getValue();
										} else if (f.getKey().equals(CLASS_INSTANCE)) {
											class_i = f.getValue();
										}
										pose_i = pose_x+","+pose_y+","+pose_z;

										// Publish data
										std_msgs.String str = publisher.newMessage();
										
										String msgToSend = UPDATE_HEADER+SEPARATOR+ERROR+","+error+SEPARATOR+CLASS_INSTANCE+","+class_i+SEPARATOR+URI+","+uri_i+SEPARATOR+COORDINATES_CLASS+","+pose_i+SEPARATOR+PREF_REF+","+ref_i+SEPARATOR+TERMINATOR;
										//String msgToSend = UPDATE_HEADER+SEPARATOR+error+SEPARATOR+class_i+SEPARATOR+uri_i+SEPARATOR+pose_i+SEPARATOR+ref_i+SEPARATOR+TERMINATOR;
										
	    								if (topicVerbosity)
											System.out.print("\n" + NODE_NAME + "\tPreparing to send: " + msgToSend+"\n\n");

										str.setData(msgToSend);
						    			publisher.publish(str);
									}

									//System.out.println("Pose: " + pose_i);
								}
							}

						} else {
							// Error during update -> Consistency problem
							error = "1";
							System.out.print(""+ NODE_NAME + "\tConsistency problems during update");
						}
					}

	    		}  else if (mess[0].equals(EXPORT_HEADER)) {

					// Publish data
					std_msgs.String strToSend = publisher.newMessage();
					String ackEportToSend = "";
					String errorExport = "0";
					String baseFilename = mess[1];

				    Calendar c = Calendar.getInstance();		
	    			String newName = baseFilename /* +  Long.toString(c.getTimeInMillis()) */+".owl";
	    			if (exportTo(infModel,abox,JENA_PATH,newName)){
	    				System.out.println("\n" + NODE_NAME + SEPARATOR + EXPORT_ACK_HEADER + "\tExported ontology to file:\n\t\t"+ newName + "\n\nIn Folder:\t"+ JENA_PATH);
						
						ackEportToSend = EXPORT_ACK_HEADER+SEPARATOR+ERROR+","+errorExport+SEPARATOR+CLASS_INSTANCE+","+class_i+SEPARATOR+URI+","+uri_i+SEPARATOR+COORDINATES_CLASS+","+pose_i+SEPARATOR+PREF_REF+","+ref_i+SEPARATOR+TERMINATOR;
						

	    			} else {
						System.out.println("\n\n"+ NODE_NAME + "\tAttention could not export ontology inconsistency problem");
						errorExport = "1";
						ackEportToSend = EXPORT_ACK_HEADER+SEPARATOR+ERROR+","+errorExport+SEPARATOR+CLASS_INSTANCE+","+class_i+SEPARATOR+URI+","+uri_i+SEPARATOR+COORDINATES_CLASS+","+pose_i+SEPARATOR+PREF_REF+","+ref_i+SEPARATOR+TERMINATOR;
						
	    			}

					if (topicVerbosity)
						System.out.print("\n" + NODE_NAME + SEPARATOR + EXPORT_ACK_HEADER + "\tPreparing to send: " + ackEportToSend+"\n\n");
					strToSend.setData(ackEportToSend);
	    			publisher.publish(strToSend);

	    		}  else if (mess[0].equals(ADD_HEADER)) {


	    			System.out.print(""+ NODE_NAME + "\tReceived external add request");
				    Calendar c = Calendar.getInstance();

				    // get Coordinate from request
				    String coordx[] = mess[3].split(",");
				    pose_x = coordx[0];
				    pose_y = coordx[1];
				    pose_z = coordx[2];

				    // get Lexical Reference from request
				    ref_i = mess[4];
				    // get Class string from request and fetch corresponding ontClass
				    class_i = mess[1];
	    			//System.out.print("\n\nWe have:\n\nuri: " + DOMAIN_MODEL_NS+"#"+class_i);
	    			OntClass classetta = abox.getOntClass(DOMAIN_MODEL_NS+"#" + class_i);

	    			if (classetta == null) {
	    				System.out.println("\n"+ NODE_NAME + "\tError");
	    			} else {
	    				System.out.println("\n"+ NODE_NAME + "\tclass: " + classetta.getURI() + "\n");
	    			}
	    			// Retrieve uri from request
	    			uri_i = mess[2];

	    			System.out.print("\n"+ NODE_NAME + "\tWe are about to add:\n\nuri: " + uri_i + "\nclass: " + class_i +"\npos: "+pose_x+","+pose_y+","+pose_z+"\nref: " + ref_i);

	    			
	    			String newName = SEMANTIC_MAP_NS+"#" + uri_i + Long.toString(c.getTimeInMillis());//+".owl";
	    			if (AddEntityRequest(abox,classetta,newName,pose_x,pose_y,pose_z,ref_i)){
	    				System.out.println("\n"+ NODE_NAME + "\tInstance inserted successfully");
						// Send ack
						std_msgs.String str = publisher.newMessage();
						
						String ackToSend = ADD_ACK_HEADER+SEPARATOR+ERROR+","+error+SEPARATOR+CLASS_INSTANCE+","+class_i+SEPARATOR+URI+","+uri_i+SEPARATOR+COORDINATES_CLASS+","+pose_i+SEPARATOR+PREF_REF+","+ref_i+SEPARATOR+TERMINATOR;
						
						if (true/*topicVerbosity*/)
							System.out.print("\n" + NODE_NAME + SEPARATOR + ADD_ACK_HEADER + "Preparing to send: " + ackToSend+"\n\n");

						str.setData(ackToSend);
		    			publisher.publish(str);
				    	//res = true;
				    } else {
				    	System.out.print(""+ NODE_NAME + "\tError could not insert instance consistency problem");
	   				}
					

	    			List<HashMap<String, HashMap<String, String>>> newInstances;
	    			
					newInstances = getAllFurnitureInstances(abox);
							
							

				    if (instanceVerbosity)
						System.out.print("\n\n"+ NODE_NAME + "\tListing Furniture after external update request:\n\n\n");


					for (int i = 0; i<newInstances.size(); i++) {
						HashMap<String, HashMap<String, String>> sub = newInstances.get(i);
						for (Entry<String, HashMap<String, String>> e : sub.entrySet()) {
						    String key = e.getKey();
						    if (instanceVerbosity)
								System.out.println(""+ NODE_NAME + "\turi: " + key);
							uri_i = key;
						    
							HashMap<String, String> value = e.getValue();
							//System.out.println("Property: ");
							for (Entry<String, String> f : value.entrySet()) {
							    String keyProp = f.getKey();
							    String valueProp = f.getValue();

								if (f.getKey().equals(PREF_REF)) {
									ref_i = f.getValue();
								} else if (f.getKey().equals(COORD_X)) {
									pose_x = f.getValue();
								} else if (f.getKey().equals(COORD_Y)) {
									pose_y = f.getValue();
								} else if (f.getKey().equals(COORD_Z)) {
									pose_z = f.getValue();
								} else if (f.getKey().equals(CLASS_INSTANCE)) {
									class_i = f.getValue();
								}
								pose_i = pose_x+","+pose_y+","+pose_z;

							}

							
							// Publish data
							std_msgs.String str = publisher.newMessage();
							
							String ackToSend = UPDATE_HEADER+SEPARATOR+ERROR+","+error+SEPARATOR+CLASS_INSTANCE+","+class_i+SEPARATOR+URI+","+uri_i+SEPARATOR+COORDINATES_CLASS+","+pose_i+SEPARATOR+PREF_REF+","+ref_i+SEPARATOR+TERMINATOR;
							
							if (topicVerbosity)
								System.out.print("\n" + NODE_NAME + SEPARATOR + UPDATE_HEADER +"Preparing to send: " + ackToSend+"\n\n");

							str.setData(ackToSend);
			    			publisher.publish(str);

						}

					}

	    		} else if (mess[0].equals(DELETE_HEADER)) {
	    			uri_i = mess[1];
					handleRemoveEntityRelatedRequest(abox,uri_i);

					error = "0";
					// Publish data
					std_msgs.String stri = publisher.newMessage();
					
					String ackToSend2 = DELETE_ACK_HEADER+SEPARATOR+ERROR+","+error+SEPARATOR+CLASS_INSTANCE+","+class_i+SEPARATOR+URI+","+uri_i+SEPARATOR+COORDINATES_CLASS+","+pose_i+SEPARATOR+PREF_REF+","+ref_i+SEPARATOR+TERMINATOR;
					
					if (topicVerbosity)
						System.out.print("\n" + NODE_NAME + SEPARATOR + DELETE_ACK_HEADER +"Preparing to send: " + ackToSend2+"\n\n");

					stri.setData(ackToSend2);
	    			publisher.publish(stri);

				}
				

	    		std_msgs.String str = publisher.newMessage();
	    		str.setData("publishing " + sequenceNumber);
	    		publisher.publish(str);
	    		sequenceNumber++;

	    		if (topicVerbosity)
		        	System.out.println(""+ NODE_NAME + "\tReceived from Coordinator: \"" + message.getData() + "\"");
	    	}
	    });

	}

	
	/** 
     * Exports Ontology to a file with provided name and path.
	  */
	private static boolean exportOntologyOnt(OntModel inf, String absoluteFileName, String fileName){
		
		/* Consistency Check */
		boolean res = false;
	    System.out.println("\n\n"+ NODE_NAME + "\t---- Export Requested : preliminary consistency Check ----\n\n");

	    FileWriter out = null;

    	try{
    		out = new FileWriter(absoluteFileName+fileName);
    		inf.write(out,"RDF/XML-ABBREV");
    		res = true;
	    	System.out.println(""+ NODE_NAME + "\tConsistency check passed. Writing to file:\t"+fileName);
    	}
    	catch (IOException a){
    		System.out.println(""+ NODE_NAME + "\tOcchio");

            a.printStackTrace();
    	}
    	finally{
    		if (out!=null){
    			try {out.close();} catch(IOException ex) {}
    		}
    	}

    	return res;
	}



	/**
	 * Performs SPARQL Queries on ontology
	 * 
	 * @ RETURNS: HashMap Class ,  Hashmap< Position_coord, coord.toString()>
	 * 										LexicalRef, reference
	 * 										URI, uri
	 */
	private static List<HashMap<String, HashMap<String, String>>> getAllFurnitureInstances(OntModel abox){
		
		List<HashMap<String, HashMap<String, String>>> res = new ArrayList<HashMap<String, HashMap<String, String>>>();
		//String uriClass = classOnt.getURI();
		
		if (sparqlVerbosity)
			System.out.println("\n\nSPARQL Super Queries");
		
	     
	    String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
				"prefix rdfs: <"+RDFS.getURI()+">\n" +
	    		"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
	    		"PREFIX hasPosition: <" + NS + POSITION +"> " +
	    		"PREFIX hasRef: <" + NS + PREF_REF +"> " +
				"prefix semantic_mapping_domain_model: <" + DOMAIN_MODEL_NS + "#> \n"+
				"prefix semantic_mapping_1: <" + SEMANTIC_MAP_NS + "#> \n"+
	    		
	    		"PREFIX coordx: <" + NS + COORD_X +"> " +
	    		"PREFIX coordy: <" + NS + COORD_Y +"> " +
	    		"PREFIX coordz: <" + NS + COORD_Z +"> " +
	    		"PREFIX prefRef: <" + NS + LEXICAL +"> " +
	    		
	    		
	    		"SELECT DISTINCT ?uri ?class ?x ?y ?z ?lex "+
	    		"WHERE {" + 
	    			"{"+
		    		"?uri a ?class ." + 
		    		"?class rdfs:subClassOf semantic_mapping_domain_model:Furniture ."+
		    		"?uri hasPosition: ?pos ." + 
		    		"?uri hasRef: ?ref ." + 
		    		"?ref prefRef: ?lex ." + 
		    		"?pos coordx: ?x . " + 
		    		"?pos coordy: ?y . " + 
		    		"?pos coordz: ?z " +  "} UNION {"+
		    		"?uri a ?class ." + 
		    		"?class rdfs:subClassOf semantic_mapping_domain_model:Drink ." +
		    		"?uri hasPosition: ?pos ." + 
		    		"?uri hasRef: ?ref ." + 
		    		"?ref prefRef: ?lex ." + 
		    		"?pos coordx: ?x . " + 
		    		"?pos coordy: ?y . " + 
		    		"?pos coordz: ?z " +  "}"+
	    		"}" ;
	    
	    Query query = QueryFactory.create(queryString);
	    QueryExecution qexec = QueryExecutionFactory.create(query, abox) ;
	    try {
	      ResultSet results = qexec.execSelect();
	      while (results.hasNext())
	      {
	        QuerySolution soln = results.nextSolution() ;

	        Resource ur = (Resource) soln.get("uri");
	        Literal lex = soln.getLiteral("lex");
	        Resource classInstance = (Resource) soln.get("class");	        
	        Literal cx = soln.getLiteral("x") ; 
	        Literal cy = soln.getLiteral("y") ; 
	        Literal cz = soln.getLiteral("z") ; 
	        
	        HashMap<String,HashMap<String,String>> aa = new HashMap<String,HashMap<String,String>>();
	        HashMap<String,String> a = new HashMap<String, String>();
	        a.put(COORD_X, Float.toString(cx.getFloat()));
	        a.put(COORD_Y, Float.toString(cy.getFloat()));
	        a.put(COORD_Z, Float.toString(cz.getFloat()));
	        a.put(PREF_REF, lex.toString());
	        
	        /*System.out.print("Class: " + classInstance.toString()+ "\n"+
	        		"PREf: " + lex.toString()+ "\n"+
	        		"uri: " + ur.toString() + "\n"
	        		);
	        		*/
	        a.put(CLASS_INSTANCE, classInstance.toString() );
	        aa.put(ur.toString(),a);
	        res.add(aa);
	      }
	    } finally { 
	    	qexec.close(); 
	    }
		return res;
	}

	/**
	 * Performs SPARQL Queries on ontology
	 * 
	 * @ RETURNS: HashMap Class ,  Hashmap< Position_coord, coord.toString()>
	 * 										LexicalRef, reference
	 * 										URI, uri
	 */ 
	/*
	private static int addInstanceByRefAndPos(OntModel abox, String uriInstance, String posx, String posy, String posz, String lexicalRef,){
		
		int res = 0;

		//List<HashMap<String, HashMap<String, String>>> res = new ArrayList<HashMap<String, HashMap<String, String>>>();
		//String uriClass = classOnt.getURI();
		
		System.out.println("\n\nSPARQL looking for instance with a given lexical reference and retrieving parent class");
		
	     
	    String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
				"prefix rdfs: <"+RDFS.getURI()+">\n" +
	    		"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
	    		"PREFIX hasPosition: <" + NS + POSITION +"> " +
	    		"PREFIX hasRef: <" + NS + PREF_REF +"> " +
				"prefix semantic_mapping_domain_model: <" + DOMAIN_MODEL_NS + "#> \n"+
				"prefix semantic_mapping_1: <" + SEMANTIC_MAP_NS + "#> \n"+
	    		
	    		"PREFIX coordx: <" + NS + COORD_X +"> " +
	    		"PREFIX coordy: <" + NS + COORD_Y +"> " +
	    		"PREFIX coordz: <" + NS + COORD_Z +"> " +
	    		"PREFIX prefRef: <" + NS + LEXICAL +"> " +
	    		"PREFIX prefRef: <" + NS + LEXICAL +"> " +
	    		
	    		
	    		"SELECT DISTINCT ?class "+
	    		"WHERE {" + 
	    			"{"+
		    		"?uri hasRef: ?ref ." + 
		    		"?ref prefRef: " + lexicalRef + " ." + 
		    		"?uri a ?class ." + 
		    		"?class rdfs:subClassOf semantic_mapping_domain_model:Furniture"+  "}"+
	    		"} LIMIT 1" ;
	    
	    Query query = QueryFactory.create(queryString);
		String classFound = "";
	    
	    QueryExecution qexec = QueryExecutionFactory.create(query, abox) ;
	    try {
	      ResultSet results = qexec.execSelect();
	      while (results.hasNext())
	      {
	        QuerySolution soln = results.nextSolution() ;
	        Resource classInstance = (Resource) soln.get("class");
			classFound = classInstance.toString();
	        System.out.print("Found clas: " + classFound);
	        
	        res = INSTANCE_ADDED; 

	      }
	    } finally { 
	    	qexec.close(); 
	    }


	    OntClass classetta = abox.getOntClass(classFound);
	    if (classetta == null){
	    	res = NO_CLASS_FOUND;
	    } else {
	    	if (AddEntityRequest(abox, classetta, uriInstance, posx, posy,  posz, lexicalRef)){
	    		System.out.print("\n\nOk\n\n");
	    	}

		}


		return res;
	}
	*/


	/**
	 * Performs SPARQL Queries on ontology
	 * 
	 * @ RETURNS: HashMap Class ,  Hashmap< Position_coord, coord.toString()>
	 * 										LexicalRef, reference
	 * 										URI, uri
	 */
	private static List<HashMap<String, HashMap<String, String>>> getAllInstancesInfoByClass(InfModel infModel, OntClass classOnt){
		
		List<HashMap<String, HashMap<String, String>>> res = new ArrayList<HashMap<String, HashMap<String, String>>>();
		String uriClass = classOnt.getURI();
		

		if (sparqlVerbosity)
			System.out.println("\n\n" + NODE_NAME + "\tSPARQL Super Queries: getAllInstancesInfoByClass");
		
	     
	    String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
	    		"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
	    		"PREFIX ns: <" + uriClass +"> " +
	    		"PREFIX hasPosition: <" + NS + POSITION +"> " +
	    		"PREFIX hasRef: <" + NS + PREF_REF +"> " +
	    		
	    		"PREFIX coordx: <" + NS + COORD_X +"> " +
	    		"PREFIX coordy: <" + NS + COORD_Y +"> " +
	    		"PREFIX coordz: <" + NS + COORD_Z +"> " +
	    		"PREFIX prefRef: <" + NS + LEXICAL +"> " +
	    		
	    		
	    		"SELECT ?uri ?x ?y ?z ?lex WHERE {" + 
	    		"?uri a ns: ." + 
	    		"?uri hasPosition: ?pos ." + 
	    		"?uri hasRef: ?ref ." + 
	    		"?ref prefRef: ?lex ." + 
	    		"?pos coordx: ?x . " + 
	    		"?pos coordy: ?y . " + 
	    		"?pos coordz: ?z " + 
	    		"}" ;
	    
	    Query query = QueryFactory.create(queryString);
	    QueryExecution qexec = QueryExecutionFactory.create(query, infModel) ;
	    try {
	      ResultSet results = qexec.execSelect();
	      while (results.hasNext())
	      {
	        QuerySolution soln = results.nextSolution() ;
	        // RDFNode x = soln.get("varName") ;       // Get a result variable by name.
	        Resource ur = (Resource) soln.get("uri");
	        Literal lex = soln.getLiteral("lex");	        
	        Literal cx = soln.getLiteral("x") ; // Get a result variable - must be a resource
	        Literal cy = soln.getLiteral("y") ; // Get a result variable - must be a resource
	        Literal cz = soln.getLiteral("z") ; // Get a result variable - must be a resource
	        

	        /*
	        System.out.print(" Instance: ");
	        System.out.println(ur);
	        System.out.print(" Lexical: ");
	        System.out.print(lex.toString());
	        System.out.println("\nPos: ");
	        System.out.print(cx.getFloat());
	        System.out.print(" , ");
	        System.out.print(cy.getFloat());
	        System.out.print(" , ");
	        System.out.print(cz.getFloat());
	        */
	        //res = r.getURI();
	        HashMap<String,HashMap<String,String>> aa = new HashMap<String,HashMap<String,String>>();
	        HashMap<String,String> a = new HashMap<String, String>();
	        a.put(COORD_X, Float.toString(cx.getFloat()));
	        a.put(COORD_Y, Float.toString(cy.getFloat()));
	        a.put(COORD_Z, Float.toString(cz.getFloat()));
	        a.put(PREF_REF, lex.toString());
	        aa.put(ur.toString(),a);
	        res.add(aa);
	      }
	    } finally { 
	    	qexec.close(); 
	    }
		return res;
	}
	
	
	/**
	 * Performs SPARQL Queries on an ontology. Retrieves a list of all instance of type classOnt
	 * 
	 * @ RETURNS: URI of instance
	 */
	private static List<String> getAllInstancesURIOfClass(InfModel infModel, OntClass classOnt){
		
		List<String> resList = new ArrayList<String>();
		String uri = classOnt.getURI();
		
		if (sparqlVerbosity)
			System.out.print("\n\n"+ NODE_NAME + "\tSPARQL Queries\n\n");
		
		// 		http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#Beer
	     
	    String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
	    		"PREFIX ns: <" + uri +"> " +
	    		"SELECT ?x  WHERE {?x a ns:}" ;
	    
	    Query query = QueryFactory.create(queryString);
	    QueryExecution qexec = QueryExecutionFactory.create(query, infModel) ;
	    try {
	      ResultSet results = qexec.execSelect();
	      for ( ; results.hasNext() ; )
	      {
	        QuerySolution soln = results.nextSolution() ;
	        RDFNode x = soln.get("varName") ;       // Get a result variable by name.
	        Resource r = soln.getResource("x") ; // Get a result variable - must be a resource
	        Literal l = soln.getLiteral("VarL") ;   // Get a result variable - must be a literal
	        
	        resList.add(r.getURI());

	      }
	    } finally { 
	    	qexec.close(); 
	    }
		return resList;
	}
	
	
	
	/**
	  * Handles AddEntityRequests using SPARQL
	  *	Creates a new instance, adds properties to it and perform consistency check
	  * If test is passed, leaves the newly created instance, otherwise deletes it
	  */
	private static boolean AddEntityRequest(OntModel abox, OntClass ontClass, String uriInstance, String posX,
			String posY, String posZ, String lexicalReference){
		boolean res = true;

		// TODO solve it
		
		//System.out.print("\nClass:\n"+ontClass.getURI()+"\n");
		//System.out.print("\nPos: " + posX + " , " + posY + ":" + posZ + "\n");
		
		String uri_Instance[] = uriInstance.split("#");
		
		if (ontClass == null) {
			res = false;
			System.out.print("\n"+ NODE_NAME + "\t[ Add Entity ] Class problem\n");
		} else {
			String queryString = "" + 
			"prefix rdfs: <"+ RDFS.getURI() +">\n" +
			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
			
			+ "prefix semantic_mapping_domain_model: <" + DOMAIN_MODEL_NS + "#> \n"
			+ "prefix semantic_mapping: <" + SEMANTIC_MAP_NS + "#> \n"
			+ "PREFIX class: <"+ ontClass.getURI() +">\n"
			
			
			+ "insert data { semantic_mapping:"+uri_Instance[1] +" rdf:type class: . "
			
			
			// Add hasPosition Irreflexive ObjectProperty 
			+ "semantic_mapping:" + uri_Instance[1] + " semantic_mapping_domain_model:hasPosition semantic_mapping:" + uri_Instance[1]
			+ "_coordinates . " 
			
			// Add Instance Coordinates 
			+ "semantic_mapping:" + uri_Instance[1]+ "_coordinates rdf:type semantic_mapping_domain_model:Coordinates . " 

			// Add datatype Property
			+ "semantic_mapping:" + uri_Instance[1] + "_coordinates semantic_mapping_domain_model:float_coordinates_x \""
			+ posX + "\"^^xsd:float . "
			+ "semantic_mapping:" + uri_Instance[1] + "_coordinates semantic_mapping_domain_model:float_coordinates_y \"" + posY
			+ "\"^^xsd:float . "
			+ "semantic_mapping:" + uri_Instance[1] + "_coordinates semantic_mapping_domain_model:float_coordinates_z \"" + posZ
			+ "\"^^xsd:float . "

			// Add hasPreferredReference ObjectProperty
			+ "semantic_mapping:" + uri_Instance[1] + " semantic_mapping_domain_model:hasPreferredReference semantic_mapping:" + uri_Instance[1]
			+ "_preferredReference . " 

			// Create Instance Lexical Reference
			+ "semantic_mapping:" + uri_Instance[1] + "_preferredReference rdf:type semantic_mapping_domain_model:PreferredReference . "

			+ "semantic_mapping:" + uri_Instance[1] + "_preferredReference semantic_mapping_domain_model:lexicalReference \"" + lexicalReference
			+ "\"^^xsd:string . " + "} \n ";
			
			UpdateAction.parseExecute(queryString, abox);
			
			// Check inconsistencies
		    InfModel infModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF, abox);
			res = performConsistencyCheckWith(infModel);
		}
	
		return res;
	}

	

	
	/**
	  * Handles AddEntityRequests from orchestrator using Jena API
	  *	Creates a new instance, adds properties to it and perform consistency check
	  * If test is passed, leaves the newly created instance, otherwise deletes it
	  */
	private static boolean handleAddEntityRequest(OntModel abox, OntClass ontClass, String uriInstance, String posX,
			String posY, String posZ, String lexicalReference){
		boolean res = false;

	    OntClass prefrefClass = abox.getOntClass(NS+PREF_REF_CLASS);
	    OntClass coordinatesClass = abox.getOntClass(NS+COORDINATES_CLASS);

		String uriBase = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_1#";
		// Create instance
	    //OntClass class1 = abox.getOntClass(NS+ontClass);
		
		if (ontClass == null)
			return false;
		else {
			// Create Individuals
			Individual i1 = abox.createIndividual(uriInstance,ontClass);
		    Individual prefRefInd = abox.createIndividual(uriBase+lexicalReference+"_pref_ref",prefrefClass);
		    Individual coordinatesInd = abox.createIndividual(uriBase+lexicalReference+"_pref_ref",coordinatesClass);
			
			
		    // Create Datatype Property
		    DatatypeProperty lexicalRef = abox.getDatatypeProperty(NS + LEXICAL);
			Literal ref = abox.createTypedLiteral(lexicalReference);
			Statement refStatement = abox.createStatement(prefRefInd, lexicalRef, ref);
			abox.add(refStatement);
			
			//  Bind pref reference individual to object individual
			ObjectProperty hasPrefRef = abox.getObjectProperty(NS+PREF_REF);
			Statement bindPrefRef = abox.createStatement(i1, hasPrefRef, prefRefInd);
			abox.add(bindPrefRef);
			

		    // Create Datatype Property for coordinate X
		    DatatypeProperty posx = abox.getDatatypeProperty(NS + COORD_X);
			Literal posXFloat = abox.createTypedLiteral(posX);
			Statement posXStatement = abox.createStatement(coordinatesInd, posx, posXFloat);
			abox.add(posXStatement);
		    // Create Datatype Property for coordinate X
		    DatatypeProperty posy = abox.getDatatypeProperty(NS + COORD_Y);
			Literal posYFloat = abox.createTypedLiteral(posY);
			Statement posYStatement = abox.createStatement(coordinatesInd, posy, posYFloat);
			abox.add(posYStatement);
		    // Create Datatype Property for coordinate X
		    DatatypeProperty posz = abox.getDatatypeProperty(NS + COORD_Z);
			Literal posZFloat = abox.createTypedLiteral(posZ);
			Statement posZStatement = abox.createStatement(coordinatesInd, posz, posZFloat);
			abox.add(posZStatement);
			

			//  Bind position individual to object individual
			ObjectProperty hasPosition = abox.getObjectProperty(NS+POSITION);
			Statement bindPose = abox.createStatement(i1, hasPosition, coordinatesInd);
			abox.add(bindPose);
			
			// Now perform consistency check

		    InfModel infModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM_MICRO_RULE_INF, abox);
			res = performConsistencyCheckWith(infModel);
		}
		
		return res;
	}



	/**
	  * Handles updateEntityRequests using SPARQL
	  *	Looks for an instance with uri 'uriInstance', if the instance does not exist returns flase.
	  * If there is match, looks for the properties Pose and LexicalReference and updates it. 
	  * A consistency if invoked. If the test is passed, true is returned. Otherwise False.
	  */
	private static boolean handleUpdateEntityRequest(OntModel abox,OntClass ontClass, String uriInstance, String posX,
			 String posY, String posZ, String lexicalReference) {
		boolean res = false;

		System.out.println(""+ NODE_NAME + "\tDeleting Instance " + uriInstance);
		handleRemoveEntityRelatedRequest(abox,uriInstance);
		
		System.out.print("\n"+ NODE_NAME + "\tNow inserting: "+ posX + ", " + posY + " , " + posZ);
		if (AddEntityRequest(abox,ontClass,uriInstance,posX,posY,posZ,lexicalReference)) {
	    	System.out.println("\n"+ NODE_NAME + "\tInstance inserted successfully");
	    	res = true;
	    } else {
	    	System.out.print(""+ NODE_NAME + "\tError could not insert instance consistency problem");
	    }
	    
	    res = true;
		
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
	private static boolean handleRemoveEntityRequest(OntModel abox, String uriInstance) {
		boolean res = false;

		// NOTE: I could set in the delete query the String uriInstance. I had to set a prefix and use it.
		// Ask to prof 
		String uri_Instance[] = uriInstance.split("#");

		String queryStringDelete = "" + 
				"prefix rdfs: <"+RDFS.getURI()+">\n" +
				"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "				
				+ "prefix semantic_mapping_domain_model: <" + DOMAIN_MODEL_NS + "#> \n"
				+ "prefix semantic_mapping: <" + SEMANTIC_MAP_NS + "#> \n"
				+ "delete { semantic_mapping:"+uri_Instance[1] +" ?pred ?obj }  "
                + "where { semantic_mapping:"+uri_Instance[1]+ " ?pred ?obj }";
				
	    //System.out.println("\n\n---- Query delete:  ----\n\n" + queryStringDelete);
		UpdateAction.parseExecute(queryStringDelete, abox);
		
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
	private static boolean handleRemoveEntityRelatedRequest(OntModel abox, String uriInstance) {
		boolean res = false;

		String uri_Instance[] = uriInstance.split("#");
		//  Removing IS A statement
		String queryStringDelete = "" + 
				"prefix rdfs: <"+RDFS.getURI()+">\n" +
				"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "				
				+ "prefix semantic_mapping_domain_model: <" + DOMAIN_MODEL_NS + "#> \n"
				+ "prefix semantic_mapping: <" + SEMANTIC_MAP_NS + "#> \n"
				+ "delete { semantic_mapping:"+uri_Instance[1] +" ?pred ?obj }  "
                + "where { semantic_mapping:"+uri_Instance[1] + " ?pred ?obj }";
				
				UpdateAction.parseExecute(queryStringDelete, abox);
		

		//  Removing coordinates statement
		String queryStringPosDelete = "" + 
				"prefix rdfs: <"+RDFS.getURI()+">\n" +
				"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "				
				+ "prefix semantic_mapping_domain_model: <" + DOMAIN_MODEL_NS + "#> \n"
				+ "prefix semantic_mapping: <" + SEMANTIC_MAP_NS + "#> \n"
				+ "delete { semantic_mapping:"+uri_Instance[1] + "_coordinates ?pred ?obj }  "
                + "where { semantic_mapping:"+uri_Instance[1] + "_coordinates ?pred ?obj }";
				
				UpdateAction.parseExecute(queryStringPosDelete, abox);
		
		String queryStringRefDelete = "" + 
				"prefix rdfs: <"+RDFS.getURI()+">\n" +
				"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"+
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "				
				+ "prefix semantic_mapping_domain_model: <" + DOMAIN_MODEL_NS + "#> \n"
				+ "prefix semantic_mapping: <" + SEMANTIC_MAP_NS + "#> \n"
				+ "delete { semantic_mapping:"+uri_Instance[1] + "_preferred_reference ?pred ?obj }  "
                + "where { semantic_mapping:"+uri_Instance[1] + "_preferred_reference ?pred ?obj }";
				
				UpdateAction.parseExecute(queryStringRefDelete, abox);
				
		return res;
	}
	
	/** 
     * Consistency Check. Returns true if passed
	  */
	private static boolean performConsistencyCheckWith(InfModel inf) {
		boolean res = false;
	    //System.out.println("\n\n---- Consistency Check ----\n\n");

	    ValidityReport validity = inf.validate();
	    if (validity.isValid()) {
	    	System.out.println(""+ NODE_NAME + "\tConsistency Check:\n Passed\n");
	    	res = true;
	    } else {
	    	System.out.println(""+ NODE_NAME + "\tConsistency Check:\n Conflicts\n");
	    	for (Iterator i = validity.getReports(); i.hasNext(); ) {
	    		System.out.println(" - " + i.next());
	    	}
	    }
	    return res;
	}
	
	/** 
     * Exports Ontology to a file with provided name and path.
	  */
	private static boolean exportOntology(InfModel inf, String absoluteFileName, String fileName){
		
		/* Consistency Check */
		boolean res = false;
	    System.out.println("\n\n"+ NODE_NAME + "\t---- Export Requested : preliminary consistency Check ----\n\n");

	    FileWriter out = null;

	    ValidityReport validity = inf.validate();
	    if (validity.isValid()) {
	    	System.out.println(""+ NODE_NAME + "\tConsistency Check:\n Passed\n");
	    	try{
	    		out = new FileWriter(absoluteFileName+fileName);
	    		inf.write(out,"RDF/XML-ABBREV");
	    		res = true;
		    	System.out.println(""+ NODE_NAME + "\tWriting to file:\n\t"+fileName);
	    	}
	    	catch (IOException a){
	    		System.out.println(""+ NODE_NAME + "\tOcchio");

	            a.printStackTrace();
	    	}
	    	finally{
	    		if (out!=null){
	    			try {out.close();} catch(IOException ex) {}
	    		}
	    	}
	    } else {
	    	System.out.println(""+ NODE_NAME + "\tConsistency Check:\n Conflicts\n");
	    	for (Iterator i = validity.getReports(); i.hasNext(); ) {
	    		System.out.println(" - " + i.next());
	    	}
	    }
	    return res;
	}
	
	/** 
     * Exports Ontology if consistency check passed
	  */
	private static boolean exportTo(InfModel inf, OntModel abox, String absoluteFileName, String fileName){
	
		boolean res = false;
		if (performConsistencyCheckWith(inf)) {
			//System.out.print("Passed");
			exportOntologyOnt(abox,absoluteFileName,fileName);
			res = true;
		}
		else {
			//System.out.print("Failed");
			res = false;
		}
		return res;
	}

}
