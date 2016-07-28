/*Catedra: DASL4LTD
 *Autor: Benjamin Valdes Aguirre. 
 *Matricula: 882900   Carrera: DCC 
 *Correo Electronico: bvaldesa@itesm.mx 
 *Fecha de creacion: Nov 1, 2013
 *Fecha última modificiacion: Nov 1, 2013 
 *Nombre Archivo: Attribute
 *Plataforma: Java 
 *Descripción: 
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ng.format.ontology.elements;

import jade.content.Concept;

/**
 *
 * @author Ng
 */
public class Attribute implements Concept {

    private String param;
    private String format;
    private String value;
    private String optional;

    public String getParam() {
        return param;

    }

    public String getFormat() {
        return format;
    }

    public String getValue() {
        return value;
    }

    public String getOptional() {
        return optional;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setOptional(String optional) {
        this.optional = optional;
    }
}
