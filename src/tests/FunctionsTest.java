/*Catedra: DASL4LTD
 *Autor: Benjamin Valdes Aguirre. 
 *Matricula: 882900   Carrera: DCC 
 *Correo Electronico: bvaldesa@itesm.mx 
 *Fecha de creacion: Oct 4, 2013
 *Fecha última modificiacion: Oct 4, 2013 
 *Nombre Archivo: FunctionsTest
 *Plataforma: Java 
 *Descripción: 
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Ng
 */
public class FunctionsTest {

    static String PATH = "data\\abc";
// for explanations regarding user32 methods http://www.webtropy.com/articles/art9-2.asp?lib=user32.dll
    static Object currentModel;

 
    public static void main(String[] args) throws IOException {
        Tutor.setup();
        System.out.println("NG is so Happy");
        System.out.println(Tutor.language("NG is so Happy"));
        String[] answers = Tutor.cosine("NG is so Happy");
        for (int i =0;i <answers.length ; i++){
            System.out.println(answers[i]);
        }
        
        System.out.println("coffe is a bevarage from Ethiopia");
        System.out.println(Tutor.language("coffe is a bevarage from Ethiopia"));
        answers = Tutor.cosine("coffe is a bevarage from Ethiopia");
        for (int i =0;i <answers.length ; i++){
            System.out.println(answers[i]);
        }/*
         String a = "rateleft,0,rateright,0,ratetotal,0:totalKeysPressed,84,differentKeysPressed,1:"
         + "[ \"Bo�te de r�ception (3) - benjaminva@gmail.com - Gmail - Mozilla Firefox\"=3331,  \"UserModellingEnvironment - NetBeans IDE 7.4\"=00593]:\n";
         String[] apps = a.split(":")[2].split(",");
         List<String> listApps = new ArrayList();
         listApps.add("ss  sdada");
         listApps.add("sssd ada");
         listApps.add("sss d  ada");
         System.out.println(listApps.toString().replace(", ","\",\"asd-").replace("[","{\"asd-").replace("]", "\"}"));
         for (int i = 0; i < apps.length; i++) {
         int num = Integer.parseInt(apps[i].replaceAll("(.*=)(\\d+)(.*)", "$2"));
         String temp = apps[i].replaceAll("(.*\")(.*)(\".*)", "$2");
         }

         System.out.println(apps);
         if (a.matches(".*rateleft,\\d+.*")) {
         System.out.println("yes");
         } else {
         System.out.println("no");
         }
         */
 //       FunctionsTest t = new FunctionsTest();
        // it will send a question through the ask protocol which will return the PATH of the recorded model in compliance to the requirements specified in the contents of the message
        // addBehaviour(new askBehaviour());
        //      String source = "data\\gathermousekeyappstorcuata"; // getInfo
        // with the PATH the agent may choose to get all data ever recorded or just the last update
        //    List<File> data = new ArrayList<File>();
        //  data.add(new File(source));

        //open file or model for training
        //currentModel = t.loadFromFile(t.train(t.selectFiles(data, 1)));
        //  int arg[] = {200, 20, 35, 54, 10, 432};
        // String key = t.checkCase(arg);
        //returns size of window - positionof window : [inner the tab of the app] - name of the app
//        (new FunctionsTest()).cuchi();
    }

    /**
     * Selects a case from the currentModel that has the values most similar to
     * the array
     *
     * @param testCase
     * @return the key to the most similar case in the hash
     */
    private String checkCase(int testCase[]) {
        String closestMatch = "";
        int lowestGrade = Integer.MAX_VALUE;
        HashMap<String, Case> map = (HashMap<String, Case>) currentModel;
        for (String key : map.keySet()) {
            int grade = 0;
            int memory[] = map.get(key).arg;
            for (int i = 0; i < testCase.length; i++) {
                grade = +Math.abs(memory[i] - testCase[i]);
            }
            if (grade < lowestGrade) {
                lowestGrade = grade;
                closestMatch = key;
            }
        }
        return closestMatch;
    }

    /**
     * returns a list containing lists of Strings that have the same name in
     * different folders, the returned lists are grouped by file name.
     *
     * @param data
     * @param previousInfo
     * @return
     */
    private List<List<String>> selectFiles(List<File> data, int previousInfo) {
        String date = "";
        List<List<String>> winners = new ArrayList<List<String>>();
        List<List<String>> listOfFiles = new ArrayList<List<String>>();
        if (previousInfo == 0) {
            for (File datum : data) {
                if (datum.isDirectory()) {
                    List<String> files = new ArrayList<String>();
                    for (String file : datum.list()) {
                        if (!file.contains("log.txt")) {
                            files.add(datum + "\\" + file); // use every file in the path except log
                        }
                    }
                    listOfFiles.add(files);
                }
            }
        } else {
            for (File datum : data) {
                listOfFiles.add(newTrainningSources(datum)); // use list of unsued files
            }
        }
        boolean empty = false;
        for (List<String> files : listOfFiles) { // chakc that data exists in all sources
            if (files.isEmpty()) {
                empty = true;
            }
        }
        if (empty == false) {
            // train with unused files from the path                    
            for (int i = 0; i < listOfFiles.get(0).size(); i++) {
                String fileName = listOfFiles.get(0).get(i);
                List<String> possibleWinners = new ArrayList<String>();
                possibleWinners.add(fileName);
                fileName = fileName.substring(fileName.length() - 10);
                for (int j = 1; j < listOfFiles.size(); j++) {
                    int k = 0;
                    boolean contains = false;
                    while (contains == false && k < listOfFiles.get(j).size()) {
                        String temp = listOfFiles.get(j).get(k);
                        if (fileName.equals(temp.substring(temp.length() - 10))) {
                            possibleWinners.add(temp);
                            contains = true;
                        }
                        k++;
                    }
                }
                if (possibleWinners.size() == listOfFiles.size()) {
                    winners.add(possibleWinners);
                }
            }
        }
        return winners;
    }

    /**
     * trains the model with the files in the directory specified by data if
     * overwrite is 1 the model will be trained from scratch and all of the
     * available files in data used. If overwrite is 0 the model will be loaded
     * and only new files in data will be used to updated.
     *
     * @param data is the directory of files that will be used as input data
     * @param previousInfo 0 if all info should be used, 1 inly use new info
     * @return
     */
    private File train(List<List<String>> winners) {
        try {
            if (currentModel == null) {
                HashMap<String, Case> map = new HashMap<String, Case>();
                currentModel = map;
            }
            HashMap<String, Case> map = (HashMap) currentModel;
            for (List<String> files : winners) {
                String date = files.get(0).substring(files.get(0).length() - 10);
                boolean badinput = false;
                for (String file : files) {
                    Path path = Paths.get(file);
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    for (String line : lines) {
                        if (!line.isEmpty()) {
                            /* ==== do something here to procces the lines to train ==========*/
                            line = (line.replace("\"", "")).replace(" ", "");

                            int rates[] = new int[6];
                            String arg[] = line.split(":");
                            // 0 [ JADE Remote Agent Management GUI"=2,  NetBeans IDE 7.0.1"=2]
                            // 1 totalKeysPressed,123,differentKeysPressed,12
                            // 2 rateleft,105,rateright,0,ratetotal,105
                            // 3 totalmovement,550                            

                            //select top arg
                            String temp[] = arg[0].substring(1, arg[0].length() - 1).split(",");
                            //bla-JADERemoteAgentManagementGUI"=2,ble-NetBeansIDE7.0.1"=2
                            // 0 bla-JADERemoteAgentManagementGUI"=2
                            // 1 ble-NetBeansIDE7.0.1"=2
                            int num = 0;
                            String generic = "", specific = "";
                            for (int i = 0; i < temp.length; i++) {
                                String app[] = temp[i].split("=");
                                if (Integer.parseInt(app[1]) > num) {
                                    num = Integer.parseInt(app[1]);
                                    String temp2[] = app[0].split("-");
                                    generic = temp2[0];
                                    specific = temp2[temp2.length - 1];
                                }
                            }
                            // 1 totalKeysPressed,123,differentKeysPressed,12       arg[1]
                            // 2 rateleft,105,rateright,0,ratetotal,105             arg[2]
                            // 3 totalmovement,550                                  arg[3]
                            temp = arg[1].split(",");
                            rates[0] = Integer.parseInt(temp[1]);
                            rates[1] = Integer.parseInt(temp[3]);
                            temp = arg[2].split(",");
                            rates[2] = Integer.parseInt(temp[1]);
                            rates[3] = Integer.parseInt(temp[3]);
                            rates[4] = Integer.parseInt(temp[5]);
                            temp = arg[3].split(",");
                            rates[5] = Integer.parseInt(temp[1]);

                            Case c = new Case(generic, specific, rates);
                            if (map.containsKey(c.generic)) {
                                Case memory = map.get(c.generic);
                                for (int i = 0; i < c.arg.length; i++) {
                                    memory.arg[i] = (memory.arg[i] * 70 + c.arg[i] * 30) / 100;
                                }
                                map.put(memory.generic, memory);
                            } else {
                                map.put(c.generic, c);
                            }
                            if (map.containsKey(c.generic + "-" + c.specific)) {
                                Case memory = map.get(c.generic + "-" + c.specific);
                                for (int i = 0; i < c.arg.length; i++) {
                                    memory.arg[i] = (memory.arg[i] * 70 + c.arg[i] * 30) / 100;
                                }
                                map.put(memory.generic + memory.specific, memory);
                            } else {
                                map.put(c.generic + "-" + c.specific, c);
                            }
                        }
                        /*=============================================================== */
                    }
                }
            }
            String input = "";
            for (String key : map.keySet()) {
                input += key;
                for (int i = 0; i < map.get(key).arg.length; i++) {
                    input += "," + map.get(key).arg[i];
                }
                input += ":";
            }
            writeTo(PATH, input, ".um");

            currentModel = map;

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            writeToLog(PATH, e.toString());
        }

        return getLastModel();
    }

    /**
     * checks for files that have not been used to train in the directory
     * data_path the file usedfiles.txt and adds the files to usedfiles.txt the
     * files are stored including their path
     *
     * @param data_path
     * @return list of strings
     */
    private List<String> newTrainningSources(File data_path) {
        List<String> missingFiles = new ArrayList<String>();
        if (data_path.isDirectory()) {
            try {
                Path path = Paths.get(PATH + "\\" + "filesused.txt");
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                for (String file : data_path.list()) {
                    if (!lines.contains(data_path + "\\" + file) && !file.equals("log.txt") && !file.equals("filesused.txt")) {
                        missingFiles.add(data_path + "\\" + file);
                    }
                }
                if (!missingFiles.isEmpty()) {
                    Files.write(path, missingFiles, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        // check that there is a file in the data_path(path of provider) that is not in a "usedfiles.txt" local to the PATH of this agent 
        return missingFiles;
    }

    /**
     * loads and Object from the specified file to the current model
     *
     * @param model
     * @return
     */
    private Object loadFromFile(File model) {
        Path path = Paths.get(model.getAbsolutePath());
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String cases[] = lines.get(0).split(":");
            HashMap<String, Case> map = new HashMap<String, Case>();
            for (String c : cases) {
                String temp[] = c.split(",");
                int arg[] = new int[temp.length - 1];
                for (int i = 0; i < temp.length - 1; i++) {
                    arg[i] = Integer.parseInt(temp[i + 1]);
                }
                Case newCase = null;
                if (temp[0].contains("-")) {
                    newCase = new Case(temp[0].split("-")[0], temp[0].split("-")[1], arg);
                } else {
                    newCase = new Case(temp[0], "", arg);
                }
                map.put(temp[0], newCase);
            }
            return map;
        } catch (IOException ex) {
//            Logger.getLogger(ModelAgentApplicationMinute.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private class Case {

        String generic;
        String specific;
        int arg[];

        public Case(String generic, String specific, int arg[]) {
            this.generic = generic;
            this.specific = specific;
            this.arg = arg;
        }
    }

    protected void cuchi() {
        // it will send a question through the ask protocol which will return the PATH of the recorded model in compliance to the requirements specified in the contents of the message
        // addBehaviour(new askBehaviour());

        /*do this so you can detect the patterns. you are interested in how to make this selection of 
         * files dynamic and automatic i.e. just receive the requirements and have another agent put it together
         * in format independently of how many sources the model has*/
        String source1 = "data\\appsratetorcuata"; // getInfo
        String source2 = "data\\keyratetorcuata"; // getInfo  
        String source3 = "data\\mouseclickratetorcuata"; // getInfo 
        String source4 = "data\\movementratetorcuata"; // getInfo   

        // with the PATH the agent may choose to get all data ever recorded or just the last update
        List<File> data = new ArrayList<File>();
        data.add(new File(source1));
        data.add(new File(source2));
        data.add(new File(source3));
        data.add(new File(source4));
        //open file or model for training
        Object model = loadFromFile(train(selectFiles(data, 1)));
    }

    public static interface WndEnumProc extends StdCallLibrary.StdCallCallback {

        boolean callback(int hWnd, int lParam);
    }

    public static interface User32 extends StdCallLibrary {

        final User32 instance = (User32) Native.loadLibrary("user32", User32.class);

        boolean EnumWindows(WndEnumProc wndenumproc, int lParam);

        boolean IsWindowVisible(int hWnd);

        int GetWindowRect(int hWnd, RECT r);

        void GetWindowTextA(int hWnd, byte[] buffer, int buflen);

        int GetTopWindow(int hWnd);

        int GetWindow(int hWnd, int flag);
        final int GW_HWNDNEXT = 2;
    }

    public static class RECT extends Structure {

        public int left, top, right, bottom;
    }

    public static class WindowInfo {

        int hwnd;
        RECT rect;
        String title;

        public WindowInfo(int hwnd, RECT rect, String title) {
            this.hwnd = hwnd;
            this.rect = rect;
            this.title = title;
        }

        public String toString() {
            return String.format("(%d,%d)-(%d,%d) : \"%s\"",
                    rect.left, rect.top, rect.right, rect.bottom, title);
        }
    }

    public static void trackApps() {
        final List<WindowInfo> inflList = new ArrayList<WindowInfo>();
        final List<Integer> order = new ArrayList<Integer>();
        int top = User32.instance.GetTopWindow(0);
        while (top != 0) {//generates a list with the top numbers, maybe ids?
            order.add(top);
            top = User32.instance.GetWindow(top, User32.GW_HWNDNEXT);
        }
        // nota mental para mi: pedirle a peter que me explique bien 
        // que son las instancias y como funncionan en java
        User32.instance.EnumWindows(new WndEnumProc() {

            public boolean callback(int hWnd, int lParam) {
                if (User32.instance.IsWindowVisible(hWnd)) {
                    RECT r = new RECT();
                    User32.instance.GetWindowRect(hWnd, r);
                    if (r.left > -32000) {     // minimized
                        byte[] buffer = new byte[1024];
                        byte[] buffer2 = new byte[1024];
                        User32.instance.GetWindowTextA(hWnd, buffer, buffer.length);
                        User32.instance.GetWindowTextA(hWnd, buffer2, buffer2.length);
                        String title = Native.toString(buffer);
                        inflList.add(new WindowInfo(hWnd, r, title));
                    }
                }
                return true;
            }
        }, 0);
        Collections.sort(inflList, new Comparator<WindowInfo>() {

            public int compare(WindowInfo o1, WindowInfo o2) {
                return order.indexOf(o1.hwnd) - order.indexOf(o2.hwnd);
            }
        });
        for (WindowInfo w : inflList) {
            System.out.println(w);// here is the written  part where to modify
        }
    }

    private void createFiles() {
        try {
            Files.createFile(Paths.get(PATH + "\\log.txt"));
        } catch (IOException ex) {
            writeToLog(PATH, ex.toString());
        }
        try {
            Files.createFile(Paths.get(PATH + "\\filesused.txt"));
            List<String> temp = new ArrayList<String>();
            temp.add("log.txt\n");
            Files.write(Paths.get(PATH + "\\filesused.txt"), temp, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            writeToLog(PATH, ex.toString());
        }
    }

    private synchronized boolean writeTo(String path, Object text, String ext) {
        return writeTo(path, text, ext, "");
    }

    /**
     * writes to the string a dynamically generated file named after the minute
     * it was created on the keyboard folder.
     *
     * @param path the address to the folder where the logs of the sensor will
     * be generated
     * @param text the content to be logged.
     * @param ext the extension of the file.
     */
    private synchronized boolean writeTo(String path, Object text, String ext, String date) {
        try {
            String fileName;
            if (date.equals("")) {
                fileName = getDateTime().replace(":", "").substring(0, 7) + ext;
            } else {
                fileName = date.replace(":", "").substring(0, 7) + ext;
            }

            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(path + "\\" + fileName, true)));
            bf.append(text + System.getProperty("line.separator"));
            bf.close();
            return true;
        } catch (IOException ex) {
            writeToLog(PATH, ex.toString());
            return false;
        }
    }

    /**
     * logs writing to file errors for the agent.
     *
     * @param path the address to the folder where the logs of the sensor will
     * be generated
     * @param text the content to be logged.
     * @param ext the extension of the file.
     */
    private boolean writeToLog(String path, String text) {
        try {
            text = text + "at " + getDateTime().replace(":", "").substring(0, 7) + System.getProperty("line.separator");
            PrintWriter bf = new PrintWriter(
                    new BufferedWriter(
                            new FileWriter(path + "\\" + "log.txt", true)));
            bf.append(text);
            bf.close();
            return true;
        } catch (IOException ex) {
            System.out.print(ex);
            return false;
        }
    }

    /**
     * returns the current time in the format DDD:HH:mm:ss
     *
     * @return time
     */
    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("DDD:HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * checks for the las file updated in the file and returns it
     *
     * @return
     */
    private File getLastModel() {
        File folder = new File(PATH);
        File model = null;
        if (folder.list().length > 0) {
            File[] files = folder.listFiles();
            long lastMod = Long.MIN_VALUE;
            for (File file : files) {
                if (file.lastModified() > lastMod && !file.getName().equals("log.txt") && !file.getName().equals("filesused.txt")) {
                    lastMod = file.lastModified();
                    model = file;
                }
            }
        }
        return model;
    }

    private File createModel() {
        //initilize model with random values
        String data = "";
        for (int i = 0; i < 100; i++) {
            data = data + "some data of the model\n";
        }
        // save into a file
        writeTo(PATH, data, ".um");
        return getLastModel();
    }
}
