package cool.graph.system.mutactions.client

import com.typesafe.config.Config
import cool.graph.graphql.GraphQlClient
import cool.graph.shared.errors.SystemErrors.SystemApiError
import cool.graph.shared.models.{AlgoliaSyncQuery, Model, Project}
import cool.graph.{Mutaction, MutactionExecutionResult, MutactionExecutionSuccess, MutactionVerificationSuccess}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

case class SyncModelToAlgoliaViaRequest(project: Project, model: Model, algoliaSyncQuery: AlgoliaSyncQuery, config: Config)(implicit ec: ExecutionContext)
    extends Mutaction {

  val clientApiAddress: String       = config.getString("clientApiAddress").stripSuffix("/")
  val privateClientApiSecret: String = config.getString("privateClientApiSecret")

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    Future.successful(Success(MutactionVerificationSuccess()))
  }

  override def execute: Future[MutactionExecutionResult] = {
    val graphqlClient = GraphQlClient(s"$clientApiAddress/simple/private/${project.id}", Map("Authorization" -> privateClientApiSecret))
    val query =
      s"""mutation {
         |   syncModelToAlgolia(
         |     input: {
         |       modelId: "${model.id}",
         |       syncQueryId: "${algoliaSyncQuery.id}"
         |      }
         |    ){
         |      clientMutationId
         |    }
         | }
      """.stripMargin

    graphqlClient.sendQuery(query).map { response =>
      if (response.isSuccess) {
        MutactionExecutionSuccess()
      } else {
        val error = response.firstError
        new SystemApiError(error.message, error.code) {}
      }
    }
  }
}
