package validator

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

object JsonCleaner {

  def clean(json: String): String = {
    val maybeJsObject = Try(Json.parse(json).as[JsObject])
    maybeJsObject match {
      case Success(obj) => cleanJsObject(obj).toString
      case Failure(_) => json  //We can only clean a JsObject anyway
    }
  }

  // with thanks to https://stackoverflow.com/questions/65230467/json-objects-null-filtering-in-scala
  def cleanJsObject(jsObject: JsObject): JsValue = {
    JsObject(jsObject.fields.collect {
      case (s, j: JsObject) =>
        (s, cleanJsObject(j))
      case other if (other._2 != JsNull) =>
        other
    })
  }

}
