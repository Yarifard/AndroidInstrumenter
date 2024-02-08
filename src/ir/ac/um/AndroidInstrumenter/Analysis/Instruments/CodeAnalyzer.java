package ir.ac.um.AndroidInstrumenter.Analysis.Instruments;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles.LayoutInformationExtractor;
import ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles.MenuInformationExtractor;
import ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles.StringValueExtractor;
import ir.ac.um.AndroidInstrumenter.Taging.Label;
import ir.ac.um.AndroidInstrumenter.Utils.ASTUtils;
import ir.ac.um.AndroidInstrumenter.Utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CodeAnalyzer {
    private final int VERB = 1;
    private final int NONE = 0;
    private static CompilationUnit AST = null;
    private TypeDeclaration activityNode;
    private String menuFileName;
    private String layoutFileName;
    private String contextTitle;
    private List<String> includedLayoutFileNames = new ArrayList<>();
    private List<MethodInformation> methodDeclarationList = new ArrayList<>();
    private List<MethodInformation> calledMethodList  = new ArrayList<>();
    private ProjectInformation projectInformation;
    private MethodDeclaration onCreate,onStart;
    private boolean isListActivity = false;

    public CodeAnalyzer(@NotNull ProjectInformation projectInformation, CompilationUnit AST){
        this.projectInformation = projectInformation;
        this.AST = AST;
        initial();
    }

    public void initial() {
        initialActivityNode();
        initialLayouts();
        analyzeActivitySourceCode();
    }

    public boolean isListActivity() {
        NodeList<ClassOrInterfaceType> extendList =
                ((ClassOrInterfaceDeclaration) activityNode).getExtendedTypes();
        for(ClassOrInterfaceType node: extendList)
            if(isMatchWithPattern(node.getNameAsString(),"ListActivity")){
                return true;
            }
        return false;
    }

    public void initialActivityNode() {
        AST.getTypes().forEach(node -> {
            if (node.getClass().toString().contains("ClassOrInterfaceDeclaration"))
                activityNode = node;
        });
    }

    public void initialLayouts() {
        intialLayout();
        if(layoutFileName != null){
            LayoutInformationExtractor layoutInformationExtractor = new LayoutInformationExtractor(projectInformation, layoutFileName);
            if (layoutInformationExtractor.hasIncludedLayouts())
                includedLayoutFileNames = layoutInformationExtractor.getIncludedLayout();
        }
    }

    public void intialLayout() {
        MethodDeclaration onCreateMethod = getMethodByName(activityNode.getMethods(), "onCreate");
        MethodCallExpr setContentMethod = getMethodCallExprByName(onCreateMethod, "setContentView");
        if(setContentMethod != null){
            String arg = ASTUtils.getArgument(setContentMethod, 0);
            layoutFileName = arg.substring(arg.lastIndexOf('.') + 1);
        }
    }

    public void analyzeActivitySourceCode() {
        extractAllMethodDeclarationList();
        processAutomaticCalledMethods();
        extractCalledMethodDeclarationList();
        extractEventHandlerList();
        calculateEventHandlerLevel();
        collectInformationAboutAttachedView();
        generateTagForExtractedEventHandler();
    }

    private void extractAllMethodDeclarationList() {
        AST.findAll(MethodDeclaration.class).forEach(node -> {
            MethodInformation method = new MethodInformation();
            method.setName(node.getName().toString());
            method.setMethodDeclaration(node);
            method.setSourceActivity(activityNode.getNameAsString());
            method.setTargetActivity(activityNode.getNameAsString());
            methodDeclarationList.add(method);
            if (node.getName().toString().contentEquals("onCreate"))
                onCreate = node;
            if (node.getName().toString().contentEquals("onStart"))
                onCreate = node;
        });
    }

    private void extractCalledMethodDeclarationList() {
        List<MethodCallExpr> calledMethodNames = getCalledMethodNames();
        for (MethodInformation md : methodDeclarationList)
            for (MethodCallExpr calledMethodName : calledMethodNames)
                if (md.getName().equals(calledMethodName.getName().toString()))
                    if (isLocalMethod(calledMethodName)) {
                        if (!calledMethodList.contains(md))
                            calledMethodList.add(md);
                    }
    }

    private void extractEventHandlerList() {
        for (MethodInformation event : methodDeclarationList)
                if (isValidEventHandler(event.getAttachedMethod()))
                    event.setEventHandler();
    }

    /*****************************************************
     *    This method process automated call methods     *
     *    and removes them                               *
     * ***************************************************/

    private void processAutomaticCalledMethods() {
        //ToDo: Currently, we remove auromated methods from list.
        //Idead, we must process them before remove if it requies.
        //So, we later maybe process them in this method, if it requires.
        ArrayList<MethodInformation> tmplist = new ArrayList<>();
        for (MethodInformation methodInformation : methodDeclarationList) {
            switch (methodInformation.getName().toString()) {
                case "onCreate":
                    extractActivityTitle(methodInformation);
                    break;
                case "onCreateOptionsMenu":
                    this.menuFileName = ASTUtils.extractMenuLayoutFileName(methodInformation.getAttachedMethod());
                    break;
            }
        }
    }

    private void calculateEventHandlerLevel() {
        BlockStmt blockStmt = new BlockStmt();
        MethodDeclaration parentMethod;
        //ToDO: we must inspects more precise this method's functionality.
        // Because we start from the begining of the event handler list,
        // if it is possible, the child node placed before its parent. this state must be precisely inspect.

        for (MethodInformation methodInformation : methodDeclarationList) {
//            String parentClassName = ASTUtils.getClassName(ASTUtils.getParentNode(methodInformation.getAttachedMethod()));
//            if (Utils.isPartialMatchWithPattern(parentClassName, "ObjectCreationExpr"))
            if(methodInformation.isEeventHandler()){
                parentMethod = getParentMethodDeclaration(methodInformation);
                MethodInformation parentMethodInformation;
                if (isEventHandler(parentMethod))
                    parentMethodInformation = findEventHandlerByMethodDeclaration(parentMethod);
                else
                    parentMethodInformation = findCallerEventHandler(parentMethod.getName().toString());

                if (parentMethodInformation == null)
                    methodInformation.setLevel(0);
                else {
                    parentMethodInformation.setChildEventHandler(methodInformation);
                    methodInformation.setParentEventHandler(parentMethodInformation);
                    methodInformation.setLevel(parentMethodInformation.getLevel() + 1);
                }
            } else
                methodInformation.setLevel(0);
        }
    }

    /************************************************************************
     *  This method collects the related information (e.g. Id,Label and Type)*
     *  about the extracted event handlers.                                  *
     ******************************************************************/
    private void collectInformationAboutAttachedView() {
        NodeList<Statement> methodBodyStatement;
        String viewType, viewId, viewLabel, bindingVariableName, context;
        for (MethodInformation methodInformation : methodDeclarationList) {
            if (methodInformation.isEeventHandler()) {
                context = "";
                viewId = "";
                viewLabel = "";
                bindingVariableName = "";
                context = "";
                if (isDirectAttachedEventHandler(methodInformation)) {
                    if (ASTUtils.isDialogMethod(methodInformation.getAttachedMethod())) {
                        if (ASTUtils.isDefaultDialogMethodPattern(methodInformation.getAttachedMethod())) {
                            viewLabel = extractViewLabelForDialogMethod(methodInformation);
                            methodInformation.setAttacheViewLabel(viewLabel);
                            methodInformation.setAttachedViewType("Button");
                            methodInformation.setAttachedViewId(getDefaultDialogMethodWidgetId(methodInformation.getAttachedMethod()));
                            context = extractContext(methodInformation);
                            methodInformation.setContext_title(context);
                        } else {
                            bindingVariableName = getViewBindingVariableName(methodInformation.getAttachedMethod());
                            if (!bindingVariableName.isEmpty())
                                methodInformation.setAttachedViewBindingName(bindingVariableName);
                            viewType = resolveViewType(methodInformation);
                            methodInformation.setAttachedViewType(viewType);
                            if (methodInformation.getAttachedViewId().isEmpty()) {
                                viewId = extractAttachedViewId(methodInformation);
                                methodInformation.setAttachedViewId(viewId);
                            }
                            viewLabel = extractViewLabelForDialogMethod(methodInformation);
                            if (!viewLabel.isEmpty())
                                methodInformation.setAttacheViewLabel(viewLabel);
                            context = extractContext(methodInformation);
                            methodInformation.setContext_title(context);
                        }
                    } else {
                        if (ASTUtils.isMenuEventHandlerMethod(methodInformation)) {
                            if(ASTUtils.isMainMenuItemEventHandler(methodInformation))
                                methodInformation.setAttachedViewType("MainMenuItem");
                            else
                                methodInformation.setAttachedViewType("ContextMenuItem");
                            if(ASTUtils.isAssignedMenuViewId(methodInformation)){
                                viewId = ASTUtils.extractMenuIdFrom(methodInformation.getAttachedMethod());
                                if(ASTUtils.menuHasLayout(
                                        ASTUtils.getIncludedMethod(methodInformation.getAttachedMethod()))){
                                    String menuLayout = ASTUtils.extrctMenuLayout(
                                            ASTUtils.getIncludedMethod(methodInformation.getAttachedMethod()));
                                    if(isExistMenuItem(menuLayout,viewId)){
                                        methodInformation.setAttachedViewId(viewId);
                                        viewLabel = ASTUtils.extractMenuItemLabelFromLayout(projectInformation,menuLayout,viewId);
                                        methodInformation.setAttacheViewLabel(viewLabel);
                                    }
                                    else{
                                        viewLabel = extractMenuItemLabelFromSouceCodeById(methodInformation,viewId);
                                        if(!viewLabel.isEmpty())
                                            methodInformation.setAttacheViewLabel(viewLabel);
                                    }
                                }
                                else{
                                    viewLabel = extractMenuItemLabelFromSouceCodeById(methodInformation,viewId);
                                    if(!viewLabel.isEmpty())
                                        methodInformation.setAttacheViewLabel(viewLabel);
                                }
                            }
                            else{
                                viewLabel = extractMenuItemLabelFromSouceCodeById(methodInformation,viewId);
                                if(!viewLabel.isEmpty())
                                    methodInformation.setAttacheViewLabel(viewLabel);
                            }
                            if(containDialog(methodInformation.getAttachedMethod())){
                               context = extractContext(methodInformation);
                               methodInformation.setContext_title(context);
                            }
                        } else {
                            bindingVariableName = getViewBindingVariableName(methodInformation.getAttachedMethod());
                            if (!bindingVariableName.isEmpty())
                                methodInformation.setAttachedViewBindingName(bindingVariableName);
                            viewType = resolveViewType(methodInformation);
                            methodInformation.setAttachedViewType(viewType);
                            if (methodInformation.getAttachedViewId().isEmpty()) {
                                viewId = extractAttachedViewId(methodInformation);
                                methodInformation.setAttachedViewId(viewId);
                            }
                            if (!viewId.isEmpty()) {
                                viewLabel = extractAttachedViewLabelFromLayout(methodInformation.getAttachedViewId());
                                if (!viewLabel.isEmpty())
                                    methodInformation.setAttacheViewLabel(viewLabel);
                                String contentDescription = extractAttachedViewContentDesciptionFromLayout(viewId);
                                if (!contentDescription.isEmpty())
                                    methodInformation.setAttachedViewContentDescription(contentDescription);
                            }
                        }
                        if (containDialog(methodInformation.getAttachedMethod())) {
                            context = extractContext(methodInformation);
                            methodInformation.setContext_title(context);
                        }
                    }
                }
            }
        }
    }

    private String getDefaultDialogMethodWidgetId(MethodDeclaration attachedMethod) {
        int argIndex = 0;
        String viewId = "";
        MethodCallExpr callExpr = ASTUtils.getParentMethodCallExpr(attachedMethod);
        switch (callExpr.getArguments().size()){
            case 3:
                   String agument = ASTUtils.getArgument(callExpr,argIndex);
                   if(isPartialMatchWithPattern(agument,"BUTTON_POSITIVE"))
                       viewId = "android.R.id.button1";
                   else if(isPartialMatchWithPattern(agument,"BUTTON_NEGATIVE"))
                       viewId = "android.R.id.button2";
                   else
                       viewId = "android.R.id.button3";
                   break;
            case 2:
                   String argument = callExpr.getNameAsString();
                   if(isMatchWithPattern(argument,"setPositiveButton"))
                       viewId = "android.R.id.button1";
                   else if(isMatchWithPattern(argument,"setNegativeButton"))
                       viewId = "android.R.id.button2";
                   else
                       viewId = "android.R.id.button3";
        }
        return viewId;
    }

    private String extractMenuItemLabelFromSouceCodeById(MethodInformation eventHandler,
                                                         String viewId) {
        String label = "";
        List<MethodCallExpr> menuDeclCalExprs;
        Node containBlocks = ASTUtils.getIncludedBlockNode(eventHandler.getAttachedMethod());
        menuDeclCalExprs = getMethodCallExprsByNameCalledDirectlyBy(containBlocks,"add");
        for(MethodCallExpr item:menuDeclCalExprs)
            if(item.hasScope())
                if(isMatchWithPattern(ASTUtils.getScope(item),"menu"))
                    if(isMatchWithPattern(item.getArguments().get(1).toString(),viewId))
                         label = item.getArguments().get(3).toString();
        return getContent(label);
    }

    public boolean isExistMenuItem(String menuLayout, String menuId) {
        MenuInformationExtractor menuInformationExtractor =
                new MenuInformationExtractor(projectInformation, menuLayout);
        if(menuInformationExtractor.isExistMenuItem(menuId))
            return true;
        return false;
    }

    private String getClassName(MethodDeclaration method) {
        String className = "";
        Node bindingMethod = extractBindindMethodCallExprFrom(method);
        className = resolveClassName(method, ASTUtils.getScope(bindingMethod));
        return className;
    }

    private String extractViewLabelForDialogMethod(MethodInformation method) {
        String label = "";
        if(ASTUtils.isDefaultDialogMethodPattern(method.getAttachedMethod())){
            int argIndex = 0;
            Node bindingMethodCallExpr = extractBindindMethodCallExprFrom(method.getAttachedMethod());
            if(isMatchWithPattern(((MethodCallExpr) bindingMethodCallExpr).getNameAsString(),"setButton"))
                argIndex = 1;
            label = ASTUtils.getArgument((MethodCallExpr) bindingMethodCallExpr, argIndex);
        }
        else
            label = extractViewLabelFromDialogLayout(method);
       return getContent(label);

    }

    private String extractContext(MethodInformation methodInformation) {
        String context = "";
        String className = "";
        if(containDialog(methodInformation.getAttachedMethod())){
            MethodInformation childMethod = getDialogMethodFrom(methodInformation);
            if(ASTUtils.isDefaultDialogMethodPattern(childMethod.getAttachedMethod())){
                className = getClassName(childMethod.getAttachedMethod());
                if(isMatchWithPattern(className,"DatePickerDialog"))
                    context = "pick a date";
                else
                   context = extractViewContextFromDefaultDialog(childMethod);
            }
            else{
                 context = extractViewContextFromDefaultDialog(childMethod);
                 if(context.isEmpty())
                     context = extractViewContextFromDialogLayout(methodInformation);
            }
            return getContent(context);
        }
        context = extractViewContextFromDefaultDialog(methodInformation);
        if(!context.isEmpty())
            return getContent(context);
        else if(!ASTUtils.isDefaultDialogMethodPattern(methodInformation.getAttachedMethod()))
            context = extractViewContextFromDialogLayout(methodInformation);
        return getContent(context);
    }

    private MethodInformation getDialogMethodFrom(MethodInformation eventHandler) {
        for(MethodInformation event: eventHandler.getChildEventHandlers())
            if(ASTUtils.isDialogMethod(event.getAttachedMethod()))
                return event;
        return null;
    }

    private String extractAttachedViewContentDesciptionFromLayout(String viewId) {
            String viewContentDescription = "";
            LayoutInformationExtractor layoutInformationExtractor =
                    new LayoutInformationExtractor(projectInformation, layoutFileName);
            viewContentDescription = layoutInformationExtractor.findViewContentDesciptionById(viewId);
            if(viewContentDescription.isEmpty()){
                if (layoutInformationExtractor.hasIncludedLayouts()) {
                    includedLayoutFileNames = layoutInformationExtractor.getIncludedLayout();
                    for (String includedLayoutFileName : includedLayoutFileNames) {
                        layoutInformationExtractor.setXmlFile(includedLayoutFileName);
                        viewContentDescription = layoutInformationExtractor.findViewContentDesciptionById(viewId);
                        if (!viewContentDescription.isEmpty()) {
                            break;
                        }
                    }
                }
            }
            return viewContentDescription;
        }
    public String getContent(String content){
        if(content.startsWith("getString("))
            content = content.substring(content.lastIndexOf('(') + 1, content.lastIndexOf(')'));
        if(content.startsWith("R.string."))
            content = getString(content);
        if(content.startsWith("\"") && content.endsWith("\""))
            content = content.substring(1,content.length() - 1);
        return content;
    }

    private String extractViewContextFromDefaultDialog(MethodInformation methodInformation) {
        String content = "";
        int argIndex = 0;
        Node node = ASTUtils.getIncludedBlockNode(methodInformation.getAttachedMethod());
        List<VariableDeclarator> localVariable = ASTUtils.getLocalVariables(node);
        MethodCallExpr methodCallExpr =
                getMethodCallExprByName(node, "setTitle");
        if (methodCallExpr != null)
            content =  getContent(methodCallExpr.getArgument(argIndex).toString());
        if(!content.isEmpty())
            if(!isLocalVariable(localVariable,content))
                return content;
            else
                content = extractValue(node,content);
        return content;
    }

    public String extractValue(Node node, String content) {
        AtomicReference<String> result = new AtomicReference<>(content);
        node.findAll(VariableDeclarator.class).forEach(item -> {
            if(isMatchWithPattern(item.getNameAsString(),content)){
                String value = item.getInitializer().get().toString();
                String valueId = "";
                if (value.contains("+")) {
                    String[] parts = value.split("\\+");
                    String resultString = "";
                    for(String part : parts){
                        part = part.trim();
                        if(part.startsWith("getString(")){
                            valueId = part.substring(part.lastIndexOf('(') + 1, part.lastIndexOf(')'));
                            part = getString(valueId);
                            //resultString += part;
                        }
                        else if(part.startsWith("\"") && part.endsWith("\""))
                            part = part.substring(1,part.length()-1);
                        resultString += " " + part;
                    }
                    result.set(resultString);
                } else {
                    if (value.startsWith("getString(")){
                        valueId = value.substring(value.lastIndexOf('(') + 1, value.lastIndexOf(')'));
                        value = getString(valueId);
                    }
                    result.set(value);
                }
            }
        });
        return result.get().trim();
    }

    public String getString(String valuId){
        StringValueExtractor stringValueExtractor = new StringValueExtractor(projectInformation,"strings");
        return stringValueExtractor.findViewLabelById(valuId);
    }

    private String extractAttachedViewTypeFromLayoutById(String viewId) {
        String viewType = "";
        LayoutInformationExtractor layoutInformationExtractor =
                new LayoutInformationExtractor(projectInformation, layoutFileName);
        viewType = layoutInformationExtractor.findViewTypeById(viewId);
        if (!viewType.isEmpty())
            return viewType;
        else {
            if (layoutInformationExtractor.hasIncludedLayouts()) {
                includedLayoutFileNames = layoutInformationExtractor.getIncludedLayout();
                for (String includedLayoutFileName : includedLayoutFileNames) {
                    layoutInformationExtractor.setXmlFile(includedLayoutFileName);
                    viewType = layoutInformationExtractor.findViewTypeById(viewId);
                    if (!viewType.isEmpty()) {
                        return viewType;
                    }
                }
            }
        }
        return viewType;

    }


    private String extractAttachedViewLabelFromLayout(String viewId) {
        String viewLabel = "";
        LayoutInformationExtractor layoutInformationExtractor =
                new LayoutInformationExtractor(projectInformation, layoutFileName);
        viewLabel = layoutInformationExtractor.findViewLabelById(viewId);
        if(viewLabel.isEmpty()){
            if (layoutInformationExtractor.hasIncludedLayouts()) {
                includedLayoutFileNames = layoutInformationExtractor.getIncludedLayout();
                for (String includedLayoutFileName : includedLayoutFileNames) {
                    layoutInformationExtractor.setXmlFile(includedLayoutFileName);
                    viewLabel = layoutInformationExtractor.findViewLabelById(viewId);
                    if (!viewLabel.isEmpty()) {
                        break;
                    }
                }
            }
        }
        if(!viewLabel.isEmpty() && viewLabel.startsWith("R.strings."))
            viewLabel = getString(viewLabel);
        return viewLabel;
    }

    private String extractViewLabelFromDialogLayout(MethodInformation eventHandler) {
        LayoutInformationExtractor layoutInformationExtractor;
        String dynamicLayoutFileName = getDynmicLayoutFileName(eventHandler);
        layoutInformationExtractor = new LayoutInformationExtractor(projectInformation, dynamicLayoutFileName);
        layoutInformationExtractor.setXmlFile(dynamicLayoutFileName);
        return layoutInformationExtractor.findViewLabelById(eventHandler.getAttachedViewId());
    }

    private String extractViewContextFromDialogLayout(MethodInformation eventHandler) {
        LayoutInformationExtractor layoutInformationExtractor;
        String dynamicLayoutFileName = getDynmicLayoutFileName(eventHandler);
        layoutInformationExtractor = new LayoutInformationExtractor(projectInformation, dynamicLayoutFileName);
        return layoutInformationExtractor.findViewContext();
    }

    private String extractAttachedViewId(MethodInformation eventHandler) {
        String objectName = getViewBindingVariableName(eventHandler.getAttachedMethod());
        return extractWidgetId(eventHandler.getAttachedMethod(), objectName);
    }


    private boolean isDirectAttachedEventHandler(MethodInformation methodInformation) {
        Node node = (Node) methodInformation.getAttachedMethod();
        String parentClassName = ASTUtils.getClassName(ASTUtils.getParentNode(node));
        if (isPartialMatchWithPattern(parentClassName, "ObjectCreationExpr"))
            return true;
        return false;
    }

    private void generateTagForExtractedEventHandler() {
        Label.generator(contextTitle,projectInformation.getPosTagger(),methodDeclarationList);
        refinement();
        printGeneratedTags();
    }

    private  void refinement() {
        for (int i = 0; i < methodDeclarationList.size(); i++){
            MethodInformation eventItemI = methodDeclarationList.get(i);
            if(eventItemI.isEeventHandler()){
                for (int j = i + 1; j < methodDeclarationList.size(); j++) {
                    MethodInformation eventItemJ = methodDeclarationList.get(j);
                    if(eventItemJ.isEeventHandler()) {
                        if (Utils.isMatchWithPattern(eventItemI.getTitle(), eventItemJ.getTitle())) {
                            if (isChild(eventItemI) && isChild(eventItemJ)) {
                                if (parentsHasSimilarTitle(eventItemI, eventItemJ)) {
                                    if (eventItemI.hasParent())
                                        eventItemI.getParentEventHandlerInformation().setFlag(true);
                                    else if (eventItemJ.hasParent())
                                        eventItemJ.getParentEventHandlerInformation().setFlag(true);
                                    else
                                        eventItemI.setFlag(true);
                                }

                            }
                        }
                    }
                }
            }
        }
    }


    private  boolean parentsHasSimilarTitle(MethodInformation sourceEventHandler,
                                                  MethodInformation destinationEventHandler) {
        String sourceTitle = "", destinationTitle = "";
        if(sourceEventHandler.hasParent() && destinationEventHandler.hasParent()){
            sourceTitle = sourceEventHandler.getParentEventHandlerInformation().getTitle();
            destinationTitle = destinationEventHandler.getParentEventHandlerInformation().getTitle();
            if(isMatchWithPattern(sourceTitle,destinationTitle))
                return true;
        }
        else if(sourceEventHandler.hasParent()){
            sourceTitle = sourceEventHandler.getParentEventHandlerInformation().getTitle();
            destinationTitle = Label.generateLabelForDialog(projectInformation.getPosTagger(),
                                                            destinationEventHandler.getContext_title());
            if(isMatchWithPattern(sourceTitle,destinationTitle))
                return true;
        }
        else if(destinationEventHandler.hasParent()){
            sourceTitle = Label.generateLabelForDialog(projectInformation.getPosTagger(),
                                                       sourceEventHandler.getContext_title());
            destinationTitle = destinationEventHandler.getParentEventHandlerInformation().getTitle() ;
            if(isMatchWithPattern(sourceTitle,destinationTitle))
                return true;
        }
        else{
            sourceTitle = Label.generateLabelForDialog(projectInformation.getPosTagger(),
                                                       sourceEventHandler.getContext_title());
            destinationTitle = Label.generateLabelForDialog(projectInformation.getPosTagger(),
                                                            destinationEventHandler.getContext_title());
            if(isMatchWithPattern(sourceTitle,destinationTitle))
                return true;
        }
        return false;
    }


    private  void printGeneratedTags() {
        String label = "";
        System.out.println("*****************************************************");
        for(MethodInformation method: methodDeclarationList){
            if(method.isEeventHandler()){
                if(containDialog(method.getAttachedMethod())){
                    if(isSetFlag(method)){
                        //check this block
                        label  = Label.generateTagByUsingBindingVaribale(projectInformation.getPosTagger(),method.getBindingName());
                        label += method.getTitle() + "Dialog";
                        method.setFlag(false);
                    }
                    else
                        label = method.getTitle() + "Dialog";
                    if(method.hasParent() && !ASTUtils.isDialogMethod(method.getAttachedMethod()))
                         method.setMainContext(method.getSourceActivity());

                    if(method.hasChildEvents())
                        for(MethodInformation eventItem: method.getChildEventHandlers())
                            eventItem.setMainContext(label);
                    if(label.startsWith("show") || label.startsWith("Show"))
                        method.setTitle(label);
                    else
                        method.setTitle("Show" + label);
                   // method.setMainContext(label);
                    System.out.println(method.getTitle());
                    continue;
                }
                else if(ASTUtils.isDialogMethod(method.getAttachedMethod())){
                    if(method.hasParent()){
                          if(containDialog(method.getParentEventHandlerInformation().getAttachedMethod())){
                                if (isSetFlag(method.getParentEventHandlerInformation()))
                                    label = Label.generateTagByUsingBindingVaribale(projectInformation.getPosTagger(),
                                                                 method.getParentEventHandlerInformation().getBindingName())
                                            + method.getParentEventHandlerInformation().getTitle()
                                            + "$" + method.getTitle();
                                else
                                    label = method.getParentEventHandlerInformation().getTitle()
                                            + "$" + method.getTitle();

                                if(label.startsWith("show") || label.startsWith("Show"))
                                    method.setTitle(label);
                                else
                                   // System.out.println("Show" + label);
                                    method.setTitle("Show" + label);
                                   // method.setMainContext(method.getMainContext());
                              //  System.out.println(method.getTitle());

                          }
                          else{
                                label = Label.generateTagForDialog(projectInformation.getPosTagger(),
                                        method.getContext_title(),method.getAttachedViewType()) + "Dialog";
                                method.setMainContext(label);
                                label += "$" + method.getTitle();
                                if(label.startsWith("show") || label.startsWith("Show"))
                                   // System.out.println(label);
                                    method.setTitle(label);

                                else
                                    method.setTitle("Show" + label);
                                   // method.setMainContext(method.getMainContext());
                               //System.out.println(method.getTitle());

                          }
                    }
                    else{
                        label = Label.generateTagForDialog(projectInformation.getPosTagger(),
                                method.getContext_title(),method.getAttachedViewType()) + "Dialog";
                        method.setMainContext(label);
                        label += "$" + method.getTitle();
                        if(label.startsWith("show") || label.startsWith("Show")){
                      //      System.out.println(label);
                            method.setTitle(label);
                        }
                        else{
                            method.setTitle("Show" + label);
                          //  method.setMainContext(method.getMainContext());

                        }
                    }
                    System.out.println(method.getTitle());
                       continue;
                }
                else if(ASTUtils.isMenuEventHandlerMethod(method)){
                    String generatedLabel = method.getTitle();
                    if(ASTUtils.isOpenActivityBy(method.getAttachedMethod())){
                         generatedLabel = "Open" + generatedLabel;
                        if(!generatedLabel.toLowerCase().contains("activity"))
                            generatedLabel = generatedLabel + "Activity";
                    }
                    if(Utils.isMatchWithPattern(method.getAttachedViewType(),"MainMenuItem")){
                        System.out.println( generatedLabel + "ByMenuItem");
                        method.setMainContext(generatedLabel + "ByMenuItem");
                    }
                    else{
                        System.out.println( generatedLabel + "ByContextMenuItem");
                        method.setMainContext(generatedLabel + "ByContextMenuItem");
                    }

                    continue;

                }
                else if(method.hasChildEvents() && hasOtherTypeEvent(method)){
                    System.out.println("Show" + method.getTitle() + "Panel");
                    method.setTitle("Show" + method.getTitle() + "Panel");
                    continue;
                }
                else if(ASTUtils.isOpenActivityBy(method.getAttachedMethod())){
                    String generatedLabel = "Open" + method.getTitle();
                    if(!generatedLabel.toLowerCase().contains("activity"))
                        generatedLabel = generatedLabel + "Activity";
                    System.out.println(generatedLabel);
                    method.setTitle(generatedLabel);
                    continue;
                }
                else
                    System.out.println(method.getTitle());
            }
        }

        System.out.println("*********************************************************");
    }

    private boolean hasOtherTypeEvent(MethodInformation method) {
        boolean flag = false;
        for(MethodInformation childEvent: method.getChildEventHandlers())
            if(!ASTUtils.isDialogMethod(childEvent.getAttachedMethod())){
                flag = true;
                break;
            }
        return flag;
    }

    public static boolean containDialog(MethodDeclaration method) {
        boolean result = false;
        List<MethodCallExpr> methodCallExprs =
                ASTUtils.getMethodCallExprsByNameCalledDirectlyBy(method,"show");
        for (MethodCallExpr methodCallExpr : methodCallExprs)
            if(methodCallExpr.hasScope()){
                String className = resolveClassName(methodCallExpr,ASTUtils.getScope(methodCallExpr));
                if (Utils.isPartialMatchWithPattern(className, "Dialog"))
                    if (ASTUtils.isPlacedInSameBlock(method.getBody().get(),
                        ASTUtils.getParentBlock(methodCallExpr)))
                        return true;
                    else if (!ASTUtils.isPlacedInConditionBlock(ASTUtils.getParentMethodCallExpr(methodCallExpr)))
                        return true;
                    //else
                     //return false;
        }
        List<MethodCallExpr> callExprList = ASTUtils.getMethodCallExprsCalledDirectlyBy(method);
        if(callExprList.isEmpty())
            return false;
        for(MethodCallExpr callExpr:callExprList)
            if(!callExpr.hasScope())
                if (ASTUtils.isPlacedInSameBlock(method.getBody().get(),
                    ASTUtils.getParentBlock(callExpr)) ||
                    !ASTUtils.isPlacedInConditionBlock(callExpr)){
                        MethodDeclaration targetMethod = findMethodByName(callExpr);
                        if(targetMethod != null)
                            if(ASTUtils.isLocalMethod(targetMethod) && !ASTUtils.areSameMethod(method,targetMethod))
                                if(containDialog(targetMethod)){
                                   result = true;
                                   break;
                                }
                }
        return result;
    }

    public static MethodDeclaration findMethodByName(MethodCallExpr methodCallExpr) {
        List<MethodDeclaration> methods = AST.findAll(MethodDeclaration.class);
        for(MethodDeclaration method: methods )
            if(Utils.isMatchWithPattern(methodCallExpr.getNameAsString(),method.getNameAsString()))
                return method;
        return null;
    }

    private boolean isSetFlag(MethodInformation eventHandler) {
        if(eventHandler.getFlag())
            return true;
        return false;
    }

    private static boolean isChild(MethodInformation eventHandler) {
        if(ASTUtils.isDialogMethod(eventHandler.getAttachedMethod()) || eventHandler.hasParent())
            return true;
        return false;
    }

    private String resolveViewType(MethodInformation eventHandler) {
        List<VariableDeclarator> localVariablesList;
        String objectName = eventHandler.getBindingName();//getViewBindingVariableName(eventHandler.getAttachedMethod());
        Node currentNode = ASTUtils.getParentBlock(eventHandler.getAttachedMethod());

        if(objectName.contains("."))
            objectName = objectName.substring(0,objectName.indexOf('.'));

        do{
            localVariablesList = ASTUtils.getLocalVariables(currentNode);
            if(isLocalVariable(localVariablesList,objectName))
                return ASTUtils.getVariableType(localVariablesList,objectName);
            currentNode = ASTUtils.getParentBlock(currentNode);
        }while(currentNode != null);

        List<VariableDeclarator> globalVariablesList = getGlobalVariableList();
        if(isGlobalVariable(globalVariablesList,objectName))
            return ASTUtils.getVariableType(globalVariablesList,objectName);
        return "";

    }

    //*********************************************************************
   private MethodDeclaration getMethodByName(List<MethodDeclaration> methods,String targetMethodName) {
       MethodDeclaration targetMethod = null;
       for(MethodDeclaration method : methods)
           if(isMatchWithPattern(method.getNameAsString(),targetMethodName)){
               targetMethod = method;
               break;
           }
       return targetMethod;
   }

    private MethodCallExpr getMethodCallExprByName(Node node,String targetMethodCallExpr) {
        List<MethodCallExpr> callExprsList = getMethodCallExprsListByName(node,targetMethodCallExpr);
        if(callExprsList.size() > 0)
            return callExprsList.get(0);
        else
            return null;
    }

    private List<MethodCallExpr> getMethodCallExprsListByName(Node method,String pattern){
        List<MethodCallExpr> methodCallExprsWithSimilarName = new ArrayList<>();
        List<MethodCallExpr> methodCallExprList = getMethodCallExprsListFrom(method);
        for(MethodCallExpr callExprItem : methodCallExprList)
            if(isMatchWithPattern(pattern,callExprItem.getNameAsString()))
                methodCallExprsWithSimilarName.add(callExprItem);
        return methodCallExprsWithSimilarName;
    }

    private List<MethodCallExpr> getMethodCallExprsListFrom(Node node){
        List<MethodCallExpr> wholeCallExprsList = new ArrayList<>();
        node.findAll(MethodCallExpr.class).forEach(item->{
            wholeCallExprsList.add(item);
        });
        return wholeCallExprsList;
    }


    private String getViewBindingVariableName(Node node) {
        String scope = "";
        while (!ASTUtils.isMethodCallExpr(node))
            node = ASTUtils.getParentNode(node);
        scope = ASTUtils.getScope(node);
        if(isPartialMatchWithPattern(scope,"."))
            scope = scope.substring(0,scope.indexOf('.'));
        return scope;
    }

    private String getDynmicLayoutFileName(MethodInformation eventHandler) {
        Node tmpNode;
        if(ASTUtils.isDialogMethod(eventHandler.getAttachedMethod()))
            tmpNode = ASTUtils.getParentBlock(eventHandler.getAttachedMethod());
        else
            tmpNode = ASTUtils.getParentBlock(eventHandler.getChildEventHandlers().get(0).getAttachedMethod());
        List<MethodCallExpr> setContentMethodList = getMethodCallExprsListByName(tmpNode,"setContentView");
        int argId = 0;
        for(MethodCallExpr methodCallExpr : setContentMethodList)
            if(methodCallExpr.hasScope()){
                String layoutName = ASTUtils.getArgument(methodCallExpr,argId);
                return layoutName.substring(layoutName.lastIndexOf('.') + 1);
            }
        return "";
    }

    private boolean isValidEventHandler(MethodDeclaration node){
        Node parentNode = ASTUtils.getParentNode(node);
        Node ancientNode = ASTUtils.getAncientNode(node);
        if(isPartialMatchWithPattern(ASTUtils.getClassName(parentNode),"ObjectCreationExpr")
                && (isPartialMatchWithPattern(ASTUtils.getClassName(ancientNode),"MethodCallExpr")
               ))
            return true;
        return false;
    }

    private Node extractBindindMethodCallExprFrom(Node node) {
        return ASTUtils.getAncientNode(node);
    }

//####################################################################################################
   private List<MethodCallExpr> subtract(List<MethodCallExpr> wholeSet, List<MethodCallExpr> subSet){
        List<MethodCallExpr> resultSet = new ArrayList<>();
        for(MethodCallExpr item : wholeSet)
            if(!subSet.contains(item))
                resultSet.add(item);
        return resultSet;
    }

    private boolean isMatchWithPattern(String pattern,String targetItem ){
        if(pattern.contentEquals(targetItem))
            return true;
        return false;
    }



    private boolean isPartialMatchWithPattern(String pattern,String targetItem){
        if(targetItem.contains(pattern) || pattern.contains(targetItem))
            return true;
        return false;
    }
   //=========================================================================================

    public List<MethodCallExpr> getCalledMethodNames() {
        List<MethodCallExpr> calledMethodNames = new ArrayList<>();
        AST.findAll(MethodCallExpr.class).forEach(node -> {
            if (!node.getName().getParentNode().get().toString().contains("super." + node.getName().asString()))
                calledMethodNames.add(node);
        });
        return calledMethodNames;
    }

    private boolean isLocalMethod(MethodCallExpr calledMethodName) {
        if (calledMethodName.getScope().toString().equals("this")
                || calledMethodName.getScope().isEmpty())
            return true;
        return false;
    }

    private void extractActivityTitle(MethodInformation methodInformation) {
        List<MethodCallExpr> calledMethods;
        String value = "";
        calledMethods = ASTUtils.getMethodCallExprsCalledDirectlyBy(methodInformation.getAttachedMethod());
        for(MethodCallExpr calledMethod:calledMethods){
            if(calledMethod.getName().toString().contentEquals("setTitle")){
                String parameter = calledMethod.getArgument(0).toString();
                contextTitle = getContent(parameter);
                break;
            }
        }

    }

    private boolean isAutomatedCalledMethod(String name) {
        boolean result = false;
        switch (name) {
            case "onCreate":
            case "onResume":
            case "onPause":
            case "onCreateOptionsMenu":
                result = true;
                break;
        }
        return result;
    }

    private MethodDeclaration getParentMethodDeclaration(MethodCallExpr callExpr){
        Node tmpNode = (Node) callExpr;
        while(!ASTUtils.isMethodDeclartionExpr(tmpNode) && !ASTUtils.isClassOrInterfaceExpr(tmpNode))
            tmpNode = ASTUtils.getParentNode(tmpNode);
        if(ASTUtils.isMethodDeclartionExpr(tmpNode))
            return (MethodDeclaration) tmpNode;
        return null;
    }

    private MethodDeclaration getParentMethodDeclaration(MethodInformation methodInformation) {
        MethodDeclaration tmpNode;
        Node node = (Node) methodInformation.getAttachedMethod();
        boolean flag = true;
        while (node.getParentNode().isPresent()) {
            if (node.getParentNode().get().getClass().getName().contains("MethodDeclaration")) {
                break;
            }
            node = node.getParentNode().get();
        }
        tmpNode = (MethodDeclaration) node.getParentNode().get();
        return tmpNode;
    }

    private boolean isEventHandler(MethodDeclaration method) {
        for (MethodInformation methodInformation : methodDeclarationList) {
            if (methodInformation.getAttachedMethod().getName() == method.getName() &&
                methodInformation.isEeventHandler())
                return true;
        }
        return false;
    }

    private MethodInformation findEventHandlerByMethodDeclaration(MethodDeclaration method) {
        for (MethodInformation methodInformation : methodDeclarationList) {
            if (methodInformation.getAttachedMethod().equals(method))
                return methodInformation;
        }
        return null;
    }


    private MethodInformation findCallerEventHandler(String name) {
        if (isAutomatedCalledMethod(name) || calledDirectlyByAutomatedMethod(name))
            return null;
        MethodInformation targetMethodInformation = null;
        boolean findFlag = false;
        List<MethodCallExpr> calledDirectlyMethodNames = new ArrayList<>();
        for (MethodInformation callerMethod : methodDeclarationList) {
            calledDirectlyMethodNames = ASTUtils.getMethodCallExprsCalledDirectlyBy(callerMethod.getAttachedMethod());
            for (MethodCallExpr calledMethod : calledDirectlyMethodNames) {
                if ((calledMethod.getName().toString().contentEquals(name)) &&
                        (calledMethod.getScope().isEmpty()
                        )) {
                    if (isEventHandler(callerMethod.getAttachedMethod()))
                        targetMethodInformation = findEventHandlerByMethodDeclaration(callerMethod.getAttachedMethod());
                    else
                        targetMethodInformation = findCallerEventHandler(callerMethod.getName().toString());
                    findFlag = true;
                    break;
                }
            }
            if(findFlag)
                break;
        }
        return targetMethodInformation;
    }

    private boolean calledDirectlyByAutomatedMethod(String name) {
        List<MethodCallExpr> calledDirectlyMethodNames = ASTUtils.getMethodCallExprsCalledDirectlyBy(onCreate);
        for (MethodCallExpr calledMethod : calledDirectlyMethodNames)
            if ((calledMethod.getName().toString().contentEquals(name)) &&
                    (calledMethod.getScope().isEmpty()
                    ))
                return true;
        return false;
    }

    private List<MethodCallExpr> getDirectlyCalledMethodsBy(MethodDeclaration method) {
        List<MethodCallExpr> calledMethods = new ArrayList<>();
        method.findAll(MethodCallExpr.class).forEach(methodCallExpr->{
            if(Utils.isMatchWithPattern(getParentMethodDeclaration(methodCallExpr).getNameAsString(),method.getNameAsString()))
                calledMethods.add(methodCallExpr);
        });
        return calledMethods;
    }

    /********************************************************************
     *  This method tries to extract id of the input parameter
     *  it search the method that the input parameter is used or in the
     *  onCreate method
     */
    private String  extractWidgetId(MethodDeclaration md, String objectName) {

        String widgetId = "";
        BlockStmt codeBlock = md.getBody().get();
        //==============================================================
        List<VariableDeclarator> localVariablesList = ASTUtils.getLocalVariables(codeBlock);
        if(isLocalVariable(localVariablesList,objectName))
            widgetId = searchWidgetIdInBlockBody(codeBlock,objectName);
        else{
            widgetId = searchWidgetIdInBlockBody(codeBlock,objectName);
            if(widgetId.isEmpty()){
                widgetId = searchIdInParentsBlockPath(codeBlock,objectName);
                if(widgetId.isEmpty())
                    widgetId = searchInPredefinedLocation(objectName);
            }
        }
        return widgetId;
    }

    private  String searchInPredefinedLocation(String objectName){
        String widgetId = "";
        widgetId = searchInPredefinedMethod(onCreate,objectName);
        if(widgetId.isEmpty() && onStart != null)
            widgetId = searchInPredefinedMethod(onStart,objectName);
        return widgetId;
    }

    private String searchInPredefinedMethod(MethodDeclaration method,String objectName){
        String widgetId = "";
        widgetId = searchWidgetIdInBlockBody(method.getBody().get(),objectName);
        if(widgetId.isEmpty()){
            List<MethodDeclaration> localCalledMethods = getLocalCalledMethodIn(method.getBody().get());
            localCalledMethods.remove(method);
            for(MethodDeclaration methodItem: localCalledMethods){
                widgetId = searchWidgetIdInBlockBody(methodItem.getBody().get(),objectName);
                if(!widgetId.isEmpty())
                    break;
                widgetId = searchInPredefinedMethod(methodItem,objectName);
                if(!widgetId.isEmpty())
                    break;
            }
        }
        return widgetId;
    }

    public  List<MethodDeclaration> getLocalCalledMethodIn(BlockStmt codeBlock){
        List<MethodDeclaration> localCalledMethods = new ArrayList<>();
        List<MethodDeclaration> localMethods = getLocalMethods();
        List<MethodCallExpr> methodCallExprs = getMethodCallExprsCalledDirectlyBy(codeBlock);
        localMethods.forEach(method->{
            methodCallExprs.forEach(methodCallExpr -> {
                if(isMatchWithPattern(method.getNameAsString(),methodCallExpr.getNameAsString()) &&
                        !methodCallExpr.hasScope())
                    localCalledMethods.add(method);
            });
        });
        return localCalledMethods;
    }
    private List<MethodCallExpr> getMethodCallExprsCalledDirectlyBy(Node node){
        List<Node> InnerMethodList = getInnerMethodsIn(node);
        List<MethodCallExpr>  wholeCallExprsList = getMethodCallExprsListFrom(node);
        List<MethodCallExpr>  wholeInnerMethodCallExprs = ASTUtils.getMethodCallExprsFromInnerMethods(InnerMethodList);
        return subtract(wholeCallExprsList,wholeInnerMethodCallExprs);
    }

    private List<MethodDeclaration> getLocalMethods(){
        List<MethodDeclaration> localMethods = new ArrayList<>();
        for(MethodInformation methodInformation: methodDeclarationList){
            if(!methodInformation.isEeventHandler())
                localMethods.add(methodInformation.getAttachedMethod());
        }
        return localMethods;
    }




    private String searchIdInParentsBlockPath(BlockStmt codeBlock, String objectName){
        String widgetId = "";
        codeBlock = ASTUtils.getParentBlock(codeBlock);
        if(codeBlock != null){
            List<VariableDeclarator> localVariablesList = ASTUtils.getLocalVariables(codeBlock);
            if(isLocalVariable(localVariablesList,objectName))
                widgetId = searchWidgetIdInBlockBody(codeBlock,objectName);
            else{
                widgetId = searchWidgetIdInBlockBody(codeBlock,objectName);
                if(widgetId.isEmpty())
                    widgetId = searchIdInParentsBlockPath(codeBlock,objectName);
            }
        }
        return widgetId;
    }

    private List<MethodCallExpr> getMethodCallExprsListByNameFrom(Node node, String callExpr) {
        List<MethodCallExpr> callExprsList = new ArrayList<>();
        node.findAll(MethodCallExpr.class).forEach(item->{
            if(isMatchWithPattern(item.getNameAsString(),callExpr))
                callExprsList.add(item);
        });
        return callExprsList;
    }

    private List<MethodCallExpr> getMethodCallExprsListByNameFromInnerMethods(List<Node> innerMethodsList, String callExpr){
        List<MethodCallExpr> callExprsList = new ArrayList<>();
        for(Node node : innerMethodsList)
            callExprsList.addAll(getMethodCallExprsListByNameFrom(node,callExpr));
        return callExprsList;
    }

    private List<MethodCallExpr> getMethodCallExprsByNameCalledDirectlyBy(Node node,String callExpr){
        List<Node> InnerMethodList = getInnerMethodsIn(node);
        List<MethodCallExpr>  wholeCallExprsList = getMethodCallExprsListByNameFrom(node,callExpr);
        List<MethodCallExpr>  wholeInnerMethodCallExprs = getMethodCallExprsListByNameFromInnerMethods(InnerMethodList,callExpr);
        return subtract(wholeCallExprsList,wholeInnerMethodCallExprs);
    }

    private String searchWidgetIdInBlockBody(BlockStmt blockStmt,String objectName){
        String result = "";
        List<MethodCallExpr> bindingMethodCallExprs =
                getMethodCallExprsByNameCalledDirectlyBy(blockStmt,"findViewById");
        if(bindingMethodCallExprs.isEmpty())
            return result;
        for(MethodCallExpr callExpr : bindingMethodCallExprs){
            if(isMatchWithPattern(getBiningVariableName(callExpr),objectName))
                return callExpr.getArgument(0).toString();

        }
        return result;
    }

    private String getBiningVariableName(MethodCallExpr callExpr) {
        Node tmpNode  = callExpr;
        while(!ASTUtils.isVariableDeclarator(tmpNode) && !ASTUtils.isAssignExpr(tmpNode))
            tmpNode = ASTUtils.getParentNode(tmpNode);
        if(ASTUtils.isAssignExpr(tmpNode))
            return ((AssignExpr) tmpNode).getTarget().toString();
        else
           return ((VariableDeclarator) tmpNode).getNameAsString();
    }

//    private String getContainingClassName(List<VariableDeclarator> variablesList, String objectName) {
//
//        for(VariableDeclarator variable : variablesList)
//            if(variable.getName().toString().equals(objectName)){
//                return variable.getType().toString();
//            }
//        return "";
//    }

    private boolean isLocalVariable(List<VariableDeclarator> localVariablesList, String objectName){
        return isExistVariableInSet(localVariablesList,objectName);
    }

    private static boolean isGlobalVariable(List<VariableDeclarator> globalVariablesList, String objectName) {
        return isExistVariableInSet(globalVariablesList,objectName);
    }



    private static boolean isExistVariableInSet(List<VariableDeclarator> variablesSet, String objectName){
        boolean result = false;
        if(objectName.contains("."))
            objectName = objectName.substring(0,objectName.indexOf('.'));
        for(VariableDeclarator variable : variablesSet)
            if(variable.getName().toString().equals(objectName)){
                result = true;
            }
        return result;
    }

    private Node getIncludedMethod(Node node){
        while(!ASTUtils.isMethodDeclartionExpr(node.getParentNode().get()))
            node = node.getParentNode().get();
        return node.getParentNode().get();
    }

    private List<Node> getInnerMethods(Node node){
        List<Node> innerMethodList = new ArrayList<>();
        node.findAll(MethodDeclaration.class).forEach(item->{
            if(!ASTUtils.isOuterMethod(item))
                innerMethodList.add(item);
        });
        return innerMethodList;
    }

    private List<Node> getInnerMethodsIn(Node node){
        List<Node> innerMethodList = getInnerMethods(node);
        innerMethodList.remove(node);
        return innerMethodList;
    }

    public List<MethodInformation> getMethods() {
        return methodDeclarationList;
    }

    public String getSubClassNameOf(MethodDeclaration method){
        AtomicReference<String> label = new AtomicReference<>("");
        methodDeclarationList.forEach(item->{
            if(isMatchWithPattern(item.getAttachedMethod().toString(),method.toString()) &&
               isMatchWithPattern(getIncludedMethod(item.getAttachedMethod()).toString(),getIncludedMethod(method).toString())) {
                label.set(item.getMainContext());
            }
        });
        return label.get();
    }

    public String getMethodLabel(MethodDeclaration method){
        AtomicReference<String> label = new AtomicReference<>("");
        methodDeclarationList.forEach(item->{
            if(isMatchWithPattern(item.getAttachedMethod().toString(),method.toString()) &&
                    isMatchWithPattern(getIncludedMethod(item.getAttachedMethod()).toString(),getIncludedMethod(method).toString())) {
                   label.set(prepareContent(item.getAttachedViewLable()));
            }
        });
        return label.get();
    }

    public String getMethodBindingWidgetType(MethodDeclaration method){
        AtomicReference<String> label = new AtomicReference<>("");
        methodDeclarationList.forEach(item->{
            if(isMatchWithPattern(item.getAttachedMethod().toString(),method.toString()) &&
                    isMatchWithPattern(getIncludedMethod(item.getAttachedMethod()).toString(),getIncludedMethod(method).toString())) {
                label.set(prepareContent(item.getAttachedViewType()));
            }
        });
        return label.get();
    }

    public MethodInformation getMethodInformation(MethodDeclaration method){
        MethodInformation target = null;
        for(MethodInformation methodItem : methodDeclarationList)
            if(isMatchWithPattern(methodItem.getAttachedMethod().toString(),method.toString()) &&
               isMatchWithPattern(methodItem.getAttachedMethod().getNameAsString(),method.getNameAsString()))
                 if(ASTUtils.isLocalMethod(methodItem.getAttachedMethod()) && ASTUtils.isLocalMethod(method))
                     return methodItem;
                 else


                    return methodItem;
        return target;
    }

    private String prepareContent(String label) {
        String result = "";
        String[] words = label.split(" ");
        for(String word : words)
            result += StringUtils.capitalize(word);
        return  result;
    }


    public boolean isInflateLayoutByActionBar() {
        List<MethodCallExpr> methodCallExpr = getMethodCallExprsByNameCalledDirectlyBy(onCreate,"inflate");
        return !methodCallExpr.isEmpty();
    }

    public String getActionBarLayout() {
        List<MethodCallExpr> methodCallExprs = getMethodCallExprsByNameCalledDirectlyBy(onCreate,"inflate");
        if(isMatchWithPattern(ASTUtils.getScope(methodCallExprs.get(0)),"getLayoutInflater()")){
            String layout = ASTUtils.getArgument(methodCallExprs.get(0),0).toString();
            return layout.substring(layout.lastIndexOf('.') + 1);
        }
        return "";


    }

    public MethodDeclaration findMethodByName(String onCreateOptionsMenu) {
        for(MethodInformation method : methodDeclarationList)
            if(isMatchWithPattern(method.getAttachedMethod().getNameAsString(),onCreateOptionsMenu))
                return method.getAttachedMethod();
        return null;
    }

    public static  String resolveClassName(Node block, String objectName) {
        List<VariableDeclarator> localVariablesList;
        Node involvedBlock = block;
        do{
            localVariablesList = ASTUtils.getLocalVariables(involvedBlock);
            if(ASTUtils.isLocalVariable(localVariablesList,objectName))
                return ASTUtils.getVariableType(localVariablesList,objectName);
            involvedBlock = ASTUtils.getIncludedBlockNode(involvedBlock);
                    //getParentMethodDeclaration(involvedMethod);
        }while(involvedBlock != null);

        List<VariableDeclarator> globalVariablesList = getGlobalVariableList();
        if(isGlobalVariable(globalVariablesList,objectName))
            return ASTUtils.getVariableType(globalVariablesList,objectName);

        return "";

    }

    private static List<VariableDeclarator> getGlobalVariableList() {
        List<FieldDeclaration> fieldsList= new ArrayList<>();
        List<VariableDeclarator> globalVariablesList = new ArrayList<>();
        AST.findAll(FieldDeclaration.class).forEach(item->fieldsList.add(item));
        for(FieldDeclaration field:fieldsList)
            for(int i = 0; i<field.getVariables().size();i++)
                globalVariablesList.add(field.getVariable(i));

        return globalVariablesList;
    }

    public  List<MethodInformation> getExtractedMethods(){
        return methodDeclarationList;
    }

}