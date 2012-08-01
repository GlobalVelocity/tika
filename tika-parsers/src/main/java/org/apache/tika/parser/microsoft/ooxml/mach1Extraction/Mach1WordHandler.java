package org.apache.tika.parser.microsoft.ooxml.mach1Extraction;

import org.apache.tika.metadata.Metadata;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Created by IntelliJ IDEA.
 * User: timpalmer
 * Date: 7/26/12
 * Time: 1:38:34 PM
 * Sax handler to set the mach1 metadata tag if it's present in an XML-based word doc.
 */
public class Mach1WordHandler extends DefaultHandler {

    private Metadata metadata;
    private String mach1Attribute = "typedt";
    private String markerName = "w:styles";


    public Mach1WordHandler(Metadata metadata) {
        this.metadata = metadata;
    }

    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        if (name.equals(markerName)) {
            String mach1Value = attributes.getValue(mach1Attribute);
            if (mach1Value != null) {
                metadata.set("typedt", mach1Value);
            }
        }
    }

}