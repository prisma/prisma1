package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.SlickExtensions._
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.{GCValue, GCValueExtractor, NullGCValue}
import com.prisma.shared.models.{Field, Model, Relation, Schema}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder

object QueryArgumentsHelpers {

  def generateFilterConditions(projectId: String, tableName: String, filter: Filter, quoteTableName: Boolean = true): Option[SQLActionBuilder] = {

    def getAliasAndTableName(fromModel: String, toModel: String): (String, String) = {
      var modTableName = ""
      if (!tableName.contains("_")) modTableName = projectId + """"."""" + fromModel else modTableName = tableName
      val alias = toModel + "_" + tableName
      (alias, modTableName)
    }

    def relationFilterStatement(schema: Schema,
                                field: Field,
                                fromModel: Model,
                                toModel: Model,
                                relation: Relation,
                                nestedFilter: Filter,
                                relationCondition: RelationCondition) = {
      val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
      val relationTableName     = relation.relationTableNameNew(schema)
      val column                = relation.columnForRelationSide(field.relationSide.get)
      val oppositeColumn        = relation.columnForRelationSide(field.oppositeRelationSide.get)

      val join = sql"""select *
            from "#$projectId"."#${toModel.dbName}" as "#$alias"
            inner join "#$projectId"."#${relationTableName}"
            on "#$alias"."id" = "#$projectId"."#${relationTableName}"."#${oppositeColumn}"
            where "#$projectId"."#${relationTableName}"."#${column}" = "#$modTableName"."id""""

      val nestedFilterStatement = Some(generateFilterConditions(projectId, alias, nestedFilter).getOrElse(sql"True"))

      Some(relationCondition match {
        case AtLeastOneRelatedNode => sql" exists (" ++ join ++ sql"and" ++ nestedFilterStatement ++ sql")"
        case EveryRelatedNode      => sql" not exists (" ++ join ++ sql"and not " ++ nestedFilterStatement ++ sql")"
        case NoRelatedNode         => sql" not exists (" ++ join ++ sql"and" ++ nestedFilterStatement ++ sql")"
        case NoRelationCondition   => sql" exists (" ++ join ++ sql"and" ++ nestedFilterStatement ++ sql")"
      })
    }

    val sqlParts = filter match {
      case NodeSubscriptionFilter() => None
      case AndFilter(filters)       => combineByAnd(filters.map(generateFilterConditions(projectId, tableName, _)).collect { case Some(x) => x })
      case OrFilter(filters)        => combineByOr(filters.map(generateFilterConditions(projectId, tableName, _)).collect { case Some(x) => x })
      case NotFilter(filters)       => combineByNot(filters.map(generateFilterConditions(projectId, tableName, _)).collect { case Some(x) => x })
      case NodeFilter(filters)      => combineByOr(filters.map(generateFilterConditions(projectId, tableName, _)).collect { case Some(x) => x })
      case RelationFilter(schema, field, fromModel, toModel, relation, nestedFilter, condition: RelationCondition) =>
        relationFilterStatement(schema, field, fromModel, toModel, relation, nestedFilter, condition)
      //--- non recursive
      // the boolean filter comes from precomputed fields // this is the computed stuff that we get when replacing mutation_in
      case PreComputedSubscriptionFilter(value) => Some(if (value) sql"TRUE" else sql"FALSE")

      case ScalarFilter(field, Contains(value)) =>
        Some(sql""""#$tableName"."#${field.name}" LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, NotContains(value)) =>
        Some(sql""""#$tableName"."#${field.name}" NOT LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, StartsWith(value)) =>
        Some(sql""""#$tableName"."#${field.name}" LIKE """ ++ escapeUnsafeParam(s"${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, NotStartsWith(value)) =>
        Some(sql""""#$tableName"."#${field.name}" NOT LIKE """ ++ escapeUnsafeParam(s"${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, EndsWith(value)) =>
        Some(sql""""#$tableName"."#${field.name}" LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}"))

      case ScalarFilter(field, NotEndsWith(value)) =>
        Some(sql""""#$tableName"."#${field.name}" NOT LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}"))

      case ScalarFilter(field, LessThan(value))            => Some(sql""""#$tableName"."#${field.name}" < $value""")
      case ScalarFilter(field, GreaterThan(value))         => Some(sql""""#$tableName"."#${field.name}" > $value""")
      case ScalarFilter(field, LessThanOrEquals(value))    => Some(sql""""#$tableName"."#${field.name}" <= $value""")
      case ScalarFilter(field, GreaterThanOrEquals(value)) => Some(sql""""#$tableName"."#${field.name}" >= $value""")
      case ScalarFilter(field, In(Vector(NullGCValue)))    => Some(if (field.isRequired) sql"false" else sql""""#$tableName"."#${field.name}" IS NULL""")
      case ScalarFilter(field, NotIn(Vector(NullGCValue))) => Some(if (field.isRequired) sql"true" else sql""""#$tableName"."#${field.name}" IS NOT NULL""")

      case ScalarFilter(field, In(values)) =>
        Some(if (values.nonEmpty) sql""""#$tableName"."#${field.name}" """ ++ generateInStatement(values) else sql"false")
      case ScalarFilter(field, NotIn(values)) =>
        Some(if (values.nonEmpty) sql""""#$tableName"."#${field.name}" NOT """ ++ generateInStatement(values) else sql"true")

      case ScalarFilter(field, NotEquals(NullGCValue)) => Some(sql""""#$tableName"."#${field.name}" IS NOT NULL""")
      case ScalarFilter(field, NotEquals(value))       => Some(sql""""#$tableName"."#${field.name}" != $value""")
      case ScalarFilter(field, Equals(NullGCValue))    => Some(sql""""#$tableName"."#${field.name}" IS NULL""")
      case ScalarFilter(field, Equals(value))          => Some(sql""""#$tableName"."#${field.name}" = $value""")
      case OneRelationIsNullFilter(schema, field) =>
        if (field.isList) throw APIErrors.FilterCannotBeNullOnToManyField(field.name) //todo move this error up front

        val relation          = field.relation.get
        val relationTableName = relation.relationTableNameNew(schema)
        val column            = relation.columnForRelationSide(field.relationSide.get)

        Some(sql""" not exists (select  *
                                  from    "#$projectId"."#${relationTableName}"
                                  where   "#$projectId"."#${relationTableName}"."#${column}" = "#$tableName"."id")""")
    }

    if (sqlParts.isEmpty) None else combineByAnd(sqlParts)
  }

  def generateInStatement(items: Vector[GCValue]) = sql" IN (" ++ combineByComma(items.map(v => sql"$v")) ++ sql")"
}
