package cool.graph.system.mutations

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.Project
import cool.graph.system.mutactions.internal.ExportData
import sangria.relay.Mutation
import scaldi.Injector

case class ExportDataMutation(
    client: models.Client,
    project: models.Project,
    args: ExportDataInput,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    dataResolver: DataResolver
)(implicit inj: Injector)
    extends InternalProjectMutation[ExportDataMutationPayload] {

  var url: String = ""

  override def prepareActions(): List[Mutaction] = {

    val exportData = ExportData(project, dataResolver)

    url = exportData.getUrl

    actions :+= exportData

    actions
  }

  override def getReturnValue: Option[ExportDataMutationPayload] = {
    Some(
      ExportDataMutationPayload(
        clientMutationId = args.clientMutationId,
        project = project,
        url = url
      ))
  }
}

case class ExportDataMutationPayload(clientMutationId: Option[String], project: Project, url: String) extends Mutation
case class ExportDataInput(clientMutationId: Option[String], projectId: String)
