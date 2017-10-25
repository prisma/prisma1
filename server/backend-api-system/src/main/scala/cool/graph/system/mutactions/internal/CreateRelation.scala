package cool.graph.system.mutactions.internal

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph._
import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.errors.UserInputErrors.ObjectDoesNotExistInCurrentProject
import cool.graph.shared.models.{Project, Relation}
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.database.tables.{RelationTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateRelation(project: Project,
                          relation: Relation,
                          fieldOnLeftModelIsRequired: Boolean = false,
                          fieldOnRightModelIsRequired: Boolean = false,
                          clientDbQueries: ClientDbQueries)
    extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] =
    Future.successful({
      val relations = TableQuery[RelationTable]
      val relayIds  = TableQuery[RelayIdTable]
      val addRelationRow = relations += cool.graph.system.database.tables
        .Relation(relation.id, project.id, relation.name, relation.description, relation.modelAId, relation.modelBId)
      val addRelayId = relayIds += cool.graph.system.database.tables.RelayId(relation.id, "Relation")

      SystemSqlStatementResult(sqlAction = DBIO.seq(addRelationRow, addRelayId))
    })

  override def rollback =
    Some(
      DeleteRelation(
        relation = relation,
        project = project,
        clientDbQueries = clientDbQueries
      ).execute)

  override def handleErrors =
    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
        UserInputErrors.RelationNameAlreadyExists(relation.name)
    })

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    () match {
      case _ if !NameConstraints.isValidRelationName(relation.name) =>
        Future.successful(Failure(UserInputErrors.InvalidName(name = relation.name)))

      case _ if project.relations.exists(x => x.name.toLowerCase == relation.name.toLowerCase && x.id != relation.id) =>
        Future.successful(Failure(UserInputErrors.RelationNameAlreadyExists(relation.name)))

      case _ if project.getModelById(relation.modelAId).isEmpty =>
        Future.successful(Failure(ObjectDoesNotExistInCurrentProject("modelIdA does not correspond to an existing Model")))

      case _ if project.getModelById(relation.modelBId).isEmpty =>
        Future.successful(Failure(ObjectDoesNotExistInCurrentProject("modelIdB does not correspond to an existing Model")))

      case _ if fieldOnLeftModelIsRequired || fieldOnRightModelIsRequired =>
        checkCounts()

      case _ =>
        Future.successful(Success(MutactionVerificationSuccess()))
    }
  }

  def checkCounts(): Future[Try[MutactionVerificationSuccess]] = {
    val modelA        = relation.getModelA_!(project)
    val modelB        = relation.getModelB_!(project)
    val fieldOnModelA = relation.getModelAField_!(project)
    val fieldOnModelB = relation.getModelBField_!(project)

    def checkCountResultAgainstRequired(aExists: Boolean, bExists: Boolean): Try[MutactionVerificationSuccess] = {
      (aExists, bExists) match {
        case (true, _) if fieldOnLeftModelIsRequired =>
          Failure(UserInputErrors.AddingRequiredRelationButNodesExistForModel(modelA.name, fieldOnModelA.name))
        case (_, true) if fieldOnRightModelIsRequired =>
          Failure(UserInputErrors.AddingRequiredRelationButNodesExistForModel(modelB.name, fieldOnModelB.name))
        case _ => Success(MutactionVerificationSuccess())
      }
    }

    val modelAExists = clientDbQueries.existsByModel(modelA).recover { case _: java.sql.SQLSyntaxErrorException => false }
    val modelBExists = clientDbQueries.existsByModel(modelB).recover { case _: java.sql.SQLSyntaxErrorException => false }

    for {
      aExists <- modelAExists
      bExists <- modelBExists
    } yield checkCountResultAgainstRequired(aExists, bExists)
  }
}
