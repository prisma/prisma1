package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.SlickExtensions._
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.{GCValue, GCValueExtractor, ListGCValue, NullGCValue}
import com.prisma.shared.models.{Field, Model, Relation, Schema}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder

object QueryArgumentsHelpers {

  def generateFilterConditions(projectId: String, tableName: String, filter: Seq[Any], quoteTableName: Boolean = true): Option[SQLActionBuilder] = {

    def getAliasAndTableName(fromModel: String, toModel: String): (String, String) = {
      val modTableName = if (!tableName.contains("_")) {
        projectId + """"."""" + fromModel
      } else {
        tableName
      }
      val alias = toModel + "_" + tableName
      (alias, modTableName)
    }

    def filterOnRelation(relationTableName: String, nestedFilter: DataItemFilterCollection) = {
      Some(generateFilterConditions(projectId, relationTableName, nestedFilter).getOrElse(sql"True"))
    }

    def joinRelations(schema: Schema, relation: Relation, toModel: Model, alias: String, field: Field, fromModel: Model, modTableName: String) = {
      val relationTableName = relation.relationTableName
      val column            = relation.columnForRelationSide(field.relationSide.get)
      val oppositeColumn    = relation.columnForRelationSide(field.oppositeRelationSide.get)
      sql"""select *
            from "#$projectId"."#${toModel.dbName}" as "#$alias"
            inner join "#$projectId"."#${relationTableName}"
            on "#$alias"."#${toModel.dbNameOfIdField_!}" = "#$projectId"."#${relationTableName}"."#${oppositeColumn}"
            where "#$projectId"."#${relationTableName}"."#${column}" = "#$modTableName"."#${fromModel.dbNameOfIdField_!}""""
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

        case TransitiveRelationFilter(schema, field, fromModel, toModel, relation, filterName, nestedFilter) if filterName == "_some" =>
          val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
          Some(
            sql"exists (" ++ joinRelations(schema, relation, toModel, alias, field, fromModel, modTableName) ++ sql"and" ++ filterOnRelation(
              alias,
              nestedFilter) ++ sql")")

        case TransitiveRelationFilter(schema, field, fromModel, toModel, relation, filterName, nestedFilter) if filterName == "_every" =>
          val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
          Some(
            sql"not exists (" ++ joinRelations(schema, relation, toModel, alias, field, fromModel, modTableName) ++ sql"and not" ++ filterOnRelation(
              alias,
              nestedFilter) ++ sql")")

        case TransitiveRelationFilter(schema, field, fromModel, toModel, relation, filterName, nestedFilter) if filterName == "_none" =>
          val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
          Some(
            sql"not exists (" ++ joinRelations(schema, relation, toModel, alias, field, fromModel, modTableName) ++ sql"and " ++ filterOnRelation(
              alias,
              nestedFilter) ++ sql")")

        case TransitiveRelationFilter(schema, field, fromModel, toModel, relation, filterName, nestedFilter) if filterName == "" =>
          val (alias, modTableName) = getAliasAndTableName(fromModel.name, toModel.name)
          Some(
            sql"exists (" ++ joinRelations(schema, relation, toModel, alias, field, fromModel, modTableName) ++ sql"and" ++ filterOnRelation(
              alias,
              nestedFilter) ++ sql")")

        //--- non recursive

        // the boolean filter comes from precomputed fields
        case FilterElement(key, value, None, filterName) if filterName == "boolean" => // todo probably useless
          value match {
            case true  => Some(sql"TRUE")
            case false => Some(sql"FALSE")
          }

        case FinalValueFilter(_, value, field, filterName) if filterName == "_contains" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}%"))

        case FinalValueFilter(_, value, field, filterName) if filterName == "_not_contains" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" NOT LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}%"))

        case FinalValueFilter(_, value, field, filterName) if filterName == "_starts_with" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" LIKE """ ++ escapeUnsafeParam(s"${GCValueExtractor.fromGCValue(value)}%"))

        case FinalValueFilter(_, value, field, filterName) if filterName == "_not_starts_with" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" NOT LIKE """ ++ escapeUnsafeParam(s"${GCValueExtractor.fromGCValue(value)}%"))

        case FinalValueFilter(_, value, field, filterName) if filterName == "_ends_with" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}"))

        case FinalValueFilter(_, value, field, filterName) if filterName == "_not_ends_with" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" NOT LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}"))

        case FinalValueFilter(_, value, field, filterName) if filterName == "_lt" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" < $value""")

        case FinalValueFilter(_, value, field, filterName) if filterName == "_gt" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" > $value""")

        case FinalValueFilter(_, value, field, filterName) if filterName == "_lte" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" <= $value""")

        case FinalValueFilter(_, value, field, filterName) if filterName == "_gte" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" >= $value""")

        case FinalValueFilter(_, NullGCValue, field, filterName) if filterName == "_in" =>
          Some(sql"false")

        case FinalValueFilter(_, ListGCValue(values), field, filterName) if filterName == "_in" =>
          values.nonEmpty match {
            case true  => Some(tableNameSql ++ sql"""."#${field.dbName}" """ ++ generateInStatement(values))
            case false => Some(sql"false")
          }

        case FinalValueFilter(_, NullGCValue, field, filterName) if filterName == "_not_in" =>
          Some(sql"false")

        case FinalValueFilter(_, ListGCValue(values), field, filterName) if filterName == "_not_in" =>
          values.nonEmpty match {
            case true  => Some(tableNameSql ++ sql"""."#${field.dbName}" NOT """ ++ generateInStatement(values))
            case false => Some(sql"true")
          }

        case FinalValueFilter(_, NullGCValue, field, filterName) if filterName == "_not" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" IS NOT NULL""")

        case FinalValueFilter(_, value, field, filterName) if filterName == "_not" =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" != $value""")

        case FinalValueFilter(_, NullGCValue, field, filterName) =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" IS NULL""")

        case FinalValueFilter(_, value, field, filterName) =>
          Some(tableNameSql ++ sql"""."#${field.dbName}" = $value""")

        case FinalRelationFilter(schema, key, null, field, filterName) =>
          if (field.isList) throw APIErrors.FilterCannotBeNullOnToManyField(field.name)

          val relation          = field.relation.get
          val relationTableName = relation.relationTableName
          val column            = relation.columnForRelationSide(field.relationSide.get)
          // fixme: an ugly hack that is hard to explain. ask marcus.
          val otherIdColumn = schema.models.find(_.dbName == tableName) match {
            case Some(model) => model.idField_!.dbName
            case None        => "id"
          }

          Some(sql""" not exists (select  *
                                  from    "#$projectId"."#${relationTableName}"
                                  where   "#$projectId"."#${relationTableName}"."#${column}" = """ ++ tableNameSql ++ sql"""."#$otherIdColumn"
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
