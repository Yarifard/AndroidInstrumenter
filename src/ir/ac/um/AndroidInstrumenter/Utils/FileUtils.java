package ir.ac.um.AndroidInstrumenter.Utils;


import ir.ac.um.AndroidInstrumenter.Analysis.Project.JavaClassCollector;
import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public  class FileUtils {

    public static boolean copyInstrumentPackeage(ProjectInformation projectInformation) throws IOException {
        File libFolderPath = new Dialog().getTargetFolder();
        List<File> files = new ArrayList<File>(Arrays.asList(libFolderPath.listFiles()));
        File instrumentPackage = new File(projectInformation.getSchemaFilePath() + "\\Instrumenter");
        if(!instrumentPackage.exists()){
            instrumentPackage.mkdir();
            for(File sourFile : files){
                String fileName = sourFile.getName().toString();
                File destFile = new File(instrumentPackage + "/" + fileName);
                copyFiles(sourFile,destFile,projectInformation.getAppPackageTitle());
            }
        }
        return true;
    }

    private static boolean copyFiles(File sourceFile,File destFile,String packageName) throws IOException {
        String content = "";
        try{
            if(destFile.createNewFile()){
                FileWriter fileWriter = new FileWriter(destFile);
                Scanner fileReader = new Scanner(sourceFile);
                fileWriter.write("package  " + packageName + ".Instrumenter;\n");
                while(fileReader.hasNextLine()){
                    content = fileReader.nextLine();
                    fileWriter.write(content + "\n");
                    content = "";
                }
                fileWriter.close();
                fileReader.close();
                return true;
            }
        }catch(IOException ioe){
            ioe.getMessage().toString();
        }
        return false;
    }

    private static boolean copyViewVisitorClass(String dest, String packageName) throws IOException {
        boolean result = false;
        String content = "";
        File outPutFile = new File(getClassFilePath(dest,packageName,"ViewVisitor"));
        File inPutFile  = new File("E:\\Utils\\ViewVisitor.java");
        try{
           // if(!outPutFile.exists() || !inPutFile.exists()){
                if(outPutFile.createNewFile()){
                    FileWriter fileWriter = new FileWriter(outPutFile);
                    Scanner fileReader = new Scanner(inPutFile);
                    fileWriter.write("package  " + packageName + ";\n");
                    while(fileReader.hasNextLine()){
                        content = fileReader.nextLine();
                        fileWriter.write(content + "\n");
                        content = "";
                    }
                    fileWriter.close();
                    fileReader.close();
                    return true;
                }
//            }
//            else{
//                Utils.showMessage("The source or destination path is not accessible");
//                return false;
//            }
        }
        catch (IOException ioException){
             Utils.showMessage("Error!!!");
             ioException.printStackTrace();
        }
        return false;
    }

    private static String getClassFilePath(String testFilePath, String packageName,
                                   String className) {
        return testFilePath +
                "\\java\\" + getPackagePath(packageName) +
                "\\" + className + ".java";
    }

    private static String getPackagePath(String packageName) {
        String packagePath = "";
        //packagePath = packageName;
        packagePath = packageName.replace(".","\\\\");

        return packagePath;
    }

    private static boolean copyVisitorClass(String dest, String packageName) {
        boolean result = false;
        String content = "";
        File outPutFile = new File(getClassFilePath(dest,packageName,"WidgetInfoExtractor"));
        File inPutFile  = new File("D:\\Utils\\WidgetInfoExtractor.java");
        try{
            // if(!outPutFile.exists() || !inPutFile.exists()){
            if(outPutFile.createNewFile()){
                FileWriter fileWriter = new FileWriter(outPutFile);
                Scanner fileReader = new Scanner(inPutFile);
                fileWriter.write("package  " + packageName + ";\n");
                while(fileReader.hasNextLine()){
                    content = fileReader.nextLine();
                    fileWriter.write(content + "\n");
                    content = "";
                }
                fileWriter.close();
                fileReader.close();
                return true;
            }
//            }
//            else{
//                Utils.showMessage("The source or destination path is not accessible");
//                return false;
//            }
        }
        catch (IOException ioException){
            Utils.showMessage("Error!!!");
            ioException.printStackTrace();
        }
        return false;
    }

    public static String extractFileName(String fileName){
        return fileName.substring(0,fileName.lastIndexOf('.'));
    }
}
