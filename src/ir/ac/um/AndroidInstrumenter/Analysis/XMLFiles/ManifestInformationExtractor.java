package ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles;


import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import ir.ac.um.AndroidInstrumenter.Utils.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import static org.w3c.dom.Node.ELEMENT_NODE;

public class ManifestInformationExtractor extends XMLFileInformationExtractor {
    private ProjectInformation projectInformation;
    public ManifestInformationExtractor(ProjectInformation projectInformation){
        super(projectInformation.getManifestFile().getCanonicalPath());
        this.projectInformation = projectInformation;
        Utils.showMessage("I'm in ManifestInformationExtractor:Constructor-->Starting");
        Utils.showMessage("I'm in ManifestInformationExtractor:Constructor-->Ending");

    }

    public String getLuncherActivityClassName() {
        Utils.showMessage("I'm in ManifestInformationExtractor:getLauncherActivityClassName-->Starting");
        String mainActivityName = "";
        Document doc = getXMLDocumentStructure(getXmlFilePath());
        NodeList nList = doc.getElementsByTagName("activity");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                if (isLuncherActivity(eElement)) {
                    if (eElement.hasAttribute("android:name")){
                       // mainActivityName = eElement.getAttribute("android:name");
                        mainActivityName = eElement.getAttribute("android:name");
                        mainActivityName = mainActivityName.substring(mainActivityName.lastIndexOf('.')+1);
                    }

                }
            }
        }

        Utils.showMessage("I'm in ManifestInformationExtractor:getLauncherActivityClassName-->End");
        return mainActivityName;
    }

    private boolean isLuncherActivity(Element eElement) {
        boolean result = false;
        NodeList childNodes = eElement.getChildNodes();
        for (int temp = 0; temp < childNodes.getLength(); temp++) {
            Node nNode = childNodes.item(temp);
            if (nNode.getNodeType() == ELEMENT_NODE) {
                Element element = (Element) nNode;
                if (element.getTagName().contentEquals("intent-filter")) {
                    NodeList children = element.getChildNodes();
                    for (int item = 0; item < children.getLength(); item++) {
                        Node child = children.item(item);
                        if (child.getNodeType() == ELEMENT_NODE) {
                            Element childElement = (Element) child;
                            if (childElement.getTagName().contentEquals("category"))
                                if (childElement.hasAttribute("android:name")) {
                                    String attribute = childElement.getAttribute("android:name");
                                    if (attribute.contains("LAUNCHER")) {
                                        result = true;
                                        break;
                                    }
                                }
                        }
                    }
                }

            }
        }
        return result;
    }

    public List<String> getActivitiesList(){
        List<String> appActivitiesList = new ArrayList<>();
        String label = "";
        Document doc = getXMLDocumentStructure(getXmlFilePath());
        NodeList nList = doc.getElementsByTagName("activity");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                if (eElement.hasAttribute("android:name")){
                    label = eElement.getAttribute("android:name");
                    if(!label.startsWith("android."))
                        if(label.contains("."))
                            appActivitiesList.add(label.substring(label.lastIndexOf('.')+1));
                        else
                            appActivitiesList.add(label);
                }

                }
            }

        return  appActivitiesList;
    }

    public String getAppPackageTitle(){
        Utils.showMessage("I'm in ManifestInformationExtractor:getLauncherActivityClassName-->Starting");
        String packageName = "";
        Document doc = getXMLDocumentStructure(getXmlFilePath());
        NodeList nList = doc.getElementsByTagName("*");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                if (eElement.hasAttribute("package")){
                    packageName = getAttributeValue(eElement,"package");
                    break;
                }
            }
        }

        Utils.showMessage("I'm in ManifestInformationExtractor:getLauncherActivityClassName-->End");
        return packageName;
    }

    private String getAttributeValue(Element eElement, String targetAttribute) {
        String targetAttributeValue = extractAttributeValue(eElement, targetAttribute);
        if(!targetAttributeValue.isEmpty()){
            targetAttributeValue.trim();
            if (targetAttributeValue.startsWith("@string")) {
                StringValueExtractor stringValueExtractor = new StringValueExtractor(projectInformation,"strings");
                targetAttributeValue = targetAttributeValue.substring(8);
                targetAttributeValue = stringValueExtractor.findViewLabelById(targetAttributeValue);
            }
        }
        return targetAttributeValue;
    }

}
