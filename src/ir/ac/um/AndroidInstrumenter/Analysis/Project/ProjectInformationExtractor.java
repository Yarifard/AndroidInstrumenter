package ir.ac.um.AndroidInstrumenter.Analysis.Project;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles.ManifestInformationExtractor;
import ir.ac.um.AndroidInstrumenter.Analysis.Project.JavaClassCollector;
import ir.ac.um.AndroidInstrumenter.Classes.ClassFinder;
import ir.ac.um.AndroidInstrumenter.Utils.DatabaseAdapter;
import ir.ac.um.AndroidInstrumenter.Utils.Utils;

import java.util.ArrayList;
import java.util.List;


public class ProjectInformationExtractor {

    private ProjectInformation projectInformation;
    private VirtualFile manifestFile;
    private VirtualFile baseDirectory;
    private VirtualFile srcDirectory;
    private VirtualFile mainDirectory;
    private VirtualFile resourcesDirectory;
    private VirtualFile layoutsDirectory;
    private VirtualFile menusDirectory;
    private VirtualFile valuesDirectory;
    private JavaClassCollector javaClassesCollector;

    public ProjectInformationExtractor(ProjectInformation projectInformation) {
        Utils.showMessage("I'm in Position 2");
        this.projectInformation = projectInformation;
        projectJavaClassesCollector();
        Utils.showMessage("I'm in Position 22");
    }



    private void projectJavaClassesCollector() {
        Utils.showMessage("I'm in ProjectInformationExtractor:projectJavaClassesCollector-->Start");
        this.javaClassesCollector = new JavaClassCollector();
        this.projectInformation.getProjectElement().accept(this.javaClassesCollector);
        Utils.showMessage("I'm in ProjectInformationExtractor:projectJavaClassesCollector-->End");
    }

    public VirtualFile getSrcDirectory(VirtualFile baseDirectory){
        VirtualFile srcDirectory;

        srcDirectory = findSourceDirectory(baseDirectory);
        if(srcDirectory == null)
            Utils.showMessage("Failed to detect source directory.");
        return srcDirectory;
    }

    public VirtualFile getMainDirectory(VirtualFile srcDirectory){
        VirtualFile mainDirectory;

        mainDirectory = findMainDirectory(srcDirectory);
        if(mainDirectory == null)
            Utils.showMessage("Failed to detect main directory.");
        return mainDirectory;

    }

    public VirtualFile getAndroidTestDirectory(VirtualFile srcDirectory){
        VirtualFile androidTestDirectory;
        androidTestDirectory = findAndroidTestDirectory(srcDirectory);
        if(androidTestDirectory == null)
            Utils.showMessage("Failed to detect androidTestDirectory.");
        return androidTestDirectory;
    }

    public VirtualFile getResourceDirectory(VirtualFile mainDirectory){
        VirtualFile resourceDirectory;

        resourceDirectory = findResourcesDirectory(mainDirectory);
        if(resourceDirectory == null)
            Utils.showMessage("Failed to detect resources directory.");
        return resourceDirectory;

    }

    public VirtualFile getLayoutsDirectory(VirtualFile resourceDirectory){
        VirtualFile layoutDirectory;

        layoutDirectory = findLayoutDirectory(resourceDirectory);
        if(layoutDirectory == null)
            Utils.showMessage("Failed to detect layouts directory.");

        return layoutDirectory;
    }

    public VirtualFile getJavaDirectory(VirtualFile mainDirectory){
        VirtualFile javaDirectory;

         javaDirectory = findJavaDirectory(mainDirectory);
        if(javaDirectory == null)
            Utils.showMessage("Failed to detect layouts directory.");

        return javaDirectory;
    }

    public VirtualFile getMenusDirectory(VirtualFile resourceDirectory){
        VirtualFile menuDirectory;

        menuDirectory = findMenuDirectory(resourceDirectory);
        if(menuDirectory == null)
            Utils.showMessage("Failed to detect menus directory.");
        return menuDirectory;

    }

    public VirtualFile getValuesDirectory(VirtualFile resourceDirectory){
        VirtualFile valuesDirectory;

        valuesDirectory = findValuesDirectory(resourceDirectory);
        if(valuesDirectory == null)
            Utils.showMessage("Failed to detect values directory.");
        return valuesDirectory;

    }

    private VirtualFile findSourceDirectory(VirtualFile directory) {

        return getChildDirectory("src", directory, true);
    }

    private VirtualFile findAndroidTestDirectory(VirtualFile directory) {
        return getChildDirectory("androidTest", directory, false);
    }

    private VirtualFile findMainDirectory(VirtualFile directory) {

        return getChildDirectory("main", directory, false);
    }

    private VirtualFile findResourcesDirectory(VirtualFile directory) {
        return getChildDirectory("res", directory, true);
    }

    private VirtualFile findLayoutDirectory(VirtualFile directory) {
        return getChildDirectory("layout", directory, true);
    }

    private VirtualFile findJavaDirectory(VirtualFile directory){
        return getChildDirectory("java", directory, true);

    }

    private VirtualFile findMenuDirectory(VirtualFile directory) {

        return getChildDirectory("menu", directory, true);
    }

    private VirtualFile findValuesDirectory(VirtualFile directory) {
        return getChildDirectory("values", directory, true);
    }

    private boolean hasAndroidManifest(VirtualFile directory) {
        boolean result = false;
        VirtualFile[] children = directory.getChildren();
        search:
        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                VirtualFile[] children2 = child.getChildren();
                for (VirtualFile child2 : children2) {
                    if (child2.getName().equals("AndroidManifest.xml")) {
                        result = true;
                        break;
                    }
                }
            }
        }
        return result;
    }

    public VirtualFile getAndroidManifestFile(VirtualFile mainDirectory){
        Utils.showMessage("I'm in ProjectInformationExtractor:getAndroidManifestFile-->Starting");
        VirtualFile result = null;
        VirtualFile[] children = mainDirectory.getChildren();
        search:
        for (VirtualFile child : children) {
            if (!child.isDirectory()) {
                if (child.getName().contains("AndroidManifest")) {
                    result = child;
                    break;
                }
            }
        }

        Utils.showMessage(result.getName());
        Utils.showMessage("I'm in ProjectInformationExtractor:getAndroidManifestFile-->End");
        return result;
    }

    public String getAppPackageTitle() {
        //return getDatabaseName();
        Utils.showMessage("I'm in ProjectInformationExtractor:getLaunchActivity-->Starting");
        String result = "";
        ManifestInformationExtractor manifestInformationExtractor = new ManifestInformationExtractor(this.projectInformation);
        result = manifestInformationExtractor.getAppPackageTitle();
        return result;
    }

    private VirtualFile getChildDirectory(String childDirectoryName, VirtualFile parentDirectory, boolean depthFirst) {
        VirtualFile result = null;
        VirtualFile[] children = parentDirectory.getChildren();
        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                if (child.getName().equals(childDirectoryName)) {
                    if (childDirectoryName.equals("src")) {
                        if (containsSubDirectories(child, "main", "androidTest")
                                || containsSubDirectories(child, "main", "test")
                                || (containsSubDirectories(child, "main") && hasAndroidManifest(child))) {
                            result = child;
                            break;
                        }
                    } else {
                        result = child;
                        break;
                    }
                } else if (depthFirst) {
                    VirtualFile temp = getChildDirectory(childDirectoryName, child, depthFirst);
                    if (temp != null) {
                        result = temp;
                        break;
                    }
                }
            }
        }
        return result;
    }

    private boolean containsSubDirectories(VirtualFile directory, String... subDirectoryNames) {
        boolean result = true;
        VirtualFile[] children = directory.getChildren();
        for (String subDirectoryName : subDirectoryNames) {
            boolean exists = false;
            for (VirtualFile child : children) {
                if (child.isDirectory() && child.getName().equals(subDirectoryName)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                result = false;
                break;
            }
        }
        return result;
    }

    public VirtualFile getSourceDirectory(){ return  this.srcDirectory;}

    public VirtualFile getMenusDirectory(){ return this.menusDirectory; }

    public VirtualFile getValuesDirectory(){ return  this.valuesDirectory;}

    public PsiClass getLuncherActivity(VirtualFile manifestFile){
        Utils.showMessage("I'm in ProjectInformationExtractor:getLaunchActivity-->Starting");
        PsiClass result = null;
        String manifestFilePath;
        String luncherActivityClassName;

        ManifestInformationExtractor manifestInformationExtractor =
                new ManifestInformationExtractor(projectInformation);
        manifestFilePath = manifestFile.getCanonicalPath();
        Utils.showMessage(manifestFilePath);
        luncherActivityClassName = manifestInformationExtractor.getLuncherActivityClassName();
       // luncherActivityClassName = luncherActivityClassName.substring(1);
        Utils.showMessage("the result is" + luncherActivityClassName);
        result = ClassFinder.getClassByName(
                this.javaClassesCollector.getJavaClasses(),luncherActivityClassName );
        Utils.showMessage("the result is:" + result.getName());
//        if(result != null){
//            PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
//            PsiMethod tmp = factory.createMethodFromText("public void test(){a=2;}",result);
//            tmp.setName("ali");
//            result.add((PsiElement) tmp);
//            PsiMethod[] list = result.getMethods();
//
//        }
        return result;
    }

    public List<PsiClass> getProjectJavaClassList(){
        return this.javaClassesCollector.getJavaClasses();
    }

    public List<PsiJavaFile> getProjectJavaFiles(){
        return this.javaClassesCollector.getJavaFiles();
    }

    public PsiClass getProjectClassByName(String className) {
        for (PsiClass projectJavaClass : javaClassesCollector.getJavaClasses()) {
            if (projectJavaClass.getQualifiedName().equals(className)) {
                return projectJavaClass;
            }
        }
        return null;
    }

    public String getActivityClassPath(String activityName){
        return activityName;
    }

    public List<PsiClass> getClassListByActivityName(String activityName){
        List<PsiClass> activityClassList = new ArrayList<>();

        return activityClassList;
    }

    private String getDatabaseName(String packageName){
        return packageName.substring(packageName.lastIndexOf('.') +1);
    }


    public DatabaseAdapter prepareDatabase() {
        String datbaseName = getDatabaseName(projectInformation.getAppPackageName());
        DatabaseAdapter adapter = new DatabaseAdapter(datbaseName);
        if(adapter.createDatabase())
            return adapter;
        else
            adapter = createDatabase(datbaseName);
        return adapter;
    }

    private DatabaseAdapter createDatabase(String datbaseName) {
        DatabaseAdapter adapter = null;
        //ToDo::This method must be implementeded
        return adapter;
    }

}
