/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.filters.unsupervised.attribute.StringToWordVector;

/**
 *
 * @author NG
 */
public class BayesUpdateQuery {

    public static void main(String[] args) {
        try {
            MyFilteredLearner learner;
            learner = new MyFilteredLearner();
            learner.loadDataset("C:\\Users\\NG\\Dropbox\\Tutors\\Inverted_SFU_Review_Corpus.arff");
            // Evaluation must be done before training
            // More info in: http://weka.wikispaces.com/Use+WEKA+in+your+Java+code
            learner.evaluate();
            learner.learn();
            learner.saveModel("C:\\Users\\NG\\Dropbox\\Tutors\\YesNo.model");
            Object nba = learner.classifier;
            
            
            MyFilteredClassifier Myclassifier;
            
            Myclassifier = new MyFilteredClassifier();
            Myclassifier.load("C:\\Users\\NG\\Dropbox\\Tutors\\smstest.txt");
            Myclassifier.loadModel("C:\\Users\\NG\\Dropbox\\Tutors\\YesNo.model");
            
            Myclassifier.makeInstance();
            Myclassifier.classify();
            FilteredClassifier fc = (FilteredClassifier)nba;
            System.out.println("persistance test "+ fc.classifyInstance(Myclassifier.instances.firstInstance()));
        } catch (Exception ex) {
            Logger.getLogger(BayesUpdateQuery.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static class MyFilteredClassifier {

        /**
         * String that stores the text to classify
         */
        String text;
        /**
         * Object that stores the instance.
         */
        Instances instances;
        /**
         * Object that stores the classifier.
         */
        FilteredClassifier classifier;
        
        /**
         * This method loads the text to be classified.
         *
         * @param fileName The name of the file that stores the text.
         */
        public void load(String fileName) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(fileName));
                String line;
                text = "";
                while ((line = reader.readLine()) != null) {
                    text = text + " " + line;
                }
                System.out.println("===== Loaded text data: " + fileName + " =====");
                reader.close();
                System.out.println(text);
            } catch (IOException e) {
                System.out.println("Problem found when reading: " + fileName);
            }
        }

        /**
         * This method loads the model to be used as classifier.
         *
         * @param fileName The name of the file that stores the text.
         */
        public void loadModel(String fileName) {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
                Object tmp = in.readObject();
                classifier = (FilteredClassifier) tmp;
                in.close();
                System.out.println("===== Loaded model: " + fileName + " =====");
            } catch (Exception e) {
                // Given the cast, a ClassNotFoundException must be caught along with the IOException
                System.out.println("Problem found when reading: " + fileName);
            }
        }

        /**
         * This method creates the instance to be classified, from the text that
         * has been read.
         */
        public Instances makeInstance() {
            // Create the attributes, class and text
            FastVector fvNominalVal = new FastVector(2);
            fvNominalVal.addElement("yes");//spam
            fvNominalVal.addElement("no");//ham
            Attribute attribute1 = new Attribute("class", fvNominalVal);
            Attribute attribute2 = new Attribute("text", (FastVector) null);
            // Create list of instances with one element
            FastVector fvWekaAttributes = new FastVector(2);
            fvWekaAttributes.addElement(attribute1);
            fvWekaAttributes.addElement(attribute2);
            instances = new Instances("Test relation", fvWekaAttributes, 1);
            // Set class index
            instances.setClassIndex(0);
            // Create and add the instance
            DenseInstance instance = new DenseInstance(2);
            instance.setValue(attribute2, text);
            // Another way to do it:
            // instance.setValue((Attribute)fvWekaAttributes.elementAt(1), text);
            instances.add(instance);
            System.out.println("===== Instance created with reference dataset =====");
            System.out.println(instances);
            return instances;
        }

        /**
         * This method performs the classification of the instance. Output is
         * done at the command-line.
         */
        public void classify() {
            try {
                double pred = classifier.classifyInstance(instances.instance(0));
                System.out.println("===== Classified instance =====");
                System.out.println("Class predicted: " + instances.classAttribute().value((int) pred));
            } catch (Exception e) {
                System.out.println("Problem found when classifying the text");
            }
        }

    }

    static class MyFilteredLearner {

        /**
         * Object that stores training data.
         */
        Instances trainData;
        /**
         * Object that stores the filter
         */
        StringToWordVector filter;
        /**
         * Object that stores the classifier
         */
        FilteredClassifier classifier;

        /**
         * This method loads a dataset in ARFF format. If the file does not
         * exist, or it has a wrong format, the attribute trainData is null.
         *
         * @param fileName The name of the file that stores the dataset.
         */
        public void loadDataset(String fileName) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(fileName));
                ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader);
                trainData = arff.getData();
                System.out.println("===== Loaded dataset: " + fileName + " =====");
                reader.close();
            } catch (IOException e) {
                System.out.println("Problem found when reading: " + fileName);
            }
        }

        /**
         * This method evaluates the classifier. As recommended by WEKA
         * documentation, the classifier is defined but not trained yet.
         * Evaluation of previously trained classifiers can lead to unexpected
         * results.
         */
        public void evaluate() {
            try {
                trainData.setClassIndex(0);
                filter = new StringToWordVector();
                filter.setAttributeIndices("last");
                classifier = new FilteredClassifier();
                classifier.setFilter(filter);
                classifier.setClassifier(new NaiveBayes());
                Evaluation eval = new Evaluation(trainData);
                eval.crossValidateModel(classifier, trainData, 4, new Random(1));
                System.out.println(eval.toSummaryString());
                System.out.println(eval.toClassDetailsString());
                System.out.println("===== Evaluating on filtered (training) dataset done =====");
            } catch (Exception e) {
                System.out.println("Problem found when evaluating");
            }
        }

        /**
         * This method trains the classifier on the loaded dataset.
         */
        public void learn() {
            try {
                trainData.setClassIndex(0);
                filter = new StringToWordVector();
                filter.setAttributeIndices("last");
                classifier = new FilteredClassifier();
                classifier.setFilter(filter);
                classifier.setClassifier(new NaiveBayes());
                classifier.buildClassifier(trainData);
                // Uncomment to see the classifier
                // System.out.println(classifier);
                System.out.println("===== Training on filtered (training) dataset done =====");
            } catch (Exception e) {
                System.out.println("Problem found when training");
            }
        }

        /**
         * This method saves the trained model into a file. This is done by
         * simple serialization of the classifier object.
         *
         * @param fileName The name of the file that will store the trained
         * model.
         */
        public void saveModel(String fileName) {
            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
                out.writeObject(classifier);
                out.close();
                System.out.println("===== Saved model: " + fileName + " =====");
            } catch (IOException e) {
                System.out.println("Problem found when writing: " + fileName);
            }
        }

    }
}
