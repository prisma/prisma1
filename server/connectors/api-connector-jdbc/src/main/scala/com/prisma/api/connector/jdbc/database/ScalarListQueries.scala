package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.{QueryArguments, ResolverResult, ScalarListElement, ScalarListValues}
import com.prisma.gc_values.{CuidGCValue, IdGCValue, ListGCValue}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Model, ScalarField}

trait ScalarListQueries extends BuilderBase {
  import slickDatabase.profile.api._

  def getScalarListValues(
      model: Model,
      field: ScalarField,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[ScalarListValues]] = {
    val builder = ScalarListQueryBuilder(slickDatabase, schemaName, field, args)
    queryToDBIO(builder.query)(
      setParams = pp => SetParams.setQueryArgs(pp, args),
      readResult = { rs =>
        val result = rs.readWith(readsScalarListField(field))
        val convertedValues = result
          .groupBy(_.nodeId)
          .map { case (id, values) => ScalarListValues(CuidGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }
          .toVector

        ResolverResult(convertedValues)
      }
    )
  }

  def getScalarListValuesByNodeIds(modelName: String, field: ScalarField, nodeIds: Vector[IdGCValue]): DBIO[Vector[ScalarListValues]] = {
    val builder = ScalarListByUniquesQueryBuilder(slickDatabase, schemaName, field, nodeIds)
    queryToDBIO(builder.query)(
      setParams = pp => nodeIds.foreach(pp.setGcValue),
      readResult = { rs =>
        val scalarListElements                          = rs.readWith(readsScalarListField(field))
        val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
        grouped.map { case (id, values) => ScalarListValues(CuidGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }.toVector
      }
    )
  }

}
