package cool.graph.system.mutations

import cool.graph.GCDataTypes.GCStringConverter
import cool.graph.cuid.Cuid
import cool.graph.deprecated.packageMocks.PackageParser
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.Field
import cool.graph.system.database.client.EmptyClientDbQueries
import cool.graph.system.mutactions.client.CreateColumn
import cool.graph.system.mutactions.internal._
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class InstallPackageMutation(
    client: models.Client,
    project: models.Project,
    args: InstallPackageInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[InstallPackageMutationPayload] {

  var newPackage: Option[models.PackageDefinition] = None

  override def prepareActions(): List[Mutaction] = {
    val parsed = PackageParser.parse(args.definition)

    newPackage = Some(models.PackageDefinition(id = Cuid.createCuid(), name = parsed.name, definition = args.definition, formatVersion = 1))

    val addPackage = CreatePackageDefinition(project, newPackage.get, internalDatabase = internalDatabase.databaseDef)
    val newPat     = CreateRootTokenMutation.generate(clientId = client.id, projectId = project.id, name = newPackage.get.name, expirationInSeconds = None)

    val addPat = project.getRootTokenByName(newPackage.get.name) match {
      case None => List(CreateRootToken(project.id, newPat))
      case _    => List()
    }

    val addFields = PackageParser
      .install(parsed, project.copy(rootTokens = project.rootTokens :+ newPat))
      .interfaces
      .flatMap(i => {
        i.fields.flatMap(f => {
          // todo: this check should be more selective
          if (i.model.fields.exists(_.name == f.name)) {
            //sys.error("Cannot install interface on type that already has field with same name")
            List()
          } else {
            val newField = Field(
              id = Cuid.createCuid(),
              name = f.name,
              typeIdentifier = f.typeIdentifier,
              description = Some(f.description),
              isReadonly = false,
              isRequired = f.isRequired,
              isList = f.isList,
              isUnique = f.isUnique,
              isSystem = false,
              defaultValue = f.defaultValue.map(GCStringConverter(f.typeIdentifier, f.isList).toGCValue(_).get)
            )

            List(
              CreateColumn(projectId = project.id, model = i.model, field = newField),
              CreateField(project = project, model = i.model, field = newField, migrationValue = f.defaultValue, EmptyClientDbQueries)
            )
          }
        })
      })

    actions = List(addPackage, BumpProjectRevision(project = project), InvalidateSchema(project)) ++ addPat ++ addFields
    actions
  }

  override def getReturnValue: Option[InstallPackageMutationPayload] = {
    Some(
      InstallPackageMutationPayload(
        clientMutationId = args.clientMutationId,
        project = project.copy(packageDefinitions = project.packageDefinitions :+ newPackage.get),
        packageDefinition = newPackage.get
      ))
  }
}

case class InstallPackageMutationPayload(clientMutationId: Option[String], project: models.Project, packageDefinition: models.PackageDefinition)
    extends Mutation

case class InstallPackageInput(clientMutationId: Option[String], projectId: String, definition: String)
