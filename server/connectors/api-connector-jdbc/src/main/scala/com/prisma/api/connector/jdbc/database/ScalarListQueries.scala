package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.{QueryArguments, ResolverResult, ScalarListElement, ScalarListValues}
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.gc_values.{StringIdGCValue, IdGCValue, ListGCValue}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Model, ScalarField}

trait ScalarListQueries extends BuilderBase with FilterConditionBuilder with OrderByClauseBuilder with LimitClauseBuilder {
  import slickDatabase.profile.api._

  def getScalarListValues(
      model: Model,
      field: ScalarField,
      args: QueryArguments
  ): DBIO[ResolverResult[ScalarListValues]] = {

    require(field.isList, "This must be called only with scalar list fields")

    lazy val query = {
      val condition    = buildConditionForFilter(args.filter)
      val order        = orderByForScalarListField(topLevelAlias, args)
      val skipAndLimit = LimitClauseHelper.skipAndLimitValues(args)

      val base = sql
        .select()
        .from(scalarListTable(field).as(topLevelAlias))
        .where(condition)
        .orderBy(order: _*)
        .offset(intDummy)

      skipAndLimit.limit match {
        case Some(_) => base.limit(intDummy)
        case None    => base
      }
    }

    queryToDBIO(query)(
      setParams = pp => SetParams.setQueryArgs(pp, args),
      readResult = { rs =>
        val result = rs.readWith(readsScalarListField(field))
        val convertedValues = result
          .groupBy(_.nodeId)
          .map { case (id, values) => ScalarListValues(StringIdGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }
          .toVector

        ResolverResult(convertedValues)
      }
    )
  }

  def getScalarListValuesByNodeIds(field: ScalarField, nodeIds: Vector[IdGCValue]): DBIO[Vector[ScalarListValues]] = {
    require(field.isList, "This must be called only with scalar list fields")

    lazy val query = {
      val nodeIdField   = scalarListColumn(field, nodeIdFieldName)
      val positionField = scalarListColumn(field, positionFieldName)
      val valueField    = scalarListColumn(field, valueFieldName)
      val condition     = nodeIdField.in(Vector.fill(nodeIds.length) { stringDummy }: _*)

      sql
        .select(nodeIdField, positionField, valueField)
        .from(scalarListTable(field))
        .where(condition)
    }

    queryToDBIO(query)(
      setParams = pp => nodeIds.foreach(pp.setGcValue),
      readResult = { rs =>
        val scalarListElements                          = rs.readWith(readsScalarListField(field))
        val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
        grouped.map { case (id, values) => ScalarListValues(StringIdGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }.toVector
      }
    )
  }

}
