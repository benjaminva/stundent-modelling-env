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

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
public class Applications extends Agent {

    private String PATH;
    // to make behaviors run in different threads
    private ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    // Ontology
    private Codec codec = new SLCodec();
    private Ontology fOntology;
    private Model modelDescription = new Model();
    private String aspect;
    private boolean trained = false;
    private Agent myAgent;
    private String className;
    private int waitTimer;


    /* ====================Must fill functions nonstandard ===================*/
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
            Logger.getLogger(Applications.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        myAgent = this;
        className = Applications.class.getName();
        modelDescription.setName(PATH); //path
        modelDescription.setOutputFileExt("um"); // extension of the files where the model is saved
        waitTimer = 1000;
        List<Attribute> outputs = new ArrayList<Attribute>();
        Attribute out1 = new Attribute();
        out1.setParam("windowstart");//this are the ones used for adresses
        out1.setFormat("list,num");
        outputs.add(out1);
        Attribute out2 = new Attribute();
        out2.setParam("windowend");
        out2.setFormat("list,num");
        outputs.add(out2);
        Attribute out3 = new Attribute();
        out3.setParam("applicationname");
        out3.setFormat("list,word");
        outputs.add(out3);

        modelDescription.setOutput(outputs); //outputs
        //Description of this modelDescription aspect
        aspect = "application";
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
        //**TODO run Current model and extract current answers storing them in the values of than 

        final List<WindowInfo> inflList = new ArrayList<WindowInfo>();
        final List<Integer> order = new ArrayList<Integer>();
        int top = User32.instance.GetTopWindow(0);
        while (top != 0) {//generates a list with the top numbers, maybe ids?
            order.add(top);
            top = User32.instance.GetWindow(top, User32.GW_HWNDNEXT);
        }
        // nota mental para mi: pedirle a peter que me explique bien 
        // que son las instancias y como funncionan en java
        User32.instance.EnumWindows(new WndEnumProc() {

            public boolean callback(int hWnd, int lParam) {
                if (User32.instance.IsWindowVisible(hWnd)) {
                    RECT r = new RECT();
                    User32.instance.GetWindowRect(hWnd, r);
                    if (r.left > -32000) {     // minimized
                        byte[] buffer = new byte[1024];
                        byte[] buffer2 = new byte[1024];
                        User32.instance.GetWindowTextA(hWnd, buffer, buffer.length);
                        User32.instance.GetWindowTextA(hWnd, buffer2, buffer2.length);
                        String title = Native.toString(buffer);
                        inflList.add(new WindowInfo(hWnd, r, title));
                    }
                }
                return true;
            }
        }, 0);
        Collections.sort(inflList, new Comparator<WindowInfo>() {

            public int compare(WindowInfo o1, WindowInfo o2) {
                return order.indexOf(o1.hwnd) - order.indexOf(o2.hwnd);
            }
        });

        for (Attribute att : attributes) {
            att.setValue("");
        }
        for (WindowInfo w : inflList) {
            for (Attribute att : attributes) {
                if (att.getParam().equals("windowstart")) {
                    att.setValue(att.getValue() + w.rect.top + "," + w.rect.left);
                }
                if (att.getParam().equals("Windowend")) {
                    att.setValue(att.getValue() + w.rect.bottom + "," + w.rect.right);
                }
                if (att.getParam().equals("applicationname")) {
                    att.setValue(att.getValue() + "," + w.title);
                }
            }
        }
        return attributes;
    }

    /*
     * Recordin actions go in here
     */
    public void record() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            writeToLog(PATH, ex.toString());
        }
        final List<WindowInfo> inflList = new ArrayList<WindowInfo>();
        final List<Integer> order = new ArrayList<Integer>();
        int top = User32.instance.GetTopWindow(0);
        while (top != 0) {//generates a list with the top numbers, maybe ids?
            order.add(top);
            top = User32.instance.GetWindow(top, User32.GW_HWNDNEXT);
        }
        User32.instance.EnumWindows(new WndEnumProc() {

            public boolean callback(int hWnd, int lParam) {
                if (User32.instance.IsWindowVisible(hWnd)) {
                    RECT r = new RECT();
                    User32.instance.GetWindowRect(hWnd, r);
                    if (r.left > -32000) {     // minimized
                        byte[] buffer = new byte[1024];
                        byte[] buffer2 = new byte[1024];
                        User32.instance.GetWindowTextA(hWnd, buffer, buffer.length);
                        User32.instance.GetWindowTextA(hWnd, buffer2, buffer2.length);
                        String title = Native.toString(buffer);
                        inflList.add(new WindowInfo(hWnd, r, title));
                    }
                }
                return true;
            }
        }, 0);
        Collections.sort(inflList, new Comparator<WindowInfo>() {

            public int compare(WindowInfo o1, WindowInfo o2) {
                return order.indexOf(o1.hwnd) - order.indexOf(o2.hwnd);
            }
        });
        for (WindowInfo w : inflList) {
            writeTo(PATH, w.toString(),modelDescription.getOutputFileExt());
        }
    }

    public static interface WndEnumProc extends StdCallLibrary.StdCallCallback {

        boolean callback(int hWnd, int lParam);
    }

    public static interface User32 extends StdCallLibrary {

        final User32 instance = (User32) Native.loadLibrary("user32", User32.class);

        boolean EnumWindows(WndEnumProc wndenumproc, int lParam);

        boolean IsWindowVisible(int hWnd);

        int GetWindowRect(int hWnd, RECT r);

        void GetWindowTextA(int hWnd, byte[] buffer, int buflen);

        int GetTopWindow(int hWnd);

        int GetWindow(int hWnd, int flag);
        final int GW_HWNDNEXT = 2;
    }

    public static class RECT extends Structure {

        public int left, top, right, bottom;
    }

    public static class WindowInfo {

        int hwnd;
        RECT rect;
        String title;

        public WindowInfo(int hwnd, RECT rect, String title) {
            this.hwnd = hwnd;
            this.rect = rect;
            this.title = title;
        }

        public String toString() {
            return String.format("(%d,%d)-(%d,%d) : \"%s\"",
                    rect.left, rect.top, rect.right, rect.bottom, title);
        }
    }

    /* ==================   Do not modify from here on ==========================*/
    @Override
    protected void setup() {
        myAgent = this;
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
                    Thread.sleep(waitTimer);
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
