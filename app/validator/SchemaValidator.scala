package validator

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.examples.Utils
import com.github.fge.jsonschema.main.JsonSchemaFactory
import play.api.libs.json.{JsValue, Json}

object SchemaValidator {

  val jsonSchemaFactory = JsonSchemaFactory.byDefault()
  def validate(schemaToCheck: String, jsonToCheck: String): Boolean = {
    val schemaAsJsonNode = JsonLoader.fromString(schemaToCheck)
    val cleanedJson = JsonCleaner.clean(jsonToCheck)
    println(cleanedJson)
    val jsonAsJsonNode = JsonLoader.fromString(cleanedJson)

    val schema = jsonSchemaFactory.getJsonSchema(schemaAsJsonNode)
    val report = schema.validate(jsonAsJsonNode)
    println(report)
    report.isSuccess
//    schema.toString
  }

}


