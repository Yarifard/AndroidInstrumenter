package ir.ac.um.AndroidInstrumenter.Analysis.Instruments;

import com.github.javaparser.ast.body.MethodDeclaration;
import ir.ac.um.AndroidInstrumenter.widget.Widget;

import java.util.ArrayList;
import java.util.List;

public class MethodInformation {
    private String mainContext;
    private String context_title;
    private String context_description;
    private String title;
    private String name;
    private int level;
    private ViewInformation attachedView;
    private MethodDeclaration md;
    private List<Widget> usedWidgets;
    private boolean isEeventHandler;
    private boolean flag;
    private String sourceActivity;
    private String targetActivity;
    private List<MethodInformation> childMethodInformation;
    private MethodInformation parentMethodInformation = null;

    public MethodInformation(MethodInformation method){
        this.mainContext = "";
        this.context_title = "";
        this.context_description = "";
        this.title = "";
        this.name = method.getName();
        this.md = method.getAttachedMethod();
        this.level       = 0;
        this.flag = false;
        this.attachedView = new ViewInformation();
        this.usedWidgets = null;
        this.childMethodInformation = new ArrayList<>();
        this.targetActivity = "";
        this.sourceActivity = "";
    }

    public MethodInformation(){
        this.mainContext = "";
        this.context_title = "";
        this.context_description = "";
        this.title = "";
        this.name = "";
        this.md = null;
        this.level       = 0;
        this.attachedView = new ViewInformation();
        this.usedWidgets = null;
        this.childMethodInformation = new ArrayList<>();
        this.targetActivity = "";
        this.sourceActivity = "";
    }

    public boolean hasParent(){
        if(this.parentMethodInformation != null)
            return true;
        return false;
    }

    public String getMainContext(){ return  mainContext;}
    public String getContext_title(){return this.context_title;}

    public String getContext_description(){return this.context_description;}

    public String getTitle(){return this.title;}
    public boolean getFlag(){ return flag; }
    public String getAttachedViewId(){
        return attachedView.getViewId();

    }


//    public PsiMethod getPsiMethod(){ return  this.psiMethod; }

    public MethodInformation getParentEventHandlerInformation(){ return parentMethodInformation;}
    public List<MethodInformation> getChildEventHandlers(){ return childMethodInformation;}

    public String getTargetActivity(){ return targetActivity;}

    public String getSourceActivity(){ return sourceActivity;}

    public List<Widget> getUsedWidgets() {
        return usedWidgets;
    }

    public MethodDeclaration getAttachedMethod() {
        return md;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public String getAttachedViewLable() { return attachedView.getTitle(); }

    public String getAttachedViewContentDescription(){
        return attachedView.getContentDesciption();
    }

    public boolean hasChildEvents(){
        if(childMethodInformation.isEmpty())
            return false;
        return true;
    }

    public String getBindingName(){
        return attachedView.getBindingName();
    }
    public void loadDependentEvents(List<MethodInformation> depEventList){
        depEventList.addAll(depEventList);
    }

    public void setMainContext(String mainContext){ this.mainContext= mainContext;}
    public void setTitle(String title){ this.title = title;}

    public void setAttachedView(ViewInformation attachedView) {
        this.attachedView = attachedView;
    }

    public void setMethodDeclaration(MethodDeclaration md) {
        this.md = md;
    }

      public void setName(String name) {
        this.name = name;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setChildEventHandler(MethodInformation methodInformation){
        childMethodInformation.add(methodInformation);}
    public void setParentEventHandler(MethodInformation methodInformation){
        parentMethodInformation = methodInformation;}

    public void setUsedWidgets(List<Widget> usedWidgets) {
        this.usedWidgets = usedWidgets;
    }

    public void setContext_title(String context_title){ this.context_title = context_title;}

    public void setContext_description(String context_description){ this.context_description = context_description;}

    public void setSourceActivity(String sourceActivity){
        this.sourceActivity = sourceActivity;
    }

    public void setAttachedViewType(String viewType){
         attachedView.setViewType(viewType);
    }

    public void setAttachedViewId(String viewId){
        attachedView.setViewId(viewId);
    }

    public void setAttacheViewLabel(String viewLabel){
        attachedView.setTitle(viewLabel);
    }

    public void setAttachedViewTag(String viewTag){ attachedView.setViewTag(viewTag);}

    public void setAttachedViewBindingName(String bindingName){
        attachedView.setBindingName(bindingName);
    }

    public void setTargetActivity(String activityName){
        this.targetActivity = activityName;
    }

    public void setAttachedViewContentDescription(String contentDescription) {
        attachedView.setContentDesciption(contentDescription);

    }

    public boolean isOpenActivity(){
        boolean result = false;
        if(sourceActivity != targetActivity)
            if(targetActivity != "back")
                result = true;
        return result;
    }


    public void setEventHandler() {
        isEeventHandler = true;
    }

    public boolean isEeventHandler() {
        return isEeventHandler;
    }

    public String getAttachedViewType() {
        return attachedView.getViewType();
    }

    public void setFlag(boolean b) {
        this.flag = true;
    }
}
