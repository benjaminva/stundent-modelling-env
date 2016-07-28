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
package ng.agents.model;

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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import ng.format.ontology.FormatOntology;
import ng.format.ontology.elements.Answer;
import ng.format.ontology.elements.Ask;
import ng.format.ontology.elements.Attribute;
import ng.format.ontology.elements.Feedback;
import ng.format.ontology.elements.Model;
/* put your custom imports between this two lines*/

/* put your imports here */
/**
 *
 * @author Ng
 */
public class BKTParameters extends Agent {

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
        try {
            //register  Ontology and Language
            fOntology = FormatOntology.getInstance();
            getContentManager().registerLanguage(codec);
            getContentManager().registerOntology(fOntology);
        } catch (BeanOntologyException ex) {
            writeToLog(PATH, ex.toString());
            Logger.getLogger(className).log(Level.SEVERE, null, ex);
            return false;
        }
        className = BKTParameters.class.getName(); // use for reporting exceptions
        //Description of this modelDescription format
        modelDescription.setName(PATH); //path
        modelDescription.setOutputFileExt("txt"); // extension of the files where the model is saved
        modelDescription.setInputFileExt("txt"); // extension of the files where the model is saved
        trainningTimer = 600000; //1440000; // how often will the agent try to train in miliseconds
        waitTimer = 10000; // how long will the agent wait for other agents to reply
        List<Attribute> inputs = new ArrayList<Attribute>();

        // lesson   student	skill	right
        Attribute in1 = new Attribute();  //check model  5
        in1.setParam("lesson");
        in1.setFormat("word");
        inputs.add(in1);
        Attribute in2 = new Attribute();   //check  models 3 & 4
        in2.setParam("student");
        in2.setFormat("word");
        inputs.add(in2);
        Attribute in3 = new Attribute();   //check  models 3 & 4
        in3.setParam("skill");
        in3.setFormat("word");
        inputs.add(in3);
        Attribute in4 = new Attribute();   //check  models 3 & 4
        in4.setParam("answerisright");
        in4.setFormat("num");
        inputs.add(in4);
        modelDescription.setInput(inputs); //inputs

        List<Attribute> outputs = new ArrayList<Attribute>();
        Attribute out1 = new Attribute();
        out1.setParam("l0gstdefaultperametersperskill");//this are the ones used for adresses
        out1.setFormat("list,word,num,batch");
        outputs.add(out1);
        modelDescription.setOutput(outputs); //outputs

        //Description of this modelDescription ASPECT
        aspect = "knowledge tracing";
        modelDescription.setAspect((String) aspect);
        return true;
    }

    /**
     * loads and Object from the specified file to the current model
     *
     * @param model
     * @return
     */
    private Object loadFromFile(File model) {
        if (model == null) {
            return new computeKTparamsAll();
        } else {
            Path path = Paths.get(model.getAbsolutePath());
            List<String> lines;
            computeKTparamsAll skillparams = new computeKTparamsAll();
            try {
                lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (!lines.get(i).equals("") && !path.toString().contains("temp")) {
                        String skills[] = lines.get(i).substring(1, lines.get(i).length() - 1).replace("=", " ").split(", ");
                        for (String skill : skills) {
                            String temp[] = skill.split(" ");
                            skillparams.skill_paramenters.put(temp[0], temp[1] + " " + temp[2] + " " + temp[3] + " " + temp[4]);
                        }
                    }
                }
                return skillparams;
            } catch (IOException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            }
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
    private File train(List<List<String>> winners) {
        try {

            Path output = Paths.get(PATH + "\\temp.txt");
            PrintWriter writer = new PrintWriter(output.toFile());
            writer.print("");
            writer.close();
            for (List<String> files : winners) { // open the received files for trainning
                // Charset for read and write
                Charset charset = StandardCharsets.UTF_8;
                // Join files (lines)
                for (String file : files) {
                    List<String> lines = Files.readAllLines(Paths.get(file), charset);
                    Files.write(output, lines, charset, StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND);
                }
            }

            String input = "";
            if (winners.isEmpty() || winners.get(0).isEmpty() || winners.get(0).get(0).isEmpty()) {
                return getLastModel();
            }
            String dateSection[] = winners.get(0).get(0).split("\\\\");
            String date = dateSection[dateSection.length - 1].split("\\.")[0];

            boolean badinput = false;
            /*========Replace this code with your own for trainning your model ==================*/
            if (currentModel == null) {
                currentModel = new computeKTparamsAll();
            }
            computeKTparamsAll skillparams = (computeKTparamsAll) currentModel;
            Path output2 = Paths.get(PATH + "\\temp2.txt");
            skillparams.computelzerot(sortFileBySkill(output.toString(), output2.toString()));
            input = skillparams.skill_paramenters.toString();

            /*====================================================================================*/
            if (!badinput) { // only write congruent information
                writeTo(PATH, input, modelDescription.getOutputFileExt(), date, true);

            } else {        // elimintate faulty inputs
                writeToLog(PATH, "bad input: " + input);
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
        for (Attribute att : requestedOutput) {
            if (att.getParam().equals("l0gstdefaultperametersperskill")) {
                att.setValue(((computeKTparamsAll) currentModel).skill_paramenters.toString());
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
            if (currentModel == null) {
                uncompatible.clear();
            }
            if (uncompatible.contains(modelName)) {
                return false;
            }

            String file = modelName, input;
            String files[] = (new File(file)).list();
            if (files.length < 3) {
                return false;
            }
            for (int i = 0; i < 3; i++) {
                if (!files[i].equals("log.txt") && !files[i].equals("filesused.txt")) {
                    Path path = Paths.get(modelName + "\\" + files[i]);
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    //open file from model to check
                    for (String line : lines) {
                        /* =============== check if model can be processed by the train function ==============*/
                        String temp[] = line.split("\t");
                        if (!(temp[4].equals("cell") || temp[6].equals("eol"))) {
                            //num	lesson	student	skill	cell	right	eol
                            //131	CuClose	Stu_005	add	cell	1	eol
                            uncompatible.add(modelName);
                            return false;
                        }
                        /* =========1======================= to here ============================== */
                    }
                }
            }
            return true;
        } catch (Exception e) {
            uncompatible.add(modelName);
            return false;
        }
    }

    public String sortFileBySkill(String file, String out) {
        Path path = Paths.get(file);
        List<String> lines;
        HashMap<String, String> mapA = new HashMap<String, String>();
        String input = "";
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String skills[] = new String[lines.size()];
            int i = 0;
            for (String line : lines) {
                String temp[] = line.split("\t");
                mapA.put(temp[3], temp[3]);
                i++;
            }
            skills = mapA.keySet().toArray(new String[0]);
            Arrays.sort(skills);
            List<String> used = new ArrayList<String>();
            for (i = 0; i < skills.length; i++) {
                for (String line : lines) {
                    String temp[] = line.split("\t");
                    if (skills[i].equals(temp[3]) && !used.contains(line)) {
                        used.add(line);
                    }
                }
            }
            Path output = Paths.get(out);
            Charset charset = StandardCharsets.UTF_8;
            Files.write(output, used, charset, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
        }
        return out;
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
                if (file.lastModified() > lastMod && !file.getName().equals("log.txt") && !file.getName().contains("filesused.txt")) {
                    lastMod = file.lastModified();
                    model = file;
                }
            }
        }
        return model;
    }

    private synchronized boolean writeTo(String path, Object text, String ext, boolean append) {
        return writeTo(path, text, ext, "", append);
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
     * @param append
     */
    private synchronized boolean writeTo(String path, Object text, String ext, String date, boolean append) {
        try {
            String fileName;
            if (date.equals("")) {
                fileName = getDateTime().replace(":", "").substring(0, 7) + "." + ext;
            } else {
                fileName = date + "." + ext;
            }

            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(path + "\\" + fileName, append)));
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

    /* =============================== Custom clases =======================================================*/
    private class computeKTparamsAll {

        /**
         * This class expects data sorted on Skill and then on Student in the
         * below mentioned format num	lesson	student	skill	cell right	eol 1
         * Z3.Three-FactorZCros2008	student102	META-DETERMINE-DXO	cell	0	eol
         *
         */
        public String students_[] = new String[27600];// Number of instances
        public String skill_[] = new String[27600];
        public double right_[] = new double[27600];
        public int skillends_[] = new int[27600];//Number of Skills
        public int skillnum = -1;
        public boolean lnminus1_estimation = false;
        public boolean bounded = true;
        public boolean L0Tbounded = false;
        public HashMap<String, String> skill_paramenters = new HashMap<String, String>();

        public StreamTokenizer create_tokenizer(String infile) {

            try {
                StreamTokenizer st = new StreamTokenizer(new FileReader(infile));
                st.wordChars(95, 95);
                return st;
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            }
            return null;
        }

        public void read_in_data(StreamTokenizer st_) {
            int actnum = 0;
            try {
                int tt = 724;
                skillnum = -1;
                String prevskill = "FLURG";

                tt = st_.nextToken();
                tt = st_.nextToken();
                tt = st_.nextToken();
                tt = st_.nextToken();
                tt = st_.nextToken();
                tt = st_.nextToken();
                tt = st_.nextToken();

                while (tt != StreamTokenizer.TT_EOF) {
                    tt = st_.nextToken(); // num

                    if (tt == StreamTokenizer.TT_EOF) {
                        prevskill = skill_[actnum - 1];
                        if (skillnum > -1) {
                            skillends_[skillnum] = actnum - 1;
                        }
                        break;
                    }

                    tt = st_.nextToken(); // lesson
                    // System.out.print(st_.sval+"\t");

                    tt = st_.nextToken();
                    students_[actnum] = st_.sval;
                    // System.out.print(students_[actnum]+"\t");
                    tt = st_.nextToken();
                    skill_[actnum] = st_.sval;
                    // System.out.print(skill_[actnum]+"\t");

                    tt = st_.nextToken(); // cell

                    tt = st_.nextToken();
                    right_[actnum] = st_.nval;
                    // System.out.println(right_[actnum]);

                    tt = st_.nextToken(); // eol

                    // System.out.println(slip_[actnum]);
                    actnum++;
                    if (!skill_[actnum - 1].equals(prevskill)) {
                        prevskill = skill_[actnum - 1];
                        if (skillnum > -1) {
                            skillends_[skillnum] = actnum - 2;
                        }
                        skillnum++;
                    }

                }
            } catch (Exception e) {
                System.out.println(actnum);
                e.printStackTrace();
            }

        }

        public double findGOOF(int start, int end, double Lzero, double trans,
                double G, double S) {
            double SSR = 0.0;
            String prevstudent = "FWORPLEJOHN";
            double prevL = 0.0;
            double likelihoodcorrect = 0.0;
            double prevLgivenresult = 0.0;
            double newL = 0.0;

            // udptate Learning State for each Student
            for (int i = start; i <= end; i++) {
                // System.out.println(SSR);
                if (!students_[i].equals(prevstudent)) {
                    prevL = Lzero;
                    prevstudent = students_[i];
                }

                if (lnminus1_estimation) {
                    likelihoodcorrect = prevL;
                } else {
                    likelihoodcorrect = (prevL * (1.0 - S)) + ((1.0 - prevL) * G);
                }
                SSR += (right_[i] - likelihoodcorrect) * (right_[i] - likelihoodcorrect);

                if (right_[i] == 1.0) {
                    prevLgivenresult = ((prevL * (1.0 - S)) / ((prevL * (1 - S)) + ((1.0 - prevL) * (G))));
                } else {
                    prevLgivenresult = ((prevL * (S)) / ((prevL * (S)) + ((1.0 - prevL) * (1.0 - G))));
                }

                newL = prevLgivenresult + (1.0 - prevLgivenresult) * trans;
                prevL = newL;
            }
            return SSR;
        }

        public void fit_skill_model(int curskill) {
            double SSR = 0.0;
            double BestSSR = 9999999.0;
            double bestLzero = 0.01;
            double besttrans = 0.01;
            double bestG = 0.01;
            double bestS = 0.01;
            double topG = 0.99;
            double topS = 0.99;
            double topL0 = 0.99;
            double topT = 0.99;
            if (L0Tbounded) {
                topL0 = 0.85;
                topT = 0.3;
            }
            if (bounded) {
                topG = 0.3;
                topS = 0.1;
            }

            int startact = 0;
            if (curskill > 0) {
                startact = skillends_[curskill - 1] + 1;
            }
            int endact = skillends_[curskill];

            // System.out.print(students_[startact]);
            // System.out.print(" ");
            // System.out.println(students_[endact]);
            for (double Lzero = 0.01; Lzero <= topL0; Lzero = Lzero + 0.01) {
                for (double trans = 0.01; trans <= topT; trans = trans + 0.01) {
                    for (double G = 0.01; G <= topG; G = G + 0.01) {
                        for (double S = 0.01; S <= topS; S = S + 0.01) {
                            SSR = findGOOF(startact, endact, Lzero, trans, G, S);
                            /**
                             * System.out.print(Lzero); System.out.print("\t");
                             * System.out.println(trans);
                             */
                            if (SSR < BestSSR) {
                                BestSSR = SSR;
                                bestLzero = Lzero;
                                besttrans = trans;
                                bestG = G;
                                bestS = S;
                            }
                        }
                    }
                }
            }

            // for a bit more precision
            double startLzero = bestLzero;
            double starttrans = besttrans;
            double startG = bestG;
            double startS = bestS;
            for (double Lzero = startLzero - 0.009; ((Lzero <= startLzero + 0.009) && (Lzero <= topL0)); Lzero = Lzero + 0.001) {
                for (double G = startG - 0.009; ((G <= startG + 0.009) && (G <= topG)); G = G + 0.001) {
                    for (double S = startS - 0.009; ((S <= startS + 0.009) && (S <= topS)); S = S + 0.001) {
                        for (double trans = starttrans - 0.009; ((trans <= starttrans + 0.009) && (trans < topT)); trans = trans + 0.001) {
                            SSR = findGOOF(startact, endact, Lzero, trans, G, S);
                            if (SSR < BestSSR) {
                                BestSSR = SSR;
                                bestLzero = Lzero;
                                besttrans = trans;
                                bestG = G;
                                bestS = S;
                            }
                        }
                    }
                }
            }
             System.out.print(skill_[startact]);
             System.out.print("\t");
             System.out.print(bestLzero);
             System.out.print("\t");
             System.out.print(bestG);
             System.out.print("\t");
             System.out.print(bestS);
             System.out.print("\t");
             System.out.print(besttrans);
             System.out.println("\teol");       
             skill_paramenters.put(skill_[startact],bestLzero+" "+bestG+" "+bestS+" "+besttrans);
        }

        public void computelzerot(String infile_) {
            StreamTokenizer st_ = create_tokenizer(infile_);

            read_in_data(st_);

            System.out.println("skill\tL0\tG\tS\tT\teol");
            for (int curskill = 0; curskill <= skillnum; curskill++) {
                fit_skill_model(curskill);
            }
        }
    }
}
