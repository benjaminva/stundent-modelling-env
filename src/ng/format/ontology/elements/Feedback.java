/*Catedra: DASL4LTD
 *Autor: Benjamin Valdes Aguirre. 
 *Matricula: 882900   Carrera: DCC 
 *Correo Electronico: bvaldesa@itesm.mx 
 *Fecha de creacion: Nov 6, 2013
 *Fecha última modificiacion: Nov 6, 2013 
 *Nombre Archivo: Feedback
 *Plataforma: Java 
 *Descripción: 
 */
package ng.format.ontology.elements;

import jade.content.AgentAction;

/**
 *
 * @author Ng
 */
public class Feedback implements AgentAction {

    private boolean correct;
    private String correctAnswer;
    private int correctNum;

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public boolean isCorrect() {
        return correct;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public int getCorrectNum() {
        return correctNum;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public void setCorrectNum(int correctNum) {
        this.correctNum = correctNum;
    }
}
