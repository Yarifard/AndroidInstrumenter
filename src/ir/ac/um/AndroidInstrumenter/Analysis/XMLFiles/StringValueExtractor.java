package ir.ac.um.AndroidInstrumenter.Analysis.XMLFiles;

import ir.ac.um.AndroidInstrumenter.Analysis.Project.ProjectInformation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.w3c.dom.Node.ELEMENT_NODE;

public class StringValueExtractor extends XMLFileInformationExtractor {

    public StringValueExtractor(ProjectInformation projectInformation,String stringValueFile){
        super(projectInformation.getValuesDirectory().getCanonicalPath() + "/" + stringValueFile + ".xml");
    }

    public  String findViewLabelById(String stringId) {
        String searchItem = "";
        try {
            Document doc = getXMLDocumentStructure(super.getXmlFilePath());
            if(stringId.startsWith("@string/"))
                searchItem = stringId.substring(stringId.lastIndexOf('/')+1);
            else
                searchItem = stringId.substring(stringId.lastIndexOf(".")+1);
            NodeList nList = doc.getElementsByTagName("string");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if(eElement.hasAttribute("name")){
                        if (eElement.getAttribute("name").contentEquals(searchItem))
                            return eElement.getTextContent();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
