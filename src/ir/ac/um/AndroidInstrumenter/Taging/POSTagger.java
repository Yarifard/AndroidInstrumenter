package ir.ac.um.AndroidInstrumenter.Taging;

import ir.ac.um.AndroidInstrumenter.Utils.Utils;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class POSTagger {
    private final static int VERB = 1;
    private final static int NONE = 0;
    private  File dictionaryFilePath;
    private  File posMaxentModelFilePath;
    private  InputStream tokenModelIn;
    private  InputStream posModelIn;
    private  TokenizerModel tokenModel;
    private  Tokenizer tokenizer;
    private  POSModel posModel;
    private  POSTaggerME posTagger;

    public POSTagger(){
        dictionaryFilePath = new File(".\\Data\\en-token.bin");
        posMaxentModelFilePath = new File(".\\Data\\en-pos-perceptron.bin");
        try{
           // Utils.showMessage(dictionaryFilePath.getCanonicalPath());
            tokenModelIn = new FileInputStream(dictionaryFilePath);
            tokenModel = new TokenizerModel(tokenModelIn);
            tokenizer = new TokenizerME(tokenModel);

            posModelIn = new FileInputStream(posMaxentModelFilePath);
            posModel = new POSModel(posModelIn);
            posTagger = new POSTaggerME(posModel);
        } catch(IOException e){
            Utils.showMessage("It is not possible to read from data files");
        }
    }

    public String getVerbTagFor(String viewType,String context, int wordType){
        String result = "";
        context = prepareContent(context, wordType);
        String widgetLabel = separateWords(context);
        while(true){
            String[] widgetLabelTokens = null;
            String[] tags = null;

            if(!widgetLabel.isEmpty()) {
                widgetLabelTokens = tokenizer.tokenize(widgetLabel);
                tags = posTagger.tag(widgetLabelTokens);
            }

            int index = getSuitableWord(tags,widgetLabelTokens,viewType,wordType);

            if(index != -1){
                result = StringUtils.capitalize(widgetLabelTokens[index]);
                break;
            }
            if(widgetLabelTokens.length > 4)
               widgetLabel = widgetLabel.replaceAll(" " + widgetLabelTokens[3],"");
            else{
                if(widgetLabelTokens.length > 2)
                   result = widgetLabelTokens[3];
                else
                    result = "";
                break;
            }
        }
        return result;
    }

    public String getNoneTagFor(String viewType, String context){

        context = prepareContent(context, NONE);
        String widgetLabel = separateWords(context);
        String[] widgetLabelTokens = null;
        String[] tags = null;
        String result = "";

        if(!widgetLabel.isEmpty()) {
            widgetLabelTokens = tokenizer.tokenize(widgetLabel);
            tags = posTagger.tag(widgetLabelTokens);
        }

        int index = getSuitableWord(tags,widgetLabelTokens,viewType,NONE);
           if(index != -1){
                      if(index >= 1 && tags[index -1].contentEquals("JJ")  )
                         result = StringUtils.capitalize(widgetLabelTokens[index -1]) +
                                  StringUtils.capitalize(widgetLabelTokens[index]);
                      else if (index +1 <tags.length && tags[index + 1].contentEquals("NN"))
                          result =  StringUtils.capitalize(widgetLabelTokens[index]) +
                                    StringUtils.capitalize(widgetLabelTokens[index+1]);
                      else
                          result = StringUtils.capitalize(widgetLabelTokens[index]);
           }
           return result;
    }

    @NotNull
    private String prepareContent(String context, int wordType) {
        if(context.startsWith("R.id."))
            context = context.substring(context.lastIndexOf('.')+1);

        if(context.contains("_"))
            context = context.substring(context.lastIndexOf('_') + 1);
            //context = context.replaceAll("_"," ");

        //if(wordType == VERB)
            context = "i want to " + context;

        return context;
    }

    public int getSuitableWord(String[] tags, String[] tokens, String viewType,int wordType) {
        int result = -1;
        boolean flag = false;
        for(int i = 3; i < tags.length; i++){
            switch (wordType) {
                case VERB:
                    if (tags[i].contentEquals("VB") && isSuitable(tokens[i],viewType)) {
                    result = i;
                    flag = true;
                }
                break;
                case NONE:
                    if (tags[i].contentEquals("NN")||tags[i].contentEquals("NNS")) {
                        result = i;
                        flag = true;
                    }
                    break;
            }
            if(flag)
                break;
        }
         return result;
    }

    private boolean isSuitable(String token,String viewType) {
        boolean result = true;
        if(viewType.toLowerCase().contains(token.toLowerCase()))
            result = false;
        return result;
    }

    private static String separateWords(String content){
       String result = new String();
       int j = 0;

       for(int i = 0; i < content.length(); i++) {
            if (Character.isLowerCase(content.charAt(i))) {
                result += Character.toString(content.charAt(i));
            } else {
                result += Character.toString(' ');
                result += Character.toLowerCase(content.charAt(i));
            }
        }
        return result.toString();
    }

    public String generateLabelforDialog(String content){
        String result = "";
        String verb = "", none = "";
        String[] tokens = null;
        String[] tags = null;
        if(content != "") {
            tokens = tokenizer.tokenize(content);
            tags = posTagger.tag(tokens);
        }

        // I add the follwoing line to compatibile with instrumenter
        purify(tags,tokens);

        for(int i = 0; i < tags.length; i++)
            if(tags[i].contentEquals("VB"))
                verb = tokens[i];
            else if(tags[i].contentEquals("NN") || tags[i].contentEquals("NNS"))
                none = tokens[i];
        return StringUtils.capitalize(verb) + StringUtils.capitalize(none);
    }

    private void purify(String[] tags,String[] token){
        for(int index = 0; index < tags.length; index++){
            if(tags[index].contentEquals("``"))
                if(token[index].contentEquals("scan") || token[index].contentEquals("Scan"))
                    tags[index]="VB";
        }
    }

    private String purify(String content) {
        String[] tokens = null;
        String[] tags = null;

        if(content.contains("Please") || content.contains("please")){
            content = content.replace("Please","I want to");
            content = content.replace("please","I want to");
        }
        if(content.contains("Would you like") || content.contains("would you like ")){
            content = content.replace("Would you like to","I want to");
            content = content.replace("would you like to","I want to");
        }

        content = "I want to " + content;
        return content;
    }

}




