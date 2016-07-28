/*Catedra: DASL4LTD
 *Autor: Benjamin Valdes Aguirre. 
 *Matricula: 882900   Carrera: DCC 
 *Correo Electronico: bvaldesa@itesm.mx 
 *Fecha de creacion: Nov 6, 2013
 *Fecha última modificiacion: Nov 6, 2013 
 *Nombre Archivo: Ask
 *Plataforma: Java 
 *Descripción: 
 */
package ng.format.ontology.elements;

import jade.content.AgentAction;

/**
 *
 * @author Ng
 */
public class Ask implements AgentAction {

    private Model model;
    private Object aspect;
    private String type;

    public Ask() {
    }

    public Ask(Model model, Object aspect, String type) {
        this.model = model;
        this.aspect = aspect;
        this.type = type;
    }

    public void setAspect(Object aspect) {
        this.aspect = aspect;
    }

    public Object getAspect() {
        return aspect;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}