package ir.ac.um.AndroidInstrumenter.widget;

public class WidgetInfoExtrctor {

    public String captureWidgetInfo(String packageName,String className,Widget widget) {
        String result = "";
        switch(widget.getWidgetType()) {
            case "ListView":
                result += "variable this.ListView_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  var-kind field ListView_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.ListView_"  + widget.getWidgetDatabaseId() + "_" +
                           widget.getBindingVariableName() + "_size\n";
                result += "   var-kind field ListView_" +widget.getWidgetDatabaseId() + "_" +
                           widget.getBindingVariableName() + "_size\n";
                result += "  enclosing-var this\n";
                result += "  dec-type int\n";
                result += "  rep-type int\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";
                break;
            case "EditText":
                result += "variable this.EditText_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  var-kind field EditText_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.EditText_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  var-kind field EditText_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.EditText_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_editable\n";
                result += "  var-kind field EditText_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_editable\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.EditText_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  var-kind field EditText_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  enclosing-var this\n";
                result += "  dec-type java.lang.String\n";
                result += "  rep-type hashcode\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.EditText_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text.toString\n";
                result += "  var-kind function toString()\n";
                result += "  enclosing-var this.EditText_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  dec-type java.lang.String\n";
                result += "  rep-type java.lang.String\n";
                result += "  function-args this.EditText_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  flags synthetic to_string\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.EditText_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_length\n";
                result += "  var-kind field EditText_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_length\n";
                result += "  enclosing-var this\n";
                result += "  dec-type int\n";
                result += "  rep-type int\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";
                break;
            case "TextView":
                result += "variable this.TextView_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  var-kind field TextView_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.TextView_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  var-kind field TextView_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.TextView_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  var-kind field TextView_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  enclosing-var this\n";
                result += "  dec-type java.lang.String\n";
                result += "  rep-type hashcode\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.TextView_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text.toString\n";
                result += "  var-kind function toString()\n";
                result += "  enclosing-var this.TextView_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  dec-type java.lang.String\n";
                result += "  rep-type java.lang.String\n";
                result += "  function-args this.TextView_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  flags synthetic to_string\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";
                break;
            case "CheckBox":
                result += "variable this.CheckBox_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  var-kind field CheckBox_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.CheckBox_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  var-kind field CheckBox_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.CheckBox_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_checked\n";
                result += "  var-kind field CheckBox_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_checked\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";
                break;

            case "Switch":
                result += "variable this.Switch_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  var-kind field Switch_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.Switch_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  var-kind field Switch_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.Switch_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_checked\n";
                result += "  var-kind field Switch_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_checked\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";
                break;

            case "FloatingActionButton":
                result += "variable this.FloatingActionButton_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  var-kind field FloatingActionButton_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.FloatingActionButton_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  var-kind field FloatingActionButton_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";
                break;
            case "ImageButton":
                result += "variable this.ImageButton_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  var-kind field ImageButton_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.ImageButton_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  var-kind field ImageButton_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";
                break;
            case "MainMenuItem":
                result += "variable this.MainMenuItem_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  var-kind field MainMenuItem_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.MainMenuItem_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  var-kind field MainMenuItem_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";
                break;
            case "Button":
                result += "variable this.Button_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  var-kind field Button_" + widget.getWidgetDatabaseId() + "_" +
                        widget.getBindingVariableName() + "_visibility\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.Button_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  var-kind field Button_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_enable\n";
                result += "  enclosing-var this\n";
                result += "  dec-type boolean\n";
                result += "  rep-type boolean\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.Button_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  var-kind field Button_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  enclosing-var this\n";
                result += "  dec-type java.lang.String\n";
                result += "  rep-type hashcode\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";

                result += "variable this.Button_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text.toString\n";
                result += "  var-kind function toString()\n";
                result += "  enclosing-var this.Button_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  dec-type java.lang.String\n";
                result += "  rep-type java.lang.String\n";
                result += "  function-args this.Button_" + widget.getWidgetDatabaseId()+ "_" +
                        widget.getBindingVariableName() + "_text\n";
                result += "  flags synthetic to_string\n";
                result += "  comparability 22\n";
                result += "  parent " + packageName + "." + className + ":::OBJECT 1\n";
                break;
        }
        return result;
    }
}

