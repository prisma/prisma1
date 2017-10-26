package cool.graph.client.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder.MirrorFieldDbValues
import cool.graph.client.database.{DataResolver, DatabaseMutationBuilder}
import cool.graph.cuid.Cuid
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models._
import cool.graph.shared.{NameConstraints, RelationFieldMirrorColumn}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Notation: It's not important which side you actually put into to or from. the only important
  * thing is that fromField belongs to fromModel
  */
case class AddDataItemToManyRelation(project: Project, fromModel: Model, fromField: Field, toId: String, fromId: String, toIdAlreadyInDB: Boolean = true)
    extends ClientSqlDataChangeMutaction {

  // If this assertion fires, this mutaction is used wrong by the programmer.
  assert(fromModel.fields.exists(_.id == fromField.id))

  val relationSide: cool.graph.shared.models.RelationSide.Value = fromField.relationSide.get
  val relation: Relation                                        = fromField.relation.get

  val aValue: String = if (relationSide == RelationSide.A) fromId else toId
  val bValue: String = if (relationSide == RelationSide.A) toId else fromId

  val aModel: Model = relation.getModelA_!(project)
  val bModel: Model = relation.getModelB_!(project)

  private def getFieldMirrors(model: Model, id: String) =
    relation.fieldMirrors
      .filter(mirror => model.fields.map(_.id).contains(mirror.fieldId))
      .map(mirror => {
        val field = project.getFieldById_!(mirror.fieldId)
        MirrorFieldDbValues(
          relationColumnName = RelationFieldMirrorColumn.mirrorColumnName(project, field, relation),
          modelColumnName = field.name,
          model.name,
          id
        )
      })

  val fieldMirrors: List[MirrorFieldDbValues] = getFieldMirrors(aModel, aValue) ++ getFieldMirrors(bModel, bValue)

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .createRelationRow(project.id, relation.id, Cuid.createCuid(), aValue, bValue, fieldMirrors)))
  }

  override def handleErrors =
    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
        UserAPIErrors.ItemAlreadyInRelation()
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
        UserAPIErrors.NodeDoesNotExist("")
    })

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {

    if (toIdAlreadyInDB) {
      val toModel = if (relationSide == RelationSide.A) relation.getModelB_!(project) else relation.getModelA_!(project)
      resolver.existsByModelAndId(toModel, toId) map {
        case false => Failure(UserAPIErrors.NodeDoesNotExist(toId))
        case true =>
          (NameConstraints.isValidDataItemId(aValue), NameConstraints.isValidDataItemId(bValue)) match {
            case (false, _)    => Failure(UserAPIErrors.IdIsInvalid(aValue))
            case (true, false) => Failure(UserAPIErrors.IdIsInvalid(bValue))
            case _             => Success(MutactionVerificationSuccess())
          }
      }
    } else {
      Future.successful(
        if (!NameConstraints.isValidDataItemId(aValue)) Failure(UserAPIErrors.IdIsInvalid(aValue))
        else if (!NameConstraints.isValidDataItemId(bValue)) Failure(UserAPIErrors.IdIsInvalid(bValue))
        else Success(MutactionVerificationSuccess()))
    }
    // todo: handle case where the relation table is just being created
//    if (resolver.resolveRelation(relation.id, aValue, bValue).nonEmpty) {
//      return Future.successful(
//          Failure(RelationDoesAlreadyExist(
//                  aModel.name, bModel.name, aValue, bValue)))
//    }

  }

}
