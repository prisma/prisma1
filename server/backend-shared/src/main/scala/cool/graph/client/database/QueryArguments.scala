package cool.graph.client.database

import cool.graph._
import cool.graph.Types._
import cool.graph.shared.errors.UserAPIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}
import cool.graph.client.database.DatabaseQueryBuilder.ResultTransform
import cool.graph.shared.errors.{UserAPIErrors, UserInputErrors}
import cool.graph.shared.models.{Field, TypeIdentifier}
import slick.jdbc.SQLActionBuilder

case class QueryArguments(skip: Option[Int],
                          after: Option[String],
                          first: Option[Int],
                          before: Option[String],
                          last: Option[Int],
                          filter: Option[DataItemFilterCollection],
                          orderBy: Option[OrderBy]) {

  val MAX_NODE_COUNT = 1000

  import SlickExtensions._
  import slick.jdbc.MySQLProfile.api._

  val isReverseOrder = last.isDefined

  // The job of these methods is to return dynamically generated conditions or commands, but without the corresponding
  // keyword. For example "extractWhereConditionCommand" should return something line "q = 3 and z = '7'", without the
  // "where" keyword. This is because we might need to combine these commands with other commands. If nothing is to be
  // returned, DO NOT return an empty string, but None instead.

  def extractOrderByCommand(projectId: String, modelId: String, defaultOrderShortcut: Option[String] = None): Option[SQLActionBuilder] = {

    if (first.isDefined && last.isDefined) {
      throw UserAPIErrors.InvalidConnectionArguments()
    }

    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val defaultOrder = orderBy.map(_.sortOrder.toString).getOrElse("asc")
    val (order, idOrder) = isReverseOrder match {
      case true  => (invertOrder(defaultOrder), "desc")
      case false => (defaultOrder, "asc")
    }

    val idField = s"`$projectId`.`$modelId`.`id`"

    val res = orderBy match {
      case Some(orderByArg) if orderByArg.field.name != "id" =>
        val orderByField = s"`$projectId`.`$modelId`.`${orderByArg.field.name}`"

        // First order by the orderByField, then by id to break ties
        Some(sql"#$orderByField #$order, #$idField #$idOrder")

      case _ =>
        // be default, order by id. For performance reason use the id in the relation table
        Some(sql"#${defaultOrderShortcut.getOrElse(idField)} #$order")

    }
    res
  }

  def extractLimitCommand(projectId: String, modelId: String, maxNodeCount: Int = MAX_NODE_COUNT): Option[SQLActionBuilder] = {

    (first, last, skip) match {
      case (Some(first), _, _) if first < 0 => throw InvalidFirstArgument()
      case (_, Some(last), _) if last < 0   => throw InvalidLastArgument()
      case (_, _, Some(skip)) if skip < 0   => throw InvalidSkipArgument()
      case _ => {
        val count: Option[Int] = last.isDefined match {
          case true  => last
          case false => first
        }
        // Increase by 1 to know if we have a next page / previous page for relay queries
        val limitedCount: String = count match {
          case None => maxNodeCount.toString
          case Some(x) if x > maxNodeCount =>
            throw UserInputErrors.TooManyNodesRequested(x)
          case Some(x) => (x + 1).toString
        }
        Some(sql"${skip.getOrElse(0)}, #$limitedCount")
      }
    }
  }

  // If order is inverted we have to reverse the returned data items. We do this in-mem to keep the sql query simple.
  // Also, remove excess items from limit + 1 queries and set page info (hasNext, hasPrevious).
  def extractResultTransform(projectId: String, modelId: String): ResultTransform =
    (list: List[DataItem]) => {
      val items = isReverseOrder match {
        case true  => list.reverse
        case false => list
      }

      (first, last) match {
        case (Some(f), _) =>
          if (items.size > f) {
            ResolverResult(items.dropRight(1), hasNextPage = true)
          } else {
            ResolverResult(items)
          }

        case (_, Some(l)) =>
          if (items.size > l) {
            ResolverResult(items.tail, hasPreviousPage = true)
          } else {
            ResolverResult(items)
          }

        case _ =>
          ResolverResult(items)
      }
    }

  def extractWhereConditionCommand(projectId: String, modelId: String): Option[SQLActionBuilder] = {

    if (first.isDefined && last.isDefined) {
      throw UserAPIErrors.InvalidConnectionArguments()
    }

    val standardCondition = filter match {
      case Some(filterArg) =>
        generateFilterConditions(projectId, modelId, filterArg)
      case None => None
    }

    val cursorCondition =
      buildCursorCondition(projectId, modelId, standardCondition)

    val condition = cursorCondition match {
      case None                     => standardCondition
      case Some(cursorConditionArg) => Some(cursorConditionArg)
    }

    condition
  }

  def invertOrder(order: String) = order.trim().toLowerCase match {
    case "desc" => "asc"
    case "asc"  => "desc"
    case _      => throw new IllegalArgumentException
  }

  // This creates a query that checks if the id is in a certain set returned by a subquery Q.
  // The subquery Q fetches all the ID's defined by the cursors and order.
  // On invalid cursor params, no error is thrown. The result set will just be empty.
  def buildCursorCondition(projectId: String, modelId: String, injectedFilter: Option[SQLActionBuilder]): Option[SQLActionBuilder] = {
    // If both params are empty, don't generate any query.
    if (before.isEmpty && after.isEmpty)
      return None

    val idField = s"`$projectId`.`$modelId`.`id`"

    // First, we fetch the ordering for the query. If none is passed, we order by id, ascending.
    // We need that since before/after are dependent on the order.
    val (orderByField, sortDirection) = orderBy match {
      case Some(orderByArg) => (s"`$projectId`.`$modelId`.`${orderByArg.field.name}`", orderByArg.sortOrder.toString)
      case None             => (idField, "asc")
    }

    // Then, we select the comparison operation and construct the cursors. For instance, if we use ascending order, and we want
    // to get the items before, we use the "<" comparator on the column that defines the order.
    def cursorFor(cursor: String, cursorType: String): Option[SQLActionBuilder] = {
      val compOperator = (cursorType, sortDirection.toLowerCase.trim) match {
        case ("before", "asc")  => "<"
        case ("before", "desc") => ">"
        case ("after", "asc")   => ">"
        case ("after", "desc")  => "<"
        case _                  => throw new IllegalArgumentException
      }

      Some(sql"(#$orderByField, #$idField) #$compOperator ((select #$orderByField from `#$projectId`.`#$modelId` where #$idField = '#$cursor'), '#$cursor')")
    }

    val afterCursorFilter = after match {
      case Some(afterCursor) => cursorFor(afterCursor, "after")
      case _                 => None
    }

    val beforeCursorFilter = before match {
      case Some(beforeCursor) => cursorFor(beforeCursor, "before")
      case _                  => None
    }

    // Fuse cursor commands and injected where command
    val whereCommand = combineByAnd(List(injectedFilter, afterCursorFilter, beforeCursorFilter).flatten)

    whereCommand.map(c => sql"" concat c)
  }

  def generateInStatement(items: Seq[Any]) = {
    val combinedItems = combineByComma(items.map(escapeUnsafeParam))
    sql" IN (" concat combinedItems concat sql")"
  }

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
        case FilterElement(key, value, None, filterName, None) if filterName == "AND" => {
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect {
              case Some(x) => x
            }
          combineByAnd(values)
        }
        case FilterElement(key, value, None, filterName, None) if filterName == "AND" => {
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect {
              case Some(x) => x
            }
          combineByAnd(values)
        }
        case FilterElement(key, value, None, filterName, None) if filterName == "OR" => {
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect {
              case Some(x) => x
            }
          combineByOr(values)
        }
        case FilterElement(key, value, None, filterName, None) if filterName == "node" => {
          val values = value
            .asInstanceOf[Seq[Any]]
            .map(subFilter => generateFilterConditions(projectId, tableName, subFilter.asInstanceOf[Seq[Any]]))
            .collect {
              case Some(x) => x
            }
          combineByOr(values)
        }
        // the boolean filter comes from precomputed fields
        case FilterElement(key, value, None, filterName, None) if filterName == "boolean" => {
          value match {
            case true =>
              Some(sql"TRUE")
            case false =>
              Some(sql"FALSE")
          }
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

        case FilterElement(key, null, Some(field), filterName, None) if filterName == "_in" => {
          Some(sql"false")
        }

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_in" => {
          value.asInstanceOf[Seq[Any]].nonEmpty match {
            case true =>
              Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` " concat generateInStatement(value.asInstanceOf[Seq[Any]]))
            case false => Some(sql"false")
          }
        }

        case FilterElement(key, null, Some(field), filterName, None) if filterName == "_not_in" => {
          Some(sql"false")
        }

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_not_in" => {
          value.asInstanceOf[Seq[Any]].nonEmpty match {
            case true =>
              Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` NOT " concat generateInStatement(value.asInstanceOf[Seq[Any]]))
            case false => Some(sql"true")
          }
        }

        case FilterElement(key, null, Some(field), filterName, None) if filterName == "_not" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` IS NOT NULL")

        case FilterElement(key, value, Some(field), filterName, None) if filterName == "_not" =>
          Some(sql"`#$projectId`.`#$tableName`.`#${field.name}` != " concat escapeUnsafeParam(value))

        case FilterElement(key, null, Some(field: Field), filterName, None) if field.typeIdentifier == TypeIdentifier.Relation =>
          if (field.isList) {
            throw new UserAPIErrors.FilterCannotBeNullOnToManyField(field.name)
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
          val (alias, modTableName) =
            getAliasAndTableName(relatedFilter.fromModel.name, relatedFilter.toModel.name)
          Some(sql"""exists (
            select * from `#$projectId`.`#${relatedFilter.toModel.name}` as `#$alias`
            inner join `#$projectId`.`#${relatedFilter.relation.id}`
            on `#$alias`.`id` = `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.oppositeRelationSide.get}`
            where `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.relationSide.get}` = `#$modTableName`.`id`
            and""" concat filterOnRelation(alias, relatedFilter) concat sql")")

        case FilterElement(key, value, Some(field), filterName, Some(relatedFilter)) if filterName == "_every" =>
          val (alias, modTableName) =
            getAliasAndTableName(relatedFilter.fromModel.name, relatedFilter.toModel.name)
          Some(sql"""not exists (
            select * from `#$projectId`.`#${relatedFilter.toModel.name}` as `#$alias`
            inner join `#$projectId`.`#${relatedFilter.relation.id}`
            on `#$alias`.`id` = `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.oppositeRelationSide.get}`
            where `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.relationSide.get}` = `#$modTableName`.`id`
            and not""" concat filterOnRelation(alias, relatedFilter) concat sql")")

        case FilterElement(key, value, Some(field), filterName, Some(relatedFilter)) if filterName == "_none" =>
          val (alias, modTableName) =
            getAliasAndTableName(relatedFilter.fromModel.name, relatedFilter.toModel.name)
          Some(sql"""not exists (
            select * from `#$projectId`.`#${relatedFilter.toModel.name}` as `#$alias`
            inner join `#$projectId`.`#${relatedFilter.relation.id}`
            on `#$alias`.`id` = `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.oppositeRelationSide.get}`
            where `#$projectId`.`#${relatedFilter.relation.id}`.`#${field.relationSide.get}` = `#$modTableName`.`id`
            and """ concat filterOnRelation(alias, relatedFilter) concat sql")")

        case FilterElement(key, value, Some(field), filterName, Some(relatedFilter)) if filterName == "" =>
          val (alias, modTableName) =
            getAliasAndTableName(relatedFilter.fromModel.name, relatedFilter.toModel.name)
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

}
