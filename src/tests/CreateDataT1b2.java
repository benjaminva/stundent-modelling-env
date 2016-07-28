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

public class CreateDataT1b2 extends Agent {

    private String PATHINPUT;
    private String PATHOUTPUT;
    /* ========== Set the name of the Agent here ================== */
    private String serviceName = "Service-whatitdoes";
    private String type = "Query";
    /* ============================================================ */
    private Codec codec = new SLCodec();
    private Ontology fOntology;
    private String className = QueryAgentTest.class.getName();

    private ThreadedBehaviourFactory tbr = new ThreadedBehaviourFactory(); // so each behaviors runs in its thread

    @Override
    protected void setup() {
        try {
            /*========== Registering Agent   ========================== */
            fOntology = FormatOntology.getInstance();
            getContentManager().registerLanguage(codec);
            getContentManager().registerOntology(fOntology);
        } catch (BeanOntologyException ex) {
            Logger.getLogger(QueryAgentTest.class.getName()).log(Level.SEVERE, null, ex);
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

        addBehaviour(new CreateQueries());
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
     * logs writing to file errors for the agent.
     *
     * @param path the address to the folder where the logs of the sensor will
     * be generated
     * @param text the content to be logged.
     * @param ext the extension of the file.
     */

    private boolean writeQuery(String name, String text) {
        try {
            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(name, false)));
            bf.write(text);
            bf.close();
            return true;
        } catch (IOException ex) {
            System.out.print(ex);
            return false;
        }
    }

    /* ============================= Behaviours ============================================================ */
    private class CreateQueries extends OneShotBehaviour {// communication protocol

        @Override
        public void action() {
            String datafile = "dataset test BKT.txt";
            //read data files from data
            Path path = Paths.get("data\\testData\\" + datafile.toString());
            try {
                String format = "", input = "";
                // GNU
                if (datafile.equals("dataset test BKT.txt")) {
                    format = "aspect:tutor\n" + "output:learningstate:num\n" + "output:correctlikelyhood:num\n";
                } else if (datafile.equals("GNUtest.txt")) {
                    format = "aspect:tutor\n" + "output:semanticCosine:list,num\n" + "output:languageExpression:word\n" + "output:sentence:list,word\n";
                    input = "input:sentence:list,word:";
                } else if (datafile.equals("dataset test SA sin acentos.txt")) {
                    format = "aspect:sentiment\n" + "output:sentiment:word\n";
                    input = "input:sentence:list,word:";
                }
                // KT
              /* format = "aspect:tutor\n" + "output:learningstate:num\n" + "output:correctlikelyhood:num";
                 input = "input:lesson:word:math\n"
                 + "input:student:word:benjamin\n"
                 + "input:skill:word:fisica\n"
                 + "input:answerisright:num:0\n";

                 */
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                for (String line : lines) {

                    if (datafile.equals("dataset test BKT.txt")) {
                        //131	CurriculumAssistment2778Step02778DonotuseacalculatorPeriodWhatisparenthOpen43plus78parenthClose	Stu_00411460f7c92d2124a67ea0f4cb5f85	addition	cell	1	eol
                        String inputs[] = line.split("\t");
                        input = "input:lesson:word:" + inputs[1]
                                + "\ninput:student:word:" + inputs[2]
                                + "\ninput:skill:word:" + inputs[3]
                                + "\ninput:answerisright:num:" + inputs[5]+"\n";
                    } else if (datafile.equals("GNUtest.txt")) {
                        input = "input:sentence:list,word:" + line;
                    } else if (datafile.equals("dataset test SA sin acentos.txt")) {
                        input = "input:sentence:list,word:" + line;
                    }
                    //generate query and write query to file
                    writeQuery(PATHINPUT + "\\" + datafile.split(".txt")[0]+"T1b2.txt", format + input);
                    //wait .5 seconds  do so again
                    Thread.sleep(3000);
                }
            } catch (IOException ex) {
                Logger.getLogger(QueryAgentTest.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(QueryAgentTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}