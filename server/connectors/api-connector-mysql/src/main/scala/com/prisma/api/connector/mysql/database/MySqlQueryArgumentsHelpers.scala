package com.prisma.api.connector.mysql.database

import com.prisma.api.connector._
import com.prisma.api.connector.mysql.database.MySqlSlickExtensions._
import com.prisma.gc_values.{GCValue, GCValueExtractor, NullGCValue}
import com.prisma.shared.models.{Field, Model, Relation, Schema}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.SQLActionBuilder

object MySqlQueryArgumentsHelpers {

  def generateFilterConditions(projectId: String, alias: String, modelName: String, filter: Filter): Option[SQLActionBuilder] = {

    def getAliasAndTableName(fromModel: String, toModel: String): (String, String) = {
      val modTableName = if (!alias.contains("_")) projectId + """"."""" + fromModel else alias
      val newAlias     = toModel + "_" + alias
      (newAlias, modTableName)
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
      val column                = relation.columnForRelationSide(schema, field.relationSide.get)
      val oppositeColumn        = relation.columnForRelationSide(schema, field.oppositeRelationSide.get)

      val join = sql"""select *
            from "#$projectId"."#${toModel.dbName}" as "#$alias"
            inner join "#$projectId"."#${relationTableName}"
            on "#$alias"."id" = "#$projectId"."#${relationTableName}"."#${oppositeColumn}"
            where "#$projectId"."#${relationTableName}"."#${column}" = "#$modTableName"."id""""

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
      case NodeSubscriptionFilter() => None
      case AndFilter(filters)       => combineByAnd(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case OrFilter(filters)        => combineByOr(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case NotFilter(filters)       => combineByNot(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case NodeFilter(filters)      => combineByOr(filters.map(generateFilterConditions(projectId, alias, modelName, _)).collect { case Some(x) => x })
      case RelationFilter(schema, field, fromModel, toModel, relation, nestedFilter, condition: RelationCondition) =>
        relationFilterStatement(schema, field, fromModel, toModel, relation, nestedFilter, condition)
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

      case ScalarFilter(field, In(values)) =>
        Some(if (values.nonEmpty) sql""""#$alias"."#${field.dbName}" """ ++ generateInStatement(values) else sql"false")
      case ScalarFilter(field, NotIn(values)) =>
        Some(if (values.nonEmpty) sql""""#$alias"."#${field.dbName}" NOT """ ++ generateInStatement(values) else sql"true")

      case ScalarFilter(field, NotEquals(NullGCValue)) => Some(sql""""#$alias"."#${field.dbName}" IS NOT NULL""")
      case ScalarFilter(field, NotEquals(value))       => Some(sql""""#$alias"."#${field.dbName}" != $value""")
      case ScalarFilter(field, Equals(NullGCValue))    => Some(sql""""#$alias"."#${field.dbName}" IS NULL""")
      case ScalarFilter(field, Equals(value))          => Some(sql""""#$alias"."#${field.dbName}" = $value""")
      case OneRelationIsNullFilter(schema, field) =>
        val relation          = field.relation.get
        val relationTableName = relation.relationTableNameNew(schema)
        val column            = relation.columnForRelationSide(schema, field.relationSide.get)
        // fixme: an ugly hack that is hard to explain. ask marcus. remove this once we have the model on the field
        val otherIdColumn = schema.models.find(_.dbName == modelName) match {
          case Some(model) => model.idField_!.dbName
          case None        => "id"
        }

        Some(sql""" not exists (select  *
                                  from    "#$projectId"."#${relationTableName}"
                                  where   "#$projectId"."#${relationTableName}"."#${column}" = "#$alias"."#$otherIdColumn")""")
    }

    if (sqlParts.isEmpty) None else combineByAnd(sqlParts)
  }

  def generateInStatement(items: Vector[GCValue]) = sql" IN (" ++ combineByComma(items.map(v => sql"$v")) ++ sql")"
}
