package controllers.model

import play.api.libs.json.Json

case class ResponseBody(action: String, id: String, status: String, message: Option[String] = None) {
  def asJson = Json.toJson(this)(ResponseBody.responseFormatter)
}
object ResponseBody {
  implicit val responseFormatter = Json.format[ResponseBody]
  def successfulUpload(id: String) = ResponseBody(
    action = "uploadSchema",
    id = id,
    status = "success"
  )


  def failedUpload(id: String, message: String) = ResponseBody(
    action = "uploadSchema",
    id = id,
    status = "failure",
    message = Some(message)
  )


  def failedGet(id: String, message: String) = ResponseBody(
    action = "getSchema",
    id = id,
    status = "failure",
    message = Some(message)
  )


  def successfulValidate(id: String) = ResponseBody(
    action = "validateSchema",
    id = id,
    status = "success"
  )


  def failedValidate(id: String, message: String) = ResponseBody(
    action = "validateSchema",
    id = id,
    status = "failure",
    message = Some(message)
  )

}