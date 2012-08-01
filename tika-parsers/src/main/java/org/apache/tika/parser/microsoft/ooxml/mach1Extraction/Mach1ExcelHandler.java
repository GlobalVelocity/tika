package org.apache.tika.parser.microsoft.ooxml.mach1Extraction;

import org.apache.tika.metadata.Metadata;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Created by IntelliJ IDEA.
 * User: timpalmer
 * Date: 7/26/12
 * Time: 10:08:26 AM
 * Sax handler to set the mach1 metadata tag if it's present in an XML-based excel doc.
 */

public class Mach1ExcelHandler extends DefaultHandler {

    private Metadata metadata;
    private String mach1Attribute = "m1";
    private String markerName = "x:row";
    private String markerAttribute = "r";
    private String markerAttributeValue = "1";


    public Mach1ExcelHandler(Metadata metadata) {
        this.metadata = metadata;
    }

    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        if (name.equals(markerName)) {
            if (attributes.getValue(markerAttribute).equals(markerAttributeValue)) {
                String mach1Value = attributes.getValue(mach1Attribute);
                if (mach1Value != null) {
                    metadata.set("typedt", mach1Value);
                }
            }
        }
    }

}
