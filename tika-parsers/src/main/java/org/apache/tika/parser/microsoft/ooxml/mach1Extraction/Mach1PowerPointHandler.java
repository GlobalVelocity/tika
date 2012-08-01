package org.apache.tika.parser.microsoft.ooxml.mach1Extraction;

import org.apache.tika.metadata.Metadata;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: timpalmer
 * Date: 7/26/12
 * Time: 1:21:16 PM
 * Sax handler to set the mach1 metadata tag if it's present in an XML-based power point presentation.
 */
public class Mach1PowerPointHandler extends DefaultHandler {

    private Metadata metadata;
    private String mach1Attribute = "vt:lpwstr";
    private String markerName = "property";
    private String markerAttribute = "fmtid";
    private String markerAttributeValue = "{D5CDD505-2E9C-101B-9397-08002B2CF9AE}";
    
    private StringBuilder metadataValue = new StringBuilder();

    private Boolean mach1TagFound = false;
    private Boolean withinProperty = false;
    private Boolean withinMach1Tag = false;


    public Mach1PowerPointHandler(Metadata metadata) {
        this.metadata = metadata;
    }

    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        if (name.equals(markerName)) {
            String value = attributes.getValue(markerAttribute);
            if (value.equals(markerAttributeValue)) {
                withinProperty = true;
            }
        }
        else if (withinProperty && name.equals(mach1Attribute)) {
            withinMach1Tag = true;
            mach1TagFound = true;
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals(markerName)) {
            withinProperty = false;
            withinMach1Tag = false;
        }
        else if (qName.equals(mach1Attribute))
            withinMach1Tag = false;
    }

    public void characters(char[] ch, int start, int length) {
        if (withinMach1Tag && ch != null) {
            char[] newCh = Arrays.copyOfRange(ch, start, start+length);
            metadataValue.append(newCh);
        }
    }

    public void endDocument() throws SAXException {
        if (mach1TagFound)
            metadata.set("typedt", metadataValue.toString());
    }

}