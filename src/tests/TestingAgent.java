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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

//first:ng.agents.monitor.MonitorAgentKeyboard()
//test:tests.TestingAgent()
import jade.content.Concept;
import jade.content.ContentManager;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ng.format.ontology.elements.Ask;
import ng.format.ontology.elements.Attribute;
import ng.format.ontology.FormatOntology;
import ng.format.ontology.elements.Answer;
import ng.format.ontology.elements.Feedback;
import ng.format.ontology.elements.Model;

/**
 * @author Ng
 */
public class TestingAgent extends Agent {

    private String PATH;
    /* ========== Set the name of the Agent here ================== */
    private String Servicename = "JoeyTestsStuff";
    private String type = "Tester";
    /* ============================================================ */
    private static Object currentModel;  // the model that will be queried
    private static Object trainingModel; // the model that will be trained
    private ThreadedBehaviourFactory tbr = new ThreadedBehaviourFactory(); // so each behaviors runs in its thread
    private SLCodec sc = new SLCodec();
    private Ontology fOntology;
    private Agent myAgent;
    private int testnum = 0;

    @Override
    protected void setup() {
        myAgent = this;
        try {
            fOntology = FormatOntology.getInstance();
            getContentManager().registerLanguage(sc);
            getContentManager().registerOntology(fOntology);
//            OntologyUtils.exploreOntology(fOntology);
        } catch (OntologyException ex) {
            Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        sd.setName(Servicename);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }//
//        PATH = createPath(); // create path at agent initilization in case ti doesnt exist;
        //      createFiles();  // creates administrative files log.txt and trainningdata used.txt
        //       addBehaviour(new TestingModelMessages());
        addBehaviour(new TestingBlackboard());
        //       addBehaviour(new TestingBlackboard());
        //      addBehaviour(new TestingFeedbackMessages());
//        TestingInformedMessage();

        //     addBehaviour(new TestingQueryMessages());
        //     addBehaviour(new TestingFeedbackMessages());
        //       addBehaviour(new TestingModelMessages());
//        addBehaviour(new TestingBehaviour());
        //  addBehaviour(tbr.wrap(new TrainBehaviour(this, 60000)));  
        //  addBehaviour(tbr.wrap(new ProvideInformationBehaviour()));
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ef) {
            ef.printStackTrace();
        }
    }


    /*  ====================  funcions used in set up and shared by all behaviours ==========================*/
    /**
     * create the directory where the log and the recorded files will be kept
     *
     * @return
     */
    private String createPath() {
        String path = getAID().getName().toLowerCase().replace("@", "").replace("-", "");
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
                if (file.lastModified() > lastMod && !file.getName().equals("log.txt")) {
                    lastMod = file.lastModified();
                    model = file;
                }
            }
        }
        return model;
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
    private synchronized boolean writeTo(String path, String text, String ext) {
        try {
            String fileName = getDateTime().replace(":", "").substring(0, 7) + ext;
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

    /**
     * initialize model with random values and it save into a file
     *
     * @return the file where the model was saved
     */
    private synchronized File createModel() {
        //initialize model with random values
        int rate = 0;
        String date = getDateTime();

        writeTo(PATH, "rateleft, num ,rateright,num ,ratetotal, num", ".mod");
        return getLastModel();
    }

    /* ============================= Behaviours ============================================================ */
    private class TestingFeedbackMessages extends OneShotBehaviour {

        @Override
        public void action() {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
            ACLMessage msg = receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    sendFeedback(msg);
                } else {
                    System.out.println("received instead \n:" + msg);
                }
            } else {
                block();
            }
        }

        private void sendFeedback(ACLMessage msg) {
            ACLMessage msgtoSource = new ACLMessage(ACLMessage.INFORM);
            msgtoSource.setLanguage(sc.getName());
            msgtoSource.setOntology(fOntology.getName());
            msgtoSource.setConversationId(msg.getConversationId());
            Action act = new Action();
            Feedback fb = new Feedback();
            fb.setCorrect(false);
            fb.setCorrectAnswer("firefox");
            act.setAction(fb);
            act.setActor(getAID());
            try {
                getContentManager().fillContent(msgtoSource, act);
                msgtoSource.addReceiver(msg.getSender());
                send(msgtoSource);
            } catch (CodecException ex) {
                Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
            } catch (OntologyException oe) {
                Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, oe);
            }
        }
    }

    int TestingInformedMessage() {
        Model modelDescription = new Model();
        modelDescription.setName(getName()); //path
        List<Attribute> inputs = new ArrayList<Attribute>();
        Attribute in0 = new Attribute();
        in0.setParam("applicationused");//this are the ones used for adresses
        in0.setFormat("word");
        inputs.add(in0);
        modelDescription.setInput(inputs); //inputs

        List<Attribute> Availableinputs = new ArrayList<Attribute>();
        Attribute Ain1 = new Attribute();
        Ain1.setParam("this is a test");//this are the ones used for adresses
        Ain1.setFormat("word");
        Availableinputs.add(Ain1);
        Attribute Ain2 = new Attribute();
        Ain2.setParam(" to check external queries");//this are the ones used for adresses
        Ain2.setFormat("word");
        Availableinputs.add(Ain2);

        //Description of this modelDescription aspect
        Object aspect = "mouse activity";
        ACLMessage msgtosource = new ACLMessage(ACLMessage.REQUEST);
        Model requiredModel = new Model();
        Ask asktosource = new Ask();
        asktosource.setAspect(aspect);
        asktosource.setType("query");
        // elements to track conversation
        msgtosource.setOntology(fOntology.getName());
        msgtosource.setLanguage(new SLCodec().getName());
        long num = System.currentTimeMillis();
        msgtosource.setConversationId("query" + getName() + num);
        msgtosource.setReplyWith("query" + getName() + num);
        // clear hash for different type of sources
        // Get Ids of agents that provide the service 
        for (int i = 0; i < modelDescription.getInput().size(); i++) {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(modelDescription.getInput().get(i).getParam() + modelDescription.getInput().get(i).getFormat());
            template.addServices(sd);
            // get the arribute required for each type(service) of agent
            List<Attribute> temp = new ArrayList();
            temp.add(modelDescription.getInput().get(i));
            requiredModel.setInput(Availableinputs);
            requiredModel.setOutput(temp);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                //send the message
                for (DFAgentDescription agentid : result) {
                    asktosource.setModel(requiredModel); // set the attribute required for each agent
                    msgtosource.addReceiver(agentid.getName());
                    Action act = new Action();
                    act.setAction(asktosource);
                    act.setActor(agentid.getName());
                    getContentManager().fillContent(msgtosource, act);
                    send(msgtosource);
                    msgtosource.clearAllReceiver();
                }
                // store how many msg were sent for each type
                // create message for each type of source and send message            
            } catch (CodecException ex) {
                writeToLog(PATH, ex.toString());
            } catch (OntologyException ex) {
                writeToLog(PATH, ex.toString());
            } catch (FIPAException ex) {
                writeToLog(PATH, ex.toString());
                ex.printStackTrace();
            }
        }

        return 0;
    }

    private class TestingQueryMessages extends CyclicBehaviour {

        boolean sent = false;

        @Override
        public void action() {

            MessageTemplate templ = MessageTemplate.MatchLanguage(new SLCodec().getName());
            ACLMessage msg = receive(templ);
            if (msg != null) {
                System.out.println("test got answered :" + msg.getContent() + " at " + getDateTime());
                try {
                    Feedback feedback = new Feedback();
                    feedback.setCorrect(true);
                    feedback.setCorrectAnswer("some thing");
                    feedback.setCorrectNum(42);
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    Action act = new Action();
                    act.setActor(msg.getSender());
                    act.setAction(feedback);
                    getContentManager().fillContent(reply, act);
                    send(reply);
                    sent = false;
                } catch (CodecException ex) {
                    writeToLog(PATH, ex.toString());
                } catch (OntologyException ex) {
                    writeToLog(PATH, ex.toString());
                }
            } else if (sent == false) {
                sent = true;
                System.out.println("sent queries at:" + getDateTime());
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
                //Description of this modelDescription format
                Model modelDescription = new Model();
                modelDescription.setName("the path of " + getName()); //path
                List<Attribute> inputs = new ArrayList<Attribute>();
                Attribute in1 = new Attribute();
                in1.setParam("applicationused");//this are the ones used for adresses
                in1.setFormat("word");
                inputs.add(in1);
/*                Attribute in2 = new Attribute();
                in2.setParam("applicationrateperminute");
                in2.setFormat("list,word,num");
                inputs.add(in2);
                /* Attribute in3 = new Attribute();
                 in3.setParam("totalclickrateperminute");
                 in3.setFormat("num");
                 inputs.add(in3);
                 Attribute in4 = new Attribute();
                 in4.setParam("keysperminute");
                 in4.setFormat("num");
                 inputs.add(in4);
                 */
                Attribute in5 = new Attribute();
                in5.setParam("applicationname");
                in5.setFormat("list,word");
                inputs.add(in5);

                modelDescription.setInput(inputs); //inputs

                //Description of this modelDescription aspect
                Object aspect = "mouse activity";

                Model requiredModel = new Model();

                ACLMessage msgtosrouce = new ACLMessage(ACLMessage.REQUEST);
                requiredModel.setOutput(modelDescription.getInput());
                Ask asktosource = new Ask();
                asktosource.setModel(requiredModel);
                asktosource.setAspect(aspect);
                asktosource.setType("query");
                // elements to track conversation
                msgtosrouce.setOntology(fOntology.getName());
                msgtosrouce.setLanguage(sc.getName());
                msgtosrouce.setConversationId("query" + getName() + System.currentTimeMillis());
                msgtosrouce.setReplyWith("query" + getName() + System.currentTimeMillis());
                for (int i = 0; i < modelDescription.getInput().size(); i++) {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(modelDescription.getInput().get(i).getParam() + modelDescription.getInput().get(i).getFormat());
                    template.addServices(sd);
                    // get the arribute required for each type(service) of agent
                    List<Attribute> temp = new ArrayList();
                    temp.add(modelDescription.getInput().get(i));
                    requiredModel.setOutput(temp);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        //send the message
                        for (DFAgentDescription agentid : result) {
                            asktosource.setModel(requiredModel); // set the attribute required for each agent
                            msgtosrouce.addReceiver(agentid.getName());
                            Action act = new Action();
                            act.setAction(asktosource);
                            act.setActor(agentid.getName());
                            getContentManager().fillContent(msgtosrouce, act);
                            send(msgtosrouce);
                            msgtosrouce.clearAllReceiver();
                        }
                    } catch (CodecException ex) {
                        Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (OntologyException ex) {
                        Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (FIPAException ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    private class TestingModelMessages extends CyclicBehaviour {

        @Override
        public void action() {
            try {

                Thread.sleep(3000);
                //ont model
                if (testnum < 3) {
                    testnum++;
                    List<Attribute> outputs = new ArrayList<Attribute>();
                    Attribute out1 = new Attribute();
                    out1.setParam("average");
                    out1.setFormat("num");
                    outputs.add(out1);
                    Attribute out2 = new Attribute();
                    out2.setParam("applicationused");//this are the ones used for adresses
                    out2.setFormat("word");
                    outputs.add(out2);

                    Model requiredModel = new Model();
                    requiredModel.setOutput(outputs);
                    Ask asktosource = new Ask();
                    asktosource.setModel(requiredModel);
                    asktosource.setAspect("happy stuff");
                    asktosource.setType("model");

                    ACLMessage msgtosrouce = new ACLMessage(ACLMessage.REQUEST);

                    msgtosrouce.setLanguage(sc.getName());
                    msgtosrouce.setOntology(fOntology.getName());

                    for (int i = 0; i < requiredModel.getOutput().size(); i++) {
                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType(requiredModel.getOutput().get(i).getParam());

                        template.addServices(sd);
                        msgtosrouce.setConversationId("model" + getName() + System.currentTimeMillis());
                        int receivers = 0;
                        try { // check this
                            DFAgentDescription[] result = DFService.search(myAgent, template);
                            for (DFAgentDescription agentid : result) {
                                msgtosrouce.addReceiver(agentid.getName());
                                jade.content.onto.basic.Action action = new jade.content.onto.basic.Action();
                                action.setActor(agentid.getName());
                                action.setAction(asktosource);
                                try {
                                    getContentManager().fillContent(msgtosrouce, action);
                                } catch (CodecException ex) {
                                    Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (OntologyException ex) {
                                    Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                send(msgtosrouce);
                                getDataStore().put(msgtosrouce.getConversationId(), 0);
                                msgtosrouce.clearAllReceiver();
                                receivers++;
                            }
                        } catch (FIPAException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
            ACLMessage msg = receive();
            if (msg != null) {
                System.out.println("Agent got message" + msg);
                //process each answer by grade and hold it on a list
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    try {
                        ContentManager cm = getContentManager();
                        Concept con = ((Action) cm.extractContent(msg)).getAction();
                        //check if feedback or Answer
                        if (con instanceof Answer) {
                            Answer ans = (Answer) con;
                            if (ans.getType().equals("model")) {
                                getDataStore().put(msg.getConversationId(), ans.getAnswer());
                                System.out.println("the Answer is " + ((Model) ans.getAnswer()).getName());
                            } else if (ans.getType().equals("query")) {
                                System.out.println("\n The system says to query \n:" + msg);
                            }
                            // process answer and send answer to original query
                            // add to posible source
                            // increment hashtype
                            // if all hash type > 0 proceed to select sources
                        } else if (con instanceof Feedback) {
                            // modify internal Feedback list and send message to lower layers
                        }

                    } catch (OntologyException oe) {
                        oe.printStackTrace();
                    } catch (CodecException ce) {
                        ce.printStackTrace();
                    }
                }
            }
        }
    }

    private class TestingBlackboard extends CyclicBehaviour {

        int typeact = 0;

        @Override
        public void action() {

            try {
                Thread.sleep(20000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
            MessageTemplate templ = MessageTemplate.MatchLanguage(new SLCodec().getName());
            ACLMessage msg = receive(templ);

            if (msg != null) {
                System.out.println("test got answered :" + msg.getContent() + " at " + getDateTime());
            } else if (typeact == 0) {
                typeact = 1;
                System.out.println("registerd at:" + getDateTime());
                System.out.println("sent queries at:" + getDateTime());
                Model modelDescription = new Model();
                modelDescription.setName("the path of " + getName()); //path
                List<Attribute> outputs = new ArrayList<Attribute>();
                Attribute out1 = new Attribute();
                out1.setParam("keysperminute");
                out1.setFormat("num");
                out1.setValue("657");
                outputs.add(out1);
                Attribute out2 = new Attribute();
                out2.setParam("applicationused");
                out2.setFormat("word");
                out2.setValue("facebook 213");
                outputs.add(out2);

                modelDescription.setOutput(outputs); //inputs

                Object aspect = "something else";
                Model requiredModel = new Model();
                ACLMessage msgtosrouce = new ACLMessage(ACLMessage.INFORM);
                requiredModel.setOutput(modelDescription.getInput());
                Answer anstosource = new Answer();
                anstosource.setAnswer(modelDescription);
                anstosource.setAspect(aspect.toString());
                anstosource.setType("query");
                // elements to track conversation
                msgtosrouce.setOntology(fOntology.getName());
                msgtosrouce.setLanguage(sc.getName());
                msgtosrouce.setConversationId("query" + getName() + System.currentTimeMillis());
                msgtosrouce.setReplyWith("query" + getName() + System.currentTimeMillis());
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("blackboard");
                sd.setName("currentStatus");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    //send the message
                    msgtosrouce.addReceiver(result[0].getName());
                    Action act = new Action();
                    act.setAction(anstosource);
                    act.setActor(result[0].getName());
                    getContentManager().fillContent(msgtosrouce, act);
                    send(msgtosrouce);
                    msgtosrouce.clearAllReceiver();
                } catch (CodecException ex) {
                    Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
                } catch (OntologyException ex) {
                    Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
                } catch (FIPAException ex) {
                    ex.printStackTrace();
                }
            } else if (typeact == 1) {
                typeact = 0;
                System.out.println("sent queries at:" + getDateTime());
                Model modelDescription = new Model();
                modelDescription.setName("the path of " + getName()); //path
                List<Attribute> inputs = new ArrayList<Attribute>();
                /*                Attribute in1 = new Attribute();
                 in1.setParam("keysperminute");//this are the ones used for adresses
                 in1.setFormat("num");
                 inputs.add(in1);*/
                Attribute in2 = new Attribute();
                in2.setParam("Benji");//this are the ones used for adresses
                in2.setFormat("word");
                inputs.add(in2);
                /*                Attribute in3 = new Attribute();
                 in3.setParam("totalclickrateperminute");
                 in3.setFormat("num");
                 inputs.add(in3);*/
                modelDescription.setInput(inputs); //inputs

                Object aspect = "something else";

                Model requiredModel = new Model();
                ACLMessage msgtosrouce = new ACLMessage(ACLMessage.REQUEST);
                requiredModel.setOutput(modelDescription.getInput());
                Ask asktosource = new Ask();
                asktosource.setModel(requiredModel);
                asktosource.setAspect(aspect);
                asktosource.setType("query");
                // elements to track conversation
                msgtosrouce.setOntology(fOntology.getName());
                msgtosrouce.setLanguage(sc.getName());
                msgtosrouce.setConversationId("query" + getName() + System.currentTimeMillis());
                msgtosrouce.setReplyWith("query" + getName() + System.currentTimeMillis());
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setName("currentStatus");
                sd.setType("blackboard");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    //send the message
                    asktosource.setModel(requiredModel); // set the attribute required for each agent
                    msgtosrouce.addReceiver(result[0].getName());
                    Action act = new Action();
                    act.setAction(asktosource);
                    act.setActor(result[0].getName());
                    getContentManager().fillContent(msgtosrouce, act);
                    send(msgtosrouce);
                    msgtosrouce.clearAllReceiver();
                } catch (CodecException ex) {
                    Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
                } catch (OntologyException ex) {
                    Logger.getLogger(TestingAgent.class.getName()).log(Level.SEVERE, null, ex);
                } catch (FIPAException ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

}
