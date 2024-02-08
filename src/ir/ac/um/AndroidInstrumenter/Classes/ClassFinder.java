package ir.ac.um.AndroidInstrumenter.Classes;

import com.intellij.psi.PsiClass;

import java.util.List;

public class ClassFinder {
    public static PsiClass getClassByName(List<PsiClass> javaClass, String name){
        PsiClass result = null;
        for(PsiClass psiElement:javaClass){
            if(psiElement.getName().equals(name)){
                result = psiElement;
                break;
            }
        }
        return result;
    }


}
