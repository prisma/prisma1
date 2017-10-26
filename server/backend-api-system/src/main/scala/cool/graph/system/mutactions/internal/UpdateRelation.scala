package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models.{Project, Relation}
import cool.graph.system.database.tables.RelationTable
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class UpdateRelation(oldRelation: Relation, relation: Relation, project: Project) extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val relations = TableQuery[RelationTable]
    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for { r <- relations if r.id === relation.id } yield (r.name, r.description, r.modelAId, r.modelBId)
      q.update(relation.name, relation.description, relation.modelAId, relation.modelBId)
    })))
  }

  override def rollback = Some(UpdateRelation(oldRelation = oldRelation, relation = oldRelation, project = project).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    def otherRelationWithNameExists =
      project.relations.exists(existing => existing.name.toLowerCase == relation.name.toLowerCase && existing.id != relation.id)

    () match {
      case _ if !NameConstraints.isValidRelationName(relation.name) => Future.successful(Failure(UserInputErrors.InvalidName(name = relation.name)))
      case _ if otherRelationWithNameExists                         => Future.successful(Failure(UserInputErrors.RelationNameAlreadyExists(relation.name)))
      case _                                                        => Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
