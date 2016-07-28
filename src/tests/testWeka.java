/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.core.Instances;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayesUpdateable;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.converters.ConverterUtils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

/**
 *
 * @author NG
 */
public class testWeka {

    NaiveBayesUpdateable bsa;

    public Object updateBayes(String file, Object currentBayes) {
        try {
            DataSource source = new DataSource(file);
            Instances b = source.getDataSet();
            b.setClassIndex(b.numAttributes() - 1);

            //Create a Tokenizer to separate the text into n-grams
            NGramTokenizer ngram = new NGramTokenizer();
            ngram.setDelimiters("\\W"); // separate by words
            ngram.setNGramMinSize(1); // min gram
            ngram.setNGramMaxSize(1); // max gram
            //Use the tokenizer to filter the dataset
            StringToWordVector stw = new StringToWordVector();
            stw.setTokenizer(ngram);
            stw.setWordsToKeep(10000000);
            stw.setInputFormat(b);
            String temp[] = stw.getOptions();
            temp[7] = "-O"; // muysterious option -O which I can't find the function that sets it. 
            stw.setOptions(temp);
            //Filtered dataset by token
            Instances Filtered_data = Filter.useFilter(b, stw);

            //weka.filters.supervised.attribute.AttributeSelection -c 1 -E weka.attributeSelection.InfoGainAttributeEval -S "weka.attributeSelection.Ranker -T 0.0" -i SFU_Review_Corpus.vector.uni.arff -o SFU_Review_Corpus.vector.uni.ig0.arff 
            //weka.filters.supervised.attribute.AttributeSelection -c 1 -E weka.attributeSelection.InfoGainAttributeEval  
            weka.filters.supervised.attribute.AttributeSelection ats = new weka.filters.supervised.attribute.AttributeSelection();
            InfoGainAttributeEval igae = new InfoGainAttributeEval();
            ats.setEvaluator(igae);

            //-S "weka.attributeSelection.Ranker -T 0.0"
            Ranker rank = new Ranker();
            rank.setThreshold(0.0);
            ats.setSearch(rank);

            // -i SFU_Review_Corpus.vector.uni.arff 
            ats.setInputFormat(Filtered_data); // Filtered data contains the dataset that would be in SFU_Review_Corpus.vector.uni.arff 
            Instances filtered = Filter.useFilter(Filtered_data, ats);
            // System.out.println(filtered); // print instead of -o SFU_Review_Corpus.vector.uni.ig0.arff 
            // train NaiveBayes
            NaiveBayesUpdateable nb;
            if (currentBayes == null || currentBayes.equals("")) {
                nb = new NaiveBayesUpdateable();
                nb.buildClassifier(filtered);
            } else {
                nb = (NaiveBayesUpdateable) currentBayes;
            }
            int i = 0;
            while (i < filtered.numInstances()) {
                nb.updateClassifier(filtered.instance(i));
                i++;
            }
            return nb;
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
        return null;
    }

    public void writeTo(String path, String fileName, String text) {
        PrintWriter bf;
        try {
            bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(path + "\\" + fileName, true)));

            bf.append(text + System.getProperty("line.separator"));
            bf.close();
        } catch (IOException ex) {
            Logger.getLogger(testWeka.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public double queryBayes(String PATH, String file) {
        try {
            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(PATH + "\\temp.arff", false)));
            bf.write(file + System.getProperty("line.separator"));
            bf.close();

            ConverterUtils.DataSource source = new ConverterUtils.DataSource(PATH + "\\temp.arff");
            Instances b = source.getDataSet();
            b.setClassIndex(b.numAttributes() - 1);
            NaiveBayesUpdateable nb = (NaiveBayesUpdateable) bsa;
            int i = 0;
            double pred = 0;
            for (; i < b.numInstances(); i++) {
//                System.out.println(i + " instance is " + b.instance(i));
                pred = nb.classifyInstance(b.instance(i));
                System.out.print("ID: " + b.instance(i).value(0));
                System.out.print(", actual: " + b.classAttribute().value((int) b.instance(i).classValue()));
                System.out.println(", predicted: " + b.classAttribute().value((int) pred));
            }
            return nb.classifyInstance(b.instance(i - 1));
        } catch (Exception e) {
            e.printStackTrace();
            return -641.0;
        }
    }

    public Object updateSVM(String file, Object currentModel) {
        try {
            DataSource source = new DataSource(file);
            Instances b = source.getDataSet();
            b.setClassIndex(b.numAttributes() - 1);

            //Create a Tokenizer to separate the text into n-grams
            NGramTokenizer ngram = new NGramTokenizer();
            ngram.setDelimiters("\\W"); // separate by words
            ngram.setNGramMinSize(1); // min gram
            ngram.setNGramMaxSize(1); // max gram
            //Use the tokenizer to filter the dataset
            StringToWordVector stw = new StringToWordVector();
            stw.setTokenizer(ngram);
            stw.setWordsToKeep(10000000);
            stw.setInputFormat(b);
            String temp[] = stw.getOptions();
            temp[7] = "-O"; // muysterious option -O which I can't find the function that sets it. 
            stw.setOptions(temp);
            //Filtered dataset by token
            Instances Filtered_data = Filter.useFilter(b, stw);

            //weka.filters.supervised.attribute.AttributeSelection -c 1 -E weka.attributeSelection.InfoGainAttributeEval -S "weka.attributeSelection.Ranker -T 0.0" -i SFU_Review_Corpus.vector.uni.arff -o SFU_Review_Corpus.vector.uni.ig0.arff 
            //weka.filters.supervised.attribute.AttributeSelection -c 1 -E weka.attributeSelection.InfoGainAttributeEval  
            weka.filters.supervised.attribute.AttributeSelection ats = new weka.filters.supervised.attribute.AttributeSelection();
            InfoGainAttributeEval igae = new InfoGainAttributeEval();
            ats.setEvaluator(igae);

            //-S "weka.attributeSelection.Ranker -T 0.0"
            Ranker rank = new Ranker();
            rank.setThreshold(0.0);
            ats.setSearch(rank);

            // -i SFU_Review_Corpus.vector.uni.arff 
            ats.setInputFormat(Filtered_data); // Filtered data contains the dataset that would be in SFU_Review_Corpus.vector.uni.arff 
            Instances filtered = Filter.useFilter(Filtered_data, ats);
            // System.out.println(filtered); // print instead of -o SFU_Review_Corpus.vector.uni.ig0.arff 
            // train NaiveBayes
            SMO smo;
            if (currentModel == null || currentModel.equals("")) {
                smo = new SMO();
            } else {
                smo = (SMO) currentModel;
            }
            //   System.out.println(smo);
            return smo;
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
        return null;
    }

    public void querySVM(String file, Object currentModel) {
        try {
            DataSource source = new DataSource(file);
            Instances b = source.getDataSet();
            b.setClassIndex(b.numAttributes() - 1);

            //Create a Tokenizer to separate the text into n-grams
            NGramTokenizer ngram = new NGramTokenizer();
            ngram.setDelimiters("\\W"); // separate by words
            ngram.setNGramMinSize(1); // min gram
            ngram.setNGramMaxSize(1); // max gram
            //Use the tokenizer to filter the dataset
            StringToWordVector stw = new StringToWordVector();
            stw.setTokenizer(ngram);
            stw.setWordsToKeep(10000000);
            stw.setInputFormat(b);
            String temp[] = stw.getOptions();
            temp[7] = "-O"; // muysterious option -O which I can't find the function that sets it. 
            stw.setOptions(temp);
            //Filtered dataset by token
            Instances Filtered_data = Filter.useFilter(b, stw);

            //weka.filters.supervised.attribute.AttributeSelection -c 1 -E weka.attributeSelection.InfoGainAttributeEval -S "weka.attributeSelection.Ranker -T 0.0" -i SFU_Review_Corpus.vector.uni.arff -o SFU_Review_Corpus.vector.uni.ig0.arff 
            //weka.filters.supervised.attribute.AttributeSelection -c 1 -E weka.attributeSelection.InfoGainAttributeEval  
            weka.filters.supervised.attribute.AttributeSelection ats = new weka.filters.supervised.attribute.AttributeSelection();
            InfoGainAttributeEval igae = new InfoGainAttributeEval();
            ats.setEvaluator(igae);

            //-S "weka.attributeSelection.Ranker -T 0.0"
            Ranker rank = new Ranker();
            rank.setThreshold(0.0);
            ats.setSearch(rank);

            // -i SFU_Review_Corpus.vector.uni.arff 
            ats.setInputFormat(Filtered_data); // Filtered data contains the dataset that would be in SFU_Review_Corpus.vector.uni.arff 
            Instances filtered = Filter.useFilter(Filtered_data, ats);

            SMO smo = (SMO) currentModel;
            for (int i = 0; i < filtered.numInstances(); i++) {
                System.out.println(i + " instance value is " + smo.classifyInstance(filtered.instance(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, Exception {
       /* testWeka tw = new testWeka();
        Object model = "";
        model = tw.updateBayes("C:\\Users\\NG\\Dropbox\\Tutors\\SFU_Review_Corpus.arff", model);
        tw.bsa = (NaiveBayesUpdateable) model;
        System.out.println(tw.bsa);

        // trees.J48 -C 0.25 -M 2 -t /some/where/train.arff -d /other/place/j48.model
        weka.core.SerializationHelper.write("\\Users\\NG\\Dropbox\\Tutors\\bsatest.model", tw.bsa);
        model = null;
        tw.bsa = (NaiveBayesUpdateable) weka.core.SerializationHelper.read("\\Users\\NG\\Dropbox\\Tutors\\bsatest.model");
        // trees.J48 -l /other/place/j48.model -T /some/where/test.arff
    //    System.out.println(tw.bsa);
        Path path = Paths.get("C:\\Users\\NG\\Dropbox\\Tutors\\SFU_Review_Corpus.arff");
        List<String> lines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
        String query = "";
        boolean data = false;
        for (String line : lines) {
            if (data == true) {
                line = line.replace("no", "?");
                line = line.replace("yes", "?");
            }
            if (line.contains("data")) {
                data = true;
            }
            query = query + line + "\n";
        }
        tw.queryBayes("C:\\Users\\NG\\Dropbox\\Tutors\\", query);

        query = "@relation C__Users_NG_Dropbox_Tutors_Emotion\n"
                + "\n"
                + "@attribute text string\n"
                + "@attribute @@class@@ {Happy,Negative,Positive,Sad}\n"
                + "\n"
                + "@data\n"
                + "\n"
                + "\n" + "\'"
                + "333 instance is creo que este es uno de los mejores albumes de la banda australiana "
                + "a sido mal tratado por la critica (sinplemente porque despues del back in black que es "
                + "su obra maestra la critica esperaba mas) pero es todo un pedazo de album uno de mis preferidos"
                + " no solo de esta banda si no de toda mi discoteca. lo aprecio proque creo que tiene un "
                + "gran sonido y una gran fuerza a pesar de los años que tiene el album pero sin duda es uno de "
                + "los mejores que podria considerarse como un clasico ya si no fuese porque el clasico del grupo"
                + "es el back in black yo votaria por este si teneis la ocasion de escucharlo no os arrepentireis \',?";
        tw.queryBayes("C:\\Users\\NG\\Dropbox\\Tutors\\", query);
*/
    }
}
