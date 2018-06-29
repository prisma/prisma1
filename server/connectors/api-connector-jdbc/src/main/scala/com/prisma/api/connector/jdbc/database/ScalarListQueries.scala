package com.prisma.api.connector.jdbc.database

import java.sql.ResultSet

import com.prisma.api.connector.{QueryArguments, ResolverResult, ScalarListElement, ScalarListValues}
import com.prisma.gc_values.{CuidGCValue, IdGCValue, ListGCValue}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Model, ScalarField}
import slick.jdbc.PositionedParameters

trait ScalarListQueries extends BuilderBase {
  import slickDatabase.profile.api._

  def getScalarListValues(
      model: Model,
      field: ScalarField,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[ScalarListValues]] = {

    SimpleDBIO[ResolverResult[ScalarListValues]] { ctx =>
      val builder = ScalarListQueryBuilder(slickDatabase, schemaName, field, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()

      val result = rs.as(readsScalarListField(field))

      val convertedValues = result
        .groupBy(_.nodeId)
        .map { case (id, values) => ScalarListValues(CuidGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }
        .toVector

      ResolverResult(convertedValues)
    }
  }

  def getScalarListValuesByNodeIds(modelName: String, field: ScalarField, nodeIds: Vector[IdGCValue]): DBIO[Vector[ScalarListValues]] = {
    SimpleDBIO[Vector[ScalarListValues]] { ctx =>
      val builder = ScalarListByUniquesQueryBuilder(slickDatabase, schemaName, field, nodeIds)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      val pp      = new PositionedParameters(ps)
      nodeIds.foreach(pp.setGcValue)

      val rs                 = ps.executeQuery()
      val scalarListElements = rs.as(readsScalarListField(field))

      val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
      grouped.map { case (id, values) => ScalarListValues(CuidGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }.toVector
    }
  }

}
