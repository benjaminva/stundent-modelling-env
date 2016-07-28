/*Catedra: DASL4LTD
 *Autor: Benjamin Valdes Aguirre. 
 *Matricula: 882900   Carrera: DCC 
 *Correo Electronico: bvaldesa@itesm.mx 
 *Fecha de creacion: Nov 1, 2013
 *Fecha última modificiacion: Nov 1, 2013 
 *Nombre Archivo: Model
 *Plataforma: Java 
 *Descripción: 
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ng.format.ontology.elements;

import jade.content.Concept;
import java.util.List;

/**
 *
 * @author Ng
 */
public class Model implements Concept {

    private List<Attribute> input;
    private List<Attribute> output;
    private String name;
    private String inputFileExt;
    private String outputFileExt;
    private String aspect;

    public String getAspect() {
        return aspect;
    }

    public void setAspect(String aspect) {
        this.aspect = aspect;
    }

    public List<Attribute> getInput() {
        return input;
    }

    public List<Attribute> getOutput() {
        return output;
    }

    public String getName() {
        return name;
    }

    public String getInputFileExt() {
        return inputFileExt;
    }

    public void setInput(List<Attribute> input) {
        this.input = input;
    }

    public void setOutput(List<Attribute> output) {
        this.output = output;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInputFileExt(String inputFileExt) {
        this.inputFileExt = inputFileExt;
    }

    public void setOutputFileExt(String outputFileExt) {
        this.outputFileExt = outputFileExt;
    }

    public String getOutputFileExt() {
        return outputFileExt;
    }
    
}
