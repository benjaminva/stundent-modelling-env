/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author NG
 */
public class SortF {

    public String sortFileBySkill(String file, String out) {
        Path path = Paths.get(file);
        List<String> lines;
        HashMap <String, String> mapA = new HashMap<String, String>();
        String input = "";
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String skills[] = new String[lines.size()];
            int i = 0;
            for (String line : lines) {
                String temp[] = line.split("\t");
                mapA.put(temp[3],temp[3]);
                i++;
            }
            skills = mapA.keySet().toArray(new String[0]);
            Arrays.sort(skills);
            List<String> used = new ArrayList<String>();
            for (i = 0; i < skills.length; i++) {
                for (String line : lines) {
                    String temp[] = line.split("\t");
                    if (skills[i].equals(temp[3]) && !used.contains(line)) {
                        used.add(line);
                        if (temp[2].equals("Stu_996a7fa078cc36c46d02f9af3bef918b") && temp[3].equals("additionWITHsubtraction")) {
                            System.out.println(line); //1965
                            System.out.println(used.get(1081));
                            System.out.println(skills[i]);
                        }
                    }
                }
            }
            Path output = Paths.get(out);
            Charset charset = StandardCharsets.UTF_8;
            Files.write(output, used, charset, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
        }
        return file;
    }

    public static void main(String args[]) {
        SortF sort = new SortF();
        sort.sortFileBySkill("data\\bruteforcetorcuata\\temp.txt", "data\\bruteforcetorcuata\\temp2.txt");
    }

}
