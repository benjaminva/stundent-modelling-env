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

import com.sun.corba.se.impl.util.PackagePrefixChecker;
import ng.agents.query.*;
import jade.content.Concept;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Math.abs;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Ng
 */
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import ng.format.ontology.FormatOntology;
import ng.format.ontology.elements.Answer;
import ng.format.ontology.elements.Ask;
import ng.format.ontology.elements.Attribute;
import ng.format.ontology.elements.Feedback;
import ng.format.ontology.elements.Model;

public class QueryAgentFeedbackbyTime extends Agent {

    private String PATHINPUT;
    private String PATHOUTPUT;
    /* ========== Set the name of the Agent here ================== */
    private String serviceName = "Service-whatitdoes";
    private String type = "Query";
    /* ============================================================ */
    private Codec codec = new SLCodec();
    private Ontology fOntology;
    private String className = QueryAgentTest.class.getName();
    private ConcurrentHashMap<String, HashMap> externalSystems = new ConcurrentHashMap();
    private ConcurrentHashMap<String, String> chosenSources = new ConcurrentHashMap();
    private ConcurrentHashMap<String, Integer> numberofOutputs = new ConcurrentHashMap();
    private ConcurrentHashMap<String, List<Attribute>> receivedAnswers = new ConcurrentHashMap();
    private ConcurrentHashMap<String, List<String>> convId = new ConcurrentHashMap();
    private ConcurrentHashMap<String, Long> avoid_repetitions = new ConcurrentHashMap();
    private ConcurrentHashMap<String, Long> test_conv_Times = new ConcurrentHashMap();
    private ConcurrentHashMap<String, String> answers_hash = new ConcurrentHashMap();
    private List<Long> times = new ArrayList<Long>();
    private AID currentQuery;
    private Agent myAgent;
    private int states = 0;
    private ThreadedBehaviourFactory tbr = new ThreadedBehaviourFactory(); // so each behaviors runs in its thread

    @Override
    protected void setup() {
        try {
            try {
                /*========== Registering Agent   ========================== */
                fOntology = FormatOntology.getInstance();
                getContentManager().registerLanguage(codec);
                getContentManager().registerOntology(fOntology);
            } catch (BeanOntologyException ex) {
                Logger.getLogger(QueryAgentTest.class.getName()).log(Level.SEVERE, null, ex);
            }

            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            dfd.setName(getAID());
            sd.setType(type);
            sd.setName(serviceName);
            dfd.addServices(sd);
            try {
                DFService.register(this, dfd);
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
            ServiceDescription qsd = new ServiceDescription();
            DFAgentDescription template = new DFAgentDescription();
            qsd.setType("Query");
            qsd.setName("QueryAgent");
            template.addServices(qsd);
            DFAgentDescription[] result = DFService.search(this, template);
            currentQuery = result[0].getName();

            /*========================================================= */
            PATHINPUT = createPath("input"); // create path at agent initilization in case ti doesnt exist;
            PATHOUTPUT = createPath("output"); // create path at agent initilization in case ti doesnt exist;
            loadTable();
            try {
                Thread.sleep(20000);
            } catch (InterruptedException ex) {
                Logger.getLogger(QueryAgentFeedbackbyTime.class.getName()).log(Level.SEVERE, null, ex);
            }
            addBehaviour(tbr.wrap(new GetExternalInfo()));
            
        } catch (FIPAException ex) {
            Logger.getLogger(QueryAgentFeedbackbyTime.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ef) {
            ef.printStackTrace();
        }
    }
    /*  ====================  funcions used in set up and shared by all behaviours ========================= */

    /**
     * create the directory where the log and the recorded files will be kept
     *
     * @return
     */
    private String createPath(String name) {
        String path = "data\\" + name;
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
     * logs writing to file errors for the agent.
     *
     * @param path the address to the folder where the logs of the sensor will
     * be generated
     * @param text the content to be logged.
     * @param ext the extension of the file.
     */
    private boolean writeToLog(String path, String text) {
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

    private boolean writeTimes(String name, String text) {
        try {
            text = text + System.getProperty("line.separator");
            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter("data\\testOutput\\times" + name, true)));
            bf.append(text);
            bf.close();
            return true;
        } catch (IOException ex) {
            System.out.print(ex);
            return false;
        }
    }

    private boolean writeResults(String name, String text) {
        try {
            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter("data\\testOutput\\results" + name, true)));
            bf.write(text);
            bf.close();
            return true;
        } catch (IOException ex) {
            System.out.print(ex);
            return false;
        }
    }

    private boolean overwriteFile(String path, String text) {
        try {
            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter("data\\output\\" + path, false)));
            bf.write(text);
            bf.close();
            return true;
        } catch (IOException ex) {
            System.out.print(ex);
            return false;
        }
    }

    private void loadTable() {
        String gnu = "data\\matches\\gnu.txt";
        String bkt = "data\\matches\\bkt.txt";
        String sa = "data\\matches\\sa.txt";
        Path path = Paths.get(gnu);
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i = i + 3) {
                answers_hash.put(lines.get(i + 1), lines.get(i) + " " + lines.get(i + 2));
            }
            path = Paths.get(bkt);
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i = i + 2) {
                answers_hash.put(lines.get(i + 1), lines.get(i));
            }
            path = Paths.get(sa);
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                answers_hash.put(lines.get(i).split("\t")[0], lines.get(i).split("\t")[1]);
            }
        } catch (IOException ex) {

        }
    }

    /* ============================= Behaviours ============================================================ */
    private class GetExternalInfo extends CyclicBehaviour {// communication protocol

        private WatchService watcher;
        private Map<WatchKey, Path> keys;
        private boolean trace = false;

        @Override
        public void action() {
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ex) {
                Logger.getLogger(QueryAgentFeedbackbyTime.class.getName()).log(Level.SEVERE, null, ex);
            }
            Path name = Paths.get("gnu.txt");
            processExternalFile(name);
        }

        public void processExternalFile(Path path) {
            try {

                String gnu = "gnu.txt", bkt = "bkt.txt", sa = "sa.txt", file = path.toString(), expected = "";
                int num = 0;
                path = Paths.get(PATHOUTPUT + "\\" + path.toString());
                //System.out.println(System.ge1tProperty("user.dir"));
                boolean result = false;
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                if (!lines.isEmpty()) {
                    if (file.equals(gnu)) {
                        //languageExpression:metacom
                        //sentence:Ok
                        //semanticCosine: 0.0849120827561663 0.0541472336110534      
                        // fix format space

                        if (lines.size() < 2) {
                            states++;
                        } else {
                            states = 0;
                            expected = answers_hash.get(lines.get(1));
                            if (expected != null && expected.equals(lines.get(0) + " " + lines.get(2))) {
                                result = true;
                                expected = expected.replace(":", "");
                            }
                            System.out.println("file" + file + " expected:" + expected + " provided:" + lines.get(0) + " " + lines.get(2));
                        }

                    } else if (file.equals(bkt)) {
                        //correctlikelyhood:0.6314760000000001
                        //learningstate:0.5020000000000002: Stu_6081594975a764c8e3a691fa2b3a321dequation_solving
                        //fix formatremove:
                        expected = answers_hash.get(lines.get(1));
                        if (expected != null && expected.equals(lines.get(0))) {
                            result = true;
                            expected = expected.replace(":", "");
                        }
                    } else if (file.equals(sa)) {
                        //sentiment:Los	no
                        //Los: no
                        String[] temp = lines.get(0).split(":")[1].split("\t");
                        expected = answers_hash.get(temp[0]);
                        if (expected != null && expected.equals(temp[1])) {
                            result = true;
                            expected = expected.replace(":", "");
                        }
                    }
                    //feedback:false:rat:12
                }
                path = Paths.get(PATHINPUT + "\\" + file);
                if (expected == null) {
                    expected = "no value";
                }

                // send this to query agent test
//                sendFeedback(path,feedback);
                Feedback feedback = new Feedback();
                feedback.setCorrect(result);
                feedback.setCorrectAnswer(file + expected);
                feedback.setCorrectNum(12);
                sendFeedback(feedback);
//                Files.write(path, ("\nfeedback:" + result + ":" + expected + ":" + "12").getBytes(), StandardOpenOption.APPEND);
            } catch (IOException ex) {
                System.err.print(ex);
            }
        }

        private void sendFeedback(Feedback feedback) {
            try {
                // create message to ask for further information to sources
                ACLMessage msgtosource = new ACLMessage(ACLMessage.INFORM);
                // elements to track conversation
                msgtosource.setOntology(fOntology.getName());
                msgtosource.setLanguage(codec.getName());
                AID aid = currentQuery;
                msgtosource.addReceiver(aid);
                Action act = new Action();
                act.setAction(feedback);
                act.setActor(this.getAgent().getAID());
                getContentManager().fillContent(msgtosource, act);
                send(msgtosource);
            } catch (CodecException ex) {
                Logger.getLogger(QueryAgentTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                Logger.getLogger(QueryAgentTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void register(Path dir) throws IOException {
            WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            if (trace) {
                Path prev = keys.get(key);
                if (prev == null) {
                    System.out.format("register: %s\n", dir);
                } else {
                    if (!dir.equals(prev)) {
                        System.out.format("update: %s -> %s\n", prev, dir);
                    }
                }
            }
            keys.put(key, dir);
        }

        /**
         * Creates a WatchService and registers the given directory
         */
        public void setup(Path dir) throws IOException {
            this.watcher = FileSystems.getDefault().newWatchService();
            this.keys = new HashMap<WatchKey, Path>();
            register(dir);
            // enable trace after initial registration
            this.trace = true;
        }

        <T> WatchEvent<T> cast(WatchEvent<?> event) {
            return (WatchEvent<T>) event;
        }

    }

}
