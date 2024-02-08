package ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles;

import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import ir.ac.um.AndroidInstrumenter.Utils.Utils;
import ir.ac.um.AndroidInstrumenter.widget.Widget;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import static org.w3c.dom.Node.ELEMENT_NODE;

public class LayoutInformationExtractor extends XMLFileInformationExtractor {

    private ProjectInformation projectInformation;

    public LayoutInformationExtractor(ProjectInformation projectInformation , String layoutFilePath){
        super(projectInformation.getLayoutsDirectory().getCanonicalPath() + "/" + layoutFilePath + ".xml");
        this.projectInformation = projectInformation;
    }

    public String findViewLabelById(String viewId) {
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            String searchItem = viewId.substring(viewId.lastIndexOf('.') + 1);
            NodeList nList = doc.getElementsByTagName("*");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (eElement.hasAttribute("android:id")) {
                        if (eElement.getAttribute("android:id").contentEquals("@+id/" + searchItem)) {
                            if (eElement.hasAttribute("android:text"))
                                return getAttributeValue(eElement,"android:text");
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

    public String findViewContentDesciptionById(String viewId) {
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            String searchItem = viewId.substring(viewId.lastIndexOf('.') + 1);
            NodeList nList = doc.getElementsByTagName("*");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (eElement.hasAttribute("android:id")) {
                        if (eElement.getAttribute("android:id").contentEquals("@+id/" + searchItem)) {
                            if (eElement.hasAttribute("android:contentDescription")){
                                if (eElement.hasAttribute("android:contentDescription"))
                                    return preprocess(extractAttributeValue(eElement,"android:contentDescription"));
                            }
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


    public  String findViewTypeById(String viewId) {
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            String searchItem = viewId.substring(viewId.lastIndexOf('.') + 1);
            NodeList nList = doc.getElementsByTagName("*");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (eElement.hasAttribute("android:id")) {
                        if (eElement.getAttribute("android:id").contentEquals("@+id/" + searchItem))
                            return eElement.getTagName();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void extractOtherWidgetDescriptorsById(Widget widget, String widgetId) {
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            String searchItem = widgetId.substring(widgetId.lastIndexOf('.') + 1);
            NodeList nList = doc.getElementsByTagName("*");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (eElement.hasAttribute("android:id"))
                        if (eElement.getAttribute("android:id").contentEquals("@+id/" + searchItem)) {
                            if (eElement.hasAttribute("android:text"))
                                widget.setWidgetLabelDescriptor(extractAttributeValue(eElement,"android:text"));
                            if(eElement.hasAttribute("android:tag"))
                                widget.setWidgetTagValueDescriptor(extractAttributeValue(eElement,"android:tag"));
                            if(eElement.hasAttribute("android:hint"))
                                widget.setWidgetHintDescriptor(extractAttributeValue(eElement,"android:hint"));
                            if(eElement.hasAttribute("android:contentDescription"))
                                widget.setWidgetContentDescription(extractAttributeValue(eElement,"android:contentDescription"));
                        }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean hasIncludedLayouts(){
        boolean flag = false;
        if(hasTagName("include"))
            flag = true;
        return flag;
    }

    public List<String> getIncludedLayout(){
        List<String> includedLayout = new ArrayList<>();
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            NodeList nList = doc.getElementsByTagName("*");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeName().contentEquals("include")) {
                    if (nNode.getNodeType() == ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        if (eElement.hasAttribute("layout")){
                            String attributeValue = eElement.getAttribute("layout");
                            String layoutName = attributeValue.substring(attributeValue.lastIndexOf("/") + 1 );
                            includedLayout.add(layoutName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return includedLayout;
    }

    public boolean hasFragmentLayouts(){
        boolean flag = false;
        if(hasTagName("fragment"))
            flag = true;
        return flag;
    }

    public List<Widget> getGUIWidgetlist(){
        List<Widget> GUIWidgetElements = new ArrayList<>();
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            NodeList nList = doc.getElementsByTagName("*");
            String attributeValue ="";
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                String tagName = nNode.getNodeName();

                if(tagName.contains("."))
                    tagName = tagName.substring(tagName.lastIndexOf('.') + 1);
                if(Utils.isWidget(tagName)) {
                    Widget widget = new Widget();
                    widget.setWidgetType(tagName);
                    if (nNode.getNodeType() == ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        if (eElement.hasAttribute("android:text"))
                            widget.setWidgetLabelDescriptor(getAttributeValue(eElement,"android:text"));
                        if (eElement.hasAttribute("android:tag"))
                            widget.setWidgetTagValueDescriptor(getAttributeValue(eElement,"android:tag"));
                        if (eElement.hasAttribute("android:hint"))
                            widget.setWidgetHintDescriptor(getAttributeValue(eElement,"android:hint"));
                        if (eElement.hasAttribute("android:contentDescription"))
                            widget.setWidgetContentDescription(getAttributeValue(eElement,"android:contentDescription"));
                        if (eElement.hasAttribute("android:id")){
                            attributeValue = eElement.getAttribute("android:id");
                            if(!attributeValue.isEmpty()){
                                String value = attributeValue.substring(attributeValue.lastIndexOf("/") + 1);
                                if(attributeValue.startsWith("@android:"))
                                    attributeValue ="android.R.id." + value;
                                else
                                    attributeValue = "R.id." + value;
                                widget.setWidgetIdDescriptor(attributeValue);
                                GUIWidgetElements.add(widget);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return GUIWidgetElements;
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

    public boolean hasTagName(String targetTagName){
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            NodeList nList = doc.getElementsByTagName("*");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeName().contentEquals(targetTagName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public void setXmlFile(String xmlFile){
        super.setXmlFile(projectInformation.getLayoutsDirectory().getCanonicalPath() +"/" + xmlFile + ".xml");
    }

    private String getDialogTitleValue() {
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            NodeList nList = doc.getElementsByTagName("*");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                String tagName = nNode.getNodeName();
                if(tagName.contains("."))
                    tagName = tagName.substring(tagName.lastIndexOf('.') + 1);
                if(tagName.contentEquals("TextView"))
                    return getAttributeValue((Element) nNode,"android:text");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String preprocess(String attributeValue){
        String result = attributeValue;
        if(attributeValue.startsWith("@string/")){
            StringValueExtractor stringValueExtractor = new StringValueExtractor(projectInformation,"strings");
            result = stringValueExtractor.findViewLabelById(attributeValue);
        }
        return result;
    }

    private String getDialogTitleIdValue() {
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            NodeList nList = doc.getElementsByTagName("*");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                String tagName = nNode.getNodeName();
                if(tagName.contains("."))
                    tagName = tagName.substring(tagName.lastIndexOf('.') + 1);
                if(tagName.contentEquals("TextView"))
                    return extractAttributeValue((Element) nNode,"android:id");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getDialogMsgValue() {
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            NodeList nList = doc.getElementsByTagName("*");
            for (int index = 0; index < nList.getLength(); index++) {
                Node nNode = nList.item(index);
                String tagName = nNode.getNodeName();
                if(tagName.contains("."))
                    tagName = tagName.substring(tagName.lastIndexOf('.') + 1);
                if(tagName.contentEquals("TextView")){
                   nNode = nList.item(index++);
                   return getAttributeValue((Element) nNode,"android:text");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String extractDialogTitle() {
        if(!hasTagName("TextView"))
            return "";
        return getDialogTitleValue();
    }

    public String extractDialogTitleId(){
        if(!hasTagName("TextView"))
            return "";
        return getDialogTitleId();
    }

    private String getDialogTitleId() {
        if(!hasTagName("TextView"))
            return "";
        return getDialogTitleIdValue();
    }

    public String extractDialogMsg(){
        if(!hasTagName("TextView"))
            return "";
        return getDialogMsgValue();
    }

    public String findViewContext() {
        return  getDialogTitleValue();
    }


}