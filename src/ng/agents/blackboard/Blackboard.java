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
package ng.agents.blackboard;

import jade.content.Concept;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
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
public class Blackboard extends Agent {

    private HashMap<String, HashMap> services = new HashMap();

    private boolean writeTo(String text, String fileName) {
        try {
            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(fileName, true)));
            bf.append(text + System.getProperty("line.separator"));
            bf.close();
            return true;
        } catch (IOException ex) {
            System.out.print(ex);
            return false;
        }
    }

    @Override
    protected void setup() {
        try {
            //register  Ontology and Language
            getContentManager().registerLanguage(new SLCodec());
            getContentManager().registerOntology(FormatOntology.getInstance());
        } catch (BeanOntologyException ex) {
            Logger.getLogger(Blackboard.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*========== Registering Agent   ========================== */
        // register services of agent with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sdf = new ServiceDescription();
        sdf.setName("currentStatus");
        sdf.setType("blackboard");
        dfd.addServices(sdf);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }
        addBehaviour(new Communication());
    }

    /**
     * class used to generate an object where answer and source of the answer
     * are put together
     */
    private class Communication extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate template = MessageTemplate.MatchLanguage(new SLCodec().getName());
            ACLMessage msg = receive(template);
            if (msg != null) {
                // open message
                Concept con = null;
                try {
                    //        writeTo( msg+"\n");
                    con = ((Action) getContentManager().extractContent(msg)).getAction();
                } catch (OntologyException ex) {
                    System.out.println(msg);
                    ex.printStackTrace();
                } catch (CodecException ex) {
                    ex.printStackTrace();
                }
                // categorize message for processing
                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    if (con instanceof Ask) {
                        Ask ask = (Ask) con;
                        if (ask.getType().equals("model")) {
                            sendAnswer(ACLMessage.REFUSE, msg, "invalide type of message for Blackboard agent", "model");
                        } else if (ask.getType().equals("query")) {
                            respondQuery(ask, msg);
                        }
                    }
                } else if (msg.getPerformative() == ACLMessage.INFORM) {
                    if (con instanceof Answer) {
                        Answer ans = (Answer) con;
                        if (ans.getType().equals("model")) {
                            // process model for trainning
                            sendAnswer(ACLMessage.REFUSE, msg, "invalide type of message for Blackboard agent", "model");
                        } else if (ans.getType().equals("query")) {
                            // process answer and send answer to original query
                            updateServices(ans, msg);
                        } else {
                            System.out.print(getAID() + " answer not understood\n " + ans + "\n from message \n" + msg);
                        }
                    } else {
                        sendAnswer(ACLMessage.REFUSE, msg, "invalide type of message for Blackboard agent", "neither");
                    }
                } else if (msg.getPerformative() == ACLMessage.REFUSE) {
                } else if (msg.getPerformative() == ACLMessage.NOT_UNDERSTOOD) {
                    // ask for message to be repeated
                }
            }
        }

        /**
         * replaces the attributes in the service table from the same source
         * with the attributes in the message
         *
         * @param ans Answer object with a list of Attribute
         * @param msg containing the query
         * @return true if successful
         */
        private boolean updateServices(Answer ans, ACLMessage msg) {
            List<Attribute> answers = ((Model) ans.getAnswer()).getOutput();
            for (Attribute att : answers) {
                HashMap<String, List> temp = new HashMap<String, List>();
                List tempList = new ArrayList();
                if (services.get(att.getParam() + att.getFormat()) != null) {
                    temp = services.get(att.getParam() + att.getFormat());
                }
                tempList.add(0, att);               
                tempList.add(((Model) ans.getAnswer()).getAspect());
                temp.put(msg.getSender().getName(), tempList);
                services.put(att.getParam() + att.getFormat(), temp);
                writeTo(att.getParam() + att.getFormat()+" "+ att.getValue(), "blackboard.txt");
//                System.out.println(att.getParam() + att.getFormat()+" "+ att.getValue());
            }
            return true;
        }

        /**
         * check for fields match and domain match if true process accept, if
         * false refuses request
         *
         * @param ask
         * @param msg
         * @return
         */
        private boolean respondQuery(Ask ask, ACLMessage msg) {
            //check that the conversation has not fall in recursion
            List<Attribute> attributes = ask.getModel().getOutput();
            HashMap<String, List> temp = new HashMap<String, List>();
            List<Model> listmod = new ArrayList();
            for (Attribute att : attributes) { //get attributes
                //match each attribute and fill it with values
                if (services.get(att.getParam() + att.getFormat()) != null) {
                    temp = services.get(att.getParam() + att.getFormat());
                    for (String key : temp.keySet()) {
                        if (!key.equals(msg.getSender().getName())) {
                            Model source = new Model();
                            source.setName(key);
                            ArrayList<Attribute> listans = new ArrayList<Attribute>();
                            listans.add((Attribute) temp.get(key).get(0));
                            source.setAspect(temp.get(key).get(1).toString());
                            source.setOutput(listans);
                            listmod.add(source);
                        }
                    }
                }
            }
            if (listmod.isEmpty()) {
                sendAnswer(ACLMessage.REFUSE, msg, listmod, "query");
            } else {
                sendAnswer(ACLMessage.INFORM, msg, listmod, "query");
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
                Logger.getLogger(Blackboard.class.getName()).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                Logger.getLogger(Blackboard.class.getName()).log(Level.SEVERE, null, ex);
            }
            return false;
        }
    }
}
