package com.prisma.api.schema

import com.prisma.api.ApiDependencies
import com.prisma.api.mutations.{ClientMutationRunner, Reset}
import com.prisma.shared.models.{Model, Project}
import com.prisma.subscriptions.schema.{SubscriptionQueryError, SubscriptionQueryValidator}
import org.scalactic.{Bad, Good, Or}
import sangria.schema.{Argument, BooleanType, Context, Field, ListType, ObjectType, OptionType, Schema, SchemaValidationRule, StringType}

case class PrivateSchemaBuilder(
    project: Project
)(implicit apiDependencies: ApiDependencies) {
  import apiDependencies.executionContext

  val dataResolver                = apiDependencies.dataResolver(project)
  val masterDataResolver          = apiDependencies.masterDataResolver(project)
  val databaseMutactionExecutor   = apiDependencies.databaseMutactionExecutor
  val sideEffectMutactionExecutor = apiDependencies.sideEffectMutactionExecutor
  val mutactionVerifier           = apiDependencies.mutactionVerifier

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
        val mutation = Reset(project = project, dataResolver = masterDataResolver)
        ClientMutationRunner.run(mutation, databaseMutactionExecutor, sideEffectMutactionExecutor, mutactionVerifier).map(_ => true)
      }
    )
  }

  def validateSubscriptionQueryField: Field[ApiUserContext, Unit] = {
    Field(
      s"validateSubscriptionQuery",
      fieldType = ObjectType(
        name = "SubscriptionQueryValidationResult",
        fields = List(
          Field(
            name = "errors",
            fieldType = ListType(StringType),
            resolve = (ctx: Context[ApiUserContext, Seq[SubscriptionQueryError]]) => ctx.value.map(_.errorMessage)
          )
        )
      ),
      arguments = List(Argument("query", StringType)),
      resolve = (ctx) => {
        val query                                          = ctx.arg[String]("query")
        val validator                                      = SubscriptionQueryValidator(project)
        val result: Or[Model, Seq[SubscriptionQueryError]] = validator.validate(query)
        result match {
          case Bad(errors) => errors
          case Good(_)     => Seq.empty[SubscriptionQueryError]
        }
      }
    )
  }
}
