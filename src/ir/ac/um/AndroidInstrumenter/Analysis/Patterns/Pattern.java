package ir.ac.um.AndroidInstrumenter.Analysis.Patterns;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.ArrayList;
import java.util.List;

public class Pattern {
    public static boolean isMatch(String content, String parameterName, String parameterType,String variable) {
        List<String> patterns = createPatterns(content,parameterName, parameterType, variable);
        content.trim();
        content = content.replaceAll("\\s", "");
        for (String pattern : patterns) {
            if (content.contains(pattern))
                return true;
        }
        return false;
    }

    public static List<String> createPatterns(String content,String parameterName, String parameterType, String variable) {
        List<String> patterns = new ArrayList<>();

        if(content.contains("==")){
            if (!variable.isEmpty()) {
                patterns.add(variable + "==R.id.");
                patterns.add("==" + variable);
            }
            if(parameterType.equals("MenuItem")){
                patterns.add(parameterName + ".getItemId()==R.id.");
                patterns.add("==" + parameterName + ".getItemId()");
            }
            if(parameterType.equals("View")){
                patterns.add(parameterName + ".getId()==R.id.");
                patterns.add("==" + parameterName + ".getId()");
            }
        }else{
            if (!variable.isEmpty())
                patterns.add(variable);
            if(parameterType.contentEquals("MenuItem"))
                patterns.add(parameterName + ".getItemId()");
            if(parameterType.contentEquals("View"))
                patterns.add(parameterName + ".getId()");
        }
        return patterns;
    }

    public static boolean isMatch(MethodDeclaration callerMethod, MethodCallExpr calledMethod,
                                        String containingClassName,
                                  String[] methodPatterns, String[] classPatterns) {

        for (int i = 0; i < methodPatterns.length; i++)
            if (calledMethod.getName().toString().contentEquals(methodPatterns[i]))
                // TODO: this condition must inspected carefully.
                if (calledMethod.getScope().isEmpty())
                    return true;
                else {

                    for (int j = 0;j < classPatterns.length; j++)
                        if (containingClassName.contains(classPatterns[j]))
                            return true;
                }
        return false;

    }

    public static boolean isMatch(String[] patterns,String content){
        boolean result = false;
        for(int i = 0; i < patterns.length; i++ )
            if(patterns[i].toLowerCase().contentEquals(content.toLowerCase())){
                result = true;
                break;
            }
        return result;

    }

    public static boolean isPartialMatch(String[] patterns, String content){
        boolean result = false;
        for(int i = 0; i < patterns.length; i++ )
            if(content.toLowerCase().contains(patterns[i].toLowerCase())){
                result = true;
                break;
            }
        return result;
    }

    public static int findMathOperator(String[] patterns,String content){
        int index = -1;
        for(int i = 0; i < patterns.length; i++ )
            if(content.contains(patterns[i])){
                index = i;
            }
        return index;
    }

    public static boolean isMatch(String eventTitle, String[] patterns) {
        boolean result = false;
        for(int i = 0; i< patterns.length; i++){
            if(eventTitle.toLowerCase().contains(patterns[i].toLowerCase())){
                result = true;
                break;
            }

        }
        return result;
    }

    public static boolean isMatchWithPattern(String pattern, String target){
        if(pattern.contentEquals(target))
            return true;
        return false;
    }
}
