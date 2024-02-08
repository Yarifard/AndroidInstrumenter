package ir.ac.um.AndroidInstrumenter.Analysis.Project;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import ir.ac.um.AndroidInstrumenter.Utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Samad Paydar
 */
public class JavaClassCollector extends JavaRecursiveElementVisitor{
    private List<PsiClass> javaClasses;
    private List<PsiJavaFile> javaFiles;


    public JavaClassCollector() {
        //Utils.showMessage("I'm in JavaClassCollector:Constructor -->start");
        javaClasses = new ArrayList<>();
        javaFiles = new ArrayList<>();
        //Utils.showMessage("I'm in JavaClassCollector:Constructor -->end");
    }

    @Override
    public void visitJavaFile(PsiJavaFile psiJavaFile) {
        //javaClasses = new ArrayList<>();
        super.visitFile(psiJavaFile);
       // Utils.showMessage("I'm in JavaClassCollector:visitJavaFile-->Start");
        if (psiJavaFile.getName().endsWith(".java") && !psiJavaFile.getName().equals("R.java")) {
            javaFiles.add(psiJavaFile);
            PsiClass[] psiClasses = psiJavaFile.getClasses();
            for (PsiClass psiClass : psiClasses) {
                javaClasses.add(psiClass);
            }
        }

    }

    public List<PsiClass> getJavaClasses(){ return javaClasses;}

    public List<PsiJavaFile> getJavaFiles(){return javaFiles;}

}
