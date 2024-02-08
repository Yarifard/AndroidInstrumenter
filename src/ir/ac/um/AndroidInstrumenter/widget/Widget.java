package ir.ac.um.AndroidInstrumenter.widget;

import java.util.LinkedList;
import java.util.List;

enum WidgetType {TextView, EditText, Button, ImageButton, FloatingActionButton, RadioButton,ToggleButton,
     CheckBox,RadioGroup, ImageView,CalenderView,WebView,VideoView,ProgressBar,SeekBar,RatingBar,ListView, MenuItem,None }

public class Widget {
    private int orderInLayout;
    private String bindingVariableName;
    private String widgetType;
    private int id;
    private List<WidgetDescriptor> widgetDescriptorsList;

    public Widget(){
        this.orderInLayout = -1;
        widgetType = "";
        bindingVariableName = "";
        this.widgetDescriptorsList = new LinkedList<>();
    }

    public static boolean isWidget(String className){
        boolean result = false;
        WidgetType widgetType = getWidgetType(className);
        if(widgetType != WidgetType.None)
            result = true;
        return result;
    }

    public static WidgetType getWidgetType(String widgetName){
        WidgetType result = WidgetType.None;
        switch(widgetName){
            case "TextView"    : result = WidgetType.TextView;break;
            case "EditText"    : result = WidgetType.EditText;break;
            case "Button"      : result = WidgetType.Button;break;
            case "ImageButton" : result = WidgetType.ImageButton;break;
            case "FloatingActionButton" : result = WidgetType.FloatingActionButton; break;
            case "RadioButton" : result = WidgetType.RadioButton; break;
            case "ToggleButton": result = WidgetType.ToggleButton; break;
            case "CheckBox": result = WidgetType.CheckBox;break;
            case "RadioGroup": result = WidgetType.RadioGroup; break;
            case "ImageView" : result = WidgetType.ImageView; break;
            case "CalenderView": result = WidgetType.CalenderView; break;
            case "WebView": result = WidgetType.WebView;break;
            case "VideoView":result = WidgetType.VideoView; break;
            case "ProgressBar": result = WidgetType.ProgressBar;break;
            case "SeekBar": result = WidgetType.SeekBar; break;
            case "RatingBar": result = WidgetType.RatingBar;break;
            case "ListView": result = WidgetType.ListView;break;
            case "MenuItem": result = WidgetType.MenuItem;break;
        }
        return result;
    }

    public void setWidgetType(String widgetType){
        this.widgetType = widgetType;
    }
    public void setWidgetDatabaseId(int id){ this.id = id;}
    public void setBindingVariable(String variable){this.bindingVariableName = variable;}
    public String getBindingVariableName(){return bindingVariableName;}
    public int getWidgetDatabaseId(){return id;}

    public void setWidgetIdDescriptor(String value){
        WidgetDescriptor widgetDescriptor = new WidgetDescriptor();
        widgetDescriptor.SetDescriptor(DescriptorType.ViewId,value);
        widgetDescriptorsList.add(widgetDescriptor);
    }

    public void setWidgetLabelDescriptor(String value){
        WidgetDescriptor widgetDescriptor = new WidgetDescriptor();
        widgetDescriptor.SetDescriptor(DescriptorType.ViewLabel,value);
        widgetDescriptorsList.add(widgetDescriptor);
    }

    public void setWidgetTagValueDescriptor(String value){
         WidgetDescriptor widgetDescriptor = new WidgetDescriptor();
         widgetDescriptor.SetDescriptor(DescriptorType.ViewTagValue,value);
         widgetDescriptorsList.add(widgetDescriptor);
     }

    public void setWidgetHintDescriptor(String value){
        WidgetDescriptor widgetDescriptor = new WidgetDescriptor();
        widgetDescriptor.SetDescriptor(DescriptorType.ViewHint,value);
        widgetDescriptorsList.add(widgetDescriptor);
    }

    public void setWidgetOptionDescriptor(boolean value){
        WidgetDescriptor widgetDescriptor = new WidgetDescriptor();
        widgetDescriptor.SetDescriptor(DescriptorType.OptionMenuDescription,String.valueOf(value));
        widgetDescriptorsList.add(widgetDescriptor);
    }

    public void setWidgetContentDescription(String value){
        WidgetDescriptor widgetDescriptor = new WidgetDescriptor();
        widgetDescriptor.SetDescriptor(DescriptorType.ViewContentDescription,value);
        widgetDescriptorsList.add(widgetDescriptor);
    }

    public String getWidgetIdDescriptorValue(){
        String viewId = "";
        for(WidgetDescriptor descriptor:widgetDescriptorsList)
            if(descriptor.getViewDescriptorType() == DescriptorType.ViewId)
                viewId = descriptor.getValue();
        return viewId;
    }

    public boolean getWidgetOptionMenuDescriptorValue(){
        boolean optionMenuValue = false;
        for(WidgetDescriptor descriptor:widgetDescriptorsList)
            if(descriptor.getViewDescriptorType() == DescriptorType.OptionMenuDescription)
                optionMenuValue = Boolean.parseBoolean(descriptor.getValue());
        return optionMenuValue;
    }

    public String getWidgetLabelDescriptorValue(){
        String viewLabel = "";
        for(WidgetDescriptor descriptor:widgetDescriptorsList)
            if(descriptor.getViewDescriptorType() == DescriptorType.ViewLabel)
                viewLabel = descriptor.getValue();
        return viewLabel;
    }

    public String getWidgetTagDescriptorValue(){
        String viewTag = "";
        for(WidgetDescriptor descriptor:widgetDescriptorsList)
            if(descriptor.getViewDescriptorType() == DescriptorType.ViewTagValue)
                viewTag = descriptor.getValue();
        return viewTag;
    }

    public String getWidgetHintDescriptorValue(){
        String viewHint = "";
        for(WidgetDescriptor descriptor:widgetDescriptorsList)
            if(descriptor.getViewDescriptorType() == DescriptorType.ViewHint)
                viewHint = descriptor.getValue();
        return viewHint;
    }

    public String getWidgetContentDescriptorValue(){
        String viewContent = "";
        for(WidgetDescriptor descriptor:widgetDescriptorsList)
            if(descriptor.getViewDescriptorType() == DescriptorType.ViewContentDescription)
                viewContent = descriptor.getValue();
        return viewContent;
    }

    public List<WidgetDescriptor> getWidgetDescriptorsList(){
        return widgetDescriptorsList;
    }
    public String getWidgetType(){
        return widgetType;
    }

    public boolean hasBindingViewId() {
        if(!getWidgetIdDescriptorValue().isEmpty())
            return true;
        return false;
    }
}
