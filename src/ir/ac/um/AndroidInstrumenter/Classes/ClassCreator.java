package ir.ac.um.AndroidInstrumenter.Classes;

import ir.ac.um.AndroidInstrumenter.Utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ClassCreator {
    private String path;
    private String packageName;

    public ClassCreator(String packageName,String path){
        this.path = path;
        this.packageName = packageName;
        createBaseVisitor();
        createConcreateVisitor();
    }
    private void createBaseVisitor() {
      //  String packageFolder = packageName.replaceAll(".","/");
        File baseVisitorClassFile = new File(getClassFilePath(path,packageName,"Visitor"));
        String content="";

        try{
            if(!baseVisitorClassFile.exists())
                if(baseVisitorClassFile.createNewFile()){
                    FileWriter writer = new FileWriter(baseVisitorClassFile);
                    writer.write(prepareBasicVisitorClass());
                   // writer.write(prepareBasicVisitorClassMethods());
                    writer.close();
                }
            Utils.showMessage("Base visitor class is created successflly");
        }
        catch (IOException ioe){
            Utils.showMessage("Issue an error when the test file is created!!!");
        }

    }

    private void createConcreateVisitor() {
        File baseVisitorClassFile = new File(getClassFilePath(path,packageName,"ViewVisitor"));
        String content="";

        try{
            if(!baseVisitorClassFile.exists())
                if(baseVisitorClassFile.createNewFile()){
                    FileWriter writer = new FileWriter(baseVisitorClassFile);
                    writer.write(prepareGUIVisitorClass());
                    // writer.write(prepareBasicVisitorClassMethods());
                    writer.close();
                }
            Utils.showMessage("Base visitor class is created successflly");
        }
        catch (IOException ioe){
            Utils.showMessage("Issue an error when the test file is created!!!");
        }

    }

    private String prepareGUIVisitorClass() {
        String content="";
        content += "package" + " " + packageName + ";\n\n";
        content += "import android.view.View;\n";
        content += "import android.widget.AdapterView;\n";
        content += "import android.widget.CheckBox;\n";
        content += "import android.widget.EditText;\n";
        content += "import android.widget.LinearLayout;\n";
        content += "import android.widget.ListView;\n";
        content += "import android.widget.TextView;\n";
        content += "\npublic class ViewVisitor extends Visitor(){\n\n";
        content +=  "\t" + crateEditTextViewVisit();

        content += "\t public String visit(TextView textView);\n";
        content += "\t public String visit(CheckBox checkBox);\n";
        content += "\t public String visit(ListView listView);\n";
        content += "\t public String visit(EditText editText);\n";
        content += "}";
        return content;
    }

    private String crateEditTextViewVisit() {
        String content = "";
        content += "pubic String visit(EditText editText,String viewName){\n";
        content += "\n\t\t String result = \"\";";

        content += "\n\t\t result += \"this.\" + viewName +\"_enable\";";
        content += "\n\t\t if(editText.IsEnabled())";
        content += "\n\t\t\t result += \"1\"\n\"";
        content += "\n\t\t else\n";
        content += "\n\t\t\t result += \"0\"";

        content += "\n\t\t result += \"this.\" + viewName +\"_value\";";
        content += "\n\t\t\""+ "editText.getText()" +"\";";

        content += "\n\t\t result += \"this.\" + viewName +\"_length\";";
        content += "\n\t\t\""+ "editText.length()" +"\";";

        content +="\n\t return result;\n";
        content +="}\n";
        return content;
    }

    private String prepareBasicVisitorClass() {
        String content="";
        content += "package" + " " + packageName + ";\n\n";
        content += "import android.view.View;\n";
        content += "import android.widget.AdapterView;\n";
        content += "import android.widget.CheckBox;\n";
        content += "import android.widget.EditText;\n";
        content += "import android.widget.LinearLayout;\n";
        content += "import android.widget.ListView;\n";
        content += "import android.widget.TextView;\n\n";
        content += "public class Visitor(){\n\n";
        content += "\t public abstract String visit(EditText editText,String viewName);\n";
        content += "\t public abstract String visit(TextView textView,String viewName);\n";
        content += "\t public abstract String visit(CheckBox checkBox,String viewName);\n";
        content += "\t public abstract String visit(ListView listView,String viewName);\n";
        content += "\t public abstract String visit(EditText editText,String viewName);\n";
        content += "}";
        return content;
    }

    public String getClassFilePath(String testFilePath, String packageName,
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


}
