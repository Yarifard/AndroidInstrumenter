package ir.ac.um.AndroidInstrumenter.Analysis.Instruments;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.intellij.psi.PsiFile;
import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles.LayoutInformationExtractor;
import ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles.MenuInformationExtractor;
import ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles.StringValueExtractor;
import ir.ac.um.AndroidInstrumenter.Taging.POSTagger;
import ir.ac.um.AndroidInstrumenter.Utils.ASTUtils;
import ir.ac.um.AndroidInstrumenter.Utils.DatabaseAdapter;
import ir.ac.um.AndroidInstrumenter.Utils.Utils;
import ir.ac.um.AndroidInstrumenter.widget.Widget;
import ir.ac.um.AndroidInstrumenter.widget.WidgetInfoExtrctor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActvityInstrumneter {
    private final boolean ExitPoint = true;
    private final boolean EntryPoint = false;
    private String activityClassName;
    private String activityClassFilePath;
    private CompilationUnit AST;
    private TypeDeclaration rootNode;
    private String valuesFileName;
    private List<String> widgetList;
    private List<Object> tmpWidgetsList;
    private List<MethodDeclaration> methods;
    private ProjectInformation projectInformation;
    private String mainLayoutFileName;
    private CodeAnalyzer codeAnalyzer;
    private List<String> includedLayoutFileNames = new ArrayList<>();
    private List<String> fragmentLayoutFileNames = new ArrayList<>();
    private String schemaFilePath;
    private String packageName;

    public ActvityInstrumneter(ProjectInformation projectInformation){
        this.projectInformation = projectInformation;
        this.schemaFilePath = projectInformation.getSchemaFilePath();
        this.packageName = projectInformation.getAppPackageName();
        this.activityClassName = "";
        this.valuesFileName = "strings";
        methods = new ArrayList<>();
        this.rootNode = null;
        this.AST = null;
        widgetList = new ArrayList<>();
        tmpWidgetsList = new ArrayList<>();
        mainLayoutFileName = "";
    }

    private String getBindingViewObjectNameOfInnerMethod(Node node) {
        String bindingViewName = "";
        if(!ASTUtils.isClassMember(node)){
            node = node.getParentNode().get();
            while(!(ASTUtils.isMethodCallExpr(node) || ASTUtils.isVariableDeclarator(node)))
                node = node.getParentNode().get();
            if(ASTUtils.isMethodCallExpr(node))
                bindingViewName = ASTUtils.getScope((MethodCallExpr) node);
        }
        return  bindingViewName;
    }

    private boolean isNotInsrumented(Node node) {
        if(!ASTUtils.isMethodCallExpr(ASTUtils.getAncientNode(node)))
            return  true;
        return false;

    }

    private List<MethodCallExpr> getViewBindingCallExprFrom(Node node){
        List<MethodCallExpr> bindingViewCallExprs = ASTUtils.getMethodCallExprsListByName(node,"findViewById");
        List<MethodCallExpr> targetCallExprs  = new ArrayList<>();
        for(MethodCallExpr callExpr :bindingViewCallExprs)
            if(callExpr.hasScope() && isNotInsrumented(callExpr))
                targetCallExprs.add(callExpr);
        return targetCallExprs;
    }



    private List<MethodCallExpr> getViewBindingMethodCallExprsDirectlyBy(Node node){
        List<Node> innerMethodList = ASTUtils.getInnerMethodsIn(node);
        List<MethodCallExpr>  callExprsList = new ArrayList<>();
        List<MethodCallExpr>  bindingViewCallExpr = getViewBindingCallExprFrom(node);
        for(Node innerMethod : innerMethodList)
            bindingViewCallExpr = ASTUtils.subtract(bindingViewCallExpr, getViewBindingCallExprFrom(innerMethod));
        return bindingViewCallExpr;
    }

    private boolean isGlobalVariable(List<VariableDeclarator> globalVariablesList, String objectName) {
        return isExistVariableInSet(globalVariablesList,objectName);
    }

    private boolean isExistVariableInSet(List<VariableDeclarator> variablesSet, String objectName){
        boolean result = false;
        if(objectName.contains("."))
            objectName = objectName.substring(0,objectName.indexOf('.'));
        for(VariableDeclarator variable : variablesSet)
            if(variable.getName().toString().equals(objectName)){
                result = true;
            }
        return result;
    }

    private List<VariableDeclarator> getLocalVariablesFromNode(Node node){
        List<VariableDeclarator> variableDeclaratorList = new ArrayList<>();
        node.findAll(VariableDeclarator.class).forEach(item->{
            variableDeclaratorList.add(item);
        });
        return variableDeclaratorList;
    }

    private List<VariableDeclarator> getLocalVariables(Node callerMethod) {
        List<VariableDeclarator> localVariablesList = new ArrayList<>();
        callerMethod.findAll(VariableDeclarator.class).forEach(item->{
            Node tmpNode = (Node) item;
            while(!ASTUtils.isMethodDeclartionExpr(tmpNode.getParentNode().get()))
                tmpNode = tmpNode.getParentNode().get();
            if(!callerMethod.isAncestorOf(tmpNode.getParentNode().get()))
                localVariablesList.add(item);
        });
        return localVariablesList;
    }

    private List<VariableDeclarator> getGlobalVariableList() {
        List<FieldDeclaration> fieldsList= new ArrayList<>();
        List<VariableDeclarator> globalVariablesList = new ArrayList<>();
        rootNode.findAll(FieldDeclaration.class).forEach(item->fieldsList.add(item));
        for(FieldDeclaration field:fieldsList)
            for(int i = 0; i<field.getVariables().size();i++)
                globalVariablesList.add(field.getVariable(i));
        return globalVariablesList;
    }

    private List<Node> getHighLevelInnerMethods(Node node){
        List<Node> innerMethods = ASTUtils.getInnerMethodsIn(node);
        List<Node> highLevelMethods = new ArrayList<>();
        for(Node item: innerMethods){
            if(ASTUtils.isSimilar(ASTUtils.getIncludedMethod(item),node))
                if(!(ASTUtils.isDialogMethod(item) || ASTUtils.isObjectCreationMethod(item)))
                    highLevelMethods.add(item);
        }
        return highLevelMethods;
    }

    private String resolveClassNameFromNode(Node node, String objectName) {
        List<VariableDeclarator> localVariablesList;
        if(objectName.contains("."))
            objectName = objectName.substring(0,objectName.indexOf('.'));
        localVariablesList = getLocalVariablesFromNode(node);
        if(ASTUtils.isLocalVariable(localVariablesList,objectName))
            return ASTUtils.getVariableType(localVariablesList,objectName);
        return "";
    }

    private String extractViewObjectNameFrom(MethodCallExpr callExpr){
        String objectName = "";
        Node node = (Node) callExpr;
        while(!ASTUtils.isVariableDeclarator(ASTUtils.getParentNode(node)))
            node = ASTUtils.getParentNode(node);
        return ((VariableDeclarator) ASTUtils.getParentNode(node)).getNameAsString();
    }

    private boolean isLocalMethod(Node method ) {

        if(ASTUtils.isClassOrInterfaceExpr(ASTUtils.getParentNode(method))){
            if(isListActivity())
                if(Utils.isMatchWithPattern(((MethodDeclaration) method).getNameAsString(),"onListItemClick"))
                    return false;
            return true;
        }
        else
            return false;
    }

    private String getDefaultDialogObjectName(Node method){
        return getBindingViewObjectNameOfInnerMethod(method);

    }

    private String getCoustomizedDialogObjectName(Node method) {
        String bindingViewObjectName = getBindingViewObjectNameOfInnerMethod(method);
        if (bindingViewObjectName.isEmpty())
            return "";
        Node node = ASTUtils.getIncludedBlockNode(method);
        List<MethodCallExpr> callExprsList = getViewBindingMethodCallExprsDirectlyBy(node);
        for (MethodCallExpr callExpr : callExprsList) {
            String extractedViewObjectName = extractViewObjectNameFrom(callExpr);
            if (extractedViewObjectName.contentEquals(bindingViewObjectName))
                if (callExpr.hasScope()) {
                    String object = ASTUtils.getScope(callExpr);
                    if (Utils.isPartialMatchWithPattern("Dialog", resolveClassNameFromNode(node, object)))
                        return object;
                }
        }
        return "";
    }

    private List<MethodCallExpr> getMethodCallExprsByScopeName(Node method, String scopeName) {
        List<MethodCallExpr> objectCalledMethodExpr = new ArrayList<>();
        method.findAll(MethodCallExpr.class).forEach(item->{
            if(item.hasScope())
                if(Utils.isMatchWithPattern(item.getScope().get().toString(),scopeName))
                    objectCalledMethodExpr.add(item);
        });
        return objectCalledMethodExpr;
    }

    private boolean containDialog(Node method) {
        boolean flag = false;
        List<MethodCallExpr> methodCallExprs = ASTUtils.getMethodCallExprsCalledDirectlyBy(method);
        for(MethodCallExpr methodCallExpr : methodCallExprs)
            if(Utils.isMatchWithPattern(methodCallExpr.getNameAsString(),"show"))
                if(methodCallExpr.hasScope()){
                    String objectName = ASTUtils.getScope(methodCallExpr);
                    String className = codeAnalyzer.resolveClassName(methodCallExpr,objectName);
                    if(Utils.isPartialMatchWithPattern(className,"Dialog"))
                        return true;
                }
        return false;
    }

    private String getDialogVariableName(Node node){
        Node tmpNode = ASTUtils.getIncludedBlockNode(node);
        List<MethodCallExpr> methodCallExprs = ASTUtils.getMethodCallExprsCalledDirectlyBy(tmpNode);
        for(MethodCallExpr methodCallExpr : methodCallExprs)
            if(Utils.isMatchWithPattern(methodCallExpr.getNameAsString(),"show"))
                if(methodCallExpr.hasScope()){
                    String objectName = ASTUtils.getScope(methodCallExpr);
                    String className = codeAnalyzer.resolveClassName(tmpNode,objectName);
                    if(Utils.isPartialMatchWithPattern(className,"Dialog"))
                        return objectName;
                }
        return "";
    }

    public String resolveClassName(Node callerMethod, String objectName) {
        List<VariableDeclarator> localVariablesList;
        Node involvedMethod = callerMethod;

        if(objectName.contains("."))
            objectName = objectName.substring(0,objectName.indexOf('.'));

        do{
            localVariablesList = getLocalVariables(involvedMethod);
            if(!localVariablesList.isEmpty())
               if(ASTUtils.isLocalVariable(localVariablesList,objectName))
                 return ASTUtils.getVariableType(localVariablesList,objectName);
            involvedMethod = ASTUtils.getParentBlock(involvedMethod);

        }while(involvedMethod != null);

        List<VariableDeclarator> globalVariablesList = getGlobalVariableList();
        if(isGlobalVariable(globalVariablesList,objectName))
            return ASTUtils.getVariableType(globalVariablesList,objectName);

        return "";

    }
    /*********************************************************************************
       This section contins methods that related about extracting GUI widget and
       layout file processing
     ********************************************************************************/

    private String getValueFilePath() {
        return projectInformation.getValuesDirectory().getCanonicalPath().toString() + "/" + "strings";
    }

    private String getLayoutFilePath(String layoutFileName) {
        return projectInformation.getLayoutsDirectory().getCanonicalPath().toString() + "/" + layoutFileName;
    }

    private void initializeLayoutFileName() {
        MethodDeclaration onCreateMethod = ASTUtils.getMethodByName(rootNode.getMethods(),"onCreate");
        MethodCallExpr setContentMethod = ASTUtils.getMethodCallExprByName(onCreateMethod,"setContentView");
        if(setContentMethod != null){
            mainLayoutFileName = setContentMethod.getArgument(0).toString();
            mainLayoutFileName = mainLayoutFileName.substring(mainLayoutFileName.lastIndexOf('.') + 1 );
        }
        else
            mainLayoutFileName = null;

    }

    private List<Widget> extractBindingVariableForGUIWidgetsFromSourceCode(List<Node> nodes,
                                                                        List<Widget> GUIWidgetList) {
        int indexParemeter = 0;
        for(Node node:nodes){
            if(!ASTUtils.isDialogMethod(node)){
                List<MethodCallExpr> methodCallExprs = ASTUtils.getMethodCallExprsListByName(node,"findViewById");
                for(Widget widget : GUIWidgetList)
                    for(MethodCallExpr callExpr : methodCallExprs)
                        if(!callExpr.getArguments().isEmpty())
                          if(Utils.isMatchWithPattern(ASTUtils.getArgumentFrom(callExpr,indexParemeter),widget.getWidgetIdDescriptorValue()))
                             widget.setBindingVariable(ASTUtils.getBindingVariableName(callExpr));
            }
        }
        return GUIWidgetList;
    }

    private List<Widget> generateLabelForUnBindingWidgets(List<Widget> GUIWidgetList) {
        for(int index = 0; index <GUIWidgetList.size(); index ++){
            if(GUIWidgetList.get(index).getBindingVariableName().isEmpty()){
                Widget widget = GUIWidgetList.get(index);
                String viewTypeName = widget.getWidgetType();
                if(Utils.isMatchWithPattern(viewTypeName,"MainMenuItem"))
                    viewTypeName = widget.getWidgetLabelDescriptorValue().replaceAll(" ", "");
                else
                   viewTypeName = StringUtils.lowerCase(viewTypeName + (index + 1));
                GUIWidgetList.get(index).setBindingVariable(viewTypeName);
            }
        }
        return GUIWidgetList;
    }

    public boolean isListActivity() {
        NodeList<ClassOrInterfaceType> extendList =
                ((ClassOrInterfaceDeclaration) rootNode).getExtendedTypes();
        for(ClassOrInterfaceType node: extendList)
            if(Utils.isMatchWithPattern(node.getNameAsString(),"ListActivity")){
                return true;
            }
        return false;
    }

    private List<Widget> getGUIWidgetList(TypeDeclaration node) {
        List<Widget> GUIWidgetList = new ArrayList<>();
        if(this.mainLayoutFileName != null){
            LayoutInformationExtractor layoutInformationExtractor =
                    new LayoutInformationExtractor(projectInformation,this.mainLayoutFileName);
            GUIWidgetList = layoutInformationExtractor.getGUIWidgetlist();
            if(layoutInformationExtractor.hasIncludedLayouts()){
                includedLayoutFileNames = layoutInformationExtractor.getIncludedLayout();
                for(String includedLayoutFileName: includedLayoutFileNames){
                    layoutInformationExtractor.setXmlFile(includedLayoutFileName);
                    GUIWidgetList.addAll(layoutInformationExtractor.getGUIWidgetlist());
                }
            }
            if(codeAnalyzer.isInflateLayoutByActionBar()){
                layoutInformationExtractor.setXmlFile(codeAnalyzer.getActionBarLayout());
                GUIWidgetList.addAll(layoutInformationExtractor.getGUIWidgetlist());
            }
            GUIWidgetList = extractBindingVariableForGUIWidgetsFromSourceCode(node.getMethods(),GUIWidgetList);
        }
        else
            if(codeAnalyzer.isListActivity())
                if(mainLayoutFileName == null){
                    Widget widget = new Widget();
                    widget.setWidgetType("ListView");
                    widget.setWidgetIdDescriptor("android.R.id.list");
                    widget.setBindingVariable("listItems");
                    GUIWidgetList.add(widget);
                }
        // GUIWidgetList = generateLabelForUnBindingWidgets(GUIWidgetList);
        MethodDeclaration onCreateOptionMenu = codeAnalyzer.findMethodByName("onCreateOptionsMenu");
        if(onCreateOptionMenu != null)
            GUIWidgetList.addAll(getMenuItems(onCreateOptionMenu));
        GUIWidgetList = generateLabelForUnBindingWidgets(GUIWidgetList);
        return GUIWidgetList;
    }


    private  List<Widget> getMenuItems(MethodDeclaration onCreateOptionsMenu) {
        List<Widget> widgetList = new ArrayList<>();
        List<String> menuItemIds = new ArrayList<>();
        Parameter parameter = onCreateOptionsMenu.getParameter(0);
        String menuLayout = "";
        if(ASTUtils.menuHasLayout(onCreateOptionsMenu))
           menuLayout = ASTUtils.extrctMenuLayout(onCreateOptionsMenu);
        List<MethodCallExpr> callExprs = ASTUtils.getMethodCallExprsListByName(onCreateOptionsMenu, "findItem");
        if (!callExprs.isEmpty())
            for (MethodCallExpr callExpr : callExprs)
                if (callExpr.hasScope()){
                    if(Utils.isMatchWithPattern(ASTUtils.getScope(callExpr),parameter.getNameAsString()))
                        if(Utils.isMatchWithPattern(parameter.getTypeAsString(),"Menu")){
                            Widget widget = new Widget();
                            widget.setWidgetType("MainMenuItem");
                            String menuItemId = callExpr.getArgument(0).toString();
                            widget.setWidgetIdDescriptor(menuItemId);
                            if(!menuLayout.isEmpty())
                                if(codeAnalyzer.isExistMenuItem(menuLayout,menuItemId)){
                                    widget.setWidgetLabelDescriptor(getMenuLabel(menuLayout,menuItemId));
                                    menuItemIds.add(menuItemId);
                                    widgetList.add(widget);
                                }
                        }
                }
        callExprs = ASTUtils.getMethodCallExprsListByName(onCreateOptionsMenu,"add");
        if(!callExprs.isEmpty())
            for(MethodCallExpr callExpr : callExprs)
                if(callExpr.hasScope()){
                    if(Utils.isMatchWithPattern(ASTUtils.getScope(callExpr),parameter.getNameAsString()))
                        if(Utils.isMatchWithPattern(parameter.getTypeAsString(),"Menu")){
                            Widget widget = new Widget();
                            widget.setWidgetType("MainMenuItem");
                            if(callExpr.getArguments().size() == 1){
                                String content = codeAnalyzer.getContent(ASTUtils.getArgument(callExpr,0));
                                widget.setWidgetLabelDescriptor(content);
                                widgetList.add(widget);
                            }
                            else{
                                    String menuItemId = ASTUtils.getArgument(callExpr,1);
                                    widget.setWidgetIdDescriptor(menuItemId);
                                    String content = codeAnalyzer.getContent(ASTUtils.getArgument(callExpr,3));
                                    widget.setWidgetLabelDescriptor(content);
                                    if(!menuItemIds.contains(menuItemId)){
                                         widgetList.add(widget);
                                         menuItemIds.add(menuItemId);
                                    }
                                    else{
                                        widget = findWidgetById(widgetList,menuItemId);
                                        if(widget != null)
                                            widget.setWidgetLabelDescriptor(content);
                                    }
                            }
                    }
                }

        return widgetList;
    }

    private Widget findWidgetById(List<Widget> widgetList, String menuItemId) {
        for(Widget widget:widgetList)
            if(Utils.isMatchWithPattern(widget.getWidgetIdDescriptorValue(),menuItemId))
                return widget;
        return null;
    }

    private String getMenuLabel(String menuLayout,String menuItemId) {
        MenuInformationExtractor menuInformationExtractor = new
                MenuInformationExtractor(projectInformation,menuLayout);
        return getStringValue(menuInformationExtractor.findViewLabelById(menuItemId));
    }

    private String getStringValue(String content) {
        if(content.startsWith("R.strings.")){
             StringValueExtractor stringValueExtractor =
                new StringValueExtractor(projectInformation,this.valuesFileName);
             return  stringValueExtractor.findViewLabelById(content);
        }
        return content;
    }

    private boolean hasStaticLayout(Node node) {
        List<String> patterns = new ArrayList<>(){{
            add("inflate");
            add("setContentView");
        }};
        List<MethodCallExpr> methodCallExprs = ASTUtils.getMethodCallExprsCalledDirectlyBy(node);
        for(MethodCallExpr methodCallExpr : methodCallExprs)
            if(Utils.isMatchWithPatterns(patterns,methodCallExpr.getNameAsString()))
                if(methodCallExpr.hasScope())
                   return  true;
        return false;
    }

    private String extractDescriptorValue(MethodCallExpr methodCallExpr) {
        String value = "";
        int argIndex = 0;
        StringValueExtractor stringValueExtractor = new StringValueExtractor(projectInformation,this.valuesFileName);
        if(!methodCallExpr.getArguments().isEmpty())
            if(ASTUtils.getArgumentFrom(methodCallExpr,argIndex).startsWith("R.string.")){
                String key = ASTUtils.getArgumentFrom(methodCallExpr,argIndex);
                key = key.substring(key.lastIndexOf('.') +1 );
                value = stringValueExtractor.findViewLabelById(key);
            }
            else if(ASTUtils.getArgumentFrom(methodCallExpr,argIndex).startsWith("\""))
                value = ASTUtils.getArgumentFrom(methodCallExpr,argIndex);
        return value;
    }

    private void extractWidgetDescriptorFromDynamicLayout(Node method, Widget widget) {
        List<MethodCallExpr> objectMethodCallExprs = getMethodCallExprsByScopeName(method,widget.getBindingVariableName());
        String value = "";
        for (MethodCallExpr methodCallExpr : objectMethodCallExprs)
            if (Utils.isPartialMatchWithPattern(methodCallExpr.getNameAsString(),"Hint")) {
                value = extractDescriptorValue(methodCallExpr);
                if (!value.isEmpty())
                    widget.setWidgetHintDescriptor(value);
            } else if (Utils.isPartialMatchWithPattern(methodCallExpr.getNameAsString(),"Tag")) {
                value = extractDescriptorValue(methodCallExpr);
                if (!value.isEmpty())
                    widget.setWidgetTagValueDescriptor(value);
            } else if (Utils.isPartialMatchWithPattern(methodCallExpr.getNameAsString(),"ContentDescription")) {
                value = extractDescriptorValue(methodCallExpr);
                if (!value.isEmpty())
                    widget.setWidgetContentDescription(value);
            } else if (Utils.isPartialMatchWithPattern(methodCallExpr.getNameAsString(),"Text")) {
                value = extractDescriptorValue(methodCallExpr);
                if (!value.isEmpty())
                    widget.setWidgetLabelDescriptor(value);
            }
    }

    private List<Widget> getTemporaryGUIWidgetList(Node method, List<Node> dialogEvents) {
        List<Widget> tmpGUIWidgetsList = new ArrayList<>();
        if(hasStaticLayout(method))
            tmpGUIWidgetsList = getTmpGUIWidgetsFromLayout(method);
        tmpGUIWidgetsList = extractTmpGUIWidgetsFromSourceCode(method, dialogEvents, tmpGUIWidgetsList);
        initializeDialogTitle(method,tmpGUIWidgetsList);
        for(Node dialogEvent : dialogEvents)
          if(ASTUtils.isDefaultDialogMethodPattern(dialogEvent))
              tmpGUIWidgetsList.add(extractDialogEeventBindinWidget(dialogEvent));
        tmpGUIWidgetsList = generateLabelForUnBindingWidgets(tmpGUIWidgetsList);
        return tmpGUIWidgetsList;
    }

    private void initializeDialogTitle(Node method, List<Widget> tmpGUIWidgetsList) {
        List<MethodCallExpr> methodCallExprs = ASTUtils.getMethodCallExprsListByName(method,"setTitle");
        List<VariableDeclarator> localVariable = ASTUtils.getLocalVariables(method);
        if(methodCallExprs.isEmpty()){
            for(Widget widget : tmpGUIWidgetsList)
                if(Utils.isMatchWithPattern(widget.getWidgetType(),"TextView")){
                    Widget widgetItem = widget;
                    widgetItem.setBindingVariable("dialogTitle");
                    tmpGUIWidgetsList.remove(widgetItem);
                    tmpGUIWidgetsList.add(0,widgetItem);
                    break;
                }
        }
        else {
              for(MethodCallExpr callExpr : methodCallExprs)
                  if(callExpr.hasScope())
                      if(Utils.isPartialMatchWithPattern(
                              resolveClassName(method, ASTUtils.getScope(callExpr)),"Dialog")){
                          int argIndex = 0;
                          String label = codeAnalyzer.getContent(ASTUtils.getArgumentFrom(callExpr,argIndex));
                          if(!label.isEmpty())
                             label = codeAnalyzer.extractValue(method,label);
                          Widget widget = new Widget();
                          widget.setWidgetType("TextView");
                          widget.setBindingVariable("dialogTitle");
                          widget.setWidgetIdDescriptor("android.R.id.title");
                          widget.setWidgetLabelDescriptor(label);
                          tmpGUIWidgetsList.add(0,widget);
                          break;
                      }

        }
    }

    private Widget extractDialogEeventBindinWidget(Node dialogEvent) {
        MethodInformation targetMethod = getMethodInformation(dialogEvent);
            Widget widget = new Widget();
            widget.setWidgetType(targetMethod.getAttachedViewType());
            if(!targetMethod.getAttachedViewId().isEmpty())
                widget.setWidgetIdDescriptor(targetMethod.getAttachedViewId());
            if(!targetMethod.getAttachedViewLable().isEmpty())
                widget.setWidgetLabelDescriptor(targetMethod.getAttachedViewLable());
            if(!targetMethod.getAttachedViewContentDescription().isEmpty())
                widget.setWidgetContentDescription(targetMethod.getAttachedViewContentDescription());
            return widget;
    }

    @NotNull
    private List<Widget> extractTmpGUIWidgetsFromSourceCode(Node method, List<Node> dialogEvents,
                                                            List<Widget> tmpGUIWidgetsList) {
        List<VariableDeclarator> localVariables = getLocalVariables(method);
        for(VariableDeclarator variableDeclarator : localVariables)
            if(Utils.isWidget(variableDeclarator.getTypeAsString())){
                if(!isUnbindingWithLayoutWidget(method,variableDeclarator)){
                    Widget widget = new Widget();
                    widget.setWidgetType(variableDeclarator.getTypeAsString());
                    widget.setBindingVariable(variableDeclarator.getNameAsString());
                    extractWidgetDescriptorFromDynamicLayout(method,widget);
                    tmpGUIWidgetsList.add(widget);
                }
            }
        return generateLabelForUnBindingWidgets(tmpGUIWidgetsList);
    }

    private boolean isUnbindingWithLayoutWidget(Node method, VariableDeclarator variableDeclarator) {
        List<MethodCallExpr> methodCallExprs = ASTUtils.getMethodCallExprsListByName(method,"findViewById");
        for(MethodCallExpr methodCallExpr : methodCallExprs){
            String assignedExpr = getAssignedVariable(methodCallExpr);
            if(Utils.isMatchWithPattern(assignedExpr,variableDeclarator.getNameAsString()))
                return true;

        }
        return false;
    }

    private String getAssignedVariable(Node node) {
        String variableName = "";
        while(!(ASTUtils.isAssignExpr(node) || ASTUtils.isClassOrInterfaceExpr(node) ||
                ASTUtils.isVariableDeclarator(node)))
            node = ASTUtils.getParentNode(node);
        if(ASTUtils.isVariableDeclarator(node))
            return ((VariableDeclarator) node).getNameAsString();
        if(ASTUtils.isAssignExpr(node)){
            return ((AssignExpr) node).getTarget().toString();
        }
        return  variableName;
    }

    @NotNull
    private List<Widget> getTmpGUIWidgetsFromLayout(Node method) {
        List<Widget> tmpGUIWidgetsList;
        String layoutFileName = getStaticLayout(method);
        LayoutInformationExtractor layoutInformationExtractor =
                new LayoutInformationExtractor(projectInformation,layoutFileName);
        tmpGUIWidgetsList = layoutInformationExtractor.getGUIWidgetlist();
        if(Utils.isMatchWithPattern(tmpGUIWidgetsList.get(0).getWidgetType(),"TextView"))
            tmpGUIWidgetsList.get(0).setBindingVariable("dialogTitle");
        tmpGUIWidgetsList = extractBindingVariableFortmpWidgetsFromSourceCode(method,tmpGUIWidgetsList);
        return tmpGUIWidgetsList;
    }

    private List<Widget> extractBindingVariableFortmpWidgetsFromSourceCode(Node method,
                                                                           List<Widget> GUIWidgetsList) {
        int indexParemeter = 0;
        List<MethodCallExpr> methodCallExprs = ASTUtils.getMethodCallExprsListByName(method,"findViewById");
        for(Widget widget : GUIWidgetsList)
            for(MethodCallExpr callExpr : methodCallExprs)
                if(!callExpr.getArguments().isEmpty())
                    if(Utils.isMatchWithPattern(ASTUtils.getArgumentFrom(callExpr,indexParemeter),widget.getWidgetIdDescriptorValue()))
                        widget.setBindingVariable(ASTUtils.getBindingVariableName(callExpr));

        return GUIWidgetsList;
    }

    private MethodInformation getMethodInformation(Node dialogEvent) {
        MethodInformation target = null;
        List<MethodInformation> methodInformationList = codeAnalyzer.getMethods();
        for(MethodInformation method:methodInformationList)
            if(Utils.isMatchWithPattern(method.getAttachedMethod().getBody().get().toString(),
                ((MethodDeclaration) dialogEvent).getBody().get().toString()))
                target = method;
        return target;
    }


    private String getStaticLayout(Node method) {
        String layoutFileName = "";
        List<String> patterns = new ArrayList<>(){{
            add("inflate");
            add("setContentView");
        }};

        int argIndex = 0;
        List<MethodCallExpr> methodCallExprs = ASTUtils.getMethodCallExprsByNamesDirectlyBy(method,patterns);
        for(MethodCallExpr methodCallExpr : methodCallExprs)
            if(Utils.isMatchWithPatterns(patterns,methodCallExpr.getNameAsString()))
                if(methodCallExpr.hasScope()){
                     layoutFileName = ASTUtils.getArgumentFrom(methodCallExpr,argIndex);
                     layoutFileName = layoutFileName.substring(layoutFileName.lastIndexOf('.')+1);
                     break;
                }
        return layoutFileName;
    }

    private boolean storeGUIWidgetIntoDatabase(String context,List<Widget> guiWidgetlist) {
        boolean result = true;
        if(guiWidgetlist.size() > 0) {
            DatabaseAdapter adapterDatabase = projectInformation.getDbAdapter();
                for (Widget widget : guiWidgetlist){
                    int widgetId = adapterDatabase.appendWidget(context, widget);
                    if(widgetId == -1)
                        return false;
                    widget.setWidgetDatabaseId(widgetId);
                }

        }
        return result;
    }

    /***********************************************************************************
       This section contains methods related about instrumenting codes
     ***********************************************************************************/
    private BlockStmt convertStatmentToBlockStatement(Statement statement) {
        return StaticJavaParser.parseBlock("{\n" + statement.toString() + "\n}");
    }

    private boolean isPositiveButtonOnClick(Node node){
        boolean result = false;
        Node tmpNode = node;
        while (!ASTUtils.isMethodCallExpr(tmpNode.getParentNode().get()))
            tmpNode = tmpNode.getParentNode().get();
        tmpNode = tmpNode.getParentNode().get();
        MethodCallExpr expr = (MethodCallExpr) tmpNode;
        if(Utils.isPartialMatchWithPattern(expr.getName().toString(),"setPositiveButton"))
            return true;
        if(expr.hasScope()){
            String objectName = ASTUtils.getScope(expr);
            if(!Utils.isPartialMatchWithPattern(resolveClassName(node,objectName),"Dialog"))
                if(!(Utils.isPartialMatchWithPattern(ASTUtils.getScope(expr),"cancel")
                   || Utils.isPartialMatchWithPattern(ASTUtils.getScope(expr),"Cancel")))
                    return true;
        }
        return result;
    }

    private void createAndPrepareSchemaFile(File file) {
        LocalDateTime localDateTime;
        localDateTime = LocalDateTime.now();
        try {
            file.createNewFile();
            FileWriter fwriter = new FileWriter(file,true);
            fwriter.append("// Declarations for " + packageName  + "\n");
            fwriter.append("// Declarations written by Chicory " + localDateTime + "\n\n");
            fwriter.append("decl-version 2.0\n");
            fwriter.append("var-comparability none\n\n");
            fwriter.flush();
            fwriter.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private String getGUIWidgetSchemaDescription(String packageName,String contextName,Widget widget) {
        WidgetInfoExtrctor widgetInfoExtrctor = new WidgetInfoExtrctor();
        String result = widgetInfoExtrctor.captureWidgetInfo(packageName,contextName,widget);
        return result;
    }

    private String getObjectSchemaDescription(String packageName,String contextName,
                                              List<Widget> GUIWidgetList
                                              ){
        String result = "";
        result += "parent parent " + packageName + "." + contextName + ":::OBJECT 1\n";
        result += "variable this\n";
        result += "  var-kind variable\n";
        result += "  dec-type " + packageName + "." + contextName + "\n";
        result += "  rep-type hashcode\n";
        result += "  flags is_param non_null\n";
        result += "  comparability 22\n";
        result += "  parent " + packageName + "." + contextName + ":::OBJECT 1\n";
        for(Widget widget : GUIWidgetList)
            result += getGUIWidgetSchemaDescription(packageName,contextName, widget);
        return result;
    }

    private String extractLabelForDialogMethod(MethodDeclaration method) {
        String dialogTitle = "", dialogMessage = "";
        List<String> patterns = new ArrayList<>(){{
            add("setTitle");
            add("setMessage");
        }};
        StringValueExtractor stringValueExtractor = new StringValueExtractor(
                projectInformation, this.valuesFileName);
        Node tmpNode = (Node) method.getParentNode().get();
        while (!ASTUtils.isMethodDeclartionExpr(tmpNode))
            tmpNode = tmpNode.getParentNode().get();
        final MethodDeclaration methodDeclaration = (MethodDeclaration) tmpNode;
        List<MethodCallExpr> callExprs = ASTUtils.getMethodCallExprsListByNames(tmpNode,patterns);
        int argIndex = 0;
        for (MethodCallExpr expr : callExprs) {
            if (Utils.isMatchWithPattern(expr.getNameAsString(),"setTitle")) {
                String objectName = ASTUtils.getScope(expr);
                String containingClassName = resolveClassName(methodDeclaration, objectName);
                if (Utils.isPartialMatchWithPattern(containingClassName,"Dialog")) {
                    if(!expr.getArguments().isEmpty())
                        if (ASTUtils.getArgumentFrom(expr,argIndex).startsWith("R.string."))
                            dialogTitle = stringValueExtractor.findViewLabelById(
                                    ASTUtils.getArgumentFrom(expr,argIndex));
                        else {
                            dialogTitle = ASTUtils.getArgumentFrom(expr,argIndex);
                            dialogTitle = dialogTitle.substring(1,dialogTitle.length()-1);
                        }

                }
            } else if (Utils.isMatchWithPattern(expr.getNameAsString(),"setMessage")) {
                String objectName = ASTUtils.getScope(expr);
                String containingClassName = resolveClassName(methodDeclaration, objectName);
                if (Utils.isPartialMatchWithPattern(containingClassName,"Dialog")) {
                    if (ASTUtils.getArgumentFrom(expr,argIndex).startsWith("R.string."))
                        dialogMessage = stringValueExtractor.findViewLabelById(
                                ASTUtils.getArgumentFrom(expr,argIndex));
                    else{
                        dialogMessage = expr.getArgument(0).toString();
                        dialogMessage = dialogMessage.substring(1,dialogMessage.length()-1);

                    }
                }
            }
        }
        POSTagger posTagger =  projectInformation.getPosTagger();
        String label = "";
        if (dialogTitle != "")
            label = posTagger.generateLabelforDialog("I want to " + dialogTitle.toLowerCase());
        else if (dialogMessage != "")
            label = posTagger.generateLabelforDialog(dialogMessage);
        return label;
    }

    private String getDialogMethodSignature(Node method) {
        return  codeAnalyzer.getMethodLabel((MethodDeclaration) method) +
                codeAnalyzer.getMethodBindingWidgetType((MethodDeclaration) method)+
                "_" + StringUtils.capitalize(((MethodDeclaration)method).getNameAsString());
    }

    private String getInnerMethodSignature(Node method){
        String block = "";
        Node tmpNode = (Node) method;
        while (!ASTUtils.isMethodCallExpr(tmpNode.getParentNode().get()))
            tmpNode = tmpNode.getParentNode().get();
        tmpNode = tmpNode.getParentNode().get();
        MethodCallExpr expr = (MethodCallExpr) tmpNode;
        String methodName = ASTUtils.getScope(expr);
        block += methodName;
        if(!Utils.isMatchWithPattern(((MethodDeclaration)method).getNameAsString(),"onClick"))
            block += "_" + ((MethodDeclaration)method).getNameAsString();
        else{
            if(Utils.isPartialMatchWithPattern(expr.getNameAsString(),"On") &&
               Utils.isPartialMatchWithPattern(expr.getNameAsString(),"Click"))
                block += "_" + expr.getNameAsString().substring(expr.getNameAsString().indexOf('O'), expr.getNameAsString().lastIndexOf('L'));
        }
        return block;
    }

    private String getLocalMethodSignatureInEntryPointForSchemaDescrription(String packageName,
                                                                            String contextName,
                                                                            Node method) {
        return "\n\nppt " + packageName + "." + contextName + "." + ((MethodDeclaration)method).getNameAsString() +"():::ENTER\n";
    }

    private String getDialogMethodSignatureInEntryPointForSchemaDescription(String packageName,
                                                                            String mainClassName, String subClassName,
                                                                            Node method) {
        String result = "\n\nppt " + packageName + "." + mainClassName + "$" + subClassName/*getDialogMethodSignature(method)*/ +
                "." + codeAnalyzer.getMethodLabel((MethodDeclaration) method) +
                codeAnalyzer.getMethodBindingWidgetType((MethodDeclaration) method) +
                "_" + StringUtils.capitalize(((MethodDeclaration)method).getNameAsString()) +"():::ENTER\n";;
        return result;
    }

    private String getInnerMethodSignatureInEntryPointForSchemaDecsrription( String packageName,String contextName,
                                                                            Node method) {
        if(isListActivity())
            if(Utils.isMatchWithPattern(((MethodDeclaration) method).getNameAsString(),"onListItemClick"))
                return "\n\nppt " + packageName + "." + contextName + "." +  "listItems_onItemClick" +
                        "():::ENTER\n";
        return "\n\nppt " + packageName + "." + contextName + "." +
                getInnerMethodSignature(method) +
                "():::ENTER\n";
    }

    private String getMethodEntryPointSchemaDescription(String packageName,
                                                        String mainClassName,
                                                        Node method,
                                                        List<Widget> guiWidgetList) {
        String result = "\n\n";
        if(isLocalMethod(method)){
            result += getLocalMethodSignatureInEntryPointForSchemaDescrription(packageName,mainClassName,method);
            result += "ppt-type enter\n";
            result += getObjectSchemaDescription(packageName,mainClassName,guiWidgetList);
        }
        else{

            result += getInnerMethodSignatureInEntryPointForSchemaDecsrription(packageName,mainClassName,method);
            result += "ppt-type enter\n";
            result += getObjectSchemaDescription(packageName,mainClassName,guiWidgetList);
        }
        return result;
    }

    private String getInnerMethodSignatureInExitPointForSchemaDescription(String packageName,String contextName,
                                                                           MethodDeclaration method, Integer exitPoint) {
        if(isListActivity())
            if(Utils.isMatchWithPattern(((MethodDeclaration) method).getNameAsString(),"onListItemClick"))
                return "\n\nppt " + packageName + "." + contextName + "." +  "listItems_onItemClick" +
                        "():::EXIT" + exitPoint + "\n";
        return "\n\nppt " + packageName + "." + contextName + "." +  getInnerMethodSignature(method) +
                "():::EXIT" + exitPoint + "\n";
    }

    private String getLocalMethodSignatureInExitPointForSchemaDescrription(String packageName,String contextName,
                                                                           MethodDeclaration method, Integer exitPoint) {
        return "\n\nppt " + packageName + "." + contextName + "." + method.getNameAsString() +"():::EXIT" + exitPoint + "\n";

    }

    private String getDialogMethodSignatureInExitPointForSchemaDescription(String packageName,String mainClassName,
                                                                           String subClassName,
                                                                           Node method, Integer exitPoint) {
        String result = "\n\nppt " + packageName + "." + mainClassName + "$" + subClassName +
                "." + codeAnalyzer.getMethodLabel((MethodDeclaration) method) +
                codeAnalyzer.getMethodBindingWidgetType((MethodDeclaration) method) + "_" +
                StringUtils.capitalize(((MethodDeclaration) method).getNameAsString()) + "():::EXIT" + exitPoint + "\n";
        return result;
    }

    private void extractMethodExitPoints(BlockStmt blockStatement,List<Integer> list) {
        BlockStmt blockStmt;
        for (int index = 0; index < blockStatement.getStatements().size(); index++) {
            if (blockStatement.getStatement(index).isIfStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                IfStmt ifStmt = blockStatement.getStatements().get(index).asIfStmt();
                BlockStmt thenBlockStmts;
                if (ifStmt.getThenStmt().isBlockStmt())
                    thenBlockStmts = ifStmt.getThenStmt().asBlockStmt();
                else
                    thenBlockStmts = convertStatmentToBlockStatement(ifStmt.getThenStmt());
                if (thenBlockStmts.toString().contains("return")) {
                    extractMethodExitPoints(thenBlockStmts, list);
                    ifStmt.setThenStmt((Statement) thenBlockStmts);
                }
                if (ifStmt.hasElseBlock()) {
                    BlockStmt elseBlockStmts;
                    if (ifStmt.getElseStmt().get().toString().contains("return")) {
                        if (ifStmt.getElseStmt().get().isBlockStmt())
                            elseBlockStmts = ifStmt.getElseStmt().get().asBlockStmt();
                        else
                            elseBlockStmts = convertStatmentToBlockStatement(ifStmt.getElseStmt().get());
                        extractMethodExitPoints(elseBlockStmts, list);
                        ifStmt.setElseStmt((Statement) elseBlockStmts);
                    }
                }
                blockStatement.getStatements().replace(blockStatement.getStatement(index), (Statement) ifStmt);
            } else if (blockStatement.getStatements().get(index).isForStmt() &&
                    blockStatement.getStatements().get(index).toString().contains("return")) {
                ForStmt forStmt = blockStatement.getStatement(index).asForStmt();
                if (forStmt.getBody().isBlockStmt())
                    blockStmt = forStmt.getBody().asBlockStmt();
                else
                    blockStmt = convertStatmentToBlockStatement(forStmt.getBody());
                extractMethodExitPoints(blockStmt, list);
                forStmt.setBody(blockStmt);
                blockStatement.getStatements().replace(blockStatement.getStatement(index), forStmt);
            } else if (blockStatement.getStatement(index).isForEachStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                ForEachStmt forEachStmt = blockStatement.getStatement(index).asForEachStmt();
                if (forEachStmt.getBody().isBlockStmt())
                    blockStmt = forEachStmt.getBody().asBlockStmt();
                else
                    blockStmt = convertStatmentToBlockStatement(forEachStmt.getBody());
                extractMethodExitPoints(blockStmt, list);
                // forEachStmt.setBody(blockStmt);
                // blockStatement.getStatements().replace(blockStatement.getStatement(index),forEachStmt);
            } else if (blockStatement.getStatement(index).isSwitchStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                SwitchStmt switchStmt = blockStatement.getStatement(index).asSwitchStmt();
                NodeList<SwitchEntry> switchEntries = switchStmt.getEntries();
                for (SwitchEntry switchEntry : switchEntries) {
                    if (switchEntry.toString().contains("return")) {
                        blockStmt = new BlockStmt();
                        blockStmt.setStatements(switchEntry.getStatements());
                        extractMethodExitPoints(blockStmt, list);
                        // switchEntry.setStatements(blockStmt.getStatements());
                    }
                }

            } else if (blockStatement.getStatement(index).isDoStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                DoStmt doStmt = blockStatement.getStatement(index).asDoStmt();
                if (doStmt.getBody().isBlockStmt())
                    blockStmt = doStmt.getBody().asBlockStmt();
                else
                    blockStmt = convertStatmentToBlockStatement(doStmt.getBody());
                extractMethodExitPoints(blockStmt, list);
            } else if (blockStatement.getStatement(index).isWhileStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                WhileStmt whileStmt = blockStatement.getStatement(index).asWhileStmt();
                if (whileStmt.getBody().isBlockStmt())
                    blockStmt = whileStmt.getBody().asBlockStmt();
                else
                    blockStmt = convertStatmentToBlockStatement(whileStmt.getBody());
                extractMethodExitPoints(blockStmt, list);
            } else if (blockStatement.getStatement(index).isTryStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                TryStmt tryStmt = blockStatement.getStatement(index).asTryStmt();
                blockStmt = tryStmt.getTryBlock();
                extractMethodExitPoints(blockStmt, list);
            } else if (blockStatement.getStatement(index).isReturnStmt()) {
                list.add(blockStatement.getStatement(index).getEnd().get().line);
            }
        }
    }


    private String getMethodExitPointSchemaDescription(String packageName, String contextName,
                                                       MethodDeclaration method,
                                                       List<Widget> guiWidgetList) {
        String result = "\n\n";
        List<Integer> exitPoints = new ArrayList<>();

        extractMethodExitPoints(method.getBody().get(),exitPoints);
        if(method.getType().toString().contentEquals("void")){
            int index = method.getBody().get().getStatements().size();
            if(index > 0){
                exitPoints.add(method.getBody().get().getStatement(index -1).getEnd().get().line);
            }
            else
                exitPoints.add(method.getBegin().get().line + 1);
        }

        for(Integer exitPoint :exitPoints){
            if(isLocalMethod(method))
                result += getLocalMethodSignatureInExitPointForSchemaDescrription(packageName,contextName,
                        method,exitPoint);
            else
                result += getInnerMethodSignatureInExitPointForSchemaDescription(packageName,contextName,
                        method,exitPoint);
            result += "ppt-type subexit\n";
            result += getObjectSchemaDescription(packageName,contextName,guiWidgetList);

        }
        return result;
    }

    private String extactInfoFromDefaultDialog(Node method,String targetCallExpr) {
        String content = "";
        int argIndex = 0;
        MethodCallExpr callExpr = ASTUtils.getMethodCallExprByName(method,targetCallExpr);
        if(callExpr != null){
            String objectName = callExpr.getScope().get().toString();
            String containingClassName = resolveClassName(method, objectName);
            if (Utils.isPartialMatchWithPattern(containingClassName,"Dialog")) {
                if (ASTUtils.getArgumentFrom(callExpr,argIndex).startsWith("R.string.")){
                    StringValueExtractor stringValueExtractor = new StringValueExtractor(
                            projectInformation,this.valuesFileName);
                    content = stringValueExtractor.findViewLabelById(ASTUtils.getArgumentFrom(callExpr,argIndex));
                    return content;
                }
                else {
                    content = ASTUtils.getArgumentFrom(callExpr,argIndex);
                    content = content.substring(1,content.length() - 1);
                    return content;
                }
            }
        }
        return "";
    }

    private String extractDialogTitle(Node node){
        String callExprsPattern = "setTitle";
        int argIndex = 0;
        if(hasStaticLayout(node)){
            LayoutInformationExtractor staticLayoutInfoExractor = new LayoutInformationExtractor(projectInformation,getStaticLayout(node));
            String dialogTitleId = staticLayoutInfoExractor.extractDialogTitleId();
            if(dialogTitleId.isEmpty()){
                String content = staticLayoutInfoExractor.extractDialogTitle();
                if(content.startsWith("@string")){
                    StringValueExtractor stringValueExtractor =
                            new StringValueExtractor(projectInformation,this.valuesFileName);
                    String titlId = content.substring(9);
                    return stringValueExtractor.findViewLabelById(titlId);
                }
                else
                 return content;
            }
            else{
                dialogTitleId = "R.id." + dialogTitleId.substring(dialogTitleId.lastIndexOf('/') + 1);
                String viewObjectName = "";
                List<MethodCallExpr> bindingViewCallExprList = getViewBindingMethodCallExprsDirectlyBy(node);
                for(MethodCallExpr bindingCallExpr: bindingViewCallExprList)
                    if (Utils.isMatchWithPattern(dialogTitleId,ASTUtils.getArgumentFrom(bindingCallExpr, argIndex))){
                        viewObjectName = extractViewObjectNameFrom(bindingCallExpr);
                        break;
                    }
                if(viewObjectName.isEmpty()){
                    String content = staticLayoutInfoExractor.extractDialogTitle();
                    if(content.startsWith("@string")){
                        StringValueExtractor stringValueExtractor =
                                new StringValueExtractor(projectInformation,this.valuesFileName);
                        String titlId = content.substring(9);
                        return stringValueExtractor.findViewLabelById(titlId);
                    }
                    return content;

                }
                List<MethodCallExpr> callExprList = ASTUtils.getMethodCallExprsByNameDirectlyBy(node,"setText");
                for(MethodCallExpr item :callExprList)
                    if(item.hasScope())
                        if(Utils.isMatchWithPattern(ASTUtils.getScope(item),viewObjectName))
                            if(!ASTUtils.getArgumentFrom(item,argIndex).startsWith("R.string."))
                                return ASTUtils.getArgumentFrom(item,argIndex);
                            else{
                                StringValueExtractor stringValueExtractor =
                                        new StringValueExtractor(projectInformation,this.valuesFileName);
                                String id = ASTUtils.getArgumentFrom(item,argIndex).substring(9);
                                return stringValueExtractor.findViewLabelById(id);
                            }
            }
        }
        return extactInfoFromDefaultDialog(node,callExprsPattern);
    }

    private String extractDialogMsg(Node node){
        String callExprsPattern = "setMessage";
        if(hasStaticLayout(node)){
            LayoutInformationExtractor staticLayout = new LayoutInformationExtractor(projectInformation,getStaticLayout(node));
            return staticLayout.extractDialogTitle();
        }
        return extactInfoFromDefaultDialog(node,callExprsPattern);
    }

    private String generateLabel(String content){
        POSTagger posTagger = projectInformation.getPosTagger();
        String label = "";
        if (content != "")
            label = posTagger.generateLabelforDialog( content.toLowerCase());
        return label;
    }

    private String extractLabelForInnerCLass(Node node) {
        String dialogTitle = "", dialogMessage = "";
        String label = "";
        dialogTitle = extractDialogTitle(node);
        if (!dialogTitle.isEmpty())
            label = generateLabel(dialogTitle);
        if (label.isEmpty()) {
            dialogMessage = extractDialogMsg(node);
            if (!dialogMessage.isEmpty())
                label = generateLabel(dialogMessage);
        }
        return label;
    }

    private List<Node> getBlocksContainDialog(Node method){
        List<Node> resultBlocks = new ArrayList<>();
        List<ObjectCreationExpr> objectCreationExprs = method.findAll(ObjectCreationExpr.class);
        for(ObjectCreationExpr objectCreationExpr : objectCreationExprs)
            if(Utils.isPartialMatchWithPattern(objectCreationExpr.getTypeAsString(),"Dialog")
                    && ASTUtils.isAssignExpr(ASTUtils.getParentNode(objectCreationExpr)))
                resultBlocks.add((Node) objectCreationExpr);

        List<VariableDeclarator> variableDeclarators = method.findAll(VariableDeclarator.class);
        for(VariableDeclarator variableDeclarator : variableDeclarators)
            if(Utils.isPartialMatchWithPattern(variableDeclarator.getTypeAsString(),"Dialog"))
                resultBlocks.add((Node) variableDeclarator);
        return resultBlocks;

    }

    private void generateDialogSchema( FileWriter fileWriter,String mainClassName,Node method
                                     , List<Widget> GUIWidgetList) throws IOException {
        if(containDialog((MethodDeclaration) method)){
            List<Node> blocksContainDialog = getBlocksContainDialog(method);
            for(Node blockContainDialog : blocksContainDialog){
                String content = "";
                BlockStmt containBlock = ASTUtils.getParentBlock(blockContainDialog);
                int index = containBlock.getStatements().size();
                List<Node> dialogMethods = ASTUtils.getDialogMethodsFrom(containBlock);
                if(!dialogMethods.isEmpty()){
                    String subClassName = codeAnalyzer.getSubClassNameOf((MethodDeclaration) dialogMethods.get(0));//extractLabelForInnerCLass(containBlock);
                    // Inner class constructor schema description-- Enter point
                    fileWriter.append("\n\nppt " + packageName + "." + mainClassName +
                            "$" + subClassName  + "." + subClassName + "(" + packageName + "." + mainClassName + "):::ENTER");
                    fileWriter.append("\nppt-type enter\n");
                    List<Widget> tmpGUIWidgetsList = getTemporaryGUIWidgetList(containBlock,dialogMethods);
                    content = getObjectSchemaDescription(packageName,mainClassName,GUIWidgetList);
                    content = content.substring(content.indexOf("\n") + 1, content.length());
                    content = content.replaceAll("\n  parent " + packageName + "." + mainClassName + ":::OBJECT 1", "");
                    content = content.replaceAll("this", packageName + "." + mainClassName + ".this");
                    fileWriter.append(content);
                    //Inner class constructor schema description --Exit point
                    fileWriter.append("\n\nppt " + packageName + "." + mainClassName +
                            "$" + subClassName  + "." + subClassName + "(" + packageName + "." + mainClassName + "):::EXIT" +
                            containBlock.getStatement(index -1).getEnd().get().line);
                    fileWriter.append("\nppt-type subexit\n");
                    String context = mainClassName + "$" + subClassName;// +"Dialog";
                    if(!storeGUIWidgetIntoDatabase(context,tmpGUIWidgetsList)){
                        Utils.showMessage("There is a problem in saving tmpGUIWidgetList into databse");
                        return;
                    }
                    content = getObjectSchemaDescription(packageName,mainClassName,tmpGUIWidgetsList);
                    content = content.replaceAll(packageName + "." + mainClassName,
                            packageName + "." + mainClassName + "\\$" + subClassName /* + "Dialog"*/);
                    fileWriter.append(content);
                    content = getObjectSchemaDescription(packageName,mainClassName,GUIWidgetList);
                    content = content.substring(content.indexOf("\n") +1, content.length());
                    content = content.replaceAll("this",packageName + "." + mainClassName + ".this");
                    content = content.replaceAll(packageName + "." + mainClassName + ":::",
                            packageName + "." + mainClassName + "\\$" + subClassName + ":::" /*""Dialog:::"*/);
                    fileWriter.append(content);
                    fileWriter.flush();
                    index = 0;
                    List<Integer> exitPoints = new ArrayList<>();
                    for(Node dialogMethod : dialogMethods) {
                        fileWriter.append("\n" + getDialogMethodSignatureInEntryPointForSchemaDescription(packageName,
                                mainClassName,subClassName,dialogMethod));
                        fileWriter.append("ppt-type enter\n");
                        content = getObjectSchemaDescription(packageName,mainClassName, tmpGUIWidgetsList);
                        content = content.replaceAll(packageName + "." + mainClassName,
                                packageName + "." + mainClassName + "\\$" + subClassName /*+ "Dialog"*/);
                        fileWriter.append(content);
                        content = getObjectSchemaDescription(packageName,mainClassName, GUIWidgetList);
                        content = content.substring(content.indexOf("\n") +1, content.length());
                        content = content.replaceAll("this", packageName + "." + mainClassName + ".this");
                        content = content.replaceAll(packageName + "." + mainClassName + ":::",
                                packageName + "." + mainClassName + "\\$" + subClassName + ":::" /*"Dialog:::"*/);
                        fileWriter.append(content);
                        extractMethodExitPoints(((MethodDeclaration)dialogMethod).getBody().get(),exitPoints);
                        if(((MethodDeclaration)dialogMethod).getType().toString().contentEquals("void")){
                            index = ((MethodDeclaration)dialogMethod).getBody().get().getStatements().size();
                            // int exitPoint = 0;
                            if(index > 0){
                                exitPoints.add(((MethodDeclaration)dialogMethod).getBody().get().getStatement(index -1).getEnd().get().line);
                            }
                            else
                                exitPoints.add(dialogMethod.getBegin().get().line + 1);
                        }
                        for(Integer exitpoint : exitPoints){
                            fileWriter.append("\n" + getDialogMethodSignatureInExitPointForSchemaDescription(packageName,
                                    mainClassName,subClassName,dialogMethod,exitpoint));
                            fileWriter.append("ppt-type subexit\n");
                            content = getObjectSchemaDescription(packageName,mainClassName, tmpGUIWidgetsList);
                            content = content.replaceAll(packageName + "." + mainClassName,
                                    packageName + "." + mainClassName + "\\$" + subClassName /*+ "Dialog"*/);
                            fileWriter.append(content);
                            content = getObjectSchemaDescription(packageName,mainClassName, GUIWidgetList);
                            content = content.substring(content.indexOf("\n") +1, content.length());
                            content = content.replaceAll("this", packageName + "." + mainClassName + ".this");
                            content = content.replaceAll(packageName + "." + mainClassName + ":::",
                                    packageName + "." + mainClassName + "\\$" + subClassName + ":::"/*"Dialog:::"*/);
                            fileWriter.append(content);
                        }
                        exitPoints.clear();
                    }
                    fileWriter.append("\n\nppt " + packageName + "." + mainClassName +
                            "$" + subClassName + ":::OBJECT");
                    fileWriter.append("\nppt-type object\n");
                    content = getObjectSchemaDescription(packageName,mainClassName,tmpGUIWidgetsList);
                    content = content.substring(content.indexOf("\n") +1, content.length());
                    content = content.replaceAll("\n  parent "+ packageName + "." + mainClassName + ":::OBJECT 1", "");
                    content = content.replaceAll(packageName + "." + mainClassName,
                            packageName + "." + mainClassName + "\\$" +
                                    subClassName /*+ "Dialog"*/);
                    fileWriter.append(content);
                    content = getObjectSchemaDescription(packageName,mainClassName,GUIWidgetList);
                    content = content.substring(content.indexOf("\n") +1, content.length());
                    content = content.replaceAll("\n  parent " + packageName + "." + mainClassName + ":::OBJECT 1", "");
                    content = content.replaceAll("this",packageName + "." + mainClassName + ".this");
                    content = content.replace("var-kind variable","var-kind variable\n  enclosing-var this");
                    fileWriter.append(content);
                    fileWriter.flush();
                }

            }
        }
        List<Node> highLevelInnerMethods = getHighLevelInnerMethods(method);
        for(Node item:highLevelInnerMethods)
            generateDialogSchema(fileWriter,mainClassName,item,GUIWidgetList);

    }

    private void generateActivitySchema(TypeDeclaration node) throws IOException {
        List<Node> innerMethods = new ArrayList<>();
        String contextName = node.getNameAsString();
        List<Widget> GUIWidgetlist = getGUIWidgetList(node);
        if(!storeGUIWidgetIntoDatabase(contextName,GUIWidgetlist)){
            Utils.showMessage("There is a problem in saving GUIWidgetList into databse");
            return;
        }
        DatabaseAdapter adapter = projectInformation.getDbAdapter();
        GUIWidgetlist = adapter.loadGUIWidgets(contextName);
        File file = new File(schemaFilePath,"Schema.txt");
        try{
            if(!file.exists()){
               createAndPrepareSchemaFile(file);
            }
            FileWriter fileWriter = new FileWriter(file,true);
            fileWriter.append("\n\nppt " + packageName + "." + contextName + "."
                   + node.getNameAsString() +"():::ENTER" );
            fileWriter.append("\nppt-type enter\n\n");

            fileWriter.append("\n\nppt " + packageName + "." + contextName + "." +
                   node.getNameAsString() +  "():::EXIT" + node.getRange().get().begin.line);
            fileWriter.append("\nppt-type subexit\n");
            fileWriter.append(getObjectSchemaDescription(packageName,contextName, GUIWidgetlist));
            methods = rootNode.getMethods();
            for(MethodDeclaration method : methods){
                innerMethods = ASTUtils.getInnerMethods(method);
                for(Node innerMethod : innerMethods) {
                    if(!(ASTUtils.isDialogMethod(innerMethod) ||
                         ASTUtils.isOuterMethod(innerMethod) ||
                         ASTUtils.isObjectCreationMethod(innerMethod))){
                        fileWriter.append(getMethodEntryPointSchemaDescription(packageName,contextName,
                                innerMethod, GUIWidgetlist));
                        fileWriter.append(getMethodExitPointSchemaDescription(packageName,contextName,
                                (MethodDeclaration) innerMethod, GUIWidgetlist));
                    }
                }
                innerMethods.clear();
                fileWriter.flush();
            }

            fileWriter.append("\n\nppt " + packageName + "." + contextName + ":::OBJECT");
            fileWriter.append("\nppt-type object\n");
            String content = getObjectSchemaDescription(packageName,contextName,GUIWidgetlist);
            content = content.substring(content.indexOf("\n") + 1, content.length());
            content = content.replaceAll("\n  parent " + packageName + "." + contextName + ":::OBJECT 1", "");
            fileWriter.append(content);
            fileWriter.flush();

            for(MethodDeclaration method : methods){
               generateDialogSchema(fileWriter,contextName,method,GUIWidgetlist);
            }
            fileWriter.flush();
            fileWriter.close();

        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    /********************************************************************
        This section contains methods that implemenet code instrrumention
     *********************************************************************/
    private void instrumentFieldsInActivityClass(TypeDeclaration node) {
        FieldDeclaration fieldDeclaration = new FieldDeclaration();
        VariableDeclarator variableDeclarator = new VariableDeclarator();
        variableDeclarator.setName("mainMenu");
        variableDeclarator.setType("Menu");
        variableDeclarator.setInitializer("null");
        fieldDeclaration.getVariables().add(variableDeclarator);
        fieldDeclaration.setModifiers(Modifier.Keyword.PRIVATE);
        node.getMembers().add(0,fieldDeclaration);

        fieldDeclaration = new FieldDeclaration();
        variableDeclarator = new VariableDeclarator();
        variableDeclarator.setName("widgetList");
        variableDeclarator.setType("List<Widget>");
        variableDeclarator.setInitializer("new ArrayList<>()");
        fieldDeclaration.getVariables().add(variableDeclarator);
        fieldDeclaration.setModifiers(Modifier.Keyword.PRIVATE);
        node.getMembers().add(1,fieldDeclaration);

//        fieldDeclaration = new FieldDeclaration();
//        variableDeclarator = new VariableDeclarator();
//        variableDeclarator.setName("AddOnLibrary");
//        variableDeclarator.setType("Instrumenter");
//        variableDeclarator.setInitializer("new Instrumenter()");
//        fieldDeclaration.getVariables().add(variableDeclarator);
//        fieldDeclaration.setModifiers(Modifier.Keyword.PRIVATE);
//        node.getMembers().add(2,fieldDeclaration);
    }

    private ConstructorDeclaration createInstrumentedConstructorMethodForActivityClass(
            TypeDeclaration activityClass) {
        int index = 0;
        ConstructorDeclaration constrcutor = activityClass.addConstructor(Modifier.Keyword.PUBLIC);
        constrcutor.setName(activityClass.getName().toString());
        String stmt = "super();";
        Statement statement = StaticJavaParser.parseStatement(stmt);
        constrcutor.getBody().addStatement(index, statement);
        return constrcutor;
    }

    private MethodDeclaration createEmptyWidgetListMethodForActivityClass(TypeDeclaration activityClass) {
        int index = 0;
        String stmt = "";
        Statement statement;
        MethodDeclaration instrumentedMethod = activityClass.addMethod(
                "emptyWidgetList", Modifier.Keyword.PRIVATE);
        stmt = "widgetList.clear();";
        statement = StaticJavaParser.parseStatement(stmt);
        instrumentedMethod.getBody().get().addStatement(index++, statement);
        return instrumentedMethod;
    }

    private MethodDeclaration createInitailWidgetListMethodForActivityClass(TypeDeclaration activityClass){

        String blockStmt = "";
        Statement statement;
        MethodDeclaration instrumentedMethod = activityClass.addMethod(
                "initialWidgetList", Modifier.Keyword.PRIVATE);
        DatabaseAdapter adapter = projectInformation.getDbAdapter();
            List<Widget> widgetList = adapter.loadGUIWidgets(activityClass.getNameAsString());
            blockStmt = "{\n";
            blockStmt += "Widget widget;\n";
            for(Widget widget :widgetList)
                if(widget.hasBindingViewId()){
                    blockStmt += "widget = new Widget();\n";
                    blockStmt += "widget.setWidgetDatabaseId(" + widget.getWidgetDatabaseId() + ");\n";
                    blockStmt += "widget.setWidgetType(\"" + widget.getWidgetType() + "\");\n";

                    if(widget.getBindingVariableName().length() > 0)
                        blockStmt += "widget.setViewName(\"" + widget.getBindingVariableName() + "\");\n";
                    else
                        blockStmt += "widget.setViewName(\"\");\n";

                    if(Utils.isMatchWithPattern(widget.getWidgetType(),"MainMenuItem"))
                        blockStmt += "if(mainMenu != null)" +
                                "\n\twidget.setView(mainMenu.findItem(" + widget.getWidgetIdDescriptorValue() +"));\n";
                    else if(Utils.isMatchWithPattern(widget.getWidgetType(),"ContextMenuItem"))
                        blockStmt += "widget.setView(contextMenu.findItem(" + widget.getWidgetIdDescriptorValue() +"));\n";
                    else
                        blockStmt += "widget.setView((View) findViewById(" + widget.getWidgetIdDescriptorValue() + "));\n";

                    blockStmt += "widgetList.add(widget);\n";
            }
            blockStmt += "}\n";
            BlockStmt blockStatement = StaticJavaParser.parseBlock(blockStmt);
            int index = 0;
            for(Statement stmt : blockStatement.getStatements() )
                instrumentedMethod.getBody().get().addStatement(index++, stmt);
        return instrumentedMethod;
    }

    private int findIndexOfStatementInBlock(Node node,String pattern){
        BlockStmt blockStmt = (BlockStmt) node;
        for(int index = 0; index < blockStmt.getStatements().size(); index++)
            if(Utils.isPartialMatchWithPattern(blockStmt.getStatement(index).toString(),pattern))
                return index;
        return -1;
    }

    private int getIndexOfFirstDialogMethod(Node dialogBlock){
        List<Node> dialogMethodList = ASTUtils.getDialogMethodsFrom(dialogBlock);
        if(dialogMethodList.isEmpty())
            return -1;
        Node node =  ASTUtils.getAncientNode(ASTUtils.getParentNode(dialogMethodList.get(0)));
        return findIndexOfStatementInBlock(dialogBlock,node.toString());

    }

    private void instrumenStatementsForGatheringTemporaryWidgetList(String contextName, MethodDeclaration method) {
        List<Node> dialogBlocks = getBlocksContainDialog(method);
        DatabaseAdapter adapter = projectInformation.getDbAdapter();
        for(Node dialogBlock :dialogBlocks){
            Node block = ASTUtils.getIncludedBlockNode(dialogBlock);
            BlockStmt containBlock = ASTUtils.getParentBlock(dialogBlock);
            List<Node> dialogMethods = ASTUtils.getDialogMethodsFrom(containBlock);
            if(!dialogMethods.isEmpty()){
                Statement stmt = StaticJavaParser.parseStatement("List<Widget> tmpWidgetList = new ArrayList<>();");
                ((BlockStmt)block).addStatement(0, stmt);
                String subClassName = codeAnalyzer.getSubClassNameOf((MethodDeclaration) dialogMethods.get(0));
                String context = contextName + "$" + subClassName;
                List<Widget> tmpWidgetList = adapter.loadtmpGUIWidgets(context);
                if (tmpWidgetList.size() > 0) {
                    String blockStmt = "{\n";
                    blockStmt += "Widget widget;\n";
                    for (Widget widget : tmpWidgetList) {
                        blockStmt += "widget = new Widget();\n";
                        blockStmt += "widget.setWidgetDatabaseId(" + widget.getWidgetDatabaseId() + ");\n";
                        blockStmt += "widget.setWidgetType(\"" + widget.getWidgetType() + "\");\n";
                        blockStmt += "widget.setViewName(\""  + widget.getBindingVariableName() + "\");\n";
                        if (!widget.getWidgetIdDescriptorValue().isEmpty()) {
                            switch (widget.getWidgetType()) {
                                case "TextView":
                                    if (Utils.isPartialMatchWithPattern(widget.getBindingVariableName(),
                                            "dialogTitle"))
                                        if (widget.getWidgetIdDescriptorValue().startsWith("android.R.id")) {
                                            blockStmt += "TextView dialogTitle = ";
                                            blockStmt += "(TextView) new TextView(getApplicationContext());\n";
                                            blockStmt += "dialogTitle.setText(\"" + widget.getWidgetLabelDescriptorValue() + "\");\n";
                                            blockStmt += "dialogTitle.setVisibility(View.VISIBLE);\n";
                                            blockStmt += "dialogTitle.setEnabled(true);\n";
                                            blockStmt += "widget.setView((View) dialogTitle);\n";
                                        } else
                                            blockStmt += "widget.setView((View) " +
                                                    getDialogVariableName(dialogBlock) +
                                                    ".findViewById(" + widget.getWidgetIdDescriptorValue() + "));\n";
                                    break;
                                case "Button":
                                    if(widget.getWidgetIdDescriptorValue().isEmpty())
                                        blockStmt += "widget.setView((View) " + widget.getBindingVariableName() +");\n";
                                    else{
                                        if (widget.getWidgetIdDescriptorValue().startsWith("android.R.id.button")) {
                                            String viewId = widget.getWidgetIdDescriptorValue();
                                            String identifier = viewId.substring(viewId.lastIndexOf('.') + 1);
                                            blockStmt += "widget.setView((View) " +
                                                    getDialogVariableName(dialogBlock);
                                            switch (identifier) {
                                                case "button1":
                                                    blockStmt += ".getButton(DialogInterface.BUTTON_POSITIVE));\n";
                                                    break;
                                                case "button2":
                                                    blockStmt += ".getButton(DialogInterface.BUTTON_NEGATIVE));\n";
                                                    break;
                                                case "button3":
                                                    blockStmt += ".getButton(DialogInterface.BUTTON_NEUTRAL));\n";
                                                    break;
                                            }
                                        }
                                        else
                                            blockStmt += "widget.setView((View) " +
                                                    getDialogVariableName(dialogBlock) +
                                                    ".findViewById(" + widget.getWidgetIdDescriptorValue() + "));\n";
                                    }
                                    break;
                            }
                        }
                        else
                            blockStmt +="widget.setView((View) " + widget.getBindingVariableName() + ");\n";
                        blockStmt += "tmpWidgetList.add(widget);\n";
                    }
                    blockStmt += "}\n";
                    BlockStmt blockStatement = StaticJavaParser.parseBlock(blockStmt);
                    //int index = getIndexOfFirstDialogMethod(block);
                    int index = getIndexOfDialogShowMethod(block);
                    System.out.println("Index is :" + index + "\n");
                    if(index >= 0)
                        for (Statement statement : blockStatement.getStatements())
                            ((BlockStmt)block).addStatement(++index, statement);
                }
            }

        }

    }

    private int getIndexOfDialogShowMethod(Node block) {
        int index  = -1;
        List<Node> blockContainDialog = getBlocksContainDialog(block);
        for(Node dialogItem :blockContainDialog){
            String dialogName = getDialogVariableName(dialogItem);
            Node targetBlock = getBlockContainDialogShowMethod(dialogItem);
            MethodCallExpr callExpr = getDialogShowMethodCallExpr(dialogItem,dialogName);
            if(callExpr.hasScope())
                if (Utils.isMatchWithPattern(callExpr.getScope().get().toString(), dialogName)) {
                    String pattern = callExpr.toString() + ";";
                    return findIndexOfStatementInBlock(targetBlock, pattern);
                }
        }
        return index;
    }

    private BlockStmt locateAndInstrumentExitPoint(String contextName, MethodDeclaration method,
                                                   BlockStmt blockStatement) {
        BlockStmt blockStmt;
        for(int index = 0; index < blockStatement.getStatements().size(); index++) {
            if (blockStatement.getStatement(index).isIfStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                IfStmt ifStmt = blockStatement.getStatements().get(index).asIfStmt();
                BlockStmt thenBlockStmts;
                if (ifStmt.getThenStmt().isBlockStmt())
                    thenBlockStmts = ifStmt.getThenStmt().asBlockStmt();
                else
                    thenBlockStmts = convertStatmentToBlockStatement(ifStmt.getThenStmt());
                if (thenBlockStmts.toString().contains("return")) {
                    thenBlockStmts = locateAndInstrumentExitPoint(contextName, method, thenBlockStmts);
                    ifStmt.setThenStmt((Statement) thenBlockStmts);
                }
                if (ifStmt.hasElseBlock()) {
                    BlockStmt elseBlockStmts;
                    if (ifStmt.getElseStmt().get().toString().contains("return")) {
                        if (ifStmt.getElseStmt().get().isBlockStmt())
                            elseBlockStmts = ifStmt.getElseStmt().get().asBlockStmt();
                        else
                            elseBlockStmts = convertStatmentToBlockStatement(ifStmt.getElseStmt().get());
                        elseBlockStmts = locateAndInstrumentExitPoint(contextName, method, elseBlockStmts);
                        ifStmt.setElseStmt((Statement) elseBlockStmts);
                    }
                }
                blockStatement.getStatements().replace(blockStatement.getStatement(index), (Statement) ifStmt);
            } else if (blockStatement.getStatements().get(index).isForStmt() &&
                    blockStatement.getStatements().get(index).toString().contains("return")) {
                ForStmt forStmt = blockStatement.getStatement(index).asForStmt();
                if (forStmt.getBody().isBlockStmt())
                    blockStmt = forStmt.getBody().asBlockStmt();
                else
                    blockStmt = convertStatmentToBlockStatement(forStmt.getBody());
                blockStmt = locateAndInstrumentExitPoint(contextName, method, blockStmt);
                forStmt.setBody(blockStmt);
                blockStatement.getStatements().replace(blockStatement.getStatement(index), forStmt);
            } else if (blockStatement.getStatement(index).isForEachStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                ForEachStmt forEachStmt = blockStatement.getStatement(index).asForEachStmt();
                if (forEachStmt.getBody().isBlockStmt())
                    blockStmt = forEachStmt.getBody().asBlockStmt();
                else
                    blockStmt = convertStatmentToBlockStatement(forEachStmt.getBody());
                blockStmt = locateAndInstrumentExitPoint(contextName, method, blockStmt);
                forEachStmt.setBody(blockStmt);
                blockStatement.getStatements().replace(blockStatement.getStatement(index), forEachStmt);
            } else if (blockStatement.getStatement(index).isSwitchStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                SwitchStmt switchStmt = blockStatement.getStatement(index).asSwitchStmt();
                NodeList<SwitchEntry> switchEntries = switchStmt.getEntries();
                for (SwitchEntry switchEntry : switchEntries) {
                    if (switchEntry.toString().contains("return")) {
                        blockStmt = new BlockStmt();
                        blockStmt.setStatements(switchEntry.getStatements());
                        blockStmt = locateAndInstrumentExitPoint(contextName, method, blockStmt);
                        switchEntry.setStatements(blockStmt.getStatements());
                    }
                }
                switchStmt.setEntries(switchEntries);

            } else if (blockStatement.getStatement(index).isDoStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                DoStmt doStmt = blockStatement.getStatement(index).asDoStmt();
                if (doStmt.getBody().isBlockStmt())
                    blockStmt = doStmt.getBody().asBlockStmt();
                else
                    blockStmt = convertStatmentToBlockStatement(doStmt.getBody());
                blockStmt = locateAndInstrumentExitPoint(contextName, method, blockStmt);
                doStmt.setBody(blockStmt);
                blockStatement.getStatements().replace(blockStatement.getStatement(index), doStmt);

            } else if (blockStatement.getStatement(index).isWhileStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                WhileStmt whileStmt = blockStatement.getStatement(index).asWhileStmt();
                if (whileStmt.getBody().isBlockStmt())
                    blockStmt = whileStmt.getBody().asBlockStmt();
                else
                    blockStmt = convertStatmentToBlockStatement(whileStmt.getBody());
                blockStmt = locateAndInstrumentExitPoint(contextName, method, blockStmt);
                whileStmt.setBody(blockStmt);
                blockStatement.getStatements().replace(blockStatement.getStatement(index), whileStmt);

            } else if (blockStatement.getStatement(index).isTryStmt() &&
                    blockStatement.getStatement(index).toString().contains("return")) {
                TryStmt tryStmt = blockStatement.getStatement(index).asTryStmt();
                blockStmt = tryStmt.getTryBlock();
                blockStmt = locateAndInstrumentExitPoint(contextName, method, blockStmt);
                tryStmt.setTryBlock(blockStmt);
                blockStatement.getStatements().replace(blockStatement.getStatement(index), tryStmt);

            } else if (blockStatement.getStatement(index).isReturnStmt()) {
                String block = "";
                String returnType = method.getType().toString();
                int exitPoint = blockStatement.getStatement(index).getEnd().get().line;
                if (isLocalMethod(method)) {
                    block += "{\n" +
                            "initialWidgetList();\n";
//                    if (!returnType.contentEquals("void")) {
//                        String returnValue = blockStatement.getStatement(index).asReturnStmt().getExpression().get().toString();
//                        block += "AddOnLibrary.logDataInExitPointByActivityEventHandler(this,\"" + packageName + "\",\n\"" +
//                                 contextName + "\",\"" +
//                                 method.getNameAsString() + "\",widgetList," + exitPoint +
//                                 ",\nAddOnLibrary.getReturnBlock(\"" + returnType + "\",\n" + returnValue + "));\n";
//                    }
//                    else
                        block += "AddOnLibrary.logDataInExitPointByActivityEventHandler(this,\"" + packageName + "\",\n\"" + contextName +
                                 "\",\"" + method.getNameAsString() + "\",widgetList," + exitPoint +
                                 ",\"\");\n";
                    block += "emptyWidgetList();\n" +
                            "}\n";

                } else if (ASTUtils.isDialogMethod(method)) {
                    String dialogObject = "";
                    if (ASTUtils.isDefaultDialogMethodPattern(method))
                        dialogObject = getDefaultDialogObjectName(method);
                    else
                        dialogObject = getCoustomizedDialogObjectName(method);
                    String subClassName = codeAnalyzer.getSubClassNameOf(method);
                    block += "{\n" +
                            "initialWidgetList();\n";

//                    if (!returnType.contentEquals("void")) {
//                        String returnValue = blockStatement.getStatement(index).asReturnStmt().getExpression().get().toString();
//                        block += "AddOnLibrary.logDataInExitPointByDialogEventHandler(" + contextName + ".this,\"" +
//                                packageName + "\",\"" + contextName + "\"," + dialogObject + ",\"" +
//                                subClassName + "\",\""+ getDialogMethodSignature(method) + "\",widgetList,tmpWidgetList," +
//                                exitPoint + ",AddOnLibrary.getReturnBlock(\"" + returnType + "\"," + returnValue + "));\n";
//                    }
//                    else
                        block += "{\n" +
                                "initialWidgetList();\n" +
                                "AddOnLibrary.logDataInExitPointByDialogEventHandler(" + contextName + ".this,\"" +
                                packageName + "\",\n\"" + contextName + "\"," + dialogObject +
                                ",\"" + subClassName + "\",\"" + getDialogMethodSignature(method) + "\",widgetList,tmpWidgetList,"
                                + exitPoint + ",\"\");\n";

                    block += "emptyWidgetList();" +
                             "}\n";

                } else {
                    block += "{\n" +
                            "initialWidgetList();\n";
//                    if (!returnType.contentEquals("void")) {
//                        String returnValue = blockStatement.getStatement(index).asReturnStmt().getExpression().get().toString();
//                        block += "AddOnLibrary.logDataInExitPointByActivityEventHandler(" + contextName + ".this,\"" +
//                                  packageName + "\",\n\"" + contextName + "\",\"" +
//                                  getInnerMethodSignature(method) + "\"," +
//                                  "widgetList," + exitPoint +
//                                  ",\nAddOnLibrary.getReturnBlock(\"" + returnType + "\"," + returnValue + "));\n";
//
//                    }
//                    else
                        block +="AddOnLibrary.logDataInExitPointByActivityEventHandler(" + contextName + ".this,\"" +
                                packageName + "\",\n\"" + contextName + "\",\"" +
                                getInnerMethodSignature(method) + "\"," +
                                "widgetList," + exitPoint + ",\"\");\n";

                    block += "emptyWidgetList();" +
                            "}\n";

                }
                blockStmt = StaticJavaParser.parseBlock(block);
                blockStmt.addStatement(blockStatement.getStatement(index));
                //blockStatement.getStatements().replace(blockStatement.getStatement(index), (Statement) blockStmt);
                blockStatement.getStatements().remove(index);
                for (Statement statement : blockStmt.getStatements())
                    blockStatement.addStatement(statement);
                break;
            }
        }
        return blockStatement;
    }

    private void instrumentForCaptureInnerClass(String mainClassName,MethodDeclaration method){
        String subClassName = "";
        List<Node> blockContainDialog = getBlocksContainDialog(method);
        for(Node dialogItem :blockContainDialog){
            String dialogName = getDialogVariableName(dialogItem);
            Node block = getBlockContainDialogShowMethod(dialogItem);
            MethodCallExpr callExpr = getDialogShowMethodCallExpr(dialogItem,dialogName);
            if(callExpr.hasScope()){
                if(Utils.isMatchWithPattern(callExpr.getScope().get().toString(),dialogName)){
                    String pattern = callExpr.toString() + ";";
                    int index = findIndexOfStatementInBlock(block,pattern);
                    int exitPoint = ((BlockStmt) block).getStatement(((BlockStmt) block).getStatements().size()-1).getEnd().get().line;
                    BlockStmt containBlock = ASTUtils.getParentBlock(dialogItem);
                    List<Node> dialogMethods = ASTUtils.getDialogMethodsFrom(containBlock);
                    if(!dialogMethods.isEmpty()){
                        subClassName = codeAnalyzer.getSubClassNameOf((MethodDeclaration) dialogMethods.get(0));
                        String stmts = "{" +
                                "initialWidgetList();";
                        stmts  += "AddOnLibrary.captureInnerClass(" +  mainClassName + ".this,\"" + mainClassName + "\",\n" +
                                "\"" + packageName + "\",\"" + subClassName + "\"," + callExpr.getScope().get() + ",widgetList,tmpWidgetList," + exitPoint + ");\n";
                        stmts  += "emptyWidgetList();}";
                        BlockStmt blockStmts = StaticJavaParser.parseBlock(stmts);
                        for(Statement stmt:blockStmts.getStatements())
                           ((BlockStmt) block).addStatement(++index,stmt);

                    }
                }
            }
        }
    }

    private Node getBlockContainDialogShowMethod(Node dialogItem) {
        Node  block = dialogItem;
        while(block != null){
            block = ASTUtils.getIncludedBlockNode(block);
            String dialogName = getDialogVariableName(dialogItem);
            List<MethodCallExpr> callExprs = ASTUtils.getMethodCallExprsListByName(block,"show");
            for(MethodCallExpr callExpr : callExprs)
                if(callExpr.hasScope())
                    if(Utils.isMatchWithPattern(ASTUtils.getScope(callExpr),dialogName))
                        return block;
        }
        return null;
    }

    private MethodCallExpr getDialogShowMethodCallExpr(Node dialogItem,String dialogName) {
        Node  block = dialogItem;
        while(block != null){
            block = ASTUtils.getIncludedBlockNode(block);
            //String dialogName = getDialogVariableName(dialogItem);
            List<MethodCallExpr> callExprs = ASTUtils.getMethodCallExprsListByName(block,"show");
            for(MethodCallExpr callExpr : callExprs)
                if(callExpr.hasScope())
                    if(Utils.isMatchWithPattern(ASTUtils.getScope(callExpr),dialogName))
                        return callExpr;
        }
        return null;
    }

    private void instrumentExitPoint(TypeDeclaration activity, MethodDeclaration method) {

        String contextName = activity.getNameAsString();
        if(!method.getType().toString().contentEquals("void")){
            if(containDialog(method))
                instrumentForCaptureInnerClass(contextName,method);
            method.setBody(locateAndInstrumentExitPoint(contextName,method,method.getBody().get()));

        }
        else {
            int index = method.getBody().get().getStatements().size();
            int exitPoint = 0;
            if (index > 0) {
                exitPoint = method.getBody().get().getStatement(index - 1).getEnd().get().line;
            } else
                exitPoint = method.getBegin().get().line + 1;

            if(containDialog(method))
                instrumentForCaptureInnerClass(contextName,method);
            method.setBody(locateAndInstrumentExitPoint(contextName,method,method.getBody().get()));

            String block = ""; //getStartSectionOfInstrumentedCodesAsString();
            if (isLocalMethod(method)) {
                block += "{\n";
                block += "initialWidgetList();\n";

                block += "AddOnLibrary.logDataInExitPointByActivityEventHandler(this,\"" + packageName + "\",\"" +
                        contextName + "\",\"" + method.getNameAsString() + "\",widgetList," + exitPoint + ",\"\");\n" +
                        "emptyWidgetList();\n";
                block += "}\n";
            } else if (ASTUtils.isDialogMethod(method)) {
                String dialogObject = "";
                if (ASTUtils.isDefaultDialogMethodPattern(method))
                    dialogObject = getDefaultDialogObjectName(method);
                else
                    dialogObject = getCoustomizedDialogObjectName(method);
                block += "{\n" +
                         "initialWidgetList();\n";
               // getIncludedMethod(method);
                String subClass = codeAnalyzer.getSubClassNameOf(method);
                block += "AddOnLibrary.logDataInExitPointByDialogEventHandler(" + contextName + ".this,\"" + packageName +
                        "\",\"" + contextName + "\"," + dialogObject + ",\"" + subClass + "\",\"" +
                        getDialogMethodSignature(method) + "\", widgetList,tmpWidgetList," + exitPoint + ",\"\");" +
                        "emptyWidgetList();\n" +
                        "}\n";
            }
            else{
                block += "{\n" +
                        "initialWidgetList();\n" +
                        "AddOnLibrary.logDataInExitPointByActivityEventHandler(" + contextName + ".this,\"" + packageName +
                        "\",\"" + contextName + "\",\"";
                if (isListActivity()){
                    if (Utils.isMatchWithPattern(((MethodDeclaration) method).getNameAsString(), "onListItemClick"))
                        block += "listItems_onItemClick";
                    else
                        block += getInnerMethodSignature(method);
                }
                else
                    block += getInnerMethodSignature(method);
                block += "\"," +
                        "widgetList," + exitPoint + ",\"\");\n" +
                        "emptyWidgetList();\n" +
                        "}\n";
            }

            BlockStmt blockStmt = StaticJavaParser.parseBlock(block);
            for (Statement statement : blockStmt.getStatements())
                method.getBody().get().addStatement(statement);
        }
    }

    private void instrumentEntryPoint(TypeDeclaration activity, MethodDeclaration method) {
        String block = "";
        String contextName = activity.getNameAsString();
        if(containDialog(method))
              instrumenStatementsForGatheringTemporaryWidgetList(contextName,method);

        if(isLocalMethod(method)) {
            block = "{\n" +
                    "initialWidgetList();\n";
            if (Utils.isMatchWithPattern(method.getNameAsString(), "onCreate")) {
                int exitPoint = activity.getRange().get().begin.line;
                block += "AddOnLibrary.constructor(this,\"" + packageName + "\",\n\"" + contextName + "\",widgetList," + exitPoint + ");";
            }

            block += "AddOnLibrary.logDataInEntryPointByActivityEventHandler(this,\"" + packageName + "\",\n\"" + contextName + "\",\"" +
                    method.getNameAsString() + "\",widgetList);\n" +
                    "emptyWidgetList();\n" +
                    "}\n";
        }
        else if(ASTUtils.isDialogMethod(method)) {
            String dialogObject = "";
            if (ASTUtils.isDefaultDialogMethodPattern(method))
                dialogObject = getDefaultDialogObjectName(method);
            else
                dialogObject = getCoustomizedDialogObjectName(method);
            block = "{\n" +
                    " initialWidgetList();\n";
            String subClassName = codeAnalyzer.getSubClassNameOf(method);
            block += "AddOnLibrary.logDataInEntryPointByDialogEventHandler(" + contextName + ".this,\"" + packageName +
                    "\",\n\"" + contextName + "\"," + dialogObject + ",\"" + subClassName +
                    "\",\"" + getDialogMethodSignature(method) + "\",widgetList,tmpWidgetList);\n" +
                    "emptyWidgetList();\n" +
                    "}\n";
        }
        else {
            block = "{\n" +
                    " initialWidgetList();\n" +
                    "AddOnLibrary.logDataInEntryPointByActivityEventHandler(" + contextName + ".this,\"" +
                    packageName + "\",\n\"" + contextName + "\",\"";
            if (isListActivity()){
                if (Utils.isMatchWithPattern(((MethodDeclaration) method).getNameAsString(), "onListItemClick"))
                    block += "listItems_onItemClick";
                else
                    block += getInnerMethodSignature(method);
            }
            else
                block += getInnerMethodSignature(method);
            block += "\"," + "widgetList);\n" +
                    "emptyWidgetList();\n" +
                    "}\n";
        }
        BlockStmt blockStmt = StaticJavaParser.parseBlock(block);
        int index = 0;
        for(Statement statement : blockStmt.getStatements())
            method.getBody().get().addStatement(index++,statement);
    }

    private void instrumentActivityMethods(TypeDeclaration activity, List<MethodDeclaration> methods) {
        List<Node> innerMethods = new ArrayList<>();
        for (MethodDeclaration method : methods) {
            if(!ASTUtils.isObjectCreationMethod(method))
                innerMethods = ASTUtils.getInnerMethods(method);
                for (Node innerMethod : innerMethods) {
                    if(!ASTUtils.isObjectCreationMethod(innerMethod)){
                    instrumentExitPoint(activity, (MethodDeclaration) innerMethod);
                    instrumentEntryPoint(activity, (MethodDeclaration) innerMethod);
                    }
                }
                innerMethods.clear();

        }
    }

    private void instrumentActivity(TypeDeclaration activityClass){

        instrumentFieldsInActivityClass(activityClass);
        createInitailWidgetListMethodForActivityClass(activityClass);
        createEmptyWidgetListMethodForActivityClass(activityClass);
        initialMainMenu();
       // initialContextMenu();
        instrumentActivityMethods(activityClass,methods);
    }

    private void initialMainMenu() {
        MethodDeclaration onCreateOptionMenu = codeAnalyzer.findMethodByName("onCreateOptionsMenu");
        if(onCreateOptionMenu != null){
            int location = onCreateOptionMenu.getBody().get().getStatements().size();
            Statement statement = StaticJavaParser.parseStatement("mainMenu = menu;");
            if(location > 1)
               onCreateOptionMenu.getBody().get().addStatement(location - 1 ,statement);
        }
    }

    public void initialActivityNode() {
        AST.getTypes().forEach(node -> {
            if (node.getClass().toString().contains("ClassOrInterfaceDeclaration"))
                rootNode = node;
        });
    }

    public void initialLayouts() {
        initializeLayoutFileName();
        if(mainLayoutFileName != null){
            LayoutInformationExtractor layoutInformationExtractor = new LayoutInformationExtractor(projectInformation,
                    mainLayoutFileName);
            if (layoutInformationExtractor.hasIncludedLayouts())
                includedLayoutFileNames = layoutInformationExtractor.getIncludedLayout();
        }
        else
            includedLayoutFileNames = null;
    }

    private  List<String> getImports(List<ImportDeclaration> importList){
        List<String> importTitleList = new ArrayList<>();
        for(ImportDeclaration importDeclarationItem : importList)
            importTitleList.add(importDeclarationItem.getNameAsString());
        return importTitleList;
    }

    private  void appendImports() {
        List<String> imports = new ArrayList<>(){{
            add("java.util.ArrayList");
            add("java.util.List");
            add("android.widget.Button");
            add("android.widget.TextView");
            add("android.view.Menu");

        }};
        List<String> importDeclarationList =  getImports(AST.getImports());
        for(String importItem: imports)
            if(!importDeclarationList.contains(importItem))
                AST.addImport(importItem);
        String packageName = projectInformation.getAppPackageTitle();
        AST.addImport(packageName + ".Instrumenter.AddOnLibrary");
        AST.addImport(packageName + ".Instrumenter.Widget");
    }

    private void initial(PsiFile activityClassFile){
        try {
            this.activityClassName = getActivityClassName(activityClassFile);
            this.activityClassFilePath = activityClassFile.getVirtualFile().getCanonicalPath();
            this.AST =  StaticJavaParser.parse(new File(activityClassFilePath));
            initialActivityNode();
            initialLayouts();
            AST = new StandardASTGenerator(projectInformation,AST,mainLayoutFileName,includedLayoutFileNames).get();
            appendImports();
            codeAnalyzer = new CodeAnalyzer(projectInformation,AST);
        } catch (FileNotFoundException e){
            Utils.showMessage(e.getMessage());
        }
    }

    private String getActivityClassName(PsiFile activityClassFile) {
        return activityClassFile.getName().substring(0,activityClassFile.getName().lastIndexOf('.'));
    }


    public  void saveInstrumentedCodes() {
        try{
            String context = AST.toString();
            Files.write(new File(activityClassFilePath).toPath(), Collections.singleton(AST.toString()), StandardCharsets.UTF_8);
        } catch (IOException ioe){

        }
    }

    public void instrument(PsiFile activityClassFile) {
        try {
            initial(activityClassFile);
            generateActivitySchema(rootNode);
            instrumentActivity(rootNode);
            saveInstrumentedCodes();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
