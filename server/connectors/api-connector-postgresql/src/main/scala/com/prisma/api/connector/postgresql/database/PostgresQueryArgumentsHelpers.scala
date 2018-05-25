package com.prisma.api.connector.postgresql.database

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
import com.prisma.gc_values.{GCValue, GCValueExtractor, NullGCValue}
import com.prisma.shared.models.{Field, RelationField}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder

object PostgresQueryArgumentsHelpers {

  def generateFilterConditions(projectId: String, alias: String, modelName: String, filter: Filter): Option[SQLActionBuilder] = {

    def getAliasAndTableName(fromModel: String, toModel: String): (String, String) = {
      val modTableName = if (!alias.contains("_")) projectId + """"."""" + fromModel else alias
      val newAlias     = toModel + "_" + alias
      (newAlias, modTableName)
    }

    def relationFilterStatement(field: RelationField, nestedFilter: Filter, relationCondition: RelationCondition) = {
      val (alias, modTableName) = getAliasAndTableName(field.model.name, field.relatedModel_!.name)

      val relationTableName = field.relation.relationTableName
      val column            = field.relation.columnForRelationSide(field.relationSide)
      val oppositeColumn    = field.relation.columnForRelationSide(field.oppositeRelationSide)

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

    def valueOf(field: Field): SQLActionBuilder = sql""""#$alias"."#${field.dbName}" """

    val sqlParts = filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter()                       => None
      case AndFilter(filters)                             => combineByAnd(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case OrFilter(filters)                              => combineByOr(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case NotFilter(filters)                             => combineByNot(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case NodeFilter(filters)                            => combineByOr(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case RelationFilter(field, nestedFilter, condition) => relationFilterStatement(field, nestedFilter, condition)
      //--------------------------------ANCHORS------------------------------------
      case PreComputedSubscriptionFilter(value)            => Some(if (value) sql"TRUE" else sql"FALSE")
      case ScalarFilter(field, Contains(value))            => Some(valueOf(field) ++ sql""" LIKE """ ++ escapeUnsafe(s"%${GCValueExtractor.fromGCValue(value)}%"))
      case ScalarFilter(field, NotContains(value))         => Some(valueOf(field) ++ sql""" NOT LIKE """ ++ escapeUnsafe(s"%${GCValueExtractor.fromGCValue(value)}%"))
      case ScalarFilter(field, StartsWith(value))          => Some(valueOf(field) ++ sql""" LIKE """ ++ escapeUnsafe(s"${GCValueExtractor.fromGCValue(value)}%"))
      case ScalarFilter(field, NotStartsWith(value))       => Some(valueOf(field) ++ sql""" NOT LIKE """ ++ escapeUnsafe(s"${GCValueExtractor.fromGCValue(value)}%"))
      case ScalarFilter(field, EndsWith(value))            => Some(valueOf(field) ++ sql""" LIKE """ ++ escapeUnsafe(s"%${GCValueExtractor.fromGCValue(value)}"))
      case ScalarFilter(field, NotEndsWith(value))         => Some(valueOf(field) ++ sql""" NOT LIKE """ ++ escapeUnsafe(s"%${GCValueExtractor.fromGCValue(value)}"))
      case ScalarFilter(field, LessThan(value))            => Some(valueOf(field) ++ sql""" < $value""")
      case ScalarFilter(field, GreaterThan(value))         => Some(valueOf(field) ++ sql""" > $value""")
      case ScalarFilter(field, LessThanOrEquals(value))    => Some(valueOf(field) ++ sql""" <= $value""")
      case ScalarFilter(field, GreaterThanOrEquals(value)) => Some(valueOf(field) ++ sql""" >= $value""")
      case ScalarFilter(field, NotEquals(NullGCValue))     => Some(valueOf(field) ++ sql""" IS NOT NULL""")
      case ScalarFilter(field, NotEquals(value))           => Some(valueOf(field) ++ sql""" != $value""")
      case ScalarFilter(field, Equals(NullGCValue))        => Some(valueOf(field) ++ sql""" IS NULL""")
      case ScalarFilter(field, Equals(value))              => Some(valueOf(field) ++ sql""" = $value""")
      case ScalarFilter(field, In(Vector(NullGCValue)))    => Some(if (field.isRequired) sql"false" else valueOf(field) ++ sql""" IS NULL""")
      case ScalarFilter(field, NotIn(Vector(NullGCValue))) => Some(if (field.isRequired) sql"true" else valueOf(field) ++ sql""" IS NOT NULL""")
      case ScalarFilter(field, In(values))                 => Some(if (values.nonEmpty) valueOf(field) ++ in(values) else sql"false")
      case ScalarFilter(field, NotIn(values))              => Some(if (values.nonEmpty) valueOf(field) ++ sql""" NOT """ ++ in(values) else sql"true")
      case OneRelationIsNullFilter(field) =>
        val relation          = field.relation
        val relationTableName = relation.relationTableName
        val column            = relation.columnForRelationSide(field.relationSide)
        val otherIdColumn     = field.relatedModel_!.dbNameOfIdField_!

        Some(sql""" not exists (select  *
                                  from    "#$projectId"."#${relationTableName}"
                                  where   "#$projectId"."#${relationTableName}"."#${column}" = "#$alias"."#$otherIdColumn")""")
    }

    if (sqlParts.isEmpty) None else combineByAnd(sqlParts)
  }

  def in(items: Vector[GCValue]) = sql" IN (" ++ combineByComma(items.map(v => sql"$v")) ++ sql")"
}
