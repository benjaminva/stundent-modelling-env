/*Catedra: DASL4LTD
 *Autor: Benjamin Valdes Aguirre. 
 *Matricula: 882900   Carrera: DCC 
 *Correo Electronico: bvaldesa@itesm.mx 
 *Fecha de creacion: Sep 2, 2013
 *Fecha última modificiacion: Sep 2, 2013 
 *Nombre Archivo: MonitorAgentKeyboard
 *Plataforma: Java 
 *Descripción: This is the prototype for a Monitor Agent
 * Monitor Agents are expected to:
 *      A) Record information from the System and store it in logs.
 *          i) Recording can be done through Cyclic Behaviors or Standard
 *              Behaviors depening on the sensor used and their frequency of 
 *              measurement required.
 *      B) Provide the location of this information and its format to any agent that requests it. 
 */
package ng.agents.monitor;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ng.format.ontology.FormatOntology;
import ng.format.ontology.elements.Answer;
import ng.format.ontology.elements.Ask;
import ng.format.ontology.elements.Attribute;
import ng.format.ontology.elements.Model;

/**
 *
 * @author Ng
 */
public class LessonStudentSkillRight extends Agent {

    private String PATH;
    // to make behaviors run in different threads
    private ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    // Ontology
    private Codec codec = new SLCodec();
    private Ontology fOntology;
    private Model modelDescription = new Model();
    private boolean trained = false;
    private Agent myAgent;
    private String aspect;
    private int waitTimer;

    /* ====================Must fill functions nonstandard ===================*/
    private String className = LessonStudentSkillRight.class.getName(); // change to the name of the class

    /**
     * Description of the model the monitor will generate
     *
     * @return
     */
    private boolean describeModel() {
        try {
            //register  Ontology and Language
            fOntology = FormatOntology.getInstance();
            getContentManager().registerLanguage(codec);
            getContentManager().registerOntology(fOntology);
        } catch (BeanOntologyException ex) {
            Logger.getLogger(className).log(Level.SEVERE, null, ex);
            return false;
        }
        myAgent = this;
        modelDescription.setName(PATH); //path
        modelDescription.setOutputFileExt("txt"); // extension of the files where the model is saved
        waitTimer = 3000000;
        List<Attribute> outputs = new ArrayList<Attribute>();
        Attribute out1 = new Attribute();
        out1.setParam("lesson");
        out1.setFormat("word");
        outputs.add(out1);
        Attribute out2 = new Attribute();
        out2.setParam("student");
        out2.setFormat("word");
        outputs.add(out2);
        Attribute out3 = new Attribute();
        out3.setParam("skill");
        out3.setFormat("word");
        outputs.add(out3);
        Attribute out4 = new Attribute();
        out4.setParam("answerisright");
        out4.setFormat("num");
        outputs.add(out4);
        modelDescription.setOutput(outputs); //outputs

        //Description of this modelDescription aspect
        aspect = "knowledge tracing";
        modelDescription.setAspect(aspect);
        return true;
    }

    /**
     * Makes a prediction using current model
     *
     * @param attributes
     * @return
     */
    private List<Attribute> runCurrentModel(List<Attribute> attributes) {
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
        String line = "";
        //076:12:18:50 40 [down,shift,extended]
        Path path = Paths.get(model.getAbsolutePath());
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            line = lines.get(lines.size() - 1);
        } catch (IOException ex) {
            writeToLog(PATH, ex.toString());
        }
        // 131	CurriculumAssistment2778Step02778DonotuseacalculatorPeriodWhatisparenthOpen43plus78parenthClose	Stu_00411460f7c92d2124a67ea0f4cb5f85	addition	cell	1	eol
        String arg[] = line.split("\t");
        for (Attribute att : attributes) {
            if (att.getParam().equals("lesson")) {
                att.setValue(arg[1]);
            } else if (att.getParam().equals("student")) {
                att.setValue(arg[2]);
            } else if (att.getParam().equals("skill")) {
                att.setValue(arg[3]);
            } else if (att.getParam().equals("answerisright")) {
                att.setValue(arg[5]);
            }
        }
        return attributes;
    }

    /**
     * Recording actions can be overwritten, though the model should be saved in
     * text files in a folder under the path of the agent
     */
    public void record() {
        try {
            int numlines, index = 1;
            String input = "";
            /* Sensor work  */
            Path path = Paths.get("data\\tempsource.txt");
            List<String> lines = null;
            try {
                /* Recording  */
                lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                numlines = (int) (Math.random() * 1000.0);
                for (int i = 0; i < numlines; i++) {
                    input += lines.get(index) + "\n";
                    index += (int) (Math.random() * 300.0);
                }
                input += lines.get(index);
                Thread.sleep(waitTimer);
                /*use the write to method to write in a file with format */
                if (!writeTo(PATH, input, modelDescription.getOutputFileExt())) {
                    writeToLog(PATH, "could not write to file");
                }
            } catch (IOException ex) {
                Logger.getLogger(LessonStudentSkillRight.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (Exception e) { // check the sensor doesnt crash in the above levels
            writeToLog(PATH, " threw an: " + e + "at " + getDateTime());
        }
    }

    /* ===================== add necesarry classes and functionts here======= */
    /* ==================   Do not modify from here on ====================== */
    @Override
    protected void setup() {
        PATH = createPath();  // create the dir for logs
        describeModel();//Description of this modelDescription format

        /*========== Registering Agent   ========================== */
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
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        /*========================================================= */
        try {  // try catch to validate sensor and someone can read it in the log
            // to make behaviours run in different threads
            addBehaviour(tbf.wrap(new RecordUserData()));
        } catch (Exception e) {
            writeToLog(PATH, "recording threw a " + e);
        }
        addBehaviour(new Communication());
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

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ef) {
            ef.printStackTrace();
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
     * writes to the string a dynamically generated file named after the minute
     * it was created on the keyboard folder.
     *
     * @param path the address to the folder where the logs of the sensor will
     * be generated
     * @param text the content to be logged.
     * @param ext the extension of the file.
     */
    private boolean writeTo(String path, String text, String ext) {
        try {
            String fileName = getDateTime().replace(":", "").substring(0, 7) + "." + ext;
            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(path + "\\" + fileName, true)));
            bf.append(text + System.getProperty("line.separator"));
            bf.close();
            trained = true;
            return true;
        } catch (IOException ex) {
            System.out.print(ex);
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

    /**
     * (Action-live) Wraps the recording method and the actual logging used by
     * the sensor. Sensors can be outside of the agent however they must be
     * called through this behaviour
     */
    private class RecordUserData extends CyclicBehaviour {

        @Override
        public void action() {
            record();
        }
    }

    /**
     * Behavior for InterAgent communication protocol coordination to provide
     * user information
     */
    private class Communication extends CyclicBehaviour {

        private AID currentBlackBoard;

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
                        if (ask.getType().equals("model")) {
                            processModelRequest(ask, msg); // remains the same
                        } else if (ask.getType().equals("query")) {
                            processQueryRequest(ask, msg);
                        }
                    }
                } else if (msg.getPerformative() == ACLMessage.INFORM) {
                    System.out.println("got feedback" + msg);
                } else if (msg.getPerformative() == ACLMessage.NOT_UNDERSTOOD) {
                    System.out.println("not understood: " + msg);
                }
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Applications.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (trained) {
                    updateBlackboard();
                }
            }
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
        private boolean processQueryRequest(Ask ask, ACLMessage msg) {
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            Answer ans = new Answer();
            List<Attribute> listans = runCurrentModel(ask.getModel().getOutput());
            Model modAns = new Model();
            modAns.setOutput(listans);
            modAns.setAspect(aspect);
            modAns.setName(modelDescription.getName());
            ans.setAnswer(modAns);
            ans.setType("query");
            Action act = new Action();
            act.setAction(ans);
            act.setActor(msg.getSender());
            try {
                getContentManager().fillContent(reply, act);
            } catch (CodecException ex) {
                Logger.getLogger(Keyboard.class.getName()).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                Logger.getLogger(Keyboard.class.getName()).log(Level.SEVERE, null, ex);
            }
            send(reply);
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

        private boolean updateBlackboard() {
            Model requiredModel = new Model();
            ACLMessage msgtosource = new ACLMessage(ACLMessage.INFORM);
            requiredModel.setOutput(modelDescription.getInput());
            Answer anstosource = new Answer();
            List<Attribute> listans = runCurrentModel(modelDescription.getOutput());
            Model modAns = new Model();
            modAns.setOutput(listans);
            modAns.setName(modelDescription.getName());
            modAns.setAspect(modelDescription.getAspect());
            anstosource.setAnswer(modAns);
            anstosource.setType("query");
            // elements to track conversation
            msgtosource.setOntology(fOntology.getName());
            msgtosource.setLanguage(new SLCodec().getName());
            msgtosource.setConversationId("update" + System.currentTimeMillis());
            msgtosource.setReplyWith("update" + System.currentTimeMillis());
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
                Action act = new Action();
                act.setAction(anstosource);
                act.setActor(currentBlackBoard);
                getContentManager().fillContent(msgtosource, act);
                send(msgtosource);
                msgtosource.clearAllReceiver();
            } catch (FIPAException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (CodecException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                writeToLog(PATH, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            }
            return false;
        }
    }
}
