package ir.ac.um.AndroidInstrumenter.Classes;

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;

import java.util.ArrayList;
import java.util.List;

public class JavaClassCollector extends JavaRecursiveElementVisitor {
    private List<PsiClass> javaClasses;

    public JavaClassCollector() {
        javaClasses = new ArrayList<>();
    }

    @Override
    public void visitJavaFile(PsiJavaFile psiJavaFile) {
        super.visitFile(psiJavaFile);
        if (psiJavaFile.getName().endsWith(".java") && !psiJavaFile.getName().equals("R.java")) {
            PsiClass[] psiClasses = psiJavaFile.getClasses();
            for (PsiClass psiClass : psiClasses) {
                  javaClasses.add(psiClass);
            }
        }
    }

    public List<PsiClass> getJavaClasses(){
        return javaClasses;
    }


}