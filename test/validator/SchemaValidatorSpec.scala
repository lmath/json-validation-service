package validator

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class SchemaValidatorSpec extends AnyFlatSpec with Matchers {
  val testSchema =
    """
      |{
      |  "$schema": "http://json-schema.org/draft-04/schema#",
      |  "type": "object",
      |  "properties": {
      |    "source": {
      |      "type": "string"
      |    },
      |    "destination": {
      |      "type": "string"
      |    },
      |    "timeout": {
      |      "type": "integer",
      |      "minimum": 0,
      |      "maximum": 32767
      |    },
      |    "chunks": {
      |      "type": "object",
      |      "properties": {
      |        "size": {
      |          "type": "integer"
      |        },
      |        "number": {
      |          "type": "integer"
      |        }
      |      },
      |      "required": ["size"]
      |    }
      |  },
      |  "required": ["source", "destination"]
      |}
    """.stripMargin


  it should "clean json with nulls and still be valid against the schema" in {

    val input =
      """
        |{
        |  "source": "/home/alice/image.iso",
        |  "destination": "/mnt/storage",
        |  "timeout": null,
        |  "chunks": {
        |    "size": 1024,
        |    "number": null
        |  }
        |}
      """.stripMargin

    SchemaValidator.validate(testSchema, Json.parse(input)).isRight shouldBe true
  }

  it should "return true for a cleaned json that is valid against the schema" in {
    val inputCleaned =
      """
        |{
        |  "source": "/home/alice/image.iso",
        |  "destination": "/mnt/storage",
        |  "chunks": {
        |    "size": 1024
        |  }
        |}
      """.stripMargin

    SchemaValidator.validate(testSchema, Json.parse(inputCleaned)).isRight shouldBe true
  }

  it should "return false for a json not valid against the schema" in {

    val input =
      """
        |{
        |  "source": "/home/alice/image.iso",
        |  "timeout": null,
        |  "chunks": {
        |    "size": 1024,
        |    "number": null
        |  }
        |}
      """.stripMargin

    SchemaValidator.validate(testSchema, Json.parse(input)).isLeft shouldBe true
  }

}
