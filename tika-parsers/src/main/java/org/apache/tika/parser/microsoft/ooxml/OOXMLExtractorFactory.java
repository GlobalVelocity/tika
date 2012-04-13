/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.microsoft.ooxml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLTextExtractor;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.extractor.XSSFEventBasedExcelExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.CloseShieldInputStream;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.EndDocumentShieldingContentHandler;
import org.apache.xmlbeans.XmlException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Figures out the correct {@link OOXMLExtractor} for the supplied document and
 * returns it.
 */
public class OOXMLExtractorFactory {

    public static final String XHTML = "http://www.w3.org/1999/xhtml";

    /**
     * The newline character that gets inserted after block elements.
     */
    private static final char[] NL = new char[] { '\n' };
    
    public static void parse(
            InputStream stream, ContentHandler baseHandler,
            Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {
        Locale locale = context.get(Locale.class, Locale.getDefault());
        ExtractorFactory.setThreadPrefersEventExtractors(true);
        
        try {
            OOXMLExtractor extractor;
            
            POIXMLTextExtractor poiExtractor;
            TikaInputStream tis = TikaInputStream.cast(stream);
            if (tis != null && tis.getOpenContainer() instanceof OPCPackage) {
                poiExtractor = ExtractorFactory.createExtractor(
                        (OPCPackage) tis.getOpenContainer());
            } else if (tis != null && tis.hasFile()) {
                poiExtractor = (POIXMLTextExtractor)
                        ExtractorFactory.createExtractor(tis.getFile());
            } else {
                InputStream shield = new CloseShieldInputStream(stream);
                poiExtractor = (POIXMLTextExtractor)
                        ExtractorFactory.createExtractor(shield);
            }

            POIXMLDocument document = poiExtractor.getDocument();
            if (poiExtractor instanceof XSSFEventBasedExcelExtractor) {
               extractor = new XSSFExcelExtractorDecorator(
                   context, (XSSFEventBasedExcelExtractor)poiExtractor, locale);
            } else if (document == null) {
               throw new TikaException(
                     "Expecting UserModel based POI OOXML extractor with a document, but none found. " +
                     "The extractor returned was a " + poiExtractor
               );
            } else if (document instanceof XMLSlideShow) {
                extractor = new XSLFPowerPointExtractorDecorator(
                        context, (XSLFPowerPointExtractor) poiExtractor);
            } else if (document instanceof XWPFDocument) {
                extractor = new XWPFWordExtractorDecorator(
                        context, (XWPFWordExtractor) poiExtractor);
            } else {
                extractor = new POIXMLTextExtractorDecorator(context, poiExtractor);
            }
            
            // We need to get the content first, but not end 
            //  the document just yet
            EndDocumentShieldingContentHandler handler = 
               new EndDocumentShieldingContentHandler(baseHandler);
            extractor.getXHTML(handler, metadata, context);

            // Now we can get the metadata
            extractor.getMetadataExtractor().extract(metadata);

            // Now we output the metadata SAX messages
            for (String name : metadata.names()) {
                for (String value : metadata.getValues(name)) {
                    // Putting null values into attributes causes problems, but is
                    // allowed by Metadata, so guard against that.
                    if (value != null) {
                        AttributesImpl attributes = new AttributesImpl();
                        attributes.addAttribute("", "name", "name", "CDATA", name);
                        attributes.addAttribute("", "content", "content", "CDATA", value);
                        handler.startElement(XHTML, "meta", "meta", attributes);
                        handler.endElement(XHTML, "meta", "meta");
                        handler.ignorableWhitespace(NL, 0, NL.length);
                    }
                }
            }
            
            // Then finish up
            handler.reallyEndDocument();
        } catch (IllegalArgumentException e) {
            if (e.getMessage().startsWith("No supported documents found")) {
                throw new TikaException(
                        "TIKA-418: RuntimeException while getting content"
                        + " for thmx and xps file types", e);
            } else {
                throw new TikaException("Error creating OOXML extractor", e);
            }
        } catch (InvalidFormatException e) {
            throw new TikaException("Error creating OOXML extractor", e);
        } catch (OpenXML4JException e) {
            throw new TikaException("Error creating OOXML extractor", e);
        } catch (XmlException e) {
            throw new TikaException("Error creating OOXML extractor", e);

        }
    }

}
