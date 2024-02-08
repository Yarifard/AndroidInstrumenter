package ir.ac.um.AndroidInstrumenter.Utils;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import ir.ac.um.AndroidInstrumenter.Analysis.Instruments.CodeAnalyzer;
import ir.ac.um.AndroidInstrumenter.Analysis.Instruments.MethodInformation;
import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles.MenuInformationExtractor;
import ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles.StringValueExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ASTUtils {

    public static Node getParentNode(Node node){
        return node.getParentNode().get();
    }
    public static Node getAncientNode(Node node)
    {

        return getParentNode(getParentNode(node));
    }

    public static String getScope(Node callExpr){
        if(((MethodCallExpr) callExpr).hasScope())
           return ((MethodCallExpr)callExpr).getScope().get().toString();
        return "";
    }

    public static String getClassName(Node node){

        return node.getClass().getName();
    }

    public static Node getIncludedBlockNode(Node node) {
        Node tmpNode = getParentNode(node);
        while(!(isBlockStmt(tmpNode) || isClassOrInterfaceExpr(tmpNode)))
            tmpNode = getParentNode(tmpNode);
        if(!isClassOrInterfaceExpr(tmpNode))
           return tmpNode;
        return null;
    }

    public static List<VariableDeclarator> getLocalVariablesFromNode(Node node){
        List<VariableDeclarator> variableDeclaratorList = new ArrayList<>();
        node.findAll(VariableDeclarator.class).forEach(item->{
            variableDeclaratorList.add(item);
        });
        return variableDeclaratorList;
    }

    public static List<Node> getInnerMethods(Node node){
        List<Node> innerMethodList = new ArrayList<>();
        node.findAll(MethodDeclaration.class).forEach(item->{
            if(!isOuterMethod(item))
                innerMethodList.add(item);
        });
        return innerMethodList;
    }

    public static List<Node> getInnerMethodsIn(Node node){
        List<Node> innerMethodList = getInnerMethods(node);
        innerMethodList.remove(node);
        return innerMethodList;
    }

    public static String extractViewObjectNameFrom(Node node){
        while(!isVariableDeclarator(getParentNode(node)))
            node = getParentNode(node);
        return ((VariableDeclarator) getParentNode(node)).getNameAsString();
    }

    public static String getParameterFrom(Node node,int index){
        if(isMethodDeclartionExpr(node))
            if(!((MethodDeclaration)node).getParameters().isEmpty())
                return ((MethodDeclaration)node).getParameter(index).getType().toString();
        return "";
    }

    public static List<MethodCallExpr> getViewBindingCallExprFrom(Node node){
        List<MethodCallExpr> bindingViewCallExprs = getMethodCallExprsListByName(node,"findViewById");
        List<MethodCallExpr> targetCallExprs  = new ArrayList<>();
        for(MethodCallExpr callExpr :bindingViewCallExprs)
            if(callExpr.hasScope())
                targetCallExprs.add(callExpr);
        return targetCallExprs;
    }

    public static List<MethodCallExpr> getMethodCallExprsListFrom(Node node){
        List<MethodCallExpr> wholeCallExprsList = new ArrayList<>();
        node.findAll(MethodCallExpr.class).forEach(item->{
            wholeCallExprsList.add(item);
        });
        return wholeCallExprsList;
    }


//    public static List<MethodCallExpr> getMethodCallExprsListByName(Node method,String pattern){
//        List<MethodCallExpr> methodCallExprsWithSimilarName = new ArrayList<>();
//        List<MethodCallExpr> methodCallExprList = getMethodCallExprsListFrom(method);
//        for(MethodCallExpr callExprItem : methodCallExprList)
//            if(isMatchWithPattern(pattern,callExprItem.getNameAsString()))
//                methodCallExprsWithSimilarName.add(callExprItem);
//        return methodCallExprsWithSimilarName;
//    }

    public static boolean isClassMember(Node node){
        return isClassOrInterfaceExpr(getParentNode(node));
    }

    public static boolean isAssignExpr(Node node){
        return Utils.isPartialMatchWithPattern(getClassName(node),"AssignExpr");

    }

    public static boolean isMethodDeclartionExpr(Node node){

        return Utils.isPartialMatchWithPattern(getClassName(node),"MethodDeclaration");
    }


    public static boolean isMethodCallExpr(Node node){

        return Utils.isPartialMatchWithPattern(getClassName(node),"MethodCallExpr");
    }

    public static boolean isClassOrInterfaceExpr(Node node){
       return Utils.isPartialMatchWithPattern(getClassName(node),"ClassOrInterface");
    }

    public static boolean isVariableDeclarator(Node node){
        return Utils.isPartialMatchWithPattern(getClassName(node),"VariableDeclarator");
    }

    public static boolean isLocalVariable(List<VariableDeclarator> localVariablesList, String objectName){
        return isExistVariableInSet(localVariablesList,objectName);
    }

    public static boolean isExistVariableInSet(List<VariableDeclarator> variablesSet, String objectName){
        boolean result = false;
        if(objectName.contains("."))
            objectName = objectName.substring(0,objectName.indexOf('.'));
        for(VariableDeclarator variable : variablesSet)
            if(variable.getName().toString().equals(objectName)){
                result = true;
            }
        return result;
    }

    public static boolean isMethodDeclarationExpr(Node node){
        return Utils.isPartialMatchWithPattern(getClassName(node),"MethodDeclaration");
    }

    public static boolean isBlockStmt(Node node){

        return Utils.isPartialMatchWithPattern(getClassName(node),"BlockStmt");
    }

    public static boolean isOuterMethod(Node node) {
        return isVariableDeclarator(getAncientNode(node));
    }

    public static boolean hasParameter(Node node, int index){
        if(isMethodDeclarationExpr(node))
            if(!((MethodDeclaration)node).getParameters().isEmpty())
                if(((MethodDeclaration)node).getParameter(index) != null )
                    return true;
        return false;
    }


    private static String resolveClassName(Node globalBock,Node block, String objectName) {
        List<VariableDeclarator> localVariablesList;
        Node involvedBlock = block;
        do{
            localVariablesList = ASTUtils.getLocalVariables(involvedBlock);
            if(isLocalVariable(localVariablesList,objectName))
                return getVariableType(localVariablesList,objectName);
            involvedBlock = ASTUtils.getIncludedBlockNode(involvedBlock);
            //getParentMethodDeclaration(involvedMethod);
        }while(involvedBlock != null);

        List<VariableDeclarator> globalVariablesList = getGlobalVariableList(globalBock);
        if(isGlobalVariable(globalVariablesList,objectName))
            return getVariableType(globalVariablesList,objectName);

        return "";
    }

    private static List<VariableDeclarator> getGlobalVariableList(Node node) {
        List<FieldDeclaration> fieldsList= new ArrayList<>();
        List<VariableDeclarator> globalVariablesList = new ArrayList<>();
        node.findAll(FieldDeclaration.class).forEach(item->fieldsList.add(item));
        for(FieldDeclaration field:fieldsList)
            for(int i = 0; i<field.getVariables().size();i++)
                globalVariablesList.add(field.getVariable(i));

        return globalVariablesList;
    }

    private static boolean isGlobalVariable(List<VariableDeclarator> globalVariablesList, String objectName) {
        return isExistVariableInSet(globalVariablesList,objectName);
    }

    public static boolean isPlacedInConditionBlock(Node node) {
        while(!((node instanceof IfStmt || node instanceof SwitchStmt || node instanceof MethodDeclaration )))
             node = getParentNode(node);
        if(node instanceof IfStmt)
            return  true;
        else if(node instanceof SwitchStmt)
            return true;
        else
            return false;
    }

    public static boolean isPlacedInSameBlock(BlockStmt parentBlock, BlockStmt childBlock) {
        if(isMatchWithPattern(parentBlock.toString(),childBlock.toString()))
            return true;
        return false;
    }

    public static boolean isDialogMethod(Node node) {
        if(isDefaultDialogMethodPattern(node))
            return true;
        else if(isCustomizedDialogMethodPattern(node))
            return true;
        return false;
    }

    public static boolean isDefaultDialogMethodPattern(Node node){
        int indexParameter = 0;
        if(hasParameter(node,indexParameter)){
            String parameter = getParameterFrom(node,indexParameter);
            if(isMatchWithPattern("DialogInterface",parameter))
                return true;
        }
        return false;
    }

    public static boolean isCustomizedDialogMethodPattern(Node method){
        String bindingViewObjectName = getBindingViewObjectNameOfInnerMethod(method);
        if(bindingViewObjectName.isEmpty())
            return false;
        Node node = getIncludedBlockNode(method);
        List<MethodCallExpr> callExprsList = getViewBindingMethodCallExprsDirectlyBy(node);
        for(MethodCallExpr callExpr :callExprsList){
            String extractedViewObjectName = extractViewObjectNameFrom(callExpr);
            if(extractedViewObjectName.contentEquals(bindingViewObjectName))
                if(callExpr.hasScope()){
                    String object = getScope(callExpr);
                    if(isPartialMatchWithPattern("Dialog",resolveClassNameFromNode(node,object)))
                        return true;
                }
                else
                    return false;
        }
        return false;
    }

    public static String getArgument(Node methodCallExpr, int argIndex){
        return ((MethodCallExpr) methodCallExpr).getArgument(argIndex).toString();
    }


    public static BlockStmt getParentBlock(Node node){
        BlockStmt parentBlock = null;
        Node tmpNode = node;
        do{
            tmpNode = getParentNode(tmpNode);
        }while(!(isBlockStmt(tmpNode) || isClassOrInterfaceExpr(tmpNode)));


        if(isBlockStmt(tmpNode))
            parentBlock = (BlockStmt) tmpNode;
        return parentBlock;

    }

    public static List<MethodCallExpr> getViewBindingMethodCallExprsDirectlyBy(Node node){
        List<Node> innerMethodList = getInnerMethodsIn(node);
        List<MethodCallExpr>  callExprsList = new ArrayList<>();
        List<MethodCallExpr>  bindingViewCallExpr = getViewBindingCallExprFrom(node);
        for(Node innerMethod : innerMethodList)
            bindingViewCallExpr = subtract(bindingViewCallExpr, getViewBindingCallExprFrom(innerMethod));
        return bindingViewCallExpr;
    }


    public static String getBindingViewObjectNameOfInnerMethod(Node node) {
        String bindingViewName = "";
        if(!isClassMember(node)){
           // node = getParentNode(node);
            while(!isMethodCallExpr(node)){
                if(isVariableDeclarator(node) || isAssignExpr(node))
                    break;
                node = getParentNode(node);
            }
            if(isMethodCallExpr(node))
                bindingViewName = getScope((MethodCallExpr) node);
//            if(isAssignExpr(node))
//                bindingViewName = "";
        }
        return bindingViewName;
    }

    public static String resolveClassNameFromNode(Node node, String objectName) {
        List<VariableDeclarator> localVariablesList;
        if(objectName.contains("."))
            objectName = objectName.substring(0,objectName.indexOf('.'));
        localVariablesList = getLocalVariablesFromNode(node);
        if(isLocalVariable(localVariablesList,objectName))
            return getVariableType(localVariablesList,objectName);
        return "";
    }

    public static String getVariableType(List<VariableDeclarator> variablesList, String variableName) {

        for(VariableDeclarator variable : variablesList)
            if(isMatchWithPattern(variable.getNameAsString(),variableName)){
                return variable.getType().toString();
            }
        return "";
    }

    public static List<MethodCallExpr> subtract(List<MethodCallExpr> wholeSet, List<MethodCallExpr> subSet){
        List<MethodCallExpr> resultMethodCallExprsList = new ArrayList<>();
        for(int index = 0 ; index < wholeSet.size(); index++)
            if(!subSet.contains(wholeSet.get(index)))
                resultMethodCallExprsList.add(wholeSet.get(index));
        return resultMethodCallExprsList;
    }

    public static boolean isOpenActivityBy(MethodDeclaration attachedMethod) {
        String pattern = "Intent";
        if(hasDefinedIntentCreationObjectIn(attachedMethod))
            return true;
        return false;
    }

    public static String getOpenActivityBy(MethodDeclaration method) {
        String activityName = "";
        boolean flag = false;
        List<ObjectCreationExpr> objectCreationExprs =
                ASTUtils.getDirectlyDefinedIntentCreationObjectIn(method);

        for (ObjectCreationExpr objExpr : objectCreationExprs) {
            if(ASTUtils.isPlacedInSameBlock(method.getBody().get(),
                    ASTUtils.getParentBlock(objExpr)) ||
                    !ASTUtils.isPlacedInConditionBlock(objExpr)){
                activityName = getActivityName(method,objExpr);
                if(!activityName.isEmpty())
                    return activityName;
            }
        }

        List<MethodCallExpr> callExprs = ASTUtils.getMethodCallExprsCalledDirectlyBy(method);
        for(MethodCallExpr callExpr : callExprs){
            if (ASTUtils.isPlacedInSameBlock(method.getBody().get(),
                    ASTUtils.getParentBlock(callExpr)) ||
                    !ASTUtils.isPlacedInConditionBlock(callExpr)){
                MethodDeclaration newMethod = CodeAnalyzer.findMethodByName(callExpr);
                if(newMethod != null)
                    if(ASTUtils.isLocalMethod(newMethod)){
                        if(ASTUtils.isLocalMethod(newMethod) && !ASTUtils.areSameMethod(method,newMethod))
                            activityName = getOpenActivityBy(newMethod);
                        if(!activityName.isEmpty()){
                            return activityName;
                        }
                    }
            }
        }
        return activityName;
    }

    public static String getActivityName(MethodDeclaration method, ObjectCreationExpr objExpr){
        String argument = "";
        String intent = "";
        switch (objExpr.getArguments().size()) {
            case 0:
                List<MethodCallExpr> calledIntentMethods =
                        ASTUtils.getMethodCallExprsByNameCalledDirectlyBy(method,"setAction");
                if(!calledIntentMethods.isEmpty()){
                    if(calledIntentMethods.get(0).hasScope()){
                        String className = CodeAnalyzer.resolveClassName(method.getBody().get(),ASTUtils.getScope(calledIntentMethods.get(0)));
                        if(Utils.isMatchWithPattern(className,"Intent")){
                            argument = calledIntentMethods.get(0).getArgument(0).toString();
                            argument = argument.substring(intent.lastIndexOf('.') + 1 );
                            intent = getActionView(argument);
                            return intent;
                        }
                    }
                }
                else{
                    calledIntentMethods =
                            ASTUtils.getMethodCallExprsByNameCalledDirectlyBy(method,"setClass");
                    if(!calledIntentMethods.isEmpty())
                        if(calledIntentMethods.get(0).hasScope()){
                            String className = CodeAnalyzer.resolveClassName(method.getBody().get(),ASTUtils.getScope(calledIntentMethods.get(0)));
                            if(Utils.isMatchWithPattern(className,"Intent")){
                                argument = calledIntentMethods.get(0).getArgument(1).toString();
                                intent = argument.substring(0, argument.lastIndexOf('.'));
                                return intent;
                            }
                        }
                }
                break;
            case 1:
                argument = objExpr.getArgument(0).toString();
                argument = argument.substring(argument.lastIndexOf('.') + 1 );
                intent = getActionView(argument);
                return intent;
            case 2:
                argument = objExpr.getArgument(0).toString();
                if(argument.contains("Intent.ACTION_")){
                    argument = argument.substring(argument.lastIndexOf('.') + 1 );
                    intent = getActionView(argument);
                    return intent;
                }
                else{
                    argument = objExpr.getArgument(1).toString();
                    intent = argument.substring(0, argument.lastIndexOf('.'));
                    return intent;
                }
        }
        return intent;
    }

    private static String getActionView(String argument) {
        String activityName = "";
        switch (argument) {
            case "ACTION_CALL":
                activityName = "PhoneCall";
                break;
            case "ACTION_SENDTO":
                activityName = "SendEmail";
                break;
            case "ACTION_SEND":
                activityName = "ShareContent";
                break;
            case "ACTION_IMAGE_CAPTURE":
                activityName = "CaptureImage";
                break;
            case "ACTION_VIEW":
                activityName = "ViewWebPage";
                break;
        }
        return activityName;
    }



    public static boolean hasDefinedIntentCreationObjectIn(MethodDeclaration method) {
        if(!getDirectlyDefinedIntentCreationObjectIn(method).isEmpty()){
            List<MethodCallExpr> finishMethodCallExprs = getMethodCallExprsByNameCalledDirectlyBy(method,"finish");
            for(MethodCallExpr finishMethodCallExpr :finishMethodCallExprs){
                 if(getScope(finishMethodCallExpr).isEmpty() &&
                    finishMethodCallExpr.getArguments().isEmpty())
                     return false;
            }
            return true;
        }
        else
            return getInDirectlyPredefinedObjectCreationPatternIn(method);
    }

    public static List<ObjectCreationExpr> getDirectlyDefinedIntentCreationObjectIn(Node method){
        String intent = "Intent";
        List<ObjectCreationExpr> objectCreationExprs = new ArrayList<>();
        method.findAll(ObjectCreationExpr.class).forEach(item->{
            if(Utils.isMatchWithPattern(intent,item.getType().toString()))
                if(isPlacedInSameBlock(((MethodDeclaration)method).getBody().get(),getParentBlock(item)) ||
                        !isPlacedInConditionBlock(item))
                    objectCreationExprs.add(item);
        });
        return objectCreationExprs;
    }

    private static boolean getInDirectlyPredefinedObjectCreationPatternIn(MethodDeclaration method) {
        List<MethodCallExpr> callExprsList = getMethodCallExprsCalledDirectlyBy(method);
        for(MethodCallExpr callExpr:callExprsList)
            if (ASTUtils.isPlacedInSameBlock(method.getBody().get(),
                ASTUtils.getParentBlock(callExpr)) ||
                !ASTUtils.isPlacedInConditionBlock(callExpr)){
                MethodDeclaration targetMethod = CodeAnalyzer.findMethodByName(callExpr);
                if(targetMethod != null)
                    if(ASTUtils.isLocalMethod(targetMethod))
                       if(ASTUtils.isLocalMethod(targetMethod) && !ASTUtils.areSameMethod(method,targetMethod))
                             if(hasDefinedIntentCreationObjectIn(targetMethod)){
                                 return true;
                        }
            }
        return false;
    }

    public static boolean isMatchWithPatterns(List<String> patterns,String targetItem){
        if(patterns.contains(targetItem))
            return true;
        return false;
    }

    public static boolean isMatchWithPattern(String pattern,String targetItem ){
        if(pattern.contentEquals(targetItem))
            return true;
        return false;
    }

    public static boolean isPartialMatchWithPattern(List<String> patterns,String targetItem){
        for(String item : patterns)
            if(targetItem.contains(item))
                return true;
        return false;
    }

    public static boolean isPartialMatchWithPattern(String pattern,String targetItem){
        if(targetItem.contains(pattern) || pattern.contains(targetItem))
            return true;
        return false;
    }


    public static boolean isMenuEventHandlerMethod(MethodInformation methodInformation) {
        return isMatchWithPattern(methodInformation.getAttachedMethod().getNameAsString(),
                "onMenuItemClick");
    }

    public static boolean isMainMenuItemEventHandler(MethodInformation methodInformation) {
        boolean flag = false;
        Node includedBlock = getIncludedBlockNode(methodInformation.getAttachedMethod());
        MethodDeclaration method = (MethodDeclaration) getParentNode(includedBlock);
        if(isMatchWithPattern(method.getNameAsString(),"onCreateOptionsMenu"))
            flag = true;
        return flag;

    }

    public static Node getIncludedMethod(Node node) {
        do {
            node = getParentNode(node);
        }
        while(!isMethodDeclartionExpr(node));

        return  node;
    }

    public static boolean menuHasLayout(Node method) {
        boolean result = false;
//        if(!isPartialMatchWithPattern(method.getNameAsString(),"onCreateOptionsMenu"))
//             method = (MethodDeclaration) getIncludedMethod(method);
        List<MethodCallExpr> methodCallExprs =
                getMethodCallExprsByNameCalledDirectlyBy(method,"inflate");
        if(!methodCallExprs.isEmpty())
            result = true;
        return result;
    }



    public static String extrctMenuLayout(Node method) {
//        method = (MethodDeclaration) getIncludedMethod(method);
//        if(menuHasLayout(method)){
             List<MethodCallExpr> methodCallExprs = getMethodCallExprsByNameCalledDirectlyBy(method,"inflate");
             String menuLayoutFile = methodCallExprs.get(0).getArgument(0).toString();
            return menuLayoutFile.substring(menuLayoutFile.lastIndexOf('.') + 1);
      //  }
      //  return "";
    }

    public static String extractMenuIdFrom(MethodDeclaration method) {
        String viewId = "";
        MethodCallExpr methodCallExpr = getParentMethodCallExpr(method);
        String content = methodCallExpr.getScope().toString();
        viewId = content.substring(content.indexOf('(') + 1, content.indexOf(')'));
        return viewId;
    }

    public static boolean isDefinedInMenuLayout(ProjectInformation projectInformation,String menulayoutFileName,
                                               String menuItemId){
        MenuInformationExtractor menuInformationExtractor = new MenuInformationExtractor(projectInformation,menulayoutFileName);
        if(menuInformationExtractor.isExistInMenuLayout(menuItemId))
            return true;
        return false;
    }

    public static String extractMenuItemLabelFromLayout(ProjectInformation projectInformation,
                                                  String menuLayout, String menuId) {
        if(menuId.startsWith("android.R.id."))
            return "";
        MenuInformationExtractor menuInformationExtractor =
                new MenuInformationExtractor(projectInformation, menuLayout);
         return menuInformationExtractor.findViewLabelById(menuId);
    }


    public static MethodCallExpr getParentMethodCallExpr(Node node) {
        while(node.getClass() != MethodCallExpr.class)
            node = getParentNode(node);
        return (MethodCallExpr) node;
    }

    public static  List<MethodCallExpr> getMethodCallExprsByNameCalledDirectlyBy(Node node,String callExpr){
        List<Node> InnerMethodList = getInnerMethodsIn(node);
        List<MethodCallExpr>  wholeCallExprsList = getMethodCallExprsListByNameFrom(node,callExpr);
        List<MethodCallExpr>  wholeInnerMethodCallExprs = getMethodCallExprsListByNameFromInnerMethods(InnerMethodList,callExpr);
        return subtract(wholeCallExprsList,wholeInnerMethodCallExprs);
    }

    public static List<MethodCallExpr> getMethodCallExprsCalledDirectlyBy(Node node){
        List<Node> InnerMethodList = getInnerMethodsIn(node);
        List<MethodCallExpr>  wholeCallExprsList = getMethodCallExprsListFrom(node);
        List<MethodCallExpr>  wholeInnerMethodCallExprs = getMethodCallExprsFromInnerMethods(InnerMethodList);
        return subtract(wholeCallExprsList,wholeInnerMethodCallExprs);
    }

    public static List<MethodCallExpr> getMethodCallExprsFromInnerMethods(List<Node> innerMethods){
        List<MethodCallExpr> callExprs = new ArrayList<>();
        for(Node node : innerMethods)
            callExprs.addAll(getMethodCallExprsListFrom(node));
        return callExprs;
    }

    public static boolean isLocalMethod(Node node){
        node = getParentNode(node);
        if(Utils.isPartialMatchWithPattern(getClassName(node),"ClassOrInterfaceDeclaration"))
            return true;
        else
            return false;

    }

    public static MethodDeclaration findMethodByName(List<MethodInformation> methodInformationList,
                                                      MethodCallExpr methodCallExpr) {
        for(MethodInformation method: methodInformationList)
            if(methodCallExpr.getNameAsString().contentEquals(method.getAttachedMethod().getNameAsString()))
                return method.getAttachedMethod();
        return null;
    }

    public static boolean isAssignedMenuViewId(MethodInformation methodInformation) {
        boolean result = false;
        MethodCallExpr methodCallExpr = getParentMethodCallExpr(methodInformation.getAttachedMethod());
        String content = methodCallExpr.getScope().toString();
        if (isPartialMatchWithPattern(content,"menu.findItem("))
            result = true;
        return result;
    }

    public static String extractMenuItemLabelFromSourceCodeById(ProjectInformation projectInformation,
                                                                MethodInformation methodInformation,String viewId) {
        AtomicReference<String> viewLabel = new AtomicReference<>("");
        Node includedBlock = getIncludedBlockNode(methodInformation.getAttachedMethod());
        List<MethodCallExpr>  callExprs = getMethodCallExprsByNameCalledDirectlyBy(includedBlock,"add");
        callExprs.forEach(callExpr -> {
            if(callExpr.hasScope())
                if(isMatchWithPattern(getScope(callExpr),"menu")){
                    String menuId = "";
                    if(callExpr.getArguments().size() > 1){
                        menuId = getArgument(callExpr,1);
                        if(isMatchWithPattern(menuId,viewId)){
                            String content = callExpr.getArguments().get(3).toString();
                            if(content.startsWith("getString("))
                                content = content.substring(content.indexOf('(') + 1, content.lastIndexOf(')'));
                            if(content.startsWith("R.string."))
                                content = getStringValue(projectInformation,content);
                            viewLabel.set(content);
                        }
                    }
                }
        });
        return viewLabel.get();
    }

    private static String getStringValue(ProjectInformation projectInformation,String value) {
        String result = "";
        if(value.startsWith("R.string.") || value.startsWith("@string/")){
            StringValueExtractor stringValueExtractor =
                    new StringValueExtractor(projectInformation, "strings");
            result = stringValueExtractor.findViewLabelById(value);
        }
        return result;
    }

    public static String extractMenuItemLabelFromSouceCode(ProjectInformation projectInformation,
                                                           MethodInformation methodInformation) {
        String viewLabel = "";
        MethodCallExpr methodCallExpr = (MethodCallExpr) getParentMethodCallExpr(
                methodInformation.getAttachedMethod()).getChildNodes().get(0);
        if(methodCallExpr.getArguments().size() == 1)
            viewLabel = getArgument(methodCallExpr,0);
        else
            viewLabel = getArgument(methodCallExpr,3);

        if(viewLabel.startsWith("getString"))
            viewLabel = viewLabel.substring(viewLabel.indexOf('(') + 1, viewLabel.lastIndexOf(')'));
        if(viewLabel.startsWith("R.string"))
            viewLabel = getStringValue(projectInformation,viewLabel);
        return  viewLabel;
    }

    private static List<MethodCallExpr> getMethodCallExprsListByNameFrom(Node node, String callExpr) {
        List<MethodCallExpr> callExprsList = new ArrayList<>();
        node.findAll(MethodCallExpr.class).forEach(item->{
            if(isMatchWithPattern(item.getNameAsString(),callExpr))
                callExprsList.add(item);
        });
        return callExprsList;
    }

    private static List<MethodCallExpr> getMethodCallExprsListByNameFromInnerMethods(List<Node> innerMethodsList, String callExpr){
        List<MethodCallExpr> callExprsList = new ArrayList<>();
        for(Node node : innerMethodsList)
            callExprsList.addAll(getMethodCallExprsListByNameFrom(node,callExpr));
        return callExprsList;
    }


    public static String extractMenuItemIdFromSouceCode(MethodInformation methodInformation) {
        String viewId = "";
        MethodCallExpr methodCallExpr = (MethodCallExpr) getParentMethodCallExpr(
                methodInformation.getAttachedMethod()).getChildNodes().get(0);
        if(methodCallExpr.getArguments().size() > 1)
            viewId= getArgument(methodCallExpr,1);
        return  viewId;
    }

    public static BlockStmt findTargetBlock(Node onCreateMainMenuMethod) {
        List<MethodCallExpr> methodCallExprs = getMethodCallExprsByNameCalledDirectlyBy(onCreateMainMenuMethod,"inflate");
        BlockStmt targetBlock = ((MethodDeclaration) onCreateMainMenuMethod).getBody().get();
        for(MethodCallExpr methodCallExpr :methodCallExprs)
            if(methodCallExpr.hasScope())
                if(isMatchWithPattern(getScope(methodCallExpr),"getMenuInflater()"))
                   return  (BlockStmt) getParentBlock((methodCallExpr));
        return targetBlock;
    }

    private static MethodCallExpr findMethodExprByName(List<MethodCallExpr> calledMethods, String calledMethodName) {
        MethodCallExpr result = null;
        for (MethodCallExpr item : calledMethods)
            if (item.getName().toString().equals(calledMethodName)) {
                result = item;
                break;
            }
        return result;
    }

    public static String extractMenuLayoutFileName(MethodDeclaration method) {
        String menuFileLayoutName =  "" ;
        List<MethodCallExpr> calledMethods = getMethodCallExprsCalledDirectlyBy(method);
        MethodCallExpr resultMethodCallExpr = findMethodExprByName(calledMethods, "inflate");
        if (resultMethodCallExpr != null) {
            String menuFileName = resultMethodCallExpr.getArgument(0).toString();
           menuFileLayoutName = menuFileName.substring(menuFileName.lastIndexOf('.') + 1);
        }
        return menuFileLayoutName;
    }

    public static boolean isDatePickerDialog(Node node) {
        int indexParameter = 0;
        if(hasParameter(node,indexParameter)){
            String parameter = getParameterFrom(node,indexParameter);
            if(isMatchWithPattern("DatePicker",parameter))
                return true;
        }
        return false;

    }


    public static MethodDeclaration getParentMethodDeclaration(Node method){
        MethodDeclaration parentBlock = null;
        Node tmpNode = (Node) method;
        do{
            tmpNode = getParentNode(tmpNode);
            if(isMethodDeclartionExpr(tmpNode)){
                parentBlock = (MethodDeclaration) tmpNode;
                break;
            }
        }while(!isClassOrInterfaceExpr(tmpNode));
        return parentBlock;
    }

    //ToDo::This method requires to inspect concisely.
    public static List<VariableDeclarator> getLocalVariables(Node block) {
        List<VariableDeclarator> localVariablesList = new ArrayList<>();
        block.findAll(VariableDeclarator.class).forEach(item->{
            Node tmpNode = (Node) item;
//            while(!tmpNode.getParentNode().get().getClass().getName().contains("MethodDeclaration"))
//                tmpNode = tmpNode.getParentNode().get();
            if((isMatchWithPattern(getIncludedBlockNode(item).toString(),block.toString())))
                localVariablesList.add(item);
//            if(!callerMethod.isAncestorOf(tmpNode.getParentNode().get()))
//                  localVariablesList.add(item);

        });

        return localVariablesList;
    }

    public static boolean isObjectCreationMethod(Node node) {
        while(!(isAssignExpr(node) ||isClassOrInterfaceExpr(node) || isVariableDeclarator(node)))
            node = getParentNode(node);
        if(isAssignExpr(node) || isVariableDeclarator(node))
            return true;
        return false;
    }

    public static List<MethodCallExpr> getMethodCallExprsListByNames(Node method,List<String> patterns){
        List<MethodCallExpr> methodCallExprsWithSimilarName = new ArrayList<>();
        List<MethodCallExpr> methodCallExprList = getMethodCallExprsListFrom(method);
        for(MethodCallExpr callExprItem : methodCallExprList)
            if(Utils.isMatchWithPatterns(patterns,callExprItem.getNameAsString()))
                methodCallExprsWithSimilarName.add(callExprItem);
        return methodCallExprsWithSimilarName;
    }

    public static List<MethodCallExpr> getMethodCallExprsByNamesDirectlyBy(Node node,List<String> patterns){
        List<MethodCallExpr> callExprs = getMethodCallExprsListByNames(node,patterns);
        List<MethodCallExpr> innerCallExpr = getMethodCallExprsFromInnerMethods(ASTUtils.getInnerMethodsIn(node));
        return subtract(callExprs,innerCallExpr);
    }

    public static  List<MethodCallExpr> getMethodCallExprsByNameDirectlyBy(Node node,String pattern){
        List<MethodCallExpr> callExprs = getMethodCallExprsListByName(node,pattern);
        List<MethodCallExpr> innerCallExpr = getMethodCallExprsFromInnerMethods(getInnerMethodsIn(node));
        return subtract(callExprs,innerCallExpr);

    }


    public static List<MethodCallExpr> getMethodCallExprsListByName(Node method,String pattern){
        List<MethodCallExpr> methodCallExprsWithSimilarName = new ArrayList<>();
        List<MethodCallExpr> methodCallExprList = ASTUtils.getMethodCallExprsListFrom(method);
        for(MethodCallExpr callExprItem : methodCallExprList)
            if(Utils.isMatchWithPattern(pattern,callExprItem.getNameAsString()))
                methodCallExprsWithSimilarName.add(callExprItem);
        return methodCallExprsWithSimilarName;
    }

    public static MethodCallExpr getMethodCallExprByName(Node node,String methodName) {
        List<MethodCallExpr> callExprsList = getMethodCallExprsListByName(node,methodName);
        if(callExprsList.size() > 0)
            return callExprsList.get(0);
        else
            return null;
    }

    public static MethodDeclaration getMethodByName(List<MethodDeclaration> methods,String targetMethodName) {
        MethodDeclaration targetMethod = null;
        for(MethodDeclaration method : methods)
            if(Utils.isMatchWithPattern(method.getNameAsString(),targetMethodName)){
                targetMethod = method;
                break;
            }
        return targetMethod;
    }

//    private boolean hasParameter(Node node,int index){
//        if(isMethodDeclartionExpr(node))
//            if(!((MethodDeclaration)node).getParameters().isEmpty())
//                if(((MethodDeclaration)node).getParameter(index) != null )
//                    return true;
//        return false;
//    }

    public static String getArgumentFrom(MethodCallExpr callExpr,int argIndex){
        if(!callExpr.getArguments().isEmpty())
            return callExpr.getArgument(argIndex).toString();
        return "";
    }

    public static boolean isSimilar(Node source,Node destination){
        if(source == destination)
            return true;
        return false;
    }

    public static List<Node> getDialogMethodsFrom(Node method){
        List<Node> innerMethods = getInnerMethods(method);
        List<Node> dialogMethods = new ArrayList<>();
        for(Node item: innerMethods){
            if(isSimilar(getIncludedBlockNode(item),method))
                if(isDialogMethod(item))
                    dialogMethods.add(item);
        }
        return dialogMethods;
    }

    public static String getBindingVariableName(MethodCallExpr callExpr) {
        Node node = (Node) callExpr;
        while(!(ASTUtils.isAssignExpr(node) || ASTUtils.isVariableDeclarator(node)))
            node = node.getParentNode().get();
        if(ASTUtils.isAssignExpr(node))
            return ((AssignExpr) node).getTarget().toString();
        else
            return ((VariableDeclarator) node).getNameAsString();

    }

    public static boolean areSameMethod(MethodDeclaration method, MethodDeclaration targetMethod) {
        if(Utils.isMatchWithPattern(method.toString(),targetMethod.toString()))
            return true;
        return false;
    }
}
