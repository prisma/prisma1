package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
import com.prisma.gc_values.{GCValue, GCValueExtractor, NullGCValue}
import com.prisma.shared.models.Field
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder

object PostgresQueryArgumentsHelpers {

  def generateFilterConditions(projectId: String, alias: String, modelName: String, filter: Filter): Option[SQLActionBuilder] = {

    def getAliasAndTableName(fromModel: String, toModel: String): (String, String) = {
      val modTableName = if (!alias.contains("_")) projectId + """"."""" + fromModel else alias
      val newAlias     = toModel + "_" + alias
      (newAlias, modTableName)
    }

    def relationFilterStatement(field: Field, nestedFilter: Filter, relationCondition: RelationCondition) = {
      val (alias, modTableName) = getAliasAndTableName(field.model.name, field.relatedModel_!.name)

      val relationTableName = field.relation_!.relationTableName
      val column            = field.relation_!.columnForRelationSide(field.relationSide.get)
      val oppositeColumn    = field.relation_!.columnForRelationSide(field.oppositeRelationSide.get)

      val join = sql"""select *
            from "#$projectId"."#${field.relatedModel_!.dbName}" as "#$alias"
            inner join "#$projectId"."#${relationTableName}"
            on "#$alias"."#${field.relatedModel_!.dbNameOfIdField_!}" = "#$projectId"."#${relationTableName}"."#${oppositeColumn}"
            where "#$projectId"."#${relationTableName}"."#${column}" = "#$modTableName"."#${field.model.dbNameOfIdField_!}""""

      val nestedFilterStatement = Some(generateFilterConditions(projectId, alias, modelName, nestedFilter).getOrElse(sql"True"))

      Some(relationCondition match {
        case AtLeastOneRelatedNode => sql" exists (" ++ join ++ sql"and" ++ nestedFilterStatement ++ sql")"
        case EveryRelatedNode      => sql" not exists (" ++ join ++ sql"and not " ++ nestedFilterStatement ++ sql")"
        case NoRelatedNode         => sql" not exists (" ++ join ++ sql"and" ++ nestedFilterStatement ++ sql")"
        case NoRelationCondition   => sql" exists (" ++ join ++ sql"and" ++ nestedFilterStatement ++ sql")"
      })
    }

    val sqlParts = filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter()                                          => None
      case AndFilter(filters)                                                => combineByAnd(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case OrFilter(filters)                                                 => combineByOr(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case NotFilter(filters)                                                => combineByNot(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case NodeFilter(filters)                                               => combineByOr(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case RelationFilter(field, nestedFilter, condition: RelationCondition) => relationFilterStatement(field, nestedFilter, condition)
      //--------------------------------ANCHORS------------------------------------
      case PreComputedSubscriptionFilter(value) => Some(if (value) sql"TRUE" else sql"FALSE")

      case ScalarFilter(field, Contains(value)) =>
        Some(sql""""#$alias"."#${field.dbName}" LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, NotContains(value)) =>
        Some(sql""""#$alias"."#${field.dbName}" NOT LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, StartsWith(value)) =>
        Some(sql""""#$alias"."#${field.dbName}" LIKE """ ++ escapeUnsafeParam(s"${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, NotStartsWith(value)) =>
        Some(sql""""#$alias"."#${field.dbName}" NOT LIKE """ ++ escapeUnsafeParam(s"${GCValueExtractor.fromGCValue(value)}%"))

      case ScalarFilter(field, EndsWith(value)) =>
        Some(sql""""#$alias"."#${field.dbName}" LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}"))

      case ScalarFilter(field, NotEndsWith(value)) =>
        Some(sql""""#$alias"."#${field.dbName}" NOT LIKE """ ++ escapeUnsafeParam(s"%${GCValueExtractor.fromGCValue(value)}"))

      case ScalarFilter(field, LessThan(value))            => Some(sql""""#$alias"."#${field.dbName}" < $value""")
      case ScalarFilter(field, GreaterThan(value))         => Some(sql""""#$alias"."#${field.dbName}" > $value""")
      case ScalarFilter(field, LessThanOrEquals(value))    => Some(sql""""#$alias"."#${field.dbName}" <= $value""")
      case ScalarFilter(field, GreaterThanOrEquals(value)) => Some(sql""""#$alias"."#${field.dbName}" >= $value""")
      case ScalarFilter(field, In(Vector(NullGCValue)))    => Some(if (field.isRequired) sql"false" else sql""""#$alias"."#${field.dbName}" IS NULL""")
      case ScalarFilter(field, NotIn(Vector(NullGCValue))) => Some(if (field.isRequired) sql"true" else sql""""#$alias"."#${field.dbName}" IS NOT NULL""")
      case ScalarFilter(field, In(values))                 => Some(if (values.nonEmpty) sql""""#$alias"."#${field.dbName}" """ ++ in(values) else sql"false")
      case ScalarFilter(field, NotIn(values))              => Some(if (values.nonEmpty) sql""""#$alias"."#${field.dbName}" NOT """ ++ in(values) else sql"true")
      case ScalarFilter(field, NotEquals(NullGCValue))     => Some(sql""""#$alias"."#${field.dbName}" IS NOT NULL""")
      case ScalarFilter(field, NotEquals(value))           => Some(sql""""#$alias"."#${field.dbName}" != $value""")
      case ScalarFilter(field, Equals(NullGCValue))        => Some(sql""""#$alias"."#${field.dbName}" IS NULL""")
      case ScalarFilter(field, Equals(value))              => Some(sql""""#$alias"."#${field.dbName}" = $value""")
      case OneRelationIsNullFilter(field) =>
        val relation          = field.relation_!
        val relationTableName = relation.relationTableName
        val column            = relation.columnForRelationSide(field.relationSide.get)
        val otherIdColumn     = field.relatedModel_!.dbNameOfIdField_!

        Some(sql""" not exists (select  *
                                  from    "#$projectId"."#${relationTableName}"
                                  where   "#$projectId"."#${relationTableName}"."#${column}" = "#$alias"."#$otherIdColumn")""")
    }

    if (sqlParts.isEmpty) None else combineByAnd(sqlParts)
  }

  def in(items: Vector[GCValue]) = sql" IN (" ++ combineByComma(items.map(v => sql"$v")) ++ sql")"
}
