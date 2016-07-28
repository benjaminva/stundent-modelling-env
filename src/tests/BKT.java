package tests;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a modification from Baker original bruteforce approach from
 * Baker, R.S.J.d., Corbett, A.T., Aleven, V. (2008) More Accurate Student
 * Modeling Through Contextual Estimation of Slip and Guess Probabilities in
 * Bayesian Knowledge Tracing. Proceedings of the 9th International Conference
 * on Intelligent Tutoring Systems, 406-415.
 */
public class BKT {

    /**
     * This class expects data sorted on Skill and then on Student in the below
     * mentioned format num	lesson	student	skill	cell right	eol 1
     * Z3.Three-FactorZCros2008	student102	META-DETERMINE-DXO	cell	0	eol
     *
     */
    public String students_[] = new String[27600];// Number of instances
    public String skill_[] = new String[27600];
    public double right_[] = new double[27600];
    public HashMap<String, double[]> student_models = new HashMap<String, double[]>();
    public HashMap<String, String> skill_paramenters = new HashMap<String, String>();
    public int skillends_[] = new int[15];//Number of Skills
    public double params_zero_[] = {0, 0.1, 0.3, 0.3};

    public int skillnum = -1;

    public boolean lnminus1_estimation = false;
    public boolean bounded = true;
    public boolean L0Tbounded = false;

    public StreamTokenizer create_tokenizer(String infile) {

        try {
            StreamTokenizer st = new StreamTokenizer(new FileReader(infile));
            st.wordChars(95, 95);
            return st;
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        return null;
    }

    public void read_in_data(StreamTokenizer st_) {
        int actnum = 0;
        try {
            int tt = 724;
            skillnum = -1;
            String prevskill = "FLURG";

            tt = st_.nextToken();
            tt = st_.nextToken();
            tt = st_.nextToken();
            tt = st_.nextToken();
            tt = st_.nextToken();
            tt = st_.nextToken();
            tt = st_.nextToken();

            while (tt != StreamTokenizer.TT_EOF) {
                tt = st_.nextToken(); // num

                tt = st_.nextToken(); // lesson
                // System.out.print(st_.sval+"\t");

                tt = st_.nextToken();
                students_[actnum] = st_.sval;
                // System.out.print(students_[actnum]+"\t");

                tt = st_.nextToken();
                skill_[actnum] = st_.sval;
                // System.out.print(skill_[actnum]+"\t");

                tt = st_.nextToken(); // cell

                tt = st_.nextToken();
                right_[actnum] = st_.nval;
                // System.out.println(right_[actnum]);

                tt = st_.nextToken(); // eol

                // System.out.println(slip_[actnum]);
                actnum++;
            }
        } catch (Exception e) {
            System.out.println(actnum);
            e.printStackTrace();
        }

    }

    public double Ln(double prevL,
            double G, double S, double trans, double right) {
        double prevLgivenresult;

        if (right == 1.0) {
            prevLgivenresult = ((prevL * (1.0 - S)) / ((prevL * (1 - S)) + ((1.0 - prevL) * (G))));
        } else {
            prevLgivenresult = ((prevL * (S)) / ((prevL * (S)) + ((1.0 - prevL) * (1.0 - G))));
        }

        return prevLgivenresult + (1.0 - prevLgivenresult) * trans;
    }

    public double likelihoodcorrect(double prevL,
            double G, double S, double trans) {
        return (prevL * (1.0 - S)) + ((1.0 - prevL) * G);
    }

    public void computelzerot(String infile_) {
        StreamTokenizer st_ = create_tokenizer(infile_);
        read_in_data(st_);
        for (int i = 0; students_[i] != null && i < students_.length; i++) {
            double params[] = student_models.get(students_[i] + ":" + skill_[i]);
            if (params == null) {
                if (skill_paramenters.get(skill_[i]) != null) {
                    //  System.out.println("skill:  " +skill_[i] + " values " +skill_paramenters.get(skill_[i]) );
                    String temp[] = skill_paramenters.get(skill_[i]).split(" ");
                    params = new double[4];
                    for (int j = 0; j < 4; j++) {
                        params[j] = Double.parseDouble(temp[j]);
                    }
                    //                  System.out.println("input params:  " + params[0] + " " + params[1] + " " + params[2] + " " + params[3] + "  ");
                } else {
                    params = params_zero_;
                }
                student_models.put(students_[i] + ":" + skill_[i], params);
            }
            //          System.out.println("input params:  " + params[0] + " " + params[1] + " " + params[2] + " " + params[3] + "  "+right_[i]+" ");
            double learningState = Ln(params[0], params[1], params[2], params[3], right_[i]);
            params[0] = learningState;
            student_models.put(students_[i] + ":" + skill_[i], params);
        }
    }

    public void loadStudentModels(String studentTable) {
        String studentModels[] = studentTable.substring(1, studentTable.length() - 1).replace("=", " ").split(", ");
        for (String studentModel : studentModels) {
            String temp[] = studentModel.split(" ");
            double params[] = {Double.parseDouble(temp[1]), Double.parseDouble(temp[2]), Double.parseDouble(temp[3]), Double.parseDouble(temp[4])};
            student_models.put(temp[0], params);
        }
    }

    public void setDefaultParameters(String skillTable) {
        String skills[] = skillTable.substring(1, skillTable.length() - 1).replace("=", " ").split(", ");
        for (String skill : skills) {
            String temp[] = skill.split(" ");
            skill_paramenters.put(temp[0], temp[1] + " " + temp[2] + " " + temp[3] + " " + temp[4]);
        }
    }

    public double[] getDefaultParameter(String key) {
        String id[] = key.split(":");
        String temp[] = skill_paramenters.get(id[1]).split(" ");
        double params[] = {0.0, 0.0, 0.0, 0.0};
        for (int i = 0; i < temp.length; i++) {
            params[i] = Double.parseDouble(temp[i]);
        }
        return params;
    }

    public String studentModelToString(HashMap<String, double[]> studentTable) {
        String output = "{";
        double temp[];
        for (String key : studentTable.keySet()) {
            temp = studentTable.get(key);
            output += key + "=" + temp[0] + " " + temp[1] + " " + temp[2] + " " + temp[3] + ", ";
        }
        if (output.equals("{")) {
            output = "{}";
        } else {
            output = output.substring(0, output.length() - 2) + "}";
        }
        return output;
    }

    public static void main(String args[]) throws IOException {
        String infile_ = "data\\dataset train BKT.txt";//Needs to be tab delimited
        BKT m = new BKT();
        m.setDefaultParameters("{discountWITHpercent_of=0.3000000000000001 0.2900000000000001 0.01 0.01, integers=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, finding_percents=0.5030000000000002 0.24800000000000008 0.038000000000000006 0.0010000000000000009, multiplicationWITHsubstitution=0.0010000000000000009 0.0010000000000000009 0.1 0.0010000000000000009, divide_decimals=0.6110000000000003 0.19400000000000003 0.082 0.0010000000000000009, equation_solving=0.5020000000000002 0.26100000000000007 0.0010000000000000009 0.9890000000000007, making_sense_of_expressions_and_equations=0.5710000000000003 0.22700000000000006 0.003000000000000001 0.0010000000000000009, interpreting_linear_equations=0.7280000000000004 0.03400000000000001 0.03900000000000001 0.44900000000000023, inequality_solving=0.8810000000000006 0.03800000000000001 0.0010000000000000009 0.9890000000000007, fraction_division=0.21700000000000005 0.25500000000000006 0.048000000000000015 0.0010000000000000009, additionWITHsubtraction=0.47400000000000025 0.2850000000000001 0.027000000000000003 0.0010000000000000009, interpreting_linear_equationsWITHslope=0.6000000000000003 0.03 0.02 0.01, mode=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, fraction_multiplicationWITHordering_fractions=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, multiplicationWITHunit_conversionWITHevaluating_functions=0.0010000000000000009 0.0010000000000000009 0.1 0.0010000000000000009, division=0.7740000000000005 0.07200000000000001 0.0010000000000000009 0.9890000000000007, area=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, fraction_Decimals_Percents=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, multiplication=0.8110000000000005 0.25700000000000006 0.0010000000000000009 0.9890000000000007, fractions=0.8220000000000005 0.12099999999999998 0.08000000000000002 0.0010000000000000009, evaluating_functions=0.7050000000000004 0.034 0.0010000000000000009 0.9890000000000007, mean=0.9010000000000006 0.17800000000000002 0.033 0.0010000000000000009, algebraic_manipulation=0.4220000000000002 0.2870000000000001 0.03900000000000001 0.0010000000000000009, equivalent_fractions_decimals_percentsWITHprobability=0.7500000000000004 0.21000000000000005 0.07 0.01, meaning_of_pi=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, equation_solvingWITHperimeterWITHcongruence=0.3000000000000001 0.2900000000000001 0.01 0.01, congruence=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, linear_area_volume_conversion=0.3000000000000001 0.2900000000000001 0.01 0.01, graph_shapeWITHqualitative_graph_interpretation=0.3000000000000001 0.2900000000000001 0.01 0.01, multiplying_decimals=0.6210000000000003 0.0010000000000000009 0.0010000000000000009 0.0010000000000000009, interpreting_numberline=0.6730000000000004 0.18700000000000003 0.04200000000000001 0.0010000000000000009, fraction_multiplication=0.8430000000000005 0.22500000000000006 0.065 0.0010000000000000009, inducing_functions=0.8610000000000005 0.014000000000000005 0.0010000000000000009 0.9890000000000007, exponents=0.9290000000000006 0.099 0.029000000000000005 0.0010000000000000009, meanWITHmedian=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, graph_shape=0.7650000000000005 0.002000000000000001 0.0010000000000000009 0.9890000000000007, addition=0.7940000000000005 0.033 0.0010000000000000009 0.9890000000000007, multiplicationWITHinducing_functions=0.0010000000000000009 0.0010000000000000009 0.1 0.0010000000000000009, discount=0.48000000000000026 0.22000000000000006 0.03 0.01, equivalent_fractions_decimals_percents=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, median=0.9060000000000006 0.126 0.067 0.0010000000000000009, combinatorics=0.6620000000000004 0.18000000000000002 0.0010000000000000009 0.9890000000000007, circle_graph=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, least_common_multiple=0.7500000000000004 0.26000000000000006 0.02 0.01, equation_concept=0.9020000000000006 0.10200000000000001 0.07300000000000001 0.0010000000000000009, equation_solvingWITHinducing_functions=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, inducing_functionsWITHequation_concept=0.9890000000000007 0.2990000000000001 0.0010000000000000009 0.0010000000000000009, interpreting_numberlineWITHinequality_solving=0.25000000000000006 0.26000000000000006 0.02 0.01, exponentsWITHmultiplying_positive_negative_numbers=0.12099999999999998 0.07800000000000001 0.064 0.0010000000000000009, multiplicationWITHreading_graph=0.0010000000000000009 0.0010000000000000009 0.1 0.0010000000000000009}");
        m.computelzerot(infile_);
        String a = m.studentModelToString(m.student_models);
        try {
            PrintWriter af;
            af = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter("data\\stundentmodels.txt", false)));
            af.append(a);
            af.close();
        } catch (IOException ex) {
            Logger.getLogger(computeKTparamsAll.class.getName()).log(Level.SEVERE, null, ex);
        }
        String testfile_ = "data\\testData\\dataset test BKT.txt";//Needs to be tab delimited
        //read data files from data
        Path path = Paths.get(testfile_.toString());
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        a = "";
        for (String line : lines) {
            String inputs[] = line.split("\t");
            double[] params = m.student_models.get(inputs[2] + ":" + inputs[3]);
            if (params == null) {
                if (m.skill_paramenters.get(inputs[3]) != null) {
                    String temp[] = m.skill_paramenters.get(inputs[3]).split(" ");
                    params = new double[4];
                    for (int j = 0; j < 4; j++) {
                        params[j] = Double.parseDouble(temp[j]);
                    }
                } else {
                    params = m.params_zero_;
                }
            }
            double prediction = m.likelihoodcorrect(params[0], params[1], params[2], params[3]);
            a = a + "correctlikelyhood:" + prediction + "\n" + "learningstate:" + params[0]+" " +inputs[2] + inputs[3]+ "\n";
        }
        try {
            PrintWriter bf;
            bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter("data\\answers to query.txt", false)));
            bf.append(a);
            bf.close();
        } catch (IOException ex) {
            Logger.getLogger(computeKTparamsAll.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.print(a);
        m.student_models.clear();
    }
}
