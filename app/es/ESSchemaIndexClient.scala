package es

import com.google.inject.Inject
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.mappings.TextField
import error.{ErrorReason, SchemaNotFound, ServerError}
import es.Schema._
import javax.inject.Singleton
import play.api.Logger
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SchemaIndexClient {
  def insertSchema(schema: JsValue, id: String): Future[Either[ErrorReason, String]]
  def find(id: String): Future[Either[ErrorReason, String]]
}

@Singleton
class ESSchemaIndexClient @Inject()(implicit ec: ExecutionContext) extends SchemaIndexClient {

  val logger: Logger = Logger(this.getClass())

  val client = ElasticClient(JavaClient(ElasticProperties(s"http://${sys.env.getOrElse("ES_HOST", "127.0.0.1")}:${sys.env.getOrElse("ES_PORT", "9200")}")))
  val IndexName = "schemas"

  val indexCreation = client.execute {
    createIndex(IndexName).mapping(
      properties(
        fields = Seq(TextField("schema"))
      )
    )
  }.await

  def insertSchema(schema: JsValue, id: String): Future[Either[ErrorReason, String]] = {
    client.execute {
      indexInto(IndexName).fields("schema" -> schema.toString()).id(id).refresh(RefreshPolicy.Immediate)
    } map { respEventually =>
      if(respEventually.isError)
        Left(ServerError)
      else Right(respEventually.result.id)
    }
  }

  def find(id: String): Future[Either[ErrorReason, String]] = {
     client.execute(get(IndexName, id)) map { resp =>
      if(resp.result.found && resp.body.isDefined) {
        Try(Json.parse(resp.body.get).as[FoundRecord]) match {
          case Success(value) => Right(value._source.schema)
          case Failure(msg) => {
            logger.warn(s"We looked for $id and found it, but it doesn't seem to have a schema")
            Left(ServerError)
          }
        }
      } else {
        logger.info(s"Could not find schema with id $id")
        Left(SchemaNotFound)
      }
    }
  }

}
