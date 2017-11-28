package cool.graph.api.schema

import akka.actor.ActorSystem
import cool.graph.shared.models.Project
import sangria.relay.Mutation
import sangria.schema._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future

case class TestApiUserContext(clientId: String)

trait TestSchemaBuilder {
  def apply(userContext: TestApiUserContext): Schema[TestApiUserContext, Unit]
}

object TestSchemaBuilder {
  def apply(internalDb: DatabaseDef)(implicit system: ActorSystem): TestSchemaBuilder = new TestSchemaBuilder {
    override def apply(userContext: TestApiUserContext) = TestSchemaBuilderImpl(userContext, internalDb).build()
  }
}

case class TestSchemaBuilderImpl(
    userContext: TestApiUserContext,
    internalDb: DatabaseDef
)(implicit system: ActorSystem) {
  import system.dispatcher

  def build(): Schema[TestApiUserContext, Unit] = {
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

  def testField(): Field[TestApiUserContext, Unit] = {
    Field(
      "viewer",
      fieldType = StringType,
      resolve = _ => "test"
    )
  }

}
