/*Catedra: DASL4LTD
 *Autor: Benjamin Valdes Aguirre. 
 *Matricula: 882900   Carrera: DCC 
 *Correo Electronico: bvaldesa@itesm.mx 
 *Fecha de creacion: Nov 1, 2013
 *Fecha última modificiacion: Nov 1, 2013 
 *Nombre Archivo: FormatOntology
 *Plataforma: Java 
 *Descripción: 
 */
package ng.format.ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.OntologyUtils;

/**
 *
 * @author Ng
 */
public class FormatOntology extends BeanOntology {

    public static final String ONTOLOGY_NAME = "FORMAT_ONTOLOGY";
    private static Ontology INSTANCE;

    public static Ontology getInstance() throws BeanOntologyException {
        if (INSTANCE == null) {
            INSTANCE = new FormatOntology();
        }
        return INSTANCE;
    }

    private FormatOntology() throws BeanOntologyException {
        super(ONTOLOGY_NAME);
        add("ng.format.ontology.elements");
    }

    public static void main(String args[]) throws OntologyException {
        OntologyUtils.exploreOntology(getInstance());
    }
}