package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.models
import cool.graph.system.mutactions.internal.{BumpProjectRevision, DeletePackageDefinition, DeleteRootToken, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class UninstallPackageMutation(client: models.Client,
                                    project: models.Project,
                                    args: UninstallPackageInput,
                                    projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[UninstallPackageMutationPayload] {

  var oldPackage: Option[models.PackageDefinition] = None

  override def prepareActions(): List[Mutaction] = {

    oldPackage = project.packageDefinitions.find(_.name == args.name) match {
      case None    => throw SystemErrors.InvalidPackageName(args.name)
      case Some(x) => Some(x)
    }

    val deletePackage = DeletePackageDefinition(project, oldPackage.get, internalDatabase = internalDatabase.databaseDef)

    val deletePat = project.rootTokens.filter(_.name == args.name).map(pat => DeleteRootToken(pat))

    actions = List(deletePackage, BumpProjectRevision(project = project), InvalidateSchema(project)) ++ deletePat

    actions
  }

  override def getReturnValue: Option[UninstallPackageMutationPayload] = {
    Some(
      UninstallPackageMutationPayload(
        clientMutationId = args.clientMutationId,
        project = project.copy(packageDefinitions = project.packageDefinitions :+ oldPackage.get),
        packageDefinition = oldPackage.get
      ))
  }
}

case class UninstallPackageMutationPayload(clientMutationId: Option[String], project: models.Project, packageDefinition: models.PackageDefinition)
    extends Mutation

case class UninstallPackageInput(clientMutationId: Option[String], projectId: String, name: String)
