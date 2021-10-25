package controllers

import cats.data.EitherT
import cats.implicits._
import controllers.ResponseBody._
import es.ESSchemaIndex
import javax.inject._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import validator.SchemaValidator
import error._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SchemaController @Inject()(val cc: ControllerComponents, schemaIndex: ESSchemaIndex)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def get(id: String) = Action.async {
    schemaIndex.find(id) map { schemaOrMessage =>
      schemaOrMessage match {
        case Right(schema) => Ok(Json.parse(schema))
        case Left(error) => badRequestOrServerError(id, error, failedGet)
      }
    }
  }

  private def badRequestOrServerError(id: String, error: ErrorReason, resp: (String, String) => ResponseBody) = error match {
    case ServerError => InternalServerError(asJson(resp(id, error.msg)))
    case err: JsonNotValidAgainstSchema => Ok(asJson(resp(id, err.msg)))
    case SchemaNotFound => NotFound(asJson(resp(id, error.msg)))
    case _ => BadRequest(asJson(resp(id, error.msg)))
  }


  def upload(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val uploadOrError: EitherT[Future, ErrorReason, String] = for {
      validJson <- EitherT(Future(maybeJson(request)))
      idAfterUpload <- EitherT(schemaIndex.insertSchema(validJson, id))
    } yield {
      idAfterUpload
    }

    uploadOrError.value map { result: Either[ErrorReason, String] =>
      result match {
        case Right(id) => Created(asJson(successfulUpload(id)))
        case Left(error) => badRequestOrServerError(id, error, failedUpload)
      }
    }
  }

  def validate(id: String) = Action.async { implicit request: Request[AnyContent] =>
    val validateMsg: EitherT[Future, ErrorReason, String] = for {
      validJson <- EitherT(Future(maybeJson(request)))
      schema <- EitherT(schemaIndex.find(id))
      validateMsg <- EitherT(Future(SchemaValidator.validate(schema, validJson)))
    } yield validateMsg

    validateMsg.value.map { msg =>
      msg match {
        case Right(_) => Ok(asJson(successfulValidate(id)))
        case Left(err) => badRequestOrServerError(id, err, failedValidate)
      }
    }
  }

  private def maybeJson(request: Request[AnyContent]): Either[ErrorReason, JsValue] = {
    request.body.asJson match {
      case Some(body) => Right(body)
      case None => Left(InvalidJson)
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