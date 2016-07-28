/*Catedra: DASL4LTD
 *Autor: Benjamin Valdes Aguirre. 
 *Matricula: 882900   Carrera: DCC 
 *Correo Electronico: bvaldesa@itesm.mx 
 *Fecha de creacion: Nov 6, 2013
 *Fecha última modificiacion: Nov 6, 2013 
 *Nombre Archivo: Answer
 *Plataforma: Java 
 *Descripción: 
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ng.format.ontology.elements;

import jade.content.AgentAction;

/**
 *
 * @author Ng
 */
public class Answer implements AgentAction{

    private Object answer;
    private String type;
    private String aspect;

    public Object getAnswer() {
        return answer;
    }

    public String getType() {
        return type;
    }

    public String getAspect(){
        return aspect;
    }
    
    public void setAnswer(Object answer) {
        this.answer = answer;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    public void setAspect(String aspect){
        this.aspect = aspect;
    }

}
