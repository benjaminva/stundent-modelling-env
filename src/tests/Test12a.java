/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author NG
 */
public class Test12a {

    public static void main(String[] args) {
        Test12a t = new Test12a();
        t.loadTable();
        t.processExternalFile(Paths.get("GNU.txt"));
        t.processExternalFile(Paths.get("bkt.txt"));
        t.processExternalFile(Paths.get("sa.txt"));
    }

    private ConcurrentHashMap<String, String> answers_hash = new ConcurrentHashMap();

    private void loadTable() {
        String gnu = "data\\matches\\GNU.txt";
        String bkt = "data\\matches\\bkt.txt";
        String sa = "data\\matches\\sa.txt";
        Path path = Paths.get(gnu);
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i = i + 3) {
                answers_hash.put(lines.get(i + 1), lines.get(i) + " " + lines.get(i + 2));
            }
            path = Paths.get(bkt);
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i = i + 2) {
                answers_hash.put(lines.get(i + 1), lines.get(i));
            }
            path = Paths.get(sa);
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                answers_hash.put(lines.get(i).split("\t")[0], lines.get(i).split("\t")[1]);
            }
        } catch (IOException ex) {

        }
    }

    public void processExternalFile(Path path) {
        try {
            String gnu = "GNU.txt", bkt = "bkt.txt", sa = "sa.txt", file = path.toString(), expected = "";
            int num = 0;
            path = Paths.get("data\\output\\" + path.toString());
            //System.out.println(System.getProperty("user.dir"));
            boolean result = false;
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (file.equals(gnu)) {
                //languageExpression:metacom
                //sentence:Ok
                //semanticCosine: 0.0849120827561663 0.0541472336110534      
                // fix format space
                expected = answers_hash.get(lines.get(1));
                if (expected != null && expected.equals(lines.get(0) + " " + lines.get(2))) {
                    result = true;
                }
            } else if (file.equals(bkt)) {
                //correctlikelyhood:0.6314760000000001
                //learningstate:0.5020000000000002: Stu_6081594975a764c8e3a691fa2b3a321dequation_solving
                //fix formatremove:
                expected = answers_hash.get(lines.get(1));
                if (expected != null && expected.equals(lines.get(0))) {
                    result = true;
                }
            } else if (file.equals(sa)) {
                //sentiment:Los	no
                //Los: no
                String[] temp = lines.get(0).split(":")[1].split("\t");
                expected = answers_hash.get(temp[0]);
                if (expected != null && expected.equals(temp[1])) {
                    result = true;
                }
            }
            //feedback:false:rat:12
            path = Paths.get("data\\input\\" + file);
            Files.write(path, ("\nfeedback:" + result + ":" + expected + ":" + "12").getBytes(), StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.print(ex);
        }
    }
}
