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
package ng.agents.query;

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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class QueryAgentTemplate extends Agent {

    private String PATHINPUT;
    private String PATHOUTPUT;
    /* ========== Set the name of the Agent here ================== */
    private String serviceName = "Service-whatitdoes";
    private String type = "Query";
    /* ============================================================ */
    private Codec codec = new SLCodec();
    private Ontology fOntology;
    private String className = QueryAgentTemplate.class.getName();
    private ConcurrentHashMap<String, HashMap> externalSystems = new ConcurrentHashMap();
    private ConcurrentHashMap<String, String> chosenSources = new ConcurrentHashMap();
    private ConcurrentHashMap<String, List<String>> convId = new ConcurrentHashMap();
    private ThreadedBehaviourFactory tbr = new ThreadedBehaviourFactory(); // so each behaviors runs in its thread

    @Override
    protected void setup() {
        try {
            /*========== Registering Agent   ========================== */
            fOntology = FormatOntology.getInstance();
            getContentManager().registerLanguage(codec);
            getContentManager().registerOntology(fOntology);
        } catch (BeanOntologyException ex) {
            Logger.getLogger(QueryAgentTemplate.class.getName()).log(Level.SEVERE, null, ex);
        }
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        sd.setName(serviceName);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        /*========================================================= */
        PATHINPUT = createPath("input"); // create path at agent initilization in case ti doesnt exist;
        PATHOUTPUT = createPath("output"); // create path at agent initilization in case ti doesnt exist;

        addBehaviour(tbr.wrap(new ProvideExternalInfo()));
        addBehaviour(new GetExternalInfo());
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

    /* ============================= Behaviours ============================================================ */
    private class GetExternalInfo extends CyclicBehaviour {// communication protocol

        private WatchService watcher;
        private Map<WatchKey, Path> keys;
        private boolean trace = false;

        @Override
        public void action() {
            // register directory and process its events
            Path dir = Paths.get(PATHINPUT);
            try {
                setup(dir);
            } catch (IOException IO) {
                System.err.println(IO);
            }

            for (;;) {
                // wait for key to be signalled
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException x) {
                    return;
                }

                dir = keys.get(key);
                if (dir == null) {
                    System.err.println("WatchKey not recognized!!");
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind kind = event.kind();

                    // TBD - provide example of how OVERFLOW event is handled
                    if (kind == OVERFLOW) {
                        continue;
                    } else if (kind == ENTRY_CREATE) {
                        WatchEvent<Path> ev = cast(event);
                        Path name = ev.context();
                        System.out.println("start" + name + " " + getDateTime());
                        processExternalFile(name);
                        System.out.format("%s: %s\n", event.kind().name(), name);
                    } else if (kind == ENTRY_MODIFY) {
                        WatchEvent<Path> ev = cast(event);
                        Path name = ev.context();
                        System.out.println("start" + name + " " + getDateTime());
                        processExternalFile(name);
//                        System.out.format("%s: %s\n", event.kind().name(), name);
                    }
                }
                // reset key and remove from set if directory no longer accessible
                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);
                    // all directories are inaccessible
                    if (keys.isEmpty()) {
                        System.out.println("I am emtpy");
                        break;
                    }
                }
            }
        }

        /**
         * creates and sends the queries to all possible sources
         *
         * @param path
         */
        public void processExternalFile(Path path) {
            try {
                String fileName = path.toString();
                path = Paths.get("data\\input\\" + path.toString());
                System.out.println(System.getProperty("user.dir"));
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                Action act = new Action();
                Ask ask = new Ask();
                Model requiredModel = new Model();
                List<Attribute> outputs = new ArrayList<Attribute>();
                List<Attribute> inputs = new ArrayList<Attribute>();
                Feedback feedback = new Feedback();
                for (String line : lines) {
                    if (!line.equals("")) {
                        try {
                            String arg[] = line.split(":");
                            if (arg[0].equals("aspect")) {
                                requiredModel.setAspect(arg[1]);
                            } else if (arg[0].equals("output")) {
                                Attribute out = new Attribute();
                                out.setParam(arg[1]);
                                out.setFormat(arg[2]);
                                outputs.add(out);
                            } else if (arg[0].equals("input")) {
                                Attribute in = new Attribute();
                                in.setParam(arg[1]);
                                in.setFormat(arg[2]);
                                in.setValue(arg[3]);
                                inputs.add(in);
                            } else if (arg[0].equals("feedback")) {  //feedback:true:left:1
                                feedback.setCorrect(Boolean.valueOf(arg[1])); // true or false
                                feedback.setCorrectAnswer(arg[2]);
                                feedback.setCorrectNum(Integer.parseInt(arg[3]));
                            }
                        } catch (ArrayIndexOutOfBoundsException err) {
                            System.out.println("wrong input format");
                            System.err.print(err);
                        }
                    }
                }
                if (feedback.getCorrectAnswer() != null) {
                    // get message from source
                    String chosenSource = chosenSources.get(fileName);
                    if (chosenSource != null) {

                        //update table
                        int grade = (int) externalSystems.get(fileName).get(chosenSource);

                        if (feedback.isCorrect()) {
                            externalSystems.get(fileName).put(chosenSource, grade + 1);
                        } else {
                            externalSystems.get(fileName).put(chosenSource, grade - 1);
                        }
                        sendFeedback(fileName, chosenSource, feedback);
                    }
                    // compare to table

                } else {
                    requiredModel.setInput(inputs);
                    ask.setType("query");
                    act.setAction(ask);
                    // find provider directly from DF and send messages
                    findModel(fileName, outputs, requiredModel, act);
                }
            } catch (IOException ex) {
                System.err.print(ex);
            }
        }

        // sends feedback messages to all sources in the list
        private void sendFeedback(String fileName, String source, Feedback feedback) {
            try {
                // create message to ask for further information to sources
                ACLMessage msgtosrouce = new ACLMessage(ACLMessage.INFORM);
                // elements to track conversation
                msgtosrouce.setOntology(fOntology.getName());
                msgtosrouce.setLanguage(codec.getName());
                AID aid = new AID();
                aid.setName(source);
                msgtosrouce.addReceiver(aid);
                Action act = new Action();
                act.setAction(feedback);
                act.setActor(this.getAgent().getAID());
                getContentManager().fillContent(msgtosrouce, act);
                if (!convId.isEmpty() && !convId.get(fileName + source).isEmpty()) {
                    String temp[] = convId.get(fileName + source).get(0).split(".txt");
                    while (!convId.isEmpty() && //there is file
                            !convId.get(fileName + source).isEmpty() && //the file has pending conversations
                            temp[2].equals(convId.get(fileName + source).get(0).split(".txt")[2])) {  // the conversations are of a same id
                        msgtosrouce.setConversationId(convId.get(fileName + source).get(0));
                        convId.get(fileName + source).remove(0);
                        send(msgtosrouce);
                    }
                }
            } catch (CodecException ex) {
                Logger.getLogger(QueryAgentTemplate.class.getName()).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                Logger.getLogger(QueryAgentTemplate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //sends messages to all possible required models
        private void findModel(String fileName, List<Attribute> outputs, Model requiredModel, Action act) {
            try {
                // create message to ask for further information to sources
                ACLMessage msgtosrouce = new ACLMessage(ACLMessage.REQUEST);
                // elements to track conversation
                msgtosrouce.setOntology(fOntology.getName());
                msgtosrouce.setLanguage(codec.getName());
                msgtosrouce.setReplyWith(fileName);
                HashMap<String, Integer> messageSources = new HashMap();
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                for (int i = 0; i < outputs.size(); i++) {
                    sd.setType(outputs.get(i).getParam() + outputs.get(i).getFormat());
                    List<Attribute> temp = new ArrayList<Attribute>();
                    temp.add(outputs.get(i));
                    requiredModel.setOutput(temp);
                    template.addServices(sd);
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    //send the message
                    for (DFAgentDescription agentid : result) {
                        if (!agentid.getName().equals(getAID())) {
                            msgtosrouce.addReceiver(agentid.getName());
                            ((Ask) act.getAction()).setModel(requiredModel);
                            act.setActor(agentid.getName());
                            getContentManager().fillContent(msgtosrouce, act);
                            msgtosrouce.setConversationId(fileName + outputs.get(i).getParam() + ".txt" + getDateTime());
                            send(msgtosrouce);
                            List<String> conversations = convId.get(fileName + agentid.getName().getName());
                            if (conversations == null) {
                                conversations = new ArrayList<String>();
                            } else {
                                if (conversations.size() > 100) {
                                    conversations.clear();
                                }
                            }
                            conversations.add(msgtosrouce.getConversationId());
                            convId.put(fileName + agentid.getName().getName(), conversations);
                            if (messageSources.get(agentid.getName().getName()) == null) {
                                messageSources.put(agentid.getName().getName(), 0);
                            } else {
                                int grade = messageSources.get(agentid.getName().getName());
                                messageSources.put(agentid.getName().getName(), grade + 1);
                            }
                            msgtosrouce.clearAllReceiver();
                        }
                    }
                }
                requiredModel.setOutput(outputs);
                externalSystems.put(fileName, messageSources);
            } catch (FIPAException ex) {
                writeToLog(PATHOUTPUT, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                writeToLog(PATHOUTPUT, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (CodecException ex) {
                writeToLog(PATHOUTPUT, ex.toString());
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
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

    private class ProvideExternalInfo extends CyclicBehaviour {// communication protocol

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
                    writeToLog(PATHOUTPUT, ex.toString());
                    ex.printStackTrace();
                } catch (CodecException ex) {
                    writeToLog(PATHOUTPUT, ex.toString());
                    ex.printStackTrace();
                }

                // categorize message for processing
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    if (con instanceof Ask) {
                    }
                } else if (msg.getPerformative() == ACLMessage.INFORM) {
                    if (con instanceof Answer) {
                        Answer ans = (Answer) con;
                        if (ans.getType().equals("model")) {
                            sendAnswer(ACLMessage.REFUSE, msg, "invalide type of message for Blackboard agent", "model");
                        } else if (ans.getType().equals("query")) {
                            // process answer and send answer to original query
                            // System.out.println("got " + ans.getAnswer().toString());
                            processQueryInform(ans, msg);
                        } else {
                            System.out.print(getAID() + " answer not understood\n " + ans + "\n from message \n" + msg);
                        }
                    } else if (con instanceof Feedback) {

                    }
                } else if (msg.getPerformative() == ACLMessage.REFUSE) {
                    if (con instanceof Answer) {
                        //              processAnswerRefuse(con, msg);
                    }
                } else if (msg.getPerformative() == ACLMessage.NOT_UNDERSTOOD) {
                    // ask for message to be repeated
                }
            }
        }

        private String highestGradeSource(String FileName) {
            HashMap<String, Integer> messageSources = externalSystems.get(FileName);
            int max = 0;
            String source = "";
            for (String key : messageSources.keySet()) {
                if (messageSources.get(key) >= max) {
                    max = messageSources.get(key);
                    source = key;
                }
            }
            return source;
        }

        private boolean processQueryInform(Answer ans, ACLMessage msg) {
            String[] temp = msg.getConversationId().split(".txt");
            List<Attribute> answers = ((Model) ans.getAnswer()).getOutput();
            try {
                if (highestGradeSource(temp[0] + ".txt").equals(msg.getSender().getName())) {
                    Path path = Paths.get("data\\output\\" + temp[0] + ".txt");
                    HashMap<String, String> map = new HashMap();
                    if (Files.exists(path)) {
                        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                        for (String line : lines) {
                            String arg[] = line.split(":");
                            if (!line.equals("") && arg.length > 1) {
                                map.put(arg[0], arg[1]);
                            }
                        }
                    }
                    for (Attribute att : answers) {
                        map.put(att.getParam(), att.getValue());
                    }
                    String text = "";
                    for (String key : map.keySet()) {
                        text = text + key + ":" + map.get(key) + "\n";
                    }
                    overwriteFile(temp[0] + ".txt", text);
                    chosenSources.put(temp[0] + ".txt", msg.getSender().getName()); // logs the last source used
                    // add here table for chosen source
                   System.out.println("end " + path + " " + getDateTime());

                } else {
                    System.out.println("not best sender " + msg.getSender().getName());
                }
            } catch (IOException ex) {
                System.err.print(ex);

            }
            return true;
        }

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
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                Logger.getLogger(className).log(Level.SEVERE, null, ex);
            }
            return false;
        }
    }

}
