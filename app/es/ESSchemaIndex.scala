package es

import com.google.inject.Inject
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.mappings.TextField
import javax.inject.Singleton
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger

import scala.util.{Failure, Success, Try}
import Schema._
import com.sksamuel.elastic4s.ElasticDsl._
import error.{ErrorReason, SchemaNotFound, ServerError}

trait SchemaIndex {
  def insertSchema(schema: JsValue, id: String): Future[Either[ErrorReason, String]]
  def find(id: String): Future[Either[ErrorReason, String]]
}


@Singleton
class ESSchemaIndex @Inject()(implicit ec: ExecutionContext) extends SchemaIndex {

  val logger: Logger = Logger(this.getClass())

  // in this example we create a client to a local Docker container at localhost:9200
  val client = ElasticClient(JavaClient(ElasticProperties(s"http://${sys.env.getOrElse("ES_HOST", "127.0.0.1")}:${sys.env.getOrElse("ES_PORT", "9200")}")))


  val IndexName = "schemas"

  // Next we create an index in advance ready to receive documents. //TODO do this at app startup
  // await is a helper method to make this operation synchronous instead of async
  // You would normally avoid doing this in a real program as it will block
  // the calling thread but is useful when testing
  client.execute {
    createIndex(IndexName).mapping(
      properties(
        fields = Seq(TextField("schema")) //TODO should this be a text field?!?!
      )
    )
  }.await

  def insertSchema(schema: JsValue, id: String): Future[Either[ErrorReason, String]] = {
    val resp: Future[Response[IndexResponse]] = client.execute {
      indexInto(IndexName).fields("schema" -> schema.toString()).id(id).refresh(RefreshPolicy.Immediate) //todo what is refresh policy tbh
    }

    resp map { respEventually =>
      if(respEventually.isError)
        Left(ServerError)
      else Right(respEventually.result.id)
    }
  }

  def find(id: String): Future[Either[ErrorReason, String]] = {
    val searchResponse = client.execute(get(IndexName, id))

    searchResponse map { resp =>
      if(resp.result.found && resp.body.isDefined) {
        Try(Json.parse(resp.body.get).as[FoundRecord]) match {
          case Success(value) => Right(value._source.schema)
          case Failure(msg) => {
            logger.warn(s"We looked for ${id} and found it, but it doesn't seem to be a schema.")
            Left(ServerError)
          }
        }
      } else {
        logger.info(s"Could not find schema with id ${id}")
        Left(SchemaNotFound)
      }

    }
  }


}
