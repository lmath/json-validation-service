package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import error.{ErrorReason, InvalidJson, SchemaNotFound}
import es.{ESSchemaIndex, SchemaIndex}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsValue, Json}
import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class SchemaControllerSpec extends AnyFlatSpec with Matchers {

  implicit val materializer = ActorMaterializer()(ActorSystem())

  val alwaysSuccessfulSchemaIndexStub = new AlwaysSuccessfulSchemaIndexStub()
  val alwaysFailsSchemaIndexStub = new AlwaysFailsSchemaIndexStub()

  val invalidJson =
    """
      |{"a":1
      |"b": "c"
      |}
    """.stripMargin

  "GET /schema/:SCHEMAID" should
    "return successful when the schema exists" in {
      val controller = new SchemaController(stubControllerComponents(), alwaysSuccessfulSchemaIndexStub)
      val resp = controller.get("correct-horse-battery-staple").apply(FakeRequest(GET, "/schema"))

      status(resp) shouldBe OK
      contentType(resp) shouldBe Some("application/json")
      contentAsString(resp) should include ("{}")
    }
  "GET /schema/:SCHEMAID" should
    "return not found when the schema is not found" in { //TODO fix me - this needs to be updated to return not found
      val controller = new SchemaController(stubControllerComponents(), alwaysFailsSchemaIndexStub)
      val resp = controller.get("correct-horse-battery-staple").apply(FakeRequest(GET, "/schema"))

      status(resp) shouldBe NOT_FOUND
      contentType(resp) shouldBe Some("application/json")
      contentAsString(resp) should include ("Schema not found")
    }

  "POST /schema/:SCHEMAID" should
    "return 201 when the schema is successfully added" in {
      val controller = new SchemaController(stubControllerComponents(), alwaysSuccessfulSchemaIndexStub)
      val request = FakeRequest(POST, "/schema").withJsonBody(Json.obj()).withHeaders("Content-Type" -> "application/json")
      val resp = controller.upload("correct-horse-battery-staple").apply(request)

      status(resp) shouldBe CREATED
      contentType(resp) shouldBe Some("application/json")
      contentAsString(resp) should include ("success")
  }

  "POST /schema/:SCHEMAID" should
    "return bad request when the schema submitted is invalid json" in {
      val controller = new SchemaController(stubControllerComponents(), alwaysSuccessfulSchemaIndexStub)

      val request = FakeRequest(POST, "/schema").withBody(invalidJson).withHeaders("Content-Type" -> "application/json")
      val resp = controller.upload("correct-horse-battery-staple").apply(request)

      status(resp) shouldBe BAD_REQUEST
      contentType(resp) shouldBe Some("application/json")
      contentAsString(resp) should include ("Invalid json")
  }

  "POST /validate/:SCHEMAID" should
    "return ok when the submitted json is valid against the schema" in {
      val controller = new SchemaController(stubControllerComponents(), alwaysSuccessfulSchemaIndexStub)
      val request = FakeRequest(POST, "/validate").withJsonBody(Json.obj()).withHeaders("Content-Type" -> "application/json")
      val resp = controller.validate("correct-horse-battery-staple").apply(request)

      status(resp) shouldBe OK
      contentType(resp) shouldBe Some("application/json")
      contentAsString(resp) should include ("success")
  }

  "POST /validate/:SCHEMAID" should
    "return not found when the specified schema doesn't exist" in {
      val controller = new SchemaController(stubControllerComponents(), alwaysFailsSchemaIndexStub)
      val request = FakeRequest(POST, "/validate").withJsonBody(Json.obj()).withHeaders("Content-Type" -> "application/json")
      val resp = controller.validate("correct-horse-battery-staple").apply(request)

      status(resp) shouldBe NOT_FOUND
      contentType(resp) shouldBe Some("application/json")
      contentAsString(resp) should include ("Schema not found")
  }

  "POST /validate/:SCHEMAID" should
    "return bad request when the schema exists but the json doesn't validate" in {
      val stringsOnlySchema =
        """
          |{ "type": "string" }
        """.stripMargin
      val stringsSchemaStub = new AlwaysSuccessfulSchemaIndexStub(stringsOnlySchema)
      val controller = new SchemaController(stubControllerComponents(), stringsSchemaStub)
      val request = FakeRequest(POST, "/validate").withJsonBody(Json.obj()).withHeaders("Content-Type" -> "application/json")
      val resp = controller.validate("correct-horse-battery-staple").apply(request)

      status(resp) shouldBe OK
      contentType(resp) shouldBe Some("application/json")
      contentAsString(resp) should include ("fail")
  }
}

class AlwaysSuccessfulSchemaIndexStub(schema: String = "{}") extends ESSchemaIndex {
  override def insertSchema(schema: JsValue, id: String): Future[Either[ErrorReason, String]] = Future.successful(Right(id))

  override def find(id: String): Future[Either[ErrorReason, String]] = Future.successful(Right(schema))
}

class AlwaysFailsSchemaIndexStub extends ESSchemaIndex {
  override def insertSchema(schema: JsValue, id: String): Future[Either[ErrorReason, String]] = Future.successful(Left(InvalidJson))

  override def find(id: String): Future[Either[ErrorReason, String]] = Future.successful(Left(SchemaNotFound))
}


