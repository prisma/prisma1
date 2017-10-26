package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models.{Model, Project}
import cool.graph.shared.schema.CustomScalarTypes
import cool.graph.system.database.ModelToDbMapper
import cool.graph.system.database.tables.Tables
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class UpdateModel(project: Project, oldModel: Model, model: Model) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    Future.successful {
      SystemSqlStatementResult(sqlAction = DBIO.seq {
        Tables.Models.filter(_.id === model.id).update(ModelToDbMapper.convertModel(project, model))
      })
    }
  }

  override def rollback = Some(UpdateModel(project = project, oldModel = oldModel, model = oldModel).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    Future.successful(() match {
      case _ if oldModel.isSystem && oldModel.name != model.name            => Failure(UserInputErrors.CantRenameSystemModels(name = oldModel.name))
      case _ if !NameConstraints.isValidModelName(model.name)               => Failure(UserInputErrors.InvalidName(name = model.name))
      case _ if CustomScalarTypes.isScalar(model.name)                      => Failure(UserInputErrors.InvalidName(name = model.name))
      case _ if project.getModelByName(model.name).exists(_.id != model.id) => Failure(UserInputErrors.ModelWithNameAlreadyExists(model.name))
      case _                                                                => Success(MutactionVerificationSuccess())
    })
  }
}
