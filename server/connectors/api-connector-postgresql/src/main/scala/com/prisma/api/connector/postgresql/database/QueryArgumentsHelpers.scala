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

    def filterOnRelation(relationTableName: String, nestedFilter: Filter) = {
      Some(generateFilterConditions(projectId, relationTableName, nestedFilter).getOrElse(sql"True"))
    }

    def joinRelations(schema: Schema, relation: Relation, toModel: Model, alias: String, field: Field, modTableName: String) = {
      val relationTableName = relation.relationTableNameNew(schema)
      val column            = relation.columnForRelationSide(field.relationSide.get)
      val oppositeColumn    = relation.columnForRelationSide(field.oppositeRelationSide.get)
      sql"""select *
            from "#$projectId"."#${toModel.dbName}" as "#$alias"
            inner join "#$projectId"."#${relationTableName}"
            on "#$alias"."id" = "#$projectId"."#${relationTableName}"."#${oppositeColumn}"
            where "#$projectId"."#${relationTableName}"."#${column}" = "#$modTableName"."id""""
    }

    val tableNameSql = if (quoteTableName) sql""""#$tableName"""" else sql"""#$tableName"""

    //key, value, field, filterName, relationFilter
    val sqlParts = filter match {
      // this is used for the node: {} field in the Subscription Filter
      case FilterElement(key, None, Some(field), filterName) => None

      //combinationFilters

      case AndFilter(filters) => combineByAnd(filters.map(generateFilterConditions(projectId, tableName, _)).collect { case Some(x) => x })

      case OrFilter(filters) => combineByOr(filters.map(generateFilterConditions(projectId, tableName, _)).collect { case Some(x) => x })

      case NotFilter(filters) => combineByNot(filters.map(generateFilterConditions(projectId, tableName, _)).collect { case Some(x) => x })

      case NodeFilter(filters) => combineByOr(filters.map(generateFilterConditions(projectId, tableName, _)).collect { case Some(x) => x })

      //transitive filters

      case RelationFilter(schema, field, fromModel, toModel, relation, nestedFilter, AtLeastOneRelatedNode) =>
        val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
        Some(
          sql"exists (" ++ joinRelations(schema, relation, toModel, alias, field, modTableName) ++ sql"and" ++ filterOnRelation(alias, nestedFilter) ++ sql")")

      case RelationFilter(schema, field, fromModel, toModel, relation, nestedFilter, EveryRelatedNode) =>
        val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
        Some(
          sql"not exists (" ++ joinRelations(schema, relation, toModel, alias, field, modTableName) ++ sql"and not" ++ filterOnRelation(alias, nestedFilter) ++ sql")")

      case RelationFilter(schema, field, fromModel, toModel, relation, nestedFilter, NoRelatedNode) =>
        val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
        Some(
          sql"not exists (" ++ joinRelations(schema, relation, toModel, alias, field, modTableName) ++ sql"and " ++ filterOnRelation(alias, nestedFilter) ++ sql")")

      case RelationFilter(schema, field, fromModel, toModel, relation, nestedFilter, NoRelationCondition) =>
        val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
        Some(
          sql"exists (" ++ joinRelations(schema, relation, toModel, alias, field, modTableName) ++ sql"and" ++ filterOnRelation(alias, nestedFilter) ++ sql")")

      //--- non recursive

      // the boolean filter comes from precomputed fields // this is the computed stuff that we get when replacing mutation_in
      case FilterElement(key, value, None, filterName) if filterName == "boolean" =>
        Some(if (value == true) sql"TRUE" else sql"FALSE")

      case ScalarFilter(field, Contains(value)) =>
        Some(tableNameSql ++ sql"""."#${field.name}" LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, NotContains(value)) =>
        Some(tableNameSql ++ sql"""."#${field.name}" NOT LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, StartsWith(value)) =>
        Some(tableNameSql ++ sql"""."#${field.name}" LIKE """ ++ escapeUnsafeParam(s"${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, NotStartsWith(value)) =>
        Some(tableNameSql ++ sql"""."#${field.name}" NOT LIKE """ ++ escapeUnsafeParam(s"${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, EndsWith(value)) =>
        Some(tableNameSql ++ sql"""."#${field.name}" LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}"))

      case ScalarFilter(field, NotEndsWith(value)) =>
        Some(tableNameSql ++ sql"""."#${field.name}" NOT LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}"))

      case ScalarFilter(field, LessThan(value)) => Some(tableNameSql ++ sql"""."#${field.name}" < $value""")

      case ScalarFilter(field, GreaterThan(value)) => Some(tableNameSql ++ sql"""."#${field.name}" > $value""")

      case ScalarFilter(field, LessThanOrEquals(value)) => Some(tableNameSql ++ sql"""."#${field.name}" <= $value""")

      case ScalarFilter(field, GreaterThanOrEquals(value)) => Some(tableNameSql ++ sql"""."#${field.name}" >= $value""")

      case ScalarFilter(field, In(Vector(NullGCValue))) => Some(if (field.isRequired) sql"false" else tableNameSql ++ sql"""."#${field.name}" IS NULL""")

      case ScalarFilter(field, In(values)) =>
        Some(if (values.nonEmpty) tableNameSql ++ sql"""."#${field.name}" """ ++ generateInStatement(values) else sql"false")

      case ScalarFilter(field, NotIn(Vector(NullGCValue))) => Some(if (field.isRequired) sql"true" else tableNameSql ++ sql"""."#${field.name}" IS NOT NULL""")

      case ScalarFilter(field, NotIn(values)) =>
        Some(if (values.nonEmpty) tableNameSql ++ sql"""."#${field.name}" NOT """ ++ generateInStatement(values) else sql"true")

      case ScalarFilter(field, NotEquals(NullGCValue)) => Some(tableNameSql ++ sql"""."#${field.name}" IS NOT NULL""")

      case ScalarFilter(field, NotEquals(value)) => Some(tableNameSql ++ sql"""."#${field.name}" != $value""")

      case ScalarFilter(field, Equals(NullGCValue)) => Some(tableNameSql ++ sql"""."#${field.name}" IS NULL""")

      case ScalarFilter(field, Equals(value)) => Some(tableNameSql ++ sql"""."#${field.name}" = $value""")

      case OneRelationIsNullFilter(schema, field) =>
        if (field.isList) throw APIErrors.FilterCannotBeNullOnToManyField(field.name) //todo move this error up front

        val relation          = field.relation.get
        val relationTableName = relation.relationTableNameNew(schema)
        val column            = relation.columnForRelationSide(field.relationSide.get)

        Some(sql""" not exists (select  *
                                  from    "#$projectId"."#${relationTableName}"
                                  where   "#$projectId"."#${relationTableName}"."#${column}" = """ ++ tableNameSql ++ sql""".id
                                  )""")
    }

    if (sqlParts.isEmpty) None else combineByAnd(sqlParts)
  }

  def generateInStatement(items: Vector[GCValue]) = sql" IN (" ++ combineByComma(items.map(v => sql"$v")) ++ sql")"

}
