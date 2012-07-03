package com.globalvelocity.tika.parser

import org.apache.tika.mime.MediaType
import org.apache.tika.parser.{AbstractParser, ParseContext}
import scala.collection.JavaConverters._
import java.io._
import org.apache.tika.metadata.{HttpHeaders, Metadata, TikaMetadataKeys}
import org.xml.sax.ContentHandler
import org.apache.tika.sax.XHTMLContentHandler
import org.apache.tika.exception.TikaException
import org.apache.tika.extractor.{ParsingEmbeddedDocumentExtractor, EmbeddedDocumentExtractor}


/**
 * Implements com.globalvelocity.tika.parser.JsonParser service
 *
 * This parser handles JSON documents and treats them like package container, where
 * each text value is an embedded resource. This allows proper parsing of HTML or
 * XML strings in a JSON object.
 */
class JsonParser extends AbstractParser {

  /**
   * Set of supported mime types
   */
  val supportedTypes = Set(MediaType.application("json"))

  /**
   * Default character encoding
   */
  val encoding = "ISO-8859-2"


  /**
   * Returns the set of supported mime types
   *
   * @param context parse context
   **/
  override def getSupportedTypes(context: ParseContext) = supportedTypes.asJava


  /**
   * Parses the input stream and delivers parsed data to the SAX content handler
   *
   * @param input input stream to parse
   * @param handler handler for the XHTML SAX events (output)
   * @param metadata document metadata (input and output)
   * @param context parse context
   */
  override def parse(input: InputStream, handler: ContentHandler, metadata: Metadata, context: ParseContext) {

    // Try to get the EmbeddedDocumentExtractor from the context
    val extractor = {
      val ex = context.get(classOf[EmbeddedDocumentExtractor])
      if (ex == null)
        new ParsingEmbeddedDocumentExtractor(context)
      else
        ex
    }

    try {

      val xhtml: XHTMLContentHandler = new XHTMLContentHandler(handler, metadata)
      xhtml.startDocument()
      xhtml.startElement("p")

      JsonExtractor.extractParseableData(input, {
        (name, value) =>
          if (name == "root")
            xhtml.characters(value)
          else {
            val entryMetaData = new Metadata()
            entryMetaData.set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN.toString)
            if (name != null && name.length() > 0)
              entryMetaData.set(TikaMetadataKeys.RESOURCE_NAME_KEY, name)
            // The following is commented out to FORCE embedded data parsing. Since this parser
            //  treats JSON documents as a PKG container, it should always parse the sub-items
            //if (extractor.shouldParseEmbedded(entryMetaData))
            extractor.parseEmbedded(new ByteArrayInputStream(value.getBytes), xhtml, entryMetaData, true)
          }
      })
      xhtml.endElement("p")
      xhtml.endDocument()
    }
    catch {
      case e: UnsupportedEncodingException => {
        throw new TikaException("Unsupported text encoding: " + e)
      }
    }
  }
}
