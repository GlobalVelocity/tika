package com.globalvelocity.tika.detector

import java.io.InputStream
import org.apache.tika.detect.Detector
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import io.Source

/**
 * Implements a JsonExtractor$$ org.apache.tika.detect.Detector Interface
 */

class JsonDetector extends Detector {
  def detect(input: InputStream, metadata: Metadata) = {

    val encoding = "ISO-8859-2"

    // Skip detection if the Http Content-Type header says its json
    if (metadata.get("Content-Type") == MediaType.application("json").toString)
      MediaType.application("json")
    else {
      val maxDepth = 1024
      input.mark(1)
      val prefix = Source.fromInputStream(input, encoding).slice(0, maxDepth).mkString
      // This will look at the first  'maxDepth' characters looking for a
      //  JsonExtractor$ like structure. This will only detect objects and arrays, not single
      //  simple values.
      """(\{|\[)\s*"\w+"\s*:\s*((true|false|null)|"\w+"|\d+|\[|\{),""".r findPrefixOf (prefix) match {
        case Some(s) =>
          input.reset()
          MediaType.application("json")
        case None =>
          input.reset()
          MediaType.OCTET_STREAM
      }
    }
  }
}
