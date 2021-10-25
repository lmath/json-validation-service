package validator

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchemaFactory
import error.{ErrorReason, JsonNotValidAgainstSchema}
import play.api.libs.json.{JsValue, Json}

object SchemaValidator {

  def validate(schemaToCheck: String, jsonToCheck: JsValue): Either[ErrorReason, String] = {
    val schemaAsJsonNode: JsonNode = JsonLoader.fromString(schemaToCheck)
    val cleanedJson = JsonCleaner.clean(Json.stringify(jsonToCheck))
    val jsonAsJsonNode = JsonLoader.fromString(cleanedJson)

    val schema = JsonSchemaFactory.byDefault().getJsonSchema(schemaAsJsonNode)
    val report: ProcessingReport = schema.validate(jsonAsJsonNode)
    val approximatedClippedMsg = report.toString.take(200)

    report.isSuccess match {
      case true => Right("success")
      case false => Left(JsonNotValidAgainstSchema(approximatedClippedMsg))
    }
  }
}


