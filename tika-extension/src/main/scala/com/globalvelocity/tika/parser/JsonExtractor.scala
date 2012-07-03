package com.globalvelocity.tika.parser

import util.parsing.combinator.JavaTokenParsers
import java.io.{InputStreamReader, InputStream}


/**
 * Generic support for Text extraction in a JsonExtractor$ document
 *
 * This will extract all text and numeric values, skipping null and boolean
 * values, and return them as strings. It uses a handler function pattern for
 * each new discovered value, with a fully qualified name. All names have a
 * base name of 'root' to indicate the top level
 */
object JsonExtractor extends JavaTokenParsers {

  /**Base name for top level value */
  val root = "root"


  /**
   * Extracts text data from the JSON document in an input stream.
   *
   * @param input input stream to extract JSON data fro
   * @param handler function to deliver name,value tuple for extracted data
   */
  def extractParseableData(input: InputStream, handler: (String, String) => Unit) {
    JsonExtractor.parseAll(value, new InputStreamReader(input)) match {
      case Success(result, rest) => handleParsedData(result, handler)
      case NoSuccess(msg, rest) =>
        //TODO use standard TIKA logging to log this
        //warn("Invalid JsonExtractor$ input, returning raw string: " + msg)
        handler(root, rest.source.toString)
    }
  }

  /**
   * Represents a JSON Object
   * @return Parsed object as a Map(String->value)
   */
  private def obj: Parser[Map[String, Any]] = "{" ~> repsep(member, ",") <~ "}" ^^ (Map() ++ _)

  /**
   * Represents a JSON array of values
   * @return  Parsed array as a list of values
   */
  private def arr: Parser[List[Any]] = "[" ~> repsep(value, ",") <~ "]"

  /**
   * Represents an object member in a JSON Document
   * @return parsed object member tuple of string,value
   */
  private def member: Parser[(String, Any)] = quotedString ~ ":" ~ value ^^ {
    case name ~ ":" ~ value => (name, value)
  }

  /**
   * Represents a valid quoted string in a JSON document
   *  It will take any value, between double quotes
   * @return Parsed string
   */
  private def quotedString: Parser[String] = ("\"").r ~ ("""(\\"|[^"])*""").r ~ ("\"").r ^^ {
    case _ ~ s ~ _ => s
  }

  /**
   * Represents any valid value in JSON document
   * @return parsed value
   */
  private def value: Parser[Any] = (
    obj | arr |
      quotedString
      | floatingPointNumber ^^ (_.toDouble)
      | "null" ^^ (x => null)
      | "true" ^^ (x => true)
      | "false" ^^ (x => false)
    )

  /**
   * Handles parsed JSON data. by walking the parsed structure, building the
   * hierarchical name, and delivering it to the client
   * @param data parsed JSON data
   * @param handler handler to deliver extracted data to
   * @param name current base name
   */
  private def handleParsedData(data: Any, handler: (String, String) => Unit, name: String = root) {
    data match {
      case obj: Map[_, _] => obj.foreach(_ match {
        case (n, v) => handleParsedData(v, handler, name + "." + n)
      })
      case arr: List[_] =>
        var cnt = 0
        for (v <- arr) {
          handleParsedData(v, handler, name + cnt)
          cnt = cnt + 1
        }
      case v: String => handler(name, v)
      case v: Double => handler(name, v.toString.stripSuffix(".0"))
      case v: Boolean => // Skip boolean values
      case _ => // Skip null values
    }

  }
}
