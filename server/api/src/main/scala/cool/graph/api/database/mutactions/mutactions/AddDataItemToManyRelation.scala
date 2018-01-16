package cool.graph.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph.api.database.{DataResolver, DatabaseMutationBuilder, NameConstraints, RelationFieldMirrorUtils}
import cool.graph.api.database.DatabaseMutationBuilder.MirrorFieldDbValues
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import cool.graph.api.mutations.ParentInfo
import cool.graph.api.schema.APIErrors
import cool.graph.cuid.Cuid
import cool.graph.shared.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Notation: It's not important which side you actually put into to or from. the only important
  * thing is that fromField belongs to fromModel
  */
case class AddDataItemToManyRelation(project: Project, parentInfo: ParentInfo, toId: String, toIdAlreadyInDB: Boolean = true)
    extends ClientSqlDataChangeMutaction {

  val relationSide: cool.graph.shared.models.RelationSide.Value = parentInfo.field.relationSide.get

  val aValue: String = if (relationSide == RelationSide.A) parentInfo.where.fieldValueAsString else toId
  val bValue: String = if (relationSide == RelationSide.A) toId else parentInfo.where.fieldValueAsString

  val aModel: Model = parentInfo.relation.getModelA_!(project.schema)
  val bModel: Model = parentInfo.relation.getModelB_!(project.schema)

  private def getFieldMirrors(model: Model, id: String) =
    parentInfo.relation.fieldMirrors
      .filter(mirror => model.fields.map(_.id).contains(mirror.fieldId))
      .map(mirror => {
        val field = project.schema.getFieldById_!(mirror.fieldId)
        MirrorFieldDbValues(
          relationColumnName = RelationFieldMirrorUtils.mirrorColumnName(project, field, parentInfo.relation),
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
          .createRelationRow(project.id, parentInfo.relation.id, Cuid.createCuid(), aValue, bValue, fieldMirrors)))
  }

  override def handleErrors =
    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 =>
        APIErrors.ItemAlreadyInRelation()
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
        APIErrors.NodeDoesNotExist("")
    })

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {

    if (toIdAlreadyInDB) {
      val toModel = if (relationSide == RelationSide.A) parentInfo.relation.getModelB_!(project.schema) else parentInfo.relation.getModelA_!(project.schema)
      resolver.existsByModelAndId(toModel, toId) map {
        case false => Failure(APIErrors.NodeDoesNotExist(toId))
        case true =>
          (NameConstraints.isValidDataItemId(aValue), NameConstraints.isValidDataItemId(bValue)) match {
            case (false, _)    => Failure(APIErrors.IdIsInvalid(aValue))
            case (true, false) => Failure(APIErrors.IdIsInvalid(bValue))
            case _             => Success(MutactionVerificationSuccess())
          }
      }
    } else {
      Future.successful(
        if (!NameConstraints.isValidDataItemId(aValue)) Failure(APIErrors.IdIsInvalid(aValue))
        else if (!NameConstraints.isValidDataItemId(bValue)) Failure(APIErrors.IdIsInvalid(bValue))
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
