package cool.graph.system.mutactions.internal

import cool.graph.Types.Id
import cool.graph._
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.database.finder.{CachedProjectResolver, ProjectQueries, ProjectResolver}
import sangria.ast.{Document, ObjectTypeDefinition, TypeDefinition}
import scaldi.{Injectable, Injector}
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future

case class UpdateTypeAndFieldPositions(
    project: Project,
    client: Client,
    newSchema: Document,
    internalDatabase: DatabaseDef,
    projectQueries: ProjectQueries
)(implicit inj: Injector)
    extends SystemSqlMutaction
    with Injectable {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val projectResolver = inject[ProjectResolver](identified by "uncachedProjectResolver")

  val mutactions: mutable.Buffer[SystemSqlMutaction] = mutable.Buffer.empty

  override def execute: Future[SystemSqlStatementResult[Any]] =
    refreshProject.flatMap { project =>
      val newTypePositions: Seq[Id] = newSchema.definitions.collect {
        case typeDef: TypeDefinition =>
          project
            .getModelByName(typeDef.name)
            .orElse(project.getEnumByName(typeDef.name))
            .map(_.id)
      }.flatten

      mutactions += UpdateProject(
        client = client,
        oldProject = project,
        project = project.copy(typePositions = newTypePositions.toList),
        internalDatabase = internalDatabase,
        projectQueries = projectQueries,
        bumpRevision = false
      )

      mutactions ++= newSchema.definitions.collect {
        case typeDef: ObjectTypeDefinition =>
          project.getModelByName(typeDef.name).map { model =>
            val newFieldPositions = typeDef.fields.flatMap { fieldDef =>
              model.getFieldByName(fieldDef.name).map(_.id)
            }.toList
            UpdateModel(project = project, oldModel = model, model = model.copy(fieldPositions = newFieldPositions))
          }
      }.flatten

      val y = mutactions.map(_.execute)
      Future.sequence(y).map { statementResults =>
        val asSingleAction = DBIOAction.sequence(statementResults.toList.map(_.sqlAction))
        SystemSqlStatementResult(sqlAction = asSingleAction)
      }
    }

  def refreshProject: Future[Project] = {
    projectResolver.resolve(project.id).map(_.get)
  }

  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] = mutactions.map(_.rollback).headOption.flatten

}
