package controllers

import cats.data.EitherT
import cats.implicits._
import controllers.model.ResponseBody
import controllers.model.ResponseBody._
import es.ESSchemaIndexClient
import javax.inject._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import validator.SchemaValidator
import error._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SchemaController @Inject()(val cc: ControllerComponents, schemaIndex: ESSchemaIndexClient)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def get(id: String): Action[AnyContent] = Action.async {
    schemaIndex.find(id) map { schemaOrMessage =>
      schemaOrMessage match {
        case Right(schema) => Ok(Json.parse(schema))
        case Left(error) => errorResponse(id, error, failedGet)
      }
    }
  }

  def upload(id: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val uploadOrError: EitherT[Future, ErrorReason, String] = for {
      validJson <- EitherT(Future(maybeJson(request)))
      idAfterUpload <- EitherT(schemaIndex.insertSchema(validJson, id))
    } yield {
      idAfterUpload
    }

    uploadOrError.value map { result: Either[ErrorReason, String] =>
      result match {
        case Right(id) => Created(successfulUpload(id).asJson)
        case Left(error) => errorResponse(id, error, failedUpload)
      }
    }
  }

  def validate(id: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val validateMsg: EitherT[Future, ErrorReason, String] = for {
      validJson <- EitherT(Future(maybeJson(request)))
      schema <- EitherT(schemaIndex.find(id))
      validateMsg <- EitherT(Future(SchemaValidator.validate(schema, validJson)))
    } yield validateMsg

    validateMsg.value.map { msg =>
      msg match {
        case Right(_) => Ok(successfulValidate(id).asJson)
        case Left(err) => errorResponse(id, err, failedValidate)
      }
    }
  }

  private def errorResponse(id: String, error: ErrorReason, resp: (String, String) => ResponseBody): Result = error match {
    case ServerError => InternalServerError(resp(id, error.msg).asJson)
    case err: JsonNotValidAgainstSchema => Ok(resp(id, err.msg).asJson)
    case SchemaNotFound => NotFound(resp(id, error.msg).asJson)
    case _ => BadRequest(resp(id, error.msg).asJson)
  }

  private def maybeJson(request: Request[AnyContent]): Either[ErrorReason, JsValue] = {
    request.body.asJson match {
      case Some(body) => Right(body)
      case None => Left(InvalidJson)
    }
  }

}
