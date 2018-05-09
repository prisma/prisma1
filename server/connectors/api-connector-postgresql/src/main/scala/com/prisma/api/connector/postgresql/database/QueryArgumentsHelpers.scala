package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.SlickExtensions._
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.{GCValue, GCValueExtractor, ListGCValue, NullGCValue}
import com.prisma.shared.models.{Field, Model, Relation}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder

object QueryArgumentsHelpers {

  def generateFilterConditions(projectId: String, tableName: String, filter: Seq[Any], quoteTableName: Boolean = true): Option[SQLActionBuilder] = {

    def getAliasAndTableName(fromModel: String, toModel: String): (String, String) = {
      var modTableName = ""
      if (!tableName.contains("_")) modTableName = projectId + """"."""" + fromModel else modTableName = tableName
      val alias = toModel + "_" + tableName
      (alias, modTableName)
    }

    def filterOnRelation(relationTableName: String, nestedFilter: DataItemFilterCollection) = {
      Some(generateFilterConditions(projectId, relationTableName, nestedFilter).getOrElse(sql"True"))
    }

    def joinRelations(relation: Relation, toModel: Model, alias: String, field: Field, modTableName: String) = {
      sql"""select *
            from "#$projectId"."#${toModel.name}" as "#$alias"
            inner join "#$projectId"."#${relation.relationTableName}"
            on "#$alias"."id" = "#$projectId"."#${relation.relationTableName}"."#${field.oppositeRelationSide.get}"
            where "#$projectId"."#${relation.relationTableName}"."#${field.relationSide.get}" = "#$modTableName"."id""""
    }

    val tableNameSql = if (quoteTableName) sql""""#$tableName"""" else sql"""#$tableName"""

    //key, value, field, filterName, relationFilter
    val sqlParts = filter
      .map {
        case FilterElement(key, None, Some(field), filterName) =>
          None

        //combinationFilters

        case FilterElement(key, value, None, filterName) if filterName == "AND" =>
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect { case Some(x) => x }

          combineByAnd(values)

        case FilterElement(key, value, None, filterName) if filterName == "OR" =>
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect { case Some(x) => x }

          combineByOr(values)

        case FilterElement(key, value, None, filterName) if filterName == "NOT" =>
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect { case Some(x) => x }

          combineByNot(values)
        case FilterElement(key, value, None, filterName) if filterName == "node" =>
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect { case Some(x) => x }

          combineByOr(values)

        //transitive filters

        case TransitiveRelationFilter(field, fromModel, toModel, relation, filterName, nestedFilter) if filterName == "_some" =>
          val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
          Some(sql"exists (" ++ joinRelations(relation, toModel, alias, field, modTableName) ++ sql"and" ++ filterOnRelation(alias, nestedFilter) ++ sql")")

        case TransitiveRelationFilter(field, fromModel, toModel, relation, filterName, nestedFilter) if filterName == "_every" =>
          val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
          Some(
            sql"not exists (" ++ joinRelations(relation, toModel, alias, field, modTableName) ++ sql"and not" ++ filterOnRelation(alias, nestedFilter) ++ sql")")

        case TransitiveRelationFilter(field, fromModel, toModel, relation, filterName, nestedFilter) if filterName == "_none" =>
          val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
          Some(
            sql"not exists (" ++ joinRelations(relation, toModel, alias, field, modTableName) ++ sql"and " ++ filterOnRelation(alias, nestedFilter) ++ sql")")

        case TransitiveRelationFilter(field, fromModel, toModel, relation, filterName, nestedFilter) if filterName == "" =>
          val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
          Some(sql"exists (" ++ joinRelations(relation, toModel, alias, field, modTableName) ++ sql"and" ++ filterOnRelation(alias, nestedFilter) ++ sql")")

        //--- non recursive

        // the boolean filter comes from precomputed fields
        case FilterElement(key, value, None, filterName) if filterName == "boolean" => // todo probably useless
          value match {
            case true  => Some(sql"TRUE")
            case false => Some(sql"FALSE")
          }

        case FinalValueFilter(key, value, field, filterName) if filterName == "_contains" =>
          Some(tableNameSql ++ sql"""."#${field.name}" LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}%"))

        case FinalValueFilter(key, value, field, filterName) if filterName == "_not_contains" =>
          Some(tableNameSql ++ sql"""."#${field.name}" NOT LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}%"))

        case FinalValueFilter(key, value, field, filterName) if filterName == "_starts_with" =>
          Some(tableNameSql ++ sql"""."#${field.name}" LIKE """ ++ escapeUnsafeParam(s"${GCValueExtractor.fromGCValue(value)}%"))

        case FinalValueFilter(key, value, field, filterName) if filterName == "_not_starts_with" =>
          Some(tableNameSql ++ sql"""."#${field.name}" NOT LIKE """ ++ escapeUnsafeParam(s"${GCValueExtractor.fromGCValue(value)}%"))

        case FinalValueFilter(key, value, field, filterName) if filterName == "_ends_with" =>
          Some(tableNameSql ++ sql"""."#${field.name}" LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}"))

        case FinalValueFilter(key, value, field, filterName) if filterName == "_not_ends_with" =>
          Some(tableNameSql ++ sql"""."#${field.name}" NOT LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}"))

        case FinalValueFilter(key, value, field, filterName) if filterName == "_lt" =>
          Some(tableNameSql ++ sql"""."#${field.name}" < $value""")

        case FinalValueFilter(key, value, field, filterName) if filterName == "_gt" =>
          Some(tableNameSql ++ sql"""."#${field.name}" > $value""")

        case FinalValueFilter(key, value, field, filterName) if filterName == "_lte" =>
          Some(tableNameSql ++ sql"""."#${field.name}" <= $value""")

        case FinalValueFilter(key, value, field, filterName) if filterName == "_gte" =>
          Some(tableNameSql ++ sql"""."#${field.name}" >= $value""")

        case FinalValueFilter(key, NullGCValue, field, filterName) if filterName == "_in" =>
          Some(sql"false")

        case FinalValueFilter(key, ListGCValue(values), field, filterName) if filterName == "_in" =>
          values.nonEmpty match {
            case true  => Some(tableNameSql ++ sql"""."#${field.name}" """ ++ generateInStatement(values))
            case false => Some(sql"false")
          }

        case FinalValueFilter(key, NullGCValue, field, filterName) if filterName == "_not_in" =>
          Some(sql"false")

        case FinalValueFilter(key, ListGCValue(values), field, filterName) if filterName == "_not_in" =>
          values.nonEmpty match {
            case true  => Some(tableNameSql ++ sql"""."#${field.name}" NOT """ ++ generateInStatement(values))
            case false => Some(sql"true")
          }

        case FinalValueFilter(key, NullGCValue, field, filterName) if filterName == "_not" =>
          Some(tableNameSql ++ sql"""."#${field.name}" IS NOT NULL""")

        case FinalValueFilter(key, value, field, filterName) if filterName == "_not" =>
          Some(tableNameSql ++ sql"""."#${field.name}" != $value""")

        case FinalValueFilter(key, NullGCValue, field, filterName) =>
          Some(tableNameSql ++ sql"""."#$key" IS NULL""")

        case FinalValueFilter(key, value, field, filterName) =>
          Some(tableNameSql ++ sql"""."#$key" = $value""")
        case FinalRelationFilter(key, null, field, filterName) =>
          if (field.isList) throw APIErrors.FilterCannotBeNullOnToManyField(field.name)

          Some(sql""" not exists (select  *
                                  from    "#$projectId"."#${field.relation.get.relationTableName}"
                                  where   "#$projectId"."#${field.relation.get.relationTableName}"."#${field.relationSide.get}" = """ ++ tableNameSql ++ sql""".id
                                  )""")

        // this is used for the node: {} field in the Subscription Filter
        case values: Seq[FilterElement @unchecked] => generateFilterConditions(projectId, tableName, values)
      }
      .filter(_.nonEmpty)
      .map(_.get)

    if (sqlParts.isEmpty) None else combineByAnd(sqlParts)
  }

  def generateInStatement(items: Vector[GCValue]) = sql" IN (" ++ combineByComma(items.map(v => sql"$v")) ++ sql")"

}
