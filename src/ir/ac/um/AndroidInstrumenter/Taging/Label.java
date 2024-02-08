package ir.ac.um.AndroidInstrumenter.Taging;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import ir.ac.um.AndroidInstrumenter.Analysis.Instruments.CodeAnalyzer;
import ir.ac.um.AndroidInstrumenter.Analysis.Instruments.MethodInformation;
import ir.ac.um.AndroidInstrumenter.Analysis.Patterns.Pattern;
import ir.ac.um.AndroidInstrumenter.Utils.ASTUtils;
import ir.ac.um.AndroidInstrumenter.Utils.Utils;
import opennlp.tools.util.StringUtil;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public  class Label {


    public static void generator(String contextTitle, POSTagger posTagger, List<MethodInformation> methodlist){
        methodlist.forEach(method->{
            if(method.isEeventHandler())
                method.setTitle(generateMeaningfulLabelForEventHandler(posTagger,methodlist,method));
            else
                method.setTitle(generateMeaningfulLabelForLocalMethod(method));
        });

    }

    public static String generateMeaningfulLabelForEventHandler(POSTagger posTagger,
                                                                List<MethodInformation> methodInformationList,MethodInformation method) {
        String generatedLabel = "";
        generatedLabel = extractMeaningfullTagFor(posTagger,methodInformationList,method);
        return generatedLabel;
    }

    public static String generateMeaningfulLabelForLocalMethod(MethodInformation method) {
        String generatedLabel = "";

        return generatedLabel;
    }

    public static String extractMeaningfullTagFor(POSTagger posTagger,
                                                   List<MethodInformation> methodInformationList,
                                                   MethodInformation method) {
        String[] confirmPatterns = {"Yes","OK"};
        String[] cancelPatterns = {"No", "Cancel","Close"};
        String verbPart = "";
        String nonePart = "";
        String tag = "";
        if(CodeAnalyzer.containDialog(method.getAttachedMethod()))
            return generateTagForDialog(posTagger, method.getContext_title(),method.getAttachedViewType());

        if(ASTUtils.isDialogMethod(method.getAttachedMethod())){
            if(Utils.isMatchWithPatterns(confirmPatterns,method.getAttachedViewLable()) ||
               Utils.isMatchWithPatterns(cancelPatterns,method.getAttachedViewLable()) ||
               ASTUtils.isDefaultDialogMethodPattern(method.getAttachedMethod())){
                if(Utils.isMatchWithPatterns(cancelPatterns,method.getAttachedViewLable()))
                     tag = "CloseDialog";
                else{
                        if(method.hasParent()){
                            if(Utils.isMatchWithPatterns(confirmPatterns,method.getAttachedViewLable()))
                                tag = method.getParentEventHandlerInformation().getTitle();
                            else
                                tag = method.getAttachedViewLable() + method.getParentEventHandlerInformation().getTitle();
                        }
                        else
                                tag = generateLabel(posTagger,method.getContext_title(),method.getAttachedViewType());
                }
            }
            else
                tag = generateLabel(posTagger,method.getContext_title(),method.getAttachedViewType());

            return StringUtils.capitalize(tag);
        }
        if(Utils.isPartialMatchWithPattern(method.getAttachedViewType(),"MenuItem")){
            if(ASTUtils.isOpenActivityBy(method.getAttachedMethod()))
                tag = generateTagForOpenActivityPattern(method.getAttachedMethod());
            else if(!method.getAttachedViewLable().isEmpty()){
                tag =  method.getAttachedViewLable();
                tag = WordUtils.capitalize(tag);
                tag = tag.replaceAll(" ","");
            }
            else{
                List<MethodCallExpr> callExprs = ASTUtils.getMethodCallExprsCalledDirectlyBy(method.getAttachedMethod());
                for(MethodCallExpr callExpr : callExprs)
                    if(!callExpr.hasScope() && ASTUtils.isLocalMethod(CodeAnalyzer.findMethodByName(callExpr))){
                        tag = callExpr.getNameAsString();
                        break;
                    }
            }
            return StringUtils.capitalize(tag);
        }

        if(ASTUtils.isOpenActivityBy(method.getAttachedMethod())){
            tag = generateTagForOpenActivityPattern(method.getAttachedMethod());
            return StringUtils.capitalize(tag);
        }

        if(!method.getAttachedViewLable().isEmpty()){
            verbPart = extractVerbPart(posTagger,method.getAttachedViewType(),method.getAttachedViewLable());
            nonePart = extractNounPart(posTagger,method.getAttachedViewType(),method.getAttachedViewLable());
            if(!(verbPart.isEmpty() || nonePart.isEmpty())){
                tag =  generateTag(verbPart,nonePart);
                return StringUtils.capitalize(tag);
            }
        }

        if(!method.getAttachedViewContentDescription().isEmpty()) {
            verbPart = extractVerbPart(posTagger,method.getAttachedViewType(), method.getAttachedViewContentDescription());
            nonePart = extractNounPart(posTagger,method.getAttachedViewType(), method.getAttachedViewContentDescription());
            if (!(verbPart.isEmpty() || nonePart.isEmpty())){
                tag = generateTag(verbPart,nonePart);
                return StringUtils.capitalize(tag);
            }
        }

        tag = prepareContent1(method.getBindingName()) +
                StringUtils.capitalize(method.getAttachedMethod().getNameAsString());;
        return StringUtils.capitalize(tag);
    }

    public static String generateTagForDialog(POSTagger posTagger, String content,String viewType) {
        String verbPart;
        String nonePart;
        String tag;
        verbPart = extractVerbPartOfTagForDialog(posTagger,content,viewType);
        nonePart = extractNonePartOfTagForDialog(posTagger,content,viewType);
        tag      = generateTag(verbPart,nonePart);
        return StringUtils.capitalize(tag);
    }

    public static String generateTagByUsingBindingVaribale(POSTagger posTagger, String content) {
        content = prepareContent(content);
        return StringUtils.capitalize(posTagger.getNoneTagFor("",content));
    }

    public static String generateTag(String verbPart, String nonePart) {
        String verb = StringUtil.toLowerCase(verbPart);
        String none = StringUtil.toLowerCase(nonePart);
        if(verb.length() >= none.length()){
            if(verb.contains(none))
                return StringUtils.capitalize(verbPart);
        }
        else{
            if(none.contains(verb))
                return StringUtils.capitalize(nonePart);
        }

        return  StringUtils.capitalize(verbPart) +
                StringUtils.capitalize(nonePart);
    }

    public static String prepareContent1(String bindingName) {
        if(bindingName.isEmpty())
            return "";
        else{
            bindingName = bindingName.substring(bindingName.lastIndexOf('.') + 1);
            bindingName = bindingName.replaceAll("_","");
            return bindingName;
        }
    }

    public static String generateLabel(POSTagger posTagger,String content,String viewType) {
        String verbPart = "";
        String nonePart = "";
        verbPart = extractVerbPartOfTagForDialog(posTagger,content,viewType);
        nonePart = extractNonePartOfTagForDialog(posTagger,content,viewType);
        return generateTag(verbPart,nonePart);

    }

    public static String extractNounPart(POSTagger posTagger,String viewType, String content) {
        String nounTag = "";
        nounTag = posTagger.getNoneTagFor(viewType,content);
        return nounTag;
    }

    public static String extractVerbPart(POSTagger posTagger,String viewType, String content) {
        String result = "";
        final int VERB = 1;
        String verbTag = posTagger.getVerbTagFor(viewType,content,VERB);
        if(!verbTag.isEmpty()){
            result = verbTag;
        }
        return result;
    }

    public static String extractVerbPartOfTagForDialog(POSTagger posTagger,String content,String viewType) {
        String result = "";
        final int VERB = 1;
        String verbTag = posTagger.getVerbTagFor(viewType,content,VERB);
        if(!verbTag.isEmpty()){
            result = verbTag;
        }
        return result;
    }

    public static String extractNonePartOfTagForDialog(POSTagger posTagger,String content,String viewType) {
        String noneTag = "";
        if(!content.isEmpty())
            noneTag = posTagger.getNoneTagFor(viewType,content);
        return noneTag;
    }

    private static String contentRefinemnet(String content) {
        String[] wordPatterns = {"Please","Would youe like to", "please","would you like to"};
        if(Pattern.isPartialMatch(wordPatterns,content))
            content = removeWordPatterns(wordPatterns,content);
        return content;
    }

    private static String removeWordPatterns(String[] wordPatterns, String content) {
        for(int index = 0; index < wordPatterns.length; index++)
            if(Utils.isPartialMatchWithPattern(wordPatterns[index],content))
                content = content.replaceAll(wordPatterns[index],"");
        return content;
    }

    private static  String prepareContent(String bindingName) {
        if(bindingName.isEmpty())
            return "";
        else{
            bindingName = bindingName.substring(bindingName.lastIndexOf('.') + 1);
            bindingName = bindingName.substring(bindingName.lastIndexOf("_") + 1);
            return bindingName;
        }
    }

    private static String generatePrefixNameFor(String content){
        String bindingVaribaleName = content;
        bindingVaribaleName = bindingVaribaleName.replace("_", " ");
        bindingVaribaleName = WordUtils.capitalize(bindingVaribaleName);
        return bindingVaribaleName.replace(" ", "");
    }


    private static String generateTagForOpenActivityPattern(MethodDeclaration attachedMethod) {
            return generatePrefixNameFor(ASTUtils.getOpenActivityBy(attachedMethod));
    }

    public static String generateLabelForDialog(POSTagger posTagger,String content){
        final int  VERB = 1;
        String verbTag = posTagger.getVerbTagFor("",contentRefinemnet(content),VERB);
        String noneTag = posTagger.getNoneTagFor("",contentRefinemnet(content));
        return StringUtils.capitalize(verbTag) + StringUtils.capitalize(noneTag);

    }
}
