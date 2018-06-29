package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.{Filter, NodeSelector, PrismaNode}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.{Model, RelationField, Schema}
import org.jooq.{Condition, Record1, SelectConditionStep}
import org.jooq.impl.DSL.{asterisk, field, name}

import scala.concurrent.ExecutionContext
import scala.language.existentials

trait NodeSingleQueries extends BuilderBase with NodeManyQueries with FilterConditionBuilder {
  import slickDatabase.profile.api._

  val relayIdTableQuery = {
    val bla = RelayIdTableWrapper(slickDatabase.profile)
    TableQuery(new bla.SlickTable(_, schemaName))
  }

  def selectByGlobalId(schema: Schema, idGCValue: IdGCValue)(implicit ec: ExecutionContext): DBIO[Option[PrismaNode]] = {
    val modelNameForId: DBIO[Option[String]] = relayIdTableQuery
      .filter(_.id === idGCValue.value.toString)
      .map(_.stableModelIdentifier)
      .take(1)
      .result
      .headOption

    for {
      stableModelIdentifier <- modelNameForId
      result <- stableModelIdentifier match {
                 case Some(stableModelIdentifier) =>
                   val model = schema.getModelByStableIdentifier_!(stableModelIdentifier.trim)
                   selectById(model, idGCValue)
                 case None =>
                   DBIO.successful(None)
               }
    } yield result
  }

  def selectById(model: Model, idGcValue: IdGCValue)(implicit ec: ExecutionContext): DBIO[Option[PrismaNode]] = {
    batchSelectFromModelByUnique(model, model.idField_!, Vector(idGcValue)).map(_.headOption)
  }

  def queryNodeByWhere(where: NodeSelector): DBIO[Option[PrismaNode]] = {
    val model = where.model
    val query = sql
      .select(asterisk())
      .from(modelTable(model))
      .where(modelColumn(model, where.field).equal(placeHolder))

    queryToDBIO(query)(
      setParams = pp => pp.setGcValue(where.fieldGCValue),
      readResult = rs => rs.as(readsPrismaNode(model)).headOption
    )
  }

  def queryIdFromWhere(where: NodeSelector): DBIO[Option[IdGCValue]] = {
    SimpleDBIO { ctx =>
      val model = where.model
      val query = sql
        .select(idField(model))
        .from(modelTable(model))
        .where(modelColumn(model, where.field).equal(placeHolder))

      val ps = ctx.connection.prepareStatement(query.getSQL)
      ps.setGcValue(1, where.fieldGCValue)

      val rs = ps.executeQuery()

      if (rs.next()) {
        Some(rs.getId(model))
      } else {
        None
      }
    }
  }

  def queryIdByParentId(parentField: RelationField, parentId: IdGCValue)(implicit ec: ExecutionContext): DBIO[Option[IdGCValue]] = {
    queryIdsByParentIds(parentField, Vector(parentId)).map(_.headOption)
  }

  def queryIdsByParentIds(parentField: RelationField, parentIds: Vector[IdGCValue]): DBIO[Vector[IdGCValue]] = {
    val model = parentField.relatedModel_!
    val q: SelectConditionStep[Record1[AnyRef]] = sql
      .select(idField(model))
      .from(modelTable(model))
      .where(parentIdCondition(parentField, parentIds))
    queryToDBIO(q)(
      setParams = pp => parentIds.foreach(pp.setGcValue),
      readResult = rs => rs.as(readNodeId(model))
    )
  }

  def queryIdsByWhereFilter(model: Model, filter: Option[Filter]): DBIO[Vector[IdGCValue]] = {
    val aliasedTable    = modelTable(model).as(topLevelAlias)
    val filterCondition = buildConditionForFilter(filter)
    val query           = sql.select(field(name(topLevelAlias, model.dbNameOfIdField_!))).from(aliasedTable).where(filterCondition)

    queryToDBIO(query)(
      setParams = pp => SetParams.setFilter(pp, filter),
      readResult = rs => rs.as(readNodeId(model))
    )
  }

  def queryIdByParentIdAndWhere(parentField: RelationField, parentId: IdGCValue, where: NodeSelector): DBIO[Option[IdGCValue]] = {
    val model                 = parentField.relatedModel_!
    val nodeSelectorCondition = modelColumn(model, where.field).equal(placeHolder)
    val q: SelectConditionStep[Record1[AnyRef]] = sql
      .select(idField(model))
      .from(modelTable(model))
      .where(parentIdCondition(parentField), nodeSelectorCondition)

    queryToDBIO(q)(
      setParams = { pp =>
        pp.setGcValue(parentId)
        pp.setGcValue(where.fieldGCValue)
      },
      readResult = { rs =>
        if (rs.next()) {
          Some(rs.getId(model))
        } else {
          None
        }
      }
    )
  }

  private def parentIdCondition(parentField: RelationField): Condition = parentIdCondition(parentField, Vector(1))

  private def parentIdCondition(parentField: RelationField, parentIds: Vector[Any]): Condition = {
    val relation      = parentField.relation
    val childIdField  = relationColumn(relation, parentField.oppositeRelationSide)
    val parentIdField = relationColumn(relation, parentField.relationSide)
    val subSelect = sql
      .select(childIdField)
      .from(relationTable(relation))
      .where(parentIdField.in(placeHolders(parentIds)))

    idField(parentField.relatedModel_!).in(subSelect)
  }
}
