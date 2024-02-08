package ir.ac.um.AndroidInstrumenter.Analysis.Project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import ir.ac.um.AndroidInstrumenter.Taging.POSTagger;
import ir.ac.um.AndroidInstrumenter.Utils.DatabaseAdapter;
import ir.ac.um.AndroidInstrumenter.Utils.Utils;

import java.util.List;


public class ProjectInformation {
    private Project project;
    private String appPackageTitle;
    private POSTagger posTagger;
    private PsiElement projectElement;
    private VirtualFile manifestFile;
    private VirtualFile javaDirectory;
    private VirtualFile baseDirectory;
    private VirtualFile srcDirectory;
    private VirtualFile mainDirectory;
    private VirtualFile resourcesDirectory;
    private VirtualFile layoutsDirectory;
    private VirtualFile menusDirectory;
    private VirtualFile valuesDirectory;
    private VirtualFile androidTestDirectory;
    private List<PsiClass> javaClasses;
    private PsiClass launcherActivityClass;
    private DatabaseAdapter dbAdapter;
    private ProjectInformationExtractor projectInformationExtractor;



    public ProjectInformation(Project project, PsiElement psiElement){

        this.project = project;
        this.appPackageTitle = "";
        this.projectElement = psiElement;
        this.posTagger = new POSTagger();
        this.projectInformationExtractor = new ProjectInformationExtractor(this);
       // collectInformation();
    }

    public boolean collectInformation() {

        if (initializeDirectories()) {
            this.manifestFile = projectInformationExtractor.getAndroidManifestFile(this.mainDirectory);
            if (this.manifestFile == null) {
                Utils.showMessage("Failed to detect manifest file");
                return false;
            }

            this.launcherActivityClass = projectInformationExtractor.getLuncherActivity(this.manifestFile);
            if (this.launcherActivityClass == null) {
                Utils.showMessage("Failed to detect launcher activity in manifest file");
                return false;
            }

            this.appPackageTitle = projectInformationExtractor.getAppPackageTitle();
            if (this.appPackageTitle == null) {
                Utils.showMessage("Failed to detect package name of the app in manifest file");
                return false;
            }

            this.dbAdapter = projectInformationExtractor.prepareDatabase();
            if(this.dbAdapter == null){
                Utils.showMessage("Failed to create the database for storing widgets information");
                return false;
            }

            this.javaClasses = projectInformationExtractor.getProjectJavaClassList();
            return true;
        }
        else
          Utils.showMessage("Failed to initialize directories");
        return false;
    }

    private boolean initializeDirectories(){
        this.baseDirectory = LocalFileSystem.getInstance().findFileByPath(this.project.getBasePath());
        if(this.baseDirectory == null)
            return false;

        this.srcDirectory  = projectInformationExtractor.getSrcDirectory(this.baseDirectory);
        if(this.srcDirectory == null)
            return false;

        this.mainDirectory = projectInformationExtractor.getMainDirectory(this.srcDirectory);
        if(this.mainDirectory == null)
            return false;

        this.resourcesDirectory = projectInformationExtractor.getResourceDirectory(this.mainDirectory);
        if(resourcesDirectory == null)
            return false;

        this.javaDirectory = projectInformationExtractor.getJavaDirectory(this.mainDirectory);
        if(this.javaDirectory == null)
            return false;

        this.androidTestDirectory = projectInformationExtractor.getAndroidTestDirectory(this.srcDirectory);
        if(androidTestDirectory == null)
            return  false;

        this.layoutsDirectory = projectInformationExtractor.getLayoutsDirectory(resourcesDirectory);
        if(this.layoutsDirectory == null)
            return false;
        this.menusDirectory = projectInformationExtractor.getMenusDirectory(resourcesDirectory);

        this.valuesDirectory = projectInformationExtractor.getValuesDirectory(resourcesDirectory);
        if(this.valuesDirectory == null)
            return false;
        return true;
    }

    public void setManifestFile(VirtualFile manifestFilePath){
        this.manifestFile = manifestFilePath;
    }

    public void setBaseDirectory(VirtualFile baseDirectory){
        this.baseDirectory = baseDirectory;
    }

    public void setMainDirectory(VirtualFile mainDirectory){
        this.mainDirectory = mainDirectory;
    }

    public void setResourcesDirectory(VirtualFile resDir){
        this.resourcesDirectory = resDir;
    }

    public void setLayoutsDirectory(VirtualFile layoutsDirectory){
        this.layoutsDirectory = layoutsDirectory;
    }

    public void setValuesDirectory(VirtualFile valuesDirectory){
        this.valuesDirectory = valuesDirectory;
    }

    public void setMenusDirectory(VirtualFile menusDirectory){ this.menusDirectory = menusDirectory;}
    public void setPackageName(String appPackageTitle){ this.appPackageTitle = appPackageTitle;}

    public void setSrcDirectory(VirtualFile srcDirectory){ this.srcDirectory = srcDirectory;}

    public VirtualFile getManifestFile(){ return this.manifestFile;}
    public VirtualFile getAndroidTestDirectory(){ return this.androidTestDirectory; }
    public Project     getProjectObject(){ return this.project; }
    public PsiElement  getProjectElement(){return this.projectElement;}
    public PsiClass    getLuncherActivity(){
        return  this.launcherActivityClass;
    }
    public String      getAppPackageTitle(){ return  this.appPackageTitle;}
    public String      getAppPackageName(){
        return appPackageTitle.substring(appPackageTitle.lastIndexOf('.') + 1);
    }

    public String    getSchemaFilePath(){
        String content = appPackageTitle.replace(".","\\\\");
        return javaDirectory.getCanonicalPath() + "\\" +
                content;
    }

    public VirtualFile getBaseDirectory(){ return this.baseDirectory;}
    public VirtualFile getMainDirectory(){ return this.mainDirectory;}
    public VirtualFile getMenusDirectory(){ return this.menusDirectory;}
    public VirtualFile getLayoutsDirectory(){ return layoutsDirectory;}
    public VirtualFile getValuesDirectory(){ return valuesDirectory;}
    public VirtualFile getSrcDirectory(){ return srcDirectory;}
    public VirtualFile getJavaDirectory(){return  javaDirectory;}
    public VirtualFile getResourcesDirectory(){ return resourcesDirectory;}
    public DatabaseAdapter getDbAdapter(){return  this.dbAdapter;}
    public POSTagger getPosTagger(){return  posTagger;}
    public List<PsiJavaFile> getProjectJavaFiles(){ return this.projectInformationExtractor.getProjectJavaFiles();}
    public ProjectInformationExtractor getProjectInformationExtractor(){
        return  this.projectInformationExtractor;
    }

    public List<PsiClass> getProjectJavaClassList() {
        return javaClasses;
    }
}
