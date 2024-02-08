package ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles;

import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.w3c.dom.Node.ELEMENT_NODE;

public class MenuInformationExtractor extends XMLFileInformationExtractor {
    private ProjectInformation projectInformation;

    public MenuInformationExtractor(ProjectInformation projectInformation, String menuXmlFilePath) {
        super(projectInformation.getMenusDirectory().getCanonicalPath() + "/" + menuXmlFilePath + ".xml");
        this.projectInformation = projectInformation;

    }

    public String findViewLabelById(String viewId) {
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            String searchItem = viewId.substring(viewId.lastIndexOf('.') + 1);
            //searchItem = "@+id/" + searchItem;
            NodeList nList = doc.getElementsByTagName("item");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (eElement.hasAttribute("android:id")) {
                        if (eElement.getAttribute("android:id").contentEquals("@+id/" + searchItem)) {
                            if (eElement.hasAttribute("android:title"))
                                return getAttributeValue(eElement, "android:title");
                            else
                                return "";
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

     private String getAttributeValue(Element eElement, String targetAttribute) {
        String targetAttributeValue = extractAttributeValue(eElement, targetAttribute);
        if (!targetAttributeValue.isEmpty()) {
            targetAttributeValue.trim();
            if (targetAttributeValue.startsWith("@string")) {
                StringValueExtractor stringValueExtractor = new StringValueExtractor(projectInformation, "strings");
                targetAttributeValue = targetAttributeValue.substring(8);
                targetAttributeValue = stringValueExtractor.findViewLabelById(targetAttributeValue);
            }
        }
        return targetAttributeValue;
    }

    public boolean isExistInMenuLayout(String menuItemId) {
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            String searchItem = menuItemId.substring(menuItemId.lastIndexOf('.') + 1);
            //searchItem = "@+id/" + searchItem;
            NodeList nList = doc.getElementsByTagName("item");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (eElement.hasAttribute("android:id")) {
                        if (eElement.getAttribute("android:id").contentEquals("@+id/" + searchItem)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isExistMenuItem(String menuId) {
        try {
            String tagValue = "";
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            String searchItem = menuId.substring(menuId.lastIndexOf('.') + 1);
            //searchItem = "@+id/" + searchItem;
            NodeList nList = doc.getElementsByTagName("item");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if(eElement.hasAttribute("android:id")){
                        if(eElement.getAttribute("android:id").contentEquals("@+id/" + searchItem)) {
                            return true;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isOptionMenu(String viewId) {
        try {
            String tagValue = "";
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            String searchItem = viewId.substring(viewId.lastIndexOf('.') + 1);
            //searchItem = "@+id/" + searchItem;
            NodeList nList = doc.getElementsByTagName("item");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if(eElement.hasAttribute("android:id")){
                        if(eElement.getAttribute("android:id").contentEquals("@+id/" + searchItem)) {
                            if(eElement.hasAttribute("app:showAsAction"))
                                tagValue = getAttributeValue(eElement,"app:showAsAction");
                        }
                    }
                }
            }
            if(tagValue.contentEquals("never"))
                return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
