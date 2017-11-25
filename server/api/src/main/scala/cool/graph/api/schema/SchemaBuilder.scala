package cool.graph.api.schema

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import cool.graph.shared.models.Project
import sangria.relay.Mutation
import sangria.schema._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

case class ApiUserContext(clientId: String)

trait SchemaBuilder {
  def apply(userContext: ApiUserContext): Schema[ApiUserContext, Unit]
}

object SchemaBuilder {
  def apply(internalDb: DatabaseDef)(implicit system: ActorSystem): SchemaBuilder = new SchemaBuilder {
    override def apply(userContext: ApiUserContext) = SchemaBuilderImpl(userContext, internalDb).build()
  }
}

case class SchemaBuilderImpl(
    userContext: ApiUserContext,
    internalDb: DatabaseDef
)(implicit system: ActorSystem) {
  import system.dispatcher

  def build(): Schema[ApiUserContext, Unit] = {
    val Query = ObjectType(
      "Query",
      testField() :: Nil
    )

//    val Mutation = ObjectType(
//      "Mutation",
//      List.empty
//    )

    Schema(Query, None)
  }

  def testField(): Field[ApiUserContext, Unit] = {
    Field(
      "viewer",
      fieldType = StringType,
      resolve = _ => akka.pattern.after(FiniteDuration(500, TimeUnit.MILLISECONDS), system.scheduler)(Future.successful("YES")) // "test"
    )
  }

}
