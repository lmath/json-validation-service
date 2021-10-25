package controllers

import cats.data.EitherT
import cats.implicits._
import controllers.ResponseBody._
import es.ESSchemaIndex
import javax.inject._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import validator.SchemaValidator

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SchemaController @Inject()(val cc: ControllerComponents, schemaIndex: ESSchemaIndex)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def get(id: String) = Action.async {
    schemaIndex.find(id) map { schemaOrMessage =>
      schemaOrMessage match {
        case Right(schema) => Ok(Json.parse(schema))
        case Left(msg) => BadRequest(asJson(failedGet(id, msg)))
      }
    }
  }

  def upload(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val uploadOrError: EitherT[Future, String, String] = for {
      validJson <- EitherT(Future(maybeJson(request)))
      idAfterUpload <- EitherT(schemaIndex.insertSchema(validJson, id))
    } yield {
      idAfterUpload
    }

    uploadOrError.value map { result =>
      result match {
        case Right(id) => Created(asJson(successfulUpload(id)))
        case Left(message) => BadRequest(asJson(failedUpload(id, message))) //TODO should throw server error if ES fails
      }
    }
  }

  def validate(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val validateMsg: EitherT[Future, String, String] = for {
      validJson <- EitherT(Future(maybeJson(request)))
      schema <- EitherT(schemaIndex.find(id))
      validateMsg <- EitherT(Future(SchemaValidator.validate(schema, validJson)))
    } yield validateMsg

    validateMsg.value.map { msg =>
      msg match {
        case Right(_) => Ok(asJson(successfulValidate(id)))
        case Left(err) => BadRequest(asJson(failedValidate(id, err)))
      }
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