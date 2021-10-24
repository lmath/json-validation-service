package controllers

import java.util.UUID

import es.SchemaIndex
import javax.inject._
import play.api._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import validator.SchemaValidator

import scala.concurrent.{ExecutionContext, Future}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class SchemaController @Inject()(val cc: ControllerComponents, schemaIndex: SchemaIndex)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def get(id: String) = Action {
    val schemaOrNot: String = schemaIndex.find(id).getOrElse("nothing found!")

    Ok(schemaOrNot)
  }

  def upload(id: String) = Action { implicit request: Request[AnyContent] =>
    val schemaAsText = request.body.asText.getOrElse("something went wrong")
    schemaIndex.insertSchema(schemaAsText, id)
    Ok(schemaAsText)
  }

  def validate(id: String) = Action { implicit request: Request[AnyContent] =>
    val jsonToCheck = request.body.asText.getOrElse("something went wrong")

    val schema = schemaIndex.find(id).getOrElse("could not find that schema! ")
    val result = SchemaValidator.validate(schema, jsonToCheck)
    Ok(result.toString)
  }

  def hi() = Action {
    Ok("HIIII")
  }
}
