package es

import com.google.inject.Inject
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, RequestFailure, RequestSuccess}
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.mappings.TextField
import javax.inject.Singleton
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json

import scala.concurrent.Future
import play.api.Logger

// we must import the dsl
import com.sksamuel.elastic4s.ElasticDsl._

//TODO convert to be async
@Singleton
class SchemaIndex @Inject()(applicationLifecycle: ApplicationLifecycle) {

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

  applicationLifecycle.addStopHook { () =>
    logger.info("Shutting down the Elasticsearch client")
    Future.successful(client.close())
  }

  def insertSchema(schema: String, id: String) = {
    client.execute {
      indexInto(IndexName).fields("schema" -> schema).id(id).refresh(RefreshPolicy.Immediate) //todo what is refresh policy tbh
    }.await
  }

  case class Schema(schema: String)
  case class FoundRecord(_id: String, _source: Schema)

  object Schema {
    implicit val schemaFormatter = Json.format[Schema]
    implicit val jsonFormatter = Json.format[FoundRecord]
  }

  def find(id: String) = {

    import Schema._
    val searchResponse: Option[String] = client.execute {
      get(IndexName, id)

    }.await.body

    searchResponse flatMap { resp =>
      val foundRecord = Json.parse(resp).asOpt[FoundRecord]
      foundRecord.map(_._source.schema)
    }
  }


}
