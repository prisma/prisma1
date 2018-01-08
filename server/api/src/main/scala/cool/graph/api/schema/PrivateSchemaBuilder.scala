package cool.graph.api.schema

import akka.actor.ActorSystem
import cool.graph.api.ApiDependencies
import cool.graph.api.mutations.ClientMutationRunner
import cool.graph.api.mutations.mutations.ResetData
import cool.graph.shared.models.Project
import sangria.schema.{BooleanType, Field, ObjectType, OptionType, Schema, SchemaValidationRule, StringType}

case class PrivateSchemaBuilder(
    project: Project
)(implicit apiDependencies: ApiDependencies, system: ActorSystem) {

  val dataResolver       = apiDependencies.dataResolver(project)
  val masterDataResolver = apiDependencies.masterDataResolver(project)

  import system.dispatcher

  def build(): Schema[ApiUserContext, Unit] = {
    val mutation = buildMutation()

    Schema(
      query = queryType,
      mutation = mutation,
      validationRules = SchemaValidationRule.empty
    )
  }

  def buildMutation(): Option[ObjectType[ApiUserContext, Unit]] = {
    val fields = List(resetDataField)

    Some(ObjectType("Mutation", fields))
  }

  def resetDataField: Field[ApiUserContext, Unit] = {
    Field(
      s"resetData",
      fieldType = OptionType(BooleanType),
      resolve = (ctx) => {
        val mutation = ResetData(project = project, dataResolver = masterDataResolver)
        ClientMutationRunner.run(mutation, dataResolver).map(_ => true)
      }
    )
  }

  lazy val queryType = {
    ObjectType(
      "Query",
      List(dummyField)
    )
  }

  lazy val dummyField: Field[ApiUserContext, Unit] = Field(
    "dummy",
    description = Some("This is only a dummy field due to the API of Schema of Sangria, as Query is not optional"),
    fieldType = StringType,
    resolve = (ctx) => ""
  )
}
