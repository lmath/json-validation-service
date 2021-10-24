package es

import play.api.libs.json.Json

case class Schema(schema: String)
case class FoundRecord(_id: String, _source: Schema)

object Schema {
  implicit val schemaFormatter = Json.format[Schema]
  implicit val jsonFormatter = Json.format[FoundRecord]
}