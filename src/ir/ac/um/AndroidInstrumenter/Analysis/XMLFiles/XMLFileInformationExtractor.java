package ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles;

import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class XMLFileInformationExtractor {
    private String xmlFile;
    //private String stringValuesFile;

    public XMLFileInformationExtractor(String xmlFilePath){
        this.xmlFile = xmlFilePath;
       // this.stringValuesFile = stringValuesFile;
    }

    public void setXmlFile (String xmlFile){
        this.xmlFile = xmlFile;
    }

//    public void setStringValuesFilePath(String valuesFilePath){
//        this.stringValuesFile = stringValuesFile;
//    }
    public  String findViewLabelById(String viewId) {
        return null;
    }
    public Document getXMLDocumentStructure(String filePath){
        Document doc = null;
        try{
            File inputFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

        } catch (Exception e) {
           e.printStackTrace();
        }
        return doc;

    }

    public String extractAttributeValue(Element eElement, String attribute) {
        String widgetLabel = eElement.getAttribute(attribute);
        if (!widgetLabel.isEmpty()) {
            widgetLabel.trim();
//            if (widgetLabel.startsWith("@string")) {
//                StringValueExtractor stringValueExtractor = new StringValueExtractor(",this.stringValuesFile);
//                widgetLabel = widgetLabel.substring(8);
//                widgetLabel = stringValueExtractor.findViewLabelById(widgetLabel);
//            }
        }
        return widgetLabel;
    }

    public String getXmlFilePath(){return this.xmlFile;}
   // public String getStringValuesFilePath(){return this.stringValuesFile;}


}
