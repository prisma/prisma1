package cool.graph.private_api.schema

import cool.graph.client.ClientInjector
import cool.graph.client.database.{DataResolver, ProjectDataresolver}
import cool.graph.private_api.mutations.{SyncModelToAlgoliaInput, SyncModelToAlgoliaMutation, SyncModelToAlgoliaPayload}
import cool.graph.shared.models.Project
import sangria.relay.Mutation
import sangria.schema._
import scaldi.Injector

import scala.concurrent.ExecutionContext

class PrivateSchemaBuilder(project: Project)(implicit injector: ClientInjector, ec: ExecutionContext) {

  val inj: Injector = injector.toScaldi

  def build(): Schema[Unit, Unit] = {
    val query = ObjectType[Unit, Unit]("Query", List(dummyField))
    val mutation = ObjectType(
      "Mutation",
      fields[Unit, Unit](
        getSyncModelToAlgoliaField()
      )
    )
    Schema(query, Some(mutation))
  }

  def getSyncModelToAlgoliaField(): Field[Unit, Unit] = {
    import SyncModelToAlgoliaMutationFields.manual
    Mutation.fieldWithClientMutationId[Unit, Unit, SyncModelToAlgoliaPayload, SyncModelToAlgoliaInput](
      fieldName = "syncModelToAlgolia",
      typeName = "SyncModelToAlgolia",
      inputFields = SyncModelToAlgoliaMutationFields.inputFields,
      outputFields = fields(
        Field("foo", fieldType = StringType, resolve = _ => "bar")
      ),
      mutateAndGetPayload = (input, _) => {
        for {
          payload <- SyncModelToAlgoliaMutation(project, input, dataResolver(project)(inj)).execute()
        } yield payload
      }
    )
  }

  val dummyField: Field[Unit, Unit] = Field(
    "dummy",
    description = Some("This is only a dummy field due to the API of Schema of Sangria, as Query is not optional"),
    fieldType = StringType,
    resolve = _ => ""
  )

  def dataResolver(project: Project)(implicit inj: Injector): DataResolver = new ProjectDataresolver(project = project, requestContext = None)
}
