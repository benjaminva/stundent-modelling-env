/*Catedra: DASL4LTD
 *Autor: Benjamin Valdes Aguirre. 
 *Matricula: 882900   Carrera: DCC 
 *Correo Electronico: bvaldesa@itesm.mx 
 *Fecha de creacion: Oct 4, 2013
 *Fecha última modificiacion: Oct 4, 2013 
 *Nombre Archivo: ModelAgentTemplate
 *Plataforma: Java 
 *Descripción: 
 */
package tests;

import ng.agents.model.*;
import jade.content.Concept;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import ng.format.ontology.FormatOntology;
import ng.format.ontology.elements.Answer;
import ng.format.ontology.elements.Ask;
import ng.format.ontology.elements.Attribute;
import ng.format.ontology.elements.Feedback;
import ng.format.ontology.elements.Model;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ConverterUtils;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;
/* put your custom imports between this two lines*/

/* put your imports here */
/**
 *
 * @author Ng
 */
public class BayesSADummy1 extends Agent {

    private String PATH;
    /* ========== Set the name of the Agent here ================== */
    private static Object currentModel;  // the model that will be queried
    private static Object trainingModel; // the model that will be trained
    private ThreadedBehaviourFactory tbr = new ThreadedBehaviourFactory(); // so each behaviors runs in its thread
    private ConcurrentHashMap<String, Grade> feedbackTable = new ConcurrentHashMap();     // stores Agent IDs of sources and their grades
    private ConcurrentHashMap<String, Object> trainningSources = new ConcurrentHashMap();     // stores Agent IDs of trainning sources
    private List<String> uncompatible = new ArrayList(); // stores sources that are not compatible for processing
    // Ontology
    private Codec codec = new SLCodec();
    private Ontology fOntology;
    private Model modelDescription = new Model();
    private String aspect;
    private Agent myAgent;
    private int trainningTimer;
    private int waitTimer;
    private String className;

    /* ====================Must fill functions nonstandard ===================*/
    private boolean describeModel() {
        className = BayesSentimentAnalysis.class.getName(); // use for reporting exceptions
        try {
            //register  Ontology and Language path do not modify this
            fOntology = FormatOntology.getInstance();
            getContentManager().registerLanguage(codec);
            getContentManager().registerOntology(fOntology);
        } catch (BeanOntologyException ex) {
            writeToLog(PATH, ex.toString());
            Logger.getLogger(className).log(Level.SEVERE, null, ex);
            return false;
        }
        modelDescription.setName(PATH); //name of the model matches its location

        //from here on you can fill in the blanks of the model 
        modelDescription.setInputFileExt("arff"); // extension of the files to be used as inputs DO NOT INCLUDE '.'
        modelDescription.setOutputFileExt("model"); // extension of the files where the model is saved DO NOT INCLUDE '.'
//  used for t1a and t1b      trainningTimer = 30000; // how often will the agent try to train in miliseconds
        trainningTimer = 1000000; // how often will the agent try to train in miliseconds
//  used for t1a and t1b      waitTimer = 100000; // how long will the agent wait before updating bb
        waitTimer = 1000; // how long will the agent wait before updating bb

        // Describe the input attributes of your model
        List<Attribute> inputs = new ArrayList<Attribute>();
        Attribute in1 = new Attribute();
        in1.setParam("labeledsentencearff"); // name of the attribute 
        in1.setFormat("list,word,num,batch"); // format in which it is stored or represented
        inputs.add(in1);
        Attribute in2 = new Attribute();
        in2.setParam("sentence");
        in2.setFormat("list,word");
        in2.setOptional("true");
        inputs.add(in2);
        modelDescription.setInput(inputs); //inputs

        // Describe the output attributes of your model
        List<Attribute> outputs = new ArrayList<Attribute>();
        Attribute out1 = new Attribute();
        out1.setParam("sentiment");
        out1.setFormat("word");
        outputs.add(out1);
        modelDescription.setOutput(outputs); //outputs

        //Description of this modelDescription aspect
        aspect = "sentiment analysis";
        modelDescription.setAspect((String) aspect);
        return true;
    }

    /**
     * loads and Object from the specified file to the current model
     *
     * @param model
     * @return
     */
    private Object loadFromFile(File file) {
        if (file == null) {
            return null;
        }
        try {
            BayesUpdateQuery buq;
            if (currentModel != null) {
                buq = (BayesUpdateQuery) currentModel;
            } else {
                buq = new BayesUpdateQuery();
            }
            buq.mfc.loadModel(file.toString());
            return buq;
        } catch (Exception ex) {
            Logger.getLogger(BayesSentimentAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * trains the model with the files in the directory specified by data if
     * overwrite is 1 the model will be trained from scratch and all of the
     * available files in data used. If overwrite is 0 the model will be loaded
     * and only new files in data will be used to updated.
     *
     * @param data is the directory of files that will be used as input data
     * @param previousInfo 0 if all info should be used, 1 inly use new info
     * @return
     */
    private File train(List<List<String>> listofSources) {
        try {
            for (List<String> files : listofSources) { // open the received files for trainning
                String dateSection[] = files.get(0).split("\\\\");
                String date = dateSection[dateSection.length - 1].split("\\.")[0];
                BayesUpdateQuery buq = (BayesUpdateQuery) currentModel;
                if (buq == null) {
                    buq = new BayesUpdateQuery();
                }
                for (String file : files) { // open each file
                    Path path = Paths.get(file);
                    List<String> lines = get_lines(path + "");
                    //open file from model to check
                    if (lines.get(0).contains("relation")) {
                        buq.mcl.loadDataset(file);
                        buq.mcl.evaluate();
                        buq.mcl.learn();
                    }
                }
                if (buq.mcl.classifier != null) {
                    buq.mcl.saveModel(PATH + "\\" + date + "." + modelDescription.getOutputFileExt());
                }
            }
        } catch (Exception ex) {
            writeToLog(PATH, ex.toString());
            ex.printStackTrace();
        }
        return getLastModel();
    }

    /**
     * run Current model and extract current answers storing them in the values
     * of than
     *
     * @param sourceId.- in case the agent needs to directly access the model of
     * the source
     * @param inputAttributes.- The attribute sent by the source agents
     * @param requestedOutput.- The attributes that are requested to be answered
     * @return the requestedOutput filled with values if there are any
     */
    private List<Attribute> runCurrentModel(String sourceId, List<Attribute> inputAttributes, List<Attribute> requestedOutput) {
        BayesUpdateQuery buq = (BayesUpdateQuery) currentModel;
        for (Attribute att : requestedOutput) {
            if (att.getParam().equals("sentiment")) {
                if (inputAttributes == null) {
                    return requestedOutput;
                }
                for (Attribute in : inputAttributes) {
                    if (in.getParam().equals("sentence")) {
//                        System.out.println("\n"+in.getValue());
                        if (null != buq.mcl.classifier) {
                            buq.mfc.classifier = buq.mcl.classifier;
                        }
                        buq.mfc.makeInstance(in.getValue());
                        att.setValue(in.getValue().split(" ")[0]+ "\t" + buq.mfc.classify()); // for testing
//                        att.setValue(buq.mfc.classify()); 
//                        System.out.println(" att " + att.getParam() + " val " + att.getValue());
                    }
                }
            }
        }
        return requestedOutput;
    }

    /**
     * checks to see if the format of the source is compatible with this model
     * for training
     *
     * @param model
     * @return true if compatible false if incompatible
     */
    private boolean isCompatible(String modelName) {
        try {
            if (!modelName.contains("data")) {
                modelName = "data\\" + modelName;
                modelName = modelName.replace("@", "");
            }
            if (currentModel == null || ((BayesUpdateQuery) currentModel).mcl == null) {
                uncompatible.clear();
            }
            if (uncompatible.contains(modelName)) {
                return false;
            }
            String files[] = (new File(modelName)).list();
            for (int i = 0; i < 5 && 4 < files.length; i++) {
                if (!files[i].equals("log.txt") && !files[i].equals("filesused.txt") && !files[i].contains("temp")) {
                    Path path = Paths.get(modelName + "\\" + files[i]);
                    List<String> lines = get_lines(path.toString());
                    //open file from model to check
                    if (lines.get(0).contains("relation")) {
                        BayesUpdateQuery test = new BayesUpdateQuery();
                        test.mcl.loadDataset(path.toString());
                        if (test.mcl.evaluate() && test.mcl.learn()) {
                            return true;
                        }
                    } else if (lines.size() > 3) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            uncompatible.add(modelName);
            return false;
        }
    }

    /* ================================== Do not modify from here on ======================================= */
    @Override
    protected void setup() {
        myAgent = this;
        PATH = createPath(); // create path at agent initilization in case it doesnt exist;
        createFiles();  // creates administrative files log.txt and trainningdata used.txt
        describeModel(); // set the description of this agent in modelDescription

        /*========== Registering Agent   ========================== */
        // register services of agent with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        for (Attribute at : modelDescription.getOutput()) {
            ServiceDescription sd = new ServiceDescription();
            sd.setType(at.getParam() + at.getFormat());
            sd.setName(at.getParam() + at.getFormat());
            dfd.addServices(sd);
        }

        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {
            writeToLog(PATH, ex.toString());
            ex.printStackTrace();
        }

        /*========================================================= */
        addBehaviour(new LoadBehavior());
        addBehaviour(tbr.wrap(new TrainBehaviour(this, trainningTimer)));
        addBehaviour(new Communication());

    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            writeToLog(PATH, ex.toString());
            ex.printStackTrace();
        }
    }
    /*  ====================  funcions used in set up and shared by all behaviours ==========================*/

    private List<String> get_lines(String fileName) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            List<String> lines = new ArrayList<String>();
            String line = "";
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
            return lines;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BayesSentimentAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BayesSentimentAnalysis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * create the directory where the log and the recorded files will be kept
     *
     * @return
     */
    private String createPath() {
        String path = "data\\" + getAID().getName().toLowerCase().replace("@", "").replace("-", "");
        File theDir = new File(path);
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            System.out.println("creating directory: " + path);
            boolean result = theDir.mkdir();
            if (result) {
                System.out.println("DIR created" + path);
            }
        } else {
            System.out.println(path + "  exists");
        }
        return path;
    }

    /**
     * create stardard adminitratives files for agent log.txt filesused.txt
     */
    private void createFiles() {
        try {
            Files.createFile(Paths.get(PATH + "\\log.txt"));
        } catch (IOException ex) {
            writeToLog(PATH, ex.toString());
        }
        try {
            Files.createFile(Paths.get(PATH + "\\filesused.txt"));
            List<String> temp = new ArrayList<String>();
            temp.add("log.txt\n");
            Files.write(Paths.get(PATH + "\\filesused.txt"), temp, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            writeToLog(PATH, ex.toString());
        }
    }

    /**
     * returns the current time in the format DDD:HH:mm:ss
     *
     * @return time
     */
    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("DDD:HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * checks for the las file updated in the file and returns it
     *
     * @return
     */
    private File getLastModel() {
        File folder = new File(PATH);
        File model = null;
        if (folder.list().length > 0) {
            File[] files = folder.listFiles();
            long lastMod = Long.MIN_VALUE;
            for (File file : files) {
                if (file.lastModified() > lastMod && !file.getName().equals("log.txt") && !file.getName().contains("filesused.txt") && !file.getName().contains("temp")) {
                    lastMod = file.lastModified();
                    model = file;
                }
            }
        }
        return model;
    }

    private synchronized boolean writeTo(String path, Object text, String ext) {
        return writeTo(path, text, ext, "");
    }

    /**
     * writes to the string a dynamically generated file named after the minute
     * it was created on the keyboard folder.
     *
     * @param path the address to the folder where the logs of the sensor will
     * be generated
     * @param text the content to be logged.
     * @param ext the extension of the file.
     * @param date date of the file.
     */
    private synchronized boolean writeTo(String path, Object text, String ext, String date) {
        try {
            String fileName;
            if (date.equals("")) {
                fileName = getDateTime().replace(":", "").substring(0, 7) + "." + ext;
            } else {
                fileName = date + "." + ext;
            }

            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(path + "\\" + fileName, true)));
            bf.append(text + System.getProperty("line.separator"));
            bf.close();
            return true;
        } catch (IOException ex) {
            writeToLog(PATH, ex.toString());
            return false;
        }
    }

    /**
     * logs writing to file errors for the agent.
     *
     * @param path the address to the folder where the logs of the sensor will
     * be generated
     * @param text the content to be logged.
     * @param ext the extension of the file.
     */
    private synchronized boolean writeToLog(String path, String text) {
        try {
            text = text + "at " + getDateTime().replace(":", "").substring(0, 7) + System.getProperty("line.separator");
            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(path + "\\" + "log.txt", true)));
            bf.append(text);
            bf.close();
            return true;
        } catch (IOException ex) {
            System.out.print(ex);
            return false;
        }
    }

    /* ============================= Behaviours ============================================================ */
    private class TrainBehaviour extends TickerBehaviour {

        private TrainBehaviour(Agent a, int time) {
            super(a, time); // time is the space it will wait before attempting to train

        }

        @Override
        protected void onTick() {
            List<Object> data = getTrainningSourceFiles();
            List<File> data1 = (List<File>) data.get(0);
            if (!data1.isEmpty()) {
                currentModel = loadFromFile(train(selectFiles(data, 1)));
            } else {
                if (!exactTrainningSources()) {
                    composedTrainningSources();
                }
            }
        }

        /**
         * returns a list of file with non repeated elements
         *
         * @param data
         * @return
         */
        private List<File> removeRepeated(List<File> data) {
            List<File> unique = new ArrayList<File>();
            for (File datum : data) {
                if (!unique.contains(datum)) {
                    unique.add(datum);
                }
            }
            return unique;
        }

        /**
         * returns a list containing lists of Strings that have the same name in
         * different folders, the returned lists are grouped by file name.
         *
         * @param data
         * @param previousInfo
         * @return
         */
        private List<List<String>> selectFiles(List<Object> dataSet, int previousInfo) {
            List<File> data = removeRepeated((List<File>) dataSet.get(0));
            List<List<String>> winners = new ArrayList<List<String>>();
            List<List<String>> listOfMandatoryFiles = new ArrayList<List<String>>();
            if (previousInfo == 0) {
                for (File datum : data) {
                    if (datum.isDirectory()) {
                        List<String> files = new ArrayList<String>();
                        for (String file : datum.list()) {
                            if (!file.contains("log.txt") && !file.contains("temp")) {
                                files.add(datum + "\\" + file); // use every file in the path except log
                            }
                        }
                        listOfMandatoryFiles.add(files);
                    }
                }
            } else {
                for (File datum : data) {
                    listOfMandatoryFiles.add(newTrainningSources(datum)); // use list of unsued files
                }
            }
            // separate the optional files of the list.
            List<Object> lo = separateTypesOfFiles(listOfMandatoryFiles, dataSet);
            listOfMandatoryFiles = (List<List<String>>) lo.get(0); // mandatory files
            List<List<String>> listOfOptionalFiles = (List<List<String>>) lo.get(1);

            boolean empty = false;//check if files are from optional exclude from empty check
            if (listOfMandatoryFiles.isEmpty()) {
                empty = true;
            }
            for (List<String> files : listOfMandatoryFiles) { // check that data exists in all sources
                if (files.isEmpty()) {
                    empty = true;
                }
            }
            //if all mandatory sources have data
            if (empty == false) {
                // train with unused files from the path
                for (int i = 0; i < listOfMandatoryFiles.get(0).size(); i++) {
                    String fileName = listOfMandatoryFiles.get(0).get(i);
                    List<String> possibleWinners = new ArrayList<String>();
                    possibleWinners.add(fileName);

                    String pathSections[] = fileName.split("\\\\");
                    fileName = pathSections[pathSections.length - 1];
                    fileName = fileName.split("\\.")[0];
                    for (int j = 1; j < listOfMandatoryFiles.size(); j++) {
                        int k = 0;
                        boolean contains = false;
                        while (contains == false && k < listOfMandatoryFiles.get(j).size()) {
                            String temp = listOfMandatoryFiles.get(j).get(k);
                            String tempSections[] = temp.split("\\\\");
                            if (fileName.equals(tempSections[tempSections.length - 1].split("\\.")[0])) {
                                possibleWinners.add(temp);
                                contains = true;
                            }
                            k++;
                        }
                    }
                    // if all the mandatory sources have matching files
                    if (possibleWinners.size() == listOfMandatoryFiles.size()) {
                        winners.add(possibleWinners);
                        // write here 
                        Path path = Paths.get(PATH + "\\" + "filesused.txt");
                        try {
                            Files.write(path, possibleWinners, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                        } catch (IOException ex) {
                            Logger.getLogger(className).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                for (int i = 0; i < listOfOptionalFiles.size(); i++) {
                    winners.add(listOfOptionalFiles.get(i));
                }
            }
            return winners;
        }

        /**
         * Creates two ArrayList<List<String>> , the first list contains all
         * mandatory attributes, the second contains files with optional
         * attributes
         *
         * @param listOfFiles
         * @param dataSet
         * @return object with two Lists
         */
        private List<Object> separateTypesOfFiles(List<List<String>> listOfFiles, List<Object> dataSet) {
            List<List<String>> listOfFiles1 = new ArrayList<List<String>>();
            List<List<String>> listOfFiles2 = new ArrayList<List<String>>();
            // for each group of file from a source
            for (List<String> files : listOfFiles) {
                if (!files.isEmpty()) {
                    boolean test = false;
                    String model = files.get(0).split("\\\\")[1];
                    //check if dataset is valid
                    if (dataSet.size() > 1 && dataSet.get(1) != null) {
                        List<String> attributes = (List<String>) dataSet.get(1);
                        // for each attribute available to all sources 
                        for (String attribute : attributes) {
                            String modelOfAttr = attribute.split(" ")[1];
                            // check if attribute corresponds to model from file of list of files
                            if (model.equals(modelOfAttr.split("\\\\")[1])) {
                                // find if the attribute is optional for this model description
                                for (int i = 0; i < modelDescription.getInput().size(); i++) {
                                    if ((modelDescription.getInput().get(i).getParam() + modelDescription.getInput().get(i).getFormat()).equals(attribute.split(" ")[0])
                                            && modelDescription.getInput().get(i).getOptional() != null) {
                                        test = true;
                                    }
                                }
                            }
                        }
                    }
                    // if the attribute is not optional include in list one if it is include in list two
                    if (test == false) {
                        listOfFiles1.add(files);
                    } else {
                        listOfFiles2.add(files);
                    }
                }
            }
            List<Object> lo = new ArrayList<Object>();
            lo.add(listOfFiles1);
            lo.add(listOfFiles2);
            return lo;
        }

        /**
         * checks for files that have not been used to train in the directory
         * data_path the file usedfiles.txt and adds the files to usedfiles.txt
         * the files are stored including their path
         *
         * @param data_path
         * @return list of strings
         */
        private List<String> newTrainningSources(File data_path) {
            List<String> missingFiles = new ArrayList<String>();
            if (data_path.isDirectory()) {
                try {
                    Path path = Paths.get(PATH + "\\" + "filesused.txt");
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    for (String file : data_path.list()) {
                        if (!lines.contains(data_path + "\\" + file) && !file.contains("temp") && !file.equals("log.txt") && !file.equals("filesused.txt")) {
                            missingFiles.add(data_path + "\\" + file);
                        }
                    }
                    if (!missingFiles.isEmpty()) {
                        //    files.write(path, missingFiles, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                    }
                } catch (FileNotFoundException ex) {
                    writeToLog(PATH, ex.toString());
                    ex.printStackTrace();
                } catch (IOException ex) {
                    writeToLog(PATH, ex.toString());
                    ex.printStackTrace();
                }
            }
            // check that there is a file in the data_path(path of provider) that is not in a "usedfiles.txt" local to the PATH of this agent 
            return missingFiles;
        }

        /**
         * searches for models with attributes compatible with this model if
         * there are not enough source to satisfy all the input attributes of
         * this agents model description then it return an empty list
         *
         * @return a list of files of the sources that are compatible
         */
        private List<Object> getTrainningSourceFiles() {
            HashMap<String, Integer> sourceGrade = new HashMap<String, Integer>();
            HashMap<String, List<String>> typeSources = new HashMap<String, List<String>>();
            List<File> lf = new ArrayList<File>();
            List<String> lk = new ArrayList<String>();
            List<Object> lo = new ArrayList<Object>();
            for (Object key : trainningSources.keySet().toArray()) {
                if (trainningSources.get(key) instanceof Model) {
                    Model dataModel = ((Model) trainningSources.get(key));
                    boolean completeMatch = true;
                    for (Attribute attDesc : modelDescription.getInput()) {
                        boolean attMatch = false;
                        int grade = 0;
                        for (Attribute attData : dataModel.getOutput()) {
                            if (attData.getParam().equals(attDesc.getParam()) && attData.getFormat().equals(attDesc.getFormat())) {
                                attMatch = true;
                                sourceGrade.put(dataModel.getName(), ++grade);
                                List<String> l;
                                if (typeSources.get(attData.getParam() + attData.getFormat()) == null) {
                                    l = new ArrayList<String>();
                                } else {
                                    l = typeSources.get(attData.getParam() + attData.getFormat());
                                }
                                l.add(dataModel.getName());
                                typeSources.put(attData.getParam() + attData.getFormat(), l);
                            }
                        }
                        if (!attMatch) {
                            completeMatch = false;
                        }
                    }
                    if (completeMatch && !dataModel.getOutput().isEmpty() && dataModel.getOutputFileExt().equals(modelDescription.getInputFileExt())) {
                        if (isCompatible(dataModel.getName())) {
                            lf.add(new File(dataModel.getName()));
                            lo.add(lf);
                            return lo;
                        }
                    }
                }
            }
            for (String key : typeSources.keySet()) {
                List<String> models = typeSources.get(key);
                int grade = 0;
                String bestModel = null;
                for (String model : models) {
                    if (sourceGrade.get(model) > grade && isCompatible(model)) {
                        bestModel = model;
                        grade = sourceGrade.get(model);
                    }
                }
                if (bestModel != null) {
                    lf.add(new File(bestModel));
                    lk.add(key + " " + bestModel);
                }
            }
            if (!(lf.size() == modelDescription.getInput().size())) {
                lf.clear();
                lk.clear();
            }
            lo.add(lf);
            lo.add(lk);
            return lo;
        }

        /**
         * Sends messages to all of the agents that have a model with at least 1
         * attribute by the required agent.
         *
         * @return
         */
        private boolean composedTrainningSources() {
            try {
                // create message to ask for further information to sources
                ACLMessage msgtosrouce = new ACLMessage(ACLMessage.REQUEST);
                Model requiredModel = new Model();
                Ask asktosource = new Ask();
                asktosource.setType("model");
                // elements to track conversation
                msgtosrouce.setOntology(fOntology.getName());
                msgtosrouce.setLanguage(codec.getName());
                long num = System.currentTimeMillis();
                msgtosrouce.setConversationId("model" + getName() + num);
                msgtosrouce.setReplyWith("model" + getName() + num);
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                for (int i = 0; i < modelDescription.getInput().size(); i++) {
                    sd.setType(modelDescription.getInput().get(i).getParam() + modelDescription.getInput().get(i).getFormat());
                    template.addServices(sd);
                    // get the arribute required for each type(service) of agent
                    List<Attribute> temp = new ArrayList();
                    temp.add(modelDescription.getInput().get(i));
                    requiredModel.setOutput(temp);
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    //send the message
                    for (DFAgentDescription agentid : result) {
                        if (!agentid.getName().equals(getAID()) && isCompatible(agentid.getName().getName())) {
                            asktosource.setModel(requiredModel); // set the attribute required for each agent
                            msgtosrouce.addReceiver(agentid.getName());
                            Action act = new Action();
                            act.setAction(asktosource);
                            act.setActor(agentid.getName());
                            getContentManager().fillContent(msgtosrouce, act);
                            send(msgtosrouce);
                            msgtosrouce.clearAllReceiver();
                        }
                    }
                }
                return true;
            } catch (FIPAException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (CodecException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            }
            return false;
        }

        /**
         * Checks with the DF if their is an exact match source for this model
         * if there is, it sends a Request-Model message to that agent
         *
         * @return
         */
        private boolean exactTrainningSources() {
            try {
                DFAgentDescription template = new DFAgentDescription();
                for (int i = 0; i < modelDescription.getInput().size(); i++) {
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(modelDescription.getInput().get(i).getParam() + modelDescription.getInput().get(i).getFormat());
                    template.addServices(sd);
                }
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0 && !((DFAgentDescription) result[0]).getName().equals(getAID())) {
                    AID bestAgent = null;
                    int grade = Integer.MIN_VALUE;
                    for (DFAgentDescription agentid : result) {
                        if (!(agentid.getName().equals(getAID())) && isCompatible(agentid.getName().getName())) {
                            if (!feedbackTable.containsKey(agentid.getName().getName() + "")) {
                                feedbackTable.put(agentid.getName().getName() + "", new Grade(0, ""));
                            }
                            if (feedbackTable.get(agentid.getName().getName() + "").grade > grade) {
                                bestAgent = agentid.getName();
                                grade = feedbackTable.get(agentid.getName().getName() + "").grade;
                            }
                        }
                    }
                    if (bestAgent != null) {
                        ACLMessage msgtosrouce = new ACLMessage(ACLMessage.REQUEST);
                        Model requiredModel = new Model();
                        requiredModel.setOutput(modelDescription.getInput());
                        Ask asktosource = new Ask();
                        asktosource.setType("model");
                        asktosource.setModel(requiredModel);
                        msgtosrouce.setOntology(fOntology.getName());
                        msgtosrouce.setLanguage(codec.getName());
                        long num = System.currentTimeMillis();
                        msgtosrouce.setConversationId("model" + getName() + num);
                        msgtosrouce.setReplyWith("model" + getName() + num);
                        msgtosrouce.addReceiver(bestAgent);
                        Action act = new Action();
                        act.setAction(asktosource);
                        act.setActor(bestAgent);
                        getContentManager().fillContent(msgtosrouce, act);
                        send(msgtosrouce);
                        return true;
                    }
                }
            } catch (CodecException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (FIPAException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            }
            return false;
        }
    }

    private class LoadBehavior extends OneShotBehaviour {

        @Override
        public void action() {
            //check if model exists, if not create one     
            File model = getLastModel();
            currentModel = loadFromFile(model);
        }
    }

    private class Communication extends CyclicBehaviour {

        final private HashMap<String, List> convIdfeedback = new HashMap();      // stores the sources that are waiting for feedback
        final private HashMap<String, Object> pendingMsgs = new HashMap();        // stores the messages that are waiting for reply
        private AID currentBlackBoard = null;

        @Override
        public void action() {
            MessageTemplate template = MessageTemplate.MatchLanguage(codec.getName());
            ACLMessage msg = receive(template);
            if (msg != null && !msg.getSender().equals(getAID())) {
                // open message
                Concept con = null;
                try {
                    con = ((Action) getContentManager().extractContent(msg)).getAction();
                } catch (OntologyException ex) {
                    writeToLog(PATH, ex.toString());
                    ex.printStackTrace();
                } catch (CodecException ex) {
                    writeToLog(PATH, ex.toString());
                    ex.printStackTrace();
                }

                // categorize message for processing
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    if (con instanceof Ask) {
                        Ask ask = (Ask) con;
                        if (currentModel == null) {
                            sendAnswer(ACLMessage.REFUSE, msg, "model not ready", "refused");
                            System.out.println("model not loaded");
                        } else if (ask.getType().equals("model")) {
                            processModelRequest(ask, msg); // remains the same
                        } else if (ask.getType().equals("query")) {
                            processQueryRequest(msg, ask);
                        }
                    }
                } else if (msg.getPerformative() == ACLMessage.INFORM) {
                    if (con instanceof Answer) {
                        Answer ans = (Answer) con;
                        if (ans.getType().equals("model")) {
                            // process model for trainning
                            trainningSources.put("model:" + ((Model) ans.getAnswer()).getName(), ans.getAnswer());
                        } else if (ans.getType().equals("query")) {
                            // process answer and send answer to original query
                            processQueryInform(ans, msg);
                        } else {
                            System.out.print(getAID() + " answer not understood\n " + ans + "\n from message \n" + msg);
                        }
                    } else if (con instanceof Feedback) {
                        // update feedback tables with the curretn feedback
                        processFeedback((Feedback) con, msg);
                    }
                } else if (msg.getPerformative() == ACLMessage.REFUSE) {
                    if (con instanceof Answer) {
                        processAnswerRefuse(con, msg);
                    }
                } else if (msg.getPerformative() == ACLMessage.NOT_UNDERSTOOD) {
                    // ask for message to be repeated
                }
            } else {
                try {
                    //block(waitTimer);
                    //add function to clean convidfeedback hash
                    Thread.sleep(waitTimer);
                } catch (InterruptedException ex) {
                    Logger.getLogger(className).log(Level.SEVERE, null, ex);
                }
                if (!(currentModel == null)) {
                    updateBlackboard();
                }
            }
        }

        private boolean processFeedback(Feedback fb, ACLMessage msg) {
            int grade = 0;
            if (fb.isCorrect()) {
                grade = 1;
            } else {
                grade = -1;
            }
            List<String> models = convIdfeedback.get(msg.getConversationId());
            if (models == null) {
                return false;
            }
            for (String agent : models) {
                feedbackTable.get(agent).grade += grade;
                if (feedbackTable.get(agent).grade > 100) {
                    feedbackTable.get(agent).grade = 100;
                }
                if (feedbackTable.get(agent).grade < -100) {
                    feedbackTable.get(agent).grade = -100;
                }
                convIdfeedback.remove(msg.getConversationId());
            }
            return true;
        }

        /**
         * decreases the counter in the type of the query for that specific
         * conversation.
         *
         * This function is to be used with @ProcessQueryRequest, where
         * ProcessQueryRequest Registers conversations and increments the
         * associated hashes
         *
         * @param con
         * @param msg
         * @return true if successful
         */
        private boolean processAnswerRefuse(Concept con, ACLMessage msg) {
            Answer ans = (Answer) con;
            if (pendingMsgs.get(msg.getConversationId()) != null) { // message is the orginal requester it will not propagate the message
                sendAnswer(ACLMessage.REFUSE, (ACLMessage) pendingMsgs.get(msg.getConversationId()), "sources refused", "refused");
            }
            return true;
        }

        /**
         * process each message answered received, when all sources have
         * responded and at least 1 source per type is valid is gathered,
         * process information through the agent model and send an inform
         * message with answer, else send a refuse message due to lack of valid
         * info and gathers.
         *
         * This function is to be used with @ProcessQueryRequest, where
         * ProcessQueryRequest Registers conversations and increments the
         * associated hashes and processQueryInform decreases the hashes and
         * deletes the conversations.
         *
         * @param ans Answer object with a list of Attribute
         * @param msg containing the query
         * @return true if successful
         */
        private boolean processQueryInform(Answer ans, ACLMessage msg) {
            for (Model mod : (List<Model>) ans.getAnswer()) {
                String key = mod.getName();
                if (!feedbackTable.containsKey(key)) {
                    feedbackTable.put(key, new Grade(0, mod.getAspect()));
                } else {
                    feedbackTable.get(key).aspect = mod.getAspect();
                }
            }

            List<Attribute> sources = selectSources((List<Model>) ans.getAnswer(), msg.getConversationId());
            List<Attribute> requestedOutput;
            try {
                if (pendingMsgs.get(msg.getConversationId()) == null) {
                    System.out.println("not pending message found for " + msg.getConversationId());
                    return false;
                }
                requestedOutput = ((Ask) ((Action) getContentManager().extractContent(((ACLMessage) pendingMsgs.get(msg.getConversationId())))).getAction()).getModel().getOutput();
                List<Attribute> listAns = runCurrentModel("", sources, requestedOutput);
                Model modAns = new Model();
                modAns.setOutput(listAns);
                modAns.setAspect(modelDescription.getAspect());
                modAns.setName(modelDescription.getName());
                modAns.setOutputFileExt(modelDescription.getOutputFileExt());
                sendAnswer(ACLMessage.INFORM, (ACLMessage) pendingMsgs.get(msg.getConversationId()), modAns, "query");
            } catch (NullPointerException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
                return false;
            } catch (CodecException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
                return false;
            } catch (UngroundedException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
                return false;
            } catch (OntologyException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
                return false;
            }
            pendingMsgs.remove(msg.getConversationId());
            trainningSources.put(msg.getConversationId(), ans.getAnswer());
            return true;
        }

        /**
         * checks for fields match and domain match checks for exact match even
         * order creates and sends a reply msg
         *
         * @param ask
         * @param msg
         */
        private void processModelRequest(Ask ask, ACLMessage msg) {
            // check for fields macth and domain match
            List<Attribute> attributes = ask.getModel().getOutput();
            // exact match even order
            boolean match = true;
            int matches = 0;
            for (Attribute attAsk : attributes) {
                for (Attribute attDesc : modelDescription.getOutput()) {
                    if (attAsk.getParam().equals(attDesc.getParam())
                            && attAsk.getFormat().equals(attDesc.getFormat())) {
                        matches++;
                    }
                }
            }
            if (matches == attributes.size()) {
                sendAnswer(ACLMessage.INFORM, msg, modelDescription, "model");
            } else {
                sendAnswer(ACLMessage.REFUSE, msg, attributes, "model");
            }
        }

        /**
         * check for fields match and domain match if true process accept, if
         * refuses request
         *
         * @param ask
         * @param msg
         * @return
         */
        private boolean processQueryRequest(ACLMessage msg, Ask ask) {
            //check that the conversation has not fall in recursion
            if (ask.getModel().getInput() != null) {
                List<Attribute> listAns = runCurrentModel("", ask.getModel().getInput(), ask.getModel().getOutput());
                Model modAns = new Model();
                modAns.setOutput(listAns);
                modAns.setAspect(modelDescription.getAspect());
                modAns.setName(modelDescription.getName());
                modAns.setOutputFileExt(modelDescription.getOutputFileExt());
                sendAnswer(ACLMessage.INFORM, msg, modAns, "query");
            } else {
                sendRequesttoBlackboard(msg);
            }
            return true;
        }

        /**
         * CReates a reply and send the answer
         *
         * @param performative
         * @param msg
         * @param content
         * @param type
         * @return true if successful
         */
        private boolean sendAnswer(int performative, ACLMessage msg, Object content, String type) {
            try {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(performative);
                Answer ans = new Answer();
                ans.setAnswer(content);
                ans.setType(type);
                Action act = new Action();
                act.setActor(msg.getSender());
                act.setAction(ans);
                getContentManager().fillContent(reply, act);
                send(reply);
                return true;
            } catch (CodecException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            }
            return false;
        }

        /**
         * create and send message to ask for further information to sources
         * this message does not handle the future Informs
         *
         * @param aks
         * @param msg
         * @return true if successful
         */
        private boolean sendRequesttoBlackboard(ACLMessage msg) {
            // create message to ask for further information to sources
            ACLMessage msgtosource = new ACLMessage(ACLMessage.REQUEST);
            Model requiredModel = new Model();
            Ask asktosource = new Ask();
            asktosource.setType("query");
            // elements to track conversation
            msgtosource.setOntology(fOntology.getName());
            msgtosource.setLanguage(codec.getName());
            long num = System.currentTimeMillis();
            msgtosource.setConversationId(msg.getConversationId());
            msgtosource.setReplyWith(msg.getConversationId());
            // get the arribute required for each type(service) of agent
            requiredModel.setAspect(modelDescription.getAspect());
            requiredModel.setOutput(modelDescription.getInput());
            try {
                if (currentBlackBoard == null) {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("blackboard");
                    sd.setName("currentStatus");
                    template.addServices(sd);
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    currentBlackBoard = result[0].getName();
                }
                msgtosource.addReceiver(currentBlackBoard);
                //send the message
                asktosource.setModel(requiredModel); // set the attribute required for each agent
                Action act = new Action();
                act.setAction(asktosource);
                act.setActor(myAgent.getAID());
                getContentManager().fillContent(msgtosource, act);
                send(msgtosource);
                pendingMsgs.put(msg.getConversationId(), msg);
            } catch (CodecException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (FIPAException ex) {
                writeToLog(PATH, ex.toString());
                ex.printStackTrace();
            }
            return true;
        }

        private boolean updateBlackboard() {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setOntology(fOntology.getName());
            msg.setLanguage(new SLCodec().getName());
            msg.setConversationId("update" + getDateTime());
            try {
                if (currentBlackBoard == null) {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("blackboard");
                    sd.setName("currentStatus");
                    template.addServices(sd);
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    currentBlackBoard = result[0].getName();
                }
                //send the message
                msg.setSender(currentBlackBoard);
                Action act = new Action();
                Ask ask = new Ask();
                ask.setModel(modelDescription);
                ask.setType("query");
                act.setAction(ask);
                act.setActor(currentBlackBoard);
                getContentManager().fillContent(msg, act);
            } catch (FIPAException ex) {
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (CodecException ex) {
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            }
            sendRequesttoBlackboard(msg);
            return true;
        }

        /**
         * select the best sources from the received list using the feedback
         * table a source contains a list of attributes with a value.
         *
         * @param possibleSources
         * @return List of Objects attributes (1 per type) with the highest
         * score in the feedback table
         */
        private List<Attribute> selectSources(List<Model> possibleSources, String convId) {
            List sources = new ArrayList();
            List<String> feedbackCandidates = new ArrayList();
            int maxgrade;
            Object source;
            String chosenKey;
            for (Attribute modelAtt : modelDescription.getInput()) {
                source = null;
                chosenKey = "";
                maxgrade = Integer.MIN_VALUE;
                for (Model model : possibleSources) {
                    for (Attribute att : model.getOutput()) {
                        if (!feedbackTable.containsKey(model.getName())) {
                            feedbackTable.put(model.getName(), new Grade(0, ""));
                        }
                        // use feedback table to select the best type of source 
                        int sourceGrade = feedbackTable.get(model.getName()).grade;
                        if (feedbackTable.get(model.getName()).aspect.equals(aspect)) {
                            sourceGrade = sourceGrade * 2;
                        }
                        if (modelAtt.getParam().equals(att.getParam())
                                && modelAtt.getFormat().equals(att.getFormat())
                                && sourceGrade > maxgrade) {
                            maxgrade = sourceGrade;
                            source = att;
                            chosenKey = model.getName();
                        }
                    }
                }
                if (source == null) {
                    return null;
                }
                sources.add(source);
                feedbackCandidates.add(chosenKey);
                if (!convId.contains("update")) {
                    if (convIdfeedback.size() > 100) {
                        convIdfeedback.clear();
                    }
                    convIdfeedback.put(convId, feedbackCandidates);
                }
            }
            return sources;
        }
    }

    private class Grade {

        int grade;
        String aspect;

        public Grade(int num, String asp) {
            grade = num;
            aspect = asp;
        }
    }

    private class BayesUpdateQuery {

        public MyFilteredClassifier mfc = new MyFilteredClassifier();
        public MyFilteredLearner mcl = new MyFilteredLearner();

        class MyFilteredClassifier {

            /**
             * String that stores the text to classify
             */
            String text;
            /**
             * Object that stores the instance.
             */
            Instances instances;
            /**
             * Object that stores the classifier.
             */
            FilteredClassifier classifier;

            /**
             * This method loads the model to be used as classifier.
             *
             * @param fileName The name of the file that stores the text.
             */
            public void loadModel(String fileName) {
                try {
                    ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
                    Object tmp = in.readObject();
                    classifier = (FilteredClassifier) tmp;
                    in.close();
                    System.out.println("===== Loaded model: " + fileName + " =====");
                } catch (Exception e) {
                    // Given the cast, a ClassNotFoundException must be caught along with the IOException
                    System.out.println("Problem found when reading: " + fileName);
                }
            }

            /**
             * This method creates the instance to be classified, from the text
             * that has been read.
             */
            public Instances makeInstance(String text) {
                // Create the attributes, class and text
                FastVector fvNominalVal = new FastVector(2);
                fvNominalVal.addElement("yes");//spam
                fvNominalVal.addElement("no");//ham
                weka.core.Attribute attribute1 = new weka.core.Attribute("class", fvNominalVal);
                weka.core.Attribute attribute2 = new weka.core.Attribute("text", (FastVector) null);
                // Create list of instances with one element
                FastVector fvWekaAttributes = new FastVector(2);
                fvWekaAttributes.addElement(attribute1);
                fvWekaAttributes.addElement(attribute2);
                instances = new Instances("Test relation", fvWekaAttributes, 1);
                // Set class index
                instances.setClassIndex(0);
                // Create and add the instance
                DenseInstance instance = new DenseInstance(2);
                instance.setValue(attribute2, text);
                // Another way to do it:
                // instance.setValue((Attribute)fvWekaAttributes.elementAt(1), text);
                instances.add(instance);
                // System.out.println("===== Instance created with reference dataset =====");
                //  System.out.println(instances);
                return instances;
            }

            /**
             * This method performs the classification of the instance. Output
             * is done at the command-line.
             */
            public String classify() {
                try {
                    double pred = classifier.classifyInstance(instances.instance(0));
                    //            System.out.println("===== Classified instance =====");
                    //            System.out.println("Class predicted: " + instances.classAttribute().value((int) pred));
                    return instances.classAttribute().value((int) pred).toString();

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Problem found when classifying the text");
                }
                return null;
            }

        }

        class MyFilteredLearner {

            /**
             * Object that stores training data.
             */
            Instances trainData;
            /**
             * Object that stores the filter
             */
            StringToWordVector filter;
            /**
             * Object that stores the classifier
             */
            FilteredClassifier classifier;

            /**
             * This method loads a dataset in ARFF format. If the file does not
             * exist, or it has a wrong format, the attribute trainData is null.
             *
             * @param fileName The name of the file that stores the dataset.
             */
            public void loadDataset(String fileName) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(fileName));
                    ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader);
                    trainData = arff.getData();
                    //  System.out.println("===== Loaded dataset: " + fileName + " =====");
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    //         System.out.println("Problem found when reading: " + fileName);
                }
            }

            /**
             * This method evaluates the classifier. As recommended by WEKA
             * documentation, the classifier is defined but not trained yet.
             * Evaluation of previously trained classifiers can lead to
             * unexpected results.
             */
            public boolean evaluate() {
                try {
                    trainData.setClassIndex(0);
                    filter = new StringToWordVector();
                    filter.setAttributeIndices("last");
                    classifier = new FilteredClassifier();
                    classifier.setFilter(filter);
                    classifier.setClassifier(new NaiveBayes());
                    Evaluation eval = new Evaluation(trainData);
                    eval.crossValidateModel(classifier, trainData, 4, new Random(1));
                    //  System.out.println(eval.toSummaryString());
                    //  System.out.println(eval.toClassDetailsString());
                    //  System.out.println("===== Evaluating on filtered (training) dataset done =====");
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                    // System.out.println("Problem found when evaluating");
                }
            }

            /**
             * This method trains the classifier on the loaded dataset.
             */
            public boolean learn() {
                try {
                    trainData.setClassIndex(0);
                    filter = new StringToWordVector();
                    filter.setAttributeIndices("last");
                    classifier = new FilteredClassifier();
                    classifier.setFilter(filter);
                    classifier.setClassifier(new NaiveBayes());
                    classifier.buildClassifier(trainData);
                    // Uncomment to see the classifier
                    // System.out.println(classifier);
                    // System.out.println("===== Training on filtered (training) dataset done =====");
                    return true;
                } catch (Exception e) {
                    System.out.println("Problem found when training");
                    return false;
                }
            }

            /**
             * This method saves the trained model into a file. This is done by
             * simple serialization of the classifier object.
             *
             * @param fileName The name of the file that will store the trained
             * model.
             */
            public void saveModel(String fileName) {
                try {
                    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
                    out.writeObject(classifier);
                    out.close();
//                    System.out.println("===== Saved model: " + fileName + " =====");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Problem found when writing: " + fileName);
                }
            }

        }
    }
}
