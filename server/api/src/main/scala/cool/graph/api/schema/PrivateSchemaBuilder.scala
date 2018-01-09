package cool.graph.api.schema

import akka.actor.ActorSystem
import cool.graph.api.ApiDependencies
import cool.graph.api.mutations.ClientMutationRunner
import cool.graph.api.mutations.mutations.ResetData
import cool.graph.api.subscriptions.schema.{SubscriptionQueryError, SubscriptionQueryValidator}
import cool.graph.shared.models.{Model, Project}
import org.scalactic.Or
import sangria.schema.{Argument, BooleanType, Field, ObjectType, OptionType, Schema, SchemaValidationRule, StringType}

case class PrivateSchemaBuilder(
    project: Project
)(implicit apiDependencies: ApiDependencies, system: ActorSystem) {

  val dataResolver       = apiDependencies.dataResolver(project)
  val masterDataResolver = apiDependencies.masterDataResolver(project)

  import system.dispatcher

  def build(): Schema[ApiUserContext, Unit] = {
    Schema(
      query = queryType,
      mutation = Some(mutationType),
      validationRules = SchemaValidationRule.empty
    )
  }

  lazy val queryType = {
    ObjectType(
      name = "Query",
      fields = List(validateSubscriptionQueryField)
    )
  }

  lazy val mutationType = {
    ObjectType(
      name = "Mutation",
      fields = List(resetDataField)
    )
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

  def validateSubscriptionQueryField: Field[ApiUserContext, Unit] = {
    Field(
      s"validateSubscriptionQuery",
      fieldType = BooleanType,
      arguments = List(Argument("query", StringType)),
      resolve = (ctx) => {
        val query                                          = ctx.arg[String]("query")
        val validator                                      = SubscriptionQueryValidator(project)
        val result: Or[Model, Seq[SubscriptionQueryError]] = validator.validate(query)
        result.isGood
      }
    )
  }
}
