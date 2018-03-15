package com.prisma.api.connector.mysql.database

import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.{Field, TypeIdentifier}
import slick.jdbc.SQLActionBuilder

object QueryArgumentsHelpers {
  import SlickExtensions._
  import slick.jdbc.MySQLProfile.api._

  def generateFilterConditions(projectId: String, tableName: String, filter: Seq[Any]): Option[SQLActionBuilder] = {
    // don't allow options that are Some(value), options that are None are ok
    //    assert(filter.count {
    //      case (key, value) =>
    //        value.isInstanceOf[Option[Any]] && (value match {
    //          case Some(v) => true
    //          case None => false
    //        })
    //    } == 0)
    def getAliasAndTableName(fromModel: String, toModel: String): (String, String) = {
      var modTableName = ""
      if (!tableName.contains("_"))
        modTableName = projectId + "`.`" + fromModel
      else modTableName = tableName
      val alias = toModel + "_" + tableName
      (alias, modTableName)
    }

    def filterOnRelation(relationTableName: String, relationFilter: FilterElementRelation) = {
      Some(generateFilterConditions(projectId, relationTableName, relationFilter.filter).getOrElse(sql"True"))
    }

    val sqlParts = filter
      .map {
        case FilterElement(key, None, Some(field), filterName, None) =>
          None
        case FilterElement(key, value, None, filterName, None) if filterName == "AND" =>
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect {
              case Some(x) => x
            }
          combineByAnd(values)
        case FilterElement(key, value, None, filterName, None) if filterName == "AND" =>
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect {
              case Some(x) => x
            }
          combineByAnd(values)
        case FilterElement(key, value, None, filterName, None) if filterName == "OR" =>
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect {
              case Some(x) => x
            }
          combineByOr(values)
        case FilterElement(key, value, None, filterName, None) if filterName == "node" =>
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect {
              case Some(x) => x
            }
          combineByOr(values)
        // the boolean filter comes from precomputed fields
        case FilterElement(key, value, None, filterName, None) if filterName == "boolean" =>
          value match {
            case true =>
              Some(sql"TRUE")
            case false =>
              Some(sql"FALSE")
          }
        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_contains" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` LIKE " concat escapeUnsafeParam(s"%$value%"))

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_not_contains" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` NOT LIKE " concat escapeUnsafeParam(s"%$value%"))

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_starts_with" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` LIKE " concat escapeUnsafeParam(s"$value%"))

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_not_starts_with" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` NOT LIKE " concat escapeUnsafeParam(s"$value%"))

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_ends_with" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` LIKE " concat escapeUnsafeParam(s"%$value"))

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_not_ends_with" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` NOT LIKE " concat escapeUnsafeParam(s"%$value"))

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_lt" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` < " concat escapeUnsafeParam(value))

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_gt" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` > " concat escapeUnsafeParam(value))

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_lte" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` <= " concat escapeUnsafeParam(value))

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_gte" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` >= " concat escapeUnsafeParam(value))

        case FilterElement(key, null, Some(field), filterName, None) if filterName == "_in" =>
          Some(sql"false")

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_in" =>
          value.asInstanceOf[Seq[Any]].nonEmpty match {
            case true  => Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` " concat generateInStatement(value.asInstanceOf[Seq[Any]]))
            case false => Some(sql"false")
          }

        case FilterElement(key, null, Some(field), filterName, None) if filterName == "_not_in" =>
          Some(sql"false")

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_not_in" =>
          value.asInstanceOf[Seq[Any]].nonEmpty match {
            case true =>
              Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` NOT " concat generateInStatement(value.asInstanceOf[Seq[Any]]))
            case false => Some(sql"true")
          }

        case FilterElement(key, null, Some(field), filterName, None) if filterName == "_not" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` IS NOT NULL")

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_not" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` != " concat escapeUnsafeParam(value))

        case FilterElement(key, null, Some(field: Field), filterName, None) if field.typeIdentifier == TypeIdentifier.Relation =>
          if (field.isList) {
            throw new APIErrors.FilterCannotBeNullOnToManyField(field.name)
          }
          Some(sql""" not exists (select  *
                                  from    `#$projectId`.`#${field.relation.get.id}`
                                  where   `#$projectId`.`#${field.relation.get.id}`.`#${field.relationSide.get}` = `#$projectId`.`#$tableName`.`id`
                                  )""")

        case FilterElement(key, null, Some(field), filterName, None) if field.typeIdentifier != TypeIdentifier.Relation =>
          Some(sql"`#$projectId`.`#$tableName`.`#$key` IS NULL")

        case FilterElement(key, value, _, filterName, None) =>
          Some(sql"`#$projectId`.`#$tableName`.`#$key` = " concat escapeUnsafeParam(value))

        case FilterElement(key, value, Some(field), filterName, Some(relatedFilter)) if filterName == "_some" =>
          val (alias, modTableName) = getAliasAndTableName(relatedFilter.fromModel.name, relatedFilter.toModel.name)
          Some(sql"""exists (
            select * from `#$projectId`.`#${relatedFilter.toModel.name}` as `#$alias`
            inner join `#$projectId`.`#${relatedFilter.relation.id}`
            on `#$alias`.`id` = `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.oppositeRelationSide.get}`
            where `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.relationSide.get}` = `#$modTableName`.`id`
            and""" concat filterOnRelation(alias, relatedFilter) concat sql")")

        case FilterElement(key, value, Some(field), filterName, Some(relatedFilter)) if filterName == "_every" =>
          val (alias, modTableName) = getAliasAndTableName(relatedFilter.fromModel.name, relatedFilter.toModel.name)
          Some(sql"""not exists (
            select * from `#$projectId`.`#${relatedFilter.toModel.name}` as `#$alias`
            inner join `#$projectId`.`#${relatedFilter.relation.id}`
            on `#$alias`.`id` = `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.oppositeRelationSide.get}`
            where `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.relationSide.get}` = `#$modTableName`.`id`
            and not""" concat filterOnRelation(alias, relatedFilter) concat sql")")

        case FilterElement(key, value, Some(field), filterName, Some(relatedFilter)) if filterName == "_none" =>
          val (alias, modTableName) = getAliasAndTableName(relatedFilter.fromModel.name, relatedFilter.toModel.name)
          Some(sql"""not exists (
            select * from `#$projectId`.`#${relatedFilter.toModel.name}` as `#$alias`
            inner join `#$projectId`.`#${relatedFilter.relation.id}`
            on `#$alias`.`id` = `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.oppositeRelationSide.get}`
            where `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.relationSide.get}` = `#$modTableName`.`id`
            and """ concat filterOnRelation(alias, relatedFilter) concat sql")")

        case FilterElement(key, value, Some(field), filterName, Some(relatedFilter)) if filterName == "" =>
          val (alias, modTableName) = getAliasAndTableName(relatedFilter.fromModel.name, relatedFilter.toModel.name)
          Some(sql"""exists (
            select * from `#$projectId`.`#${relatedFilter.toModel.name}` as `#$alias`
            inner join `#$projectId`.`#${relatedFilter.relation.id}`
            on `#$alias`.`id` = `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.oppositeRelationSide.get}`
            where `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.relationSide.get}` = `#$modTableName`.`id`
            and""" concat filterOnRelation(alias, relatedFilter) concat sql")")

        // this is used for the node: {} field in the Subscription Filter
        case values: Seq[FilterElement @unchecked] =>
          generateFilterConditions(projectId, tableName, values)
      }
      .filter(_.nonEmpty)
      .map(_.get)

    if (sqlParts.isEmpty)
      None
    else
      combineByAnd(sqlParts)
  }

  def generateInStatement(items: Seq[Any]) = {
    val combinedItems = combineByComma(items.map(escapeUnsafeParam))
    sql" IN (" concat combinedItems concat sql")"
  }

}
