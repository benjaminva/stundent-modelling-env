package tests;

import net.sf.jni4net.Bridge;
import java.io.*;
import java.util.*;
import java.io.IOException;
import gnututor.Tutorlib;
import gnututor.DenseTerm;
import gnututor.LSASpace;

public class Tutor {

    static Tutorlib tlib;
    static LSASpace space;
    static String[] cosines;

    public static void setup() throws IOException {
        Bridge.init();
        Bridge.LoadAndRegisterAssemblyFrom(new java.io.File("GnuTutor.j4n.dll"));
        tlib = new Tutorlib();
        space = new LSASpace();

        cosines = new String[100];
        for (int i = 0; i < 100; i++) {
            cosines[i] = "";
        }
        FileInputStream inputStream = null;

        /**
         * loading file to LSASpace in tlib *
         */
        Scanner sc = null;
        try {
            inputStream = new FileInputStream("termHash.txt");
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                String key = sc.nextLine(); 				//get key of hash
                double weight = Double.parseDouble(sc.nextLine()); 				// the weight
                int index = Integer.parseInt(sc.nextLine()); 				// the index
                double[] vector = new double[300];
                for (int i = 0; i < 300; i++) {
                    vector[i] = Double.parseDouble(sc.nextLine());  // the values of vector
                }
                DenseTerm term = new DenseTerm(weight, vector);
                term.setIndex(index);

                space.addTermToHash(key, term);
            }

            // note that Scanner suppresses exceptions
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (sc != null) {
                sc.close();
            }
        }
        tlib.setLSASpace(space);
    }

    public static String language(String studentMove) {
        return tlib.LanguageAnalysis(studentMove);
    }

    public static String[] cosine(String studentMove) {
        return tlib.CalculateCosines(studentMove, cosines);
    }

}
