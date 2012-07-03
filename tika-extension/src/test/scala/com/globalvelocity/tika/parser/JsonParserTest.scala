package com.globalvelocity.tika.parser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{OneInstancePerTest, Spec}
import java.io.ByteArrayInputStream
import org.easymock.EasyMockSupport
import org.easymock.EasyMock._

@RunWith(classOf[JUnitRunner])
class JsonParserTest extends EasyMockSupport with Spec with OneInstancePerTest with ShouldMatchers {

  val addressBook =
    """
    {
      "firstName": "John",
      "lastName" : "Smith",
      "age"      : 25,
      "minor"    : false,
      "address"  :
        {
          "streetAddress": "21 2nd Street",
          "city"         : "New York",
          "state"        : "NY",
          "postalCode"   : "10021"
        },
      "phoneNumber":
      [
      {
        "type"  : "home",
        "number": "212 555-1234"
      },
      {
        "type"  : "fax",
        "number": "646 555-4567"
      }
      ]
    }
    """

  val addressBookShuffled =
    """
    {
      "phoneNumber":
      [
        {
          "number": "212 555-1234",
          "type"  : "home"

        },
        {
          "type"  : "fax",
          "number": "646 555-4567"
        }
      ],
      "lastName" : "Smith",
      "firstName": "John",
      "age"      : 25,
      "minor"    : false,
      "address"  :
        {
          "postalCode"   : "10021",
          "streetAddress": "21 2nd Street",
          "city"         : "New York",
          "state"        : "NY"

        }
    }
    """

  val addressBookTruncated =
    """
    {
      "firstName": "John",
      "lastName" : "Smith",
      "age"      : 25,
      "minor"    : false,
      "address"  :
        {
          "streetAddress": "21 2nd Street",
          "city"         : "New York",
          "state"        : "NY",
          "postalCode"   : "10021"
        },
      "phoneNumber":
      [
      {
        "type"  : "ho
    """


  def addressBookTest(input: String) {
    val mockHandler = createMock(classOf[(String, String) => Unit])
    mockHandler(JsonExtractor.root + ".firstName", "John")
    expectLastCall().andReturn(Unit).once()
    mockHandler(JsonExtractor.root + ".lastName", "Smith")
    expectLastCall().andReturn(Unit).once()
    mockHandler(JsonExtractor.root + ".age", "25")
    expectLastCall().andReturn(Unit).once()
    mockHandler(JsonExtractor.root + ".address.streetAddress", "21 2nd Street")
    expectLastCall().andReturn(Unit).once()
    mockHandler(JsonExtractor.root + ".address.city", "New York")
    expectLastCall().andReturn(Unit).once()
    mockHandler(JsonExtractor.root + ".address.state", "NY")
    expectLastCall().andReturn(Unit).once()
    mockHandler(JsonExtractor.root + ".address.postalCode", "10021")
    expectLastCall().andReturn(Unit).once()
    mockHandler(JsonExtractor.root + ".phoneNumber0.type", "home")
    expectLastCall().andReturn(Unit).once()
    mockHandler(JsonExtractor.root + ".phoneNumber0.number", "212 555-1234")
    expectLastCall().andReturn(Unit).once()
    mockHandler(JsonExtractor.root + ".phoneNumber1.type", "fax")
    expectLastCall().andReturn(Unit).once()
    mockHandler(JsonExtractor.root + ".phoneNumber1.number", "646 555-4567")
    expectLastCall().andReturn(Unit).once()
    replay(mockHandler)
    JsonExtractor.extractParseableData(new ByteArrayInputStream(input.getBytes), mockHandler)
    verify(mockHandler)

  }

  describe("JsonExtractor$") {
    it("should extract the parseable data from a valid JsonExtractor$ document") {
      addressBookTest(addressBook)
      addressBookTest(addressBookShuffled)
    }

    it("should extract a raw string from an invalid JsonExtractor$ document") {
      val mockHandler = createMock(classOf[(String, String) => Unit])
      mockHandler(JsonExtractor.root, addressBookTruncated)
      expectLastCall().andReturn(Unit).once()

      replayAll()

      JsonExtractor.extractParseableData(new ByteArrayInputStream(addressBookTruncated.getBytes), mockHandler)

      verifyAll()
    }
  }

}
