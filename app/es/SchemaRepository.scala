//package es
//
//import java.util.UUID
//
//import com.google.inject.{Inject, Singleton}
//import play.api.db.slick.DatabaseConfigProvider
//import play.api.libs.json.{Format, JsValue, Json}
//import slick.jdbc.H2Profile.api._
//import slick.jdbc.PostgresProfile
//import slick.lifted.ProvenShape
//
//import scala.collection.mutable
//import scala.concurrent.{ExecutionContext, Future}
//
//case class Schema(id: String, jsonschema: String)
//object Schema {
//  implicit val format: Format[Schema] = Json.format[Schema]
//}
//trait SchemaRespository {
//  def all: Future[Seq[Schema]]
//  def get(id: UUID): Future[Option[Schema]]
//  def save(person: Schema): Future[Schema]
//  def delete(id: UUID): Future[Option[Schema]]
//}
//
//@Singleton
//class PostgresSchemaRespository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
//
//  private val dbConfig = dbConfigProvider.get[PostgresProfile]
//  implicit val configFormat = Json.format[Schema]
//  implicit val configColumnType = MappedColumnType.base[Schema, String](
//    config => Json.stringify(Json.toJson(config)),
//    column => Json.parse(column).as[Schema]
//  )
//
//  // Definition of the schemas table
//  class Schemas(tag: Tag) extends Table[(String, String)](tag, "schemas") {
//    def id: Rep[String] = column[String]("ID", O.PrimaryKey) // This is the primary key column
//    def jsonschema: Rep[String] = column[String]("jsonschema")
//    // Every table needs a * projection with the same type as the table's type parameter
//    def * : ProvenShape[(String, String)] = (id, jsonschema)
//  }
//
//  val schemas = TableQuery[Schemas]
//
//  def dbHealthCheck(): Future[Vector[Int]] = {
//    dbConfig.db.run(sql"SELECT 1".as[Int])
//  }
//
//  def get(id: String): Future[Schema] = {
//
//    val q = schemas.filter(_.id === id)
//    val action = q.result
//
//    dbConfig.db.run(action).mapTo[Schema]
//  }
//
////  def all: Future[Seq[Schema]] = {
////    dbConfig.db.run()
////  }
//}
//
//
//class InMemorySchemaRepository extends SchemaRespository {
//  private val schemas: mutable.Map[UUID, Schema] = mutable.Map.empty
//
//  def all: Future[Seq[Schema]] = Future.successful {
//    schemas.values.toSeq
//  }
//
//  def get(uuid: UUID): Future[Option[Schema]] = Future.successful {
//    schemas.get(uuid)
//  }
//
//  def save(schema: Schema): Future[Schema] = Future.successful {
//    val uuid = UUID.randomUUID
//    val newSchema = schema.copy(id = uuid.toString)
//    schemas.put(uuid, newSchema)
//    newSchema
//  }
//
//  def delete(uuid: UUID): Future[Option[Schema]] = Future.successful {
//    schemas.remove(uuid)
//  }
//
//}
