package ir.ac.um.AndroidInstrumenter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import ir.ac.um.AndroidInstrumenter.Analysis.Instruments.ActvityInstrumneter;
import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles.ManifestInformationExtractor;
import ir.ac.um.AndroidInstrumenter.Utils.BackupCreator;
import ir.ac.um.AndroidInstrumenter.Utils.FileUtils;
import ir.ac.um.AndroidInstrumenter.Utils.Utils;

import java.io.*;
import java.util.List;

public class Instrumenter implements Runnable{
    private Project project;
    private PsiElement projectElement;
    private ProjectInformation projectInformation;
    private List<PsiClass> projectJavaClasses;

    public Instrumenter(Project project, PsiElement projectElement,ProjectInformation projectInformation){
        this.project = project;
        this.projectElement = projectElement;
        this.projectInformation = projectInformation;
    }

    @Override
    public void run() {
        prepareAppForInstrumenting();
        List<PsiJavaFile> projectJavaClasses = projectInformation.getProjectJavaFiles();
        ManifestInformationExtractor manifestInformationExtractor = new ManifestInformationExtractor(projectInformation);
        List<String> appActivitiesList = manifestInformationExtractor.getActivitiesList();
        ActvityInstrumneter actvityInstrumneter = new ActvityInstrumneter(projectInformation);
        for(String activity : appActivitiesList){
            for(PsiFile javaClassFile : projectJavaClasses){
                String fileName = FileUtils.extractFileName(javaClassFile.getName());
                if(activity.contentEquals(fileName)){
                    actvityInstrumneter.instrument(javaClassFile);
                    break;
                }
            }
        }
    }

    private String getSchemaFilePath(VirtualFile javaDirectory, String packageName) {
        return javaDirectory + packageName.replaceAll(".","\\");
    }

    private void prepareAppForInstrumenting(){
        try{
            VirtualFile baseDirectory = projectInformation.getBaseDirectory();
            BackupCreator.createBackup(baseDirectory,projectInformation.getJavaDirectory());
            FileUtils.copyInstrumentPackeage(projectInformation);
        } catch (IOException ioe){
            Utils.showMessage(ioe.getMessage());
        }
    }

}
