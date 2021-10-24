package controllers

import java.util.UUID

import es.SchemaIndex
import javax.inject._
import play.api._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import validator.SchemaValidator

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import ResponseBody._

@Singleton
class SchemaController @Inject()(val cc: ControllerComponents, schemaIndex: SchemaIndex)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def get(id: String) = Action {
    schemaIndex.find(id) match {
      case Right(schema) => Ok(schema)
      case Left(msg) => BadRequest(asJson(failedGet(id, msg)))
    }
  }

  def upload(id: String) = Action { implicit request: Request[AnyContent] =>
    val uploadOrError: Either[String, String] = for {
      validJson <- maybeJson(request)
      idAfterUpload <- schemaIndex.insertSchema(validJson, id)
    } yield idAfterUpload

    uploadOrError match {
      case Right(id) => Created(asJson(successfulUpload(id)))
      case Left(message) => BadRequest(asJson(failedUpload(id, message))) //TODO should throw server error if ES fails
    }
  }

  def validate(id: String) = Action { implicit request: Request[AnyContent] =>
    val validateMsg = for {
      validJson <- maybeJson(request)
      schema <- schemaIndex.find(id)
      validateMsg <- SchemaValidator.validate(schema, validJson)
    } yield validateMsg

    validateMsg match {
      case Right(_) => Ok(asJson(successfulValidate(id)))
      case Left(msg) => BadRequest(asJson(failedValidate(id, msg)))
    }
  }

  private def maybeJson(request: Request[AnyContent]): Either[String, JsValue] = {
    request.body.asJson match {
      case Some(body) => Right(body)
      case None => Left("Invalid json or missing header Content-Type:application/json")
    }
  }

}

case class ResponseBody(action: String, id: String, status: String, message: Option[String] = None)
object ResponseBody {
  implicit val responseFormatter = Json.format[ResponseBody]


  def asJson(responseBody: ResponseBody): JsValue ={
    Json.toJson(responseBody)
  }
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