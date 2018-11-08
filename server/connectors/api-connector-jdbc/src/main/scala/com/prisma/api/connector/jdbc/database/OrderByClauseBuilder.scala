package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.{OrderBy, QueryArguments}
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.{Model, Relation, RelationSide}
import org.jooq.{Field, SortField}

trait OrderByClauseBuilder extends QueryBuilderConstants {
  import org.jooq.impl.DSL._

  def orderByForModel(model: Model, alias: String, args: QueryArguments): Vector[SortField[AnyRef]] = {
    orderByInternalWithAliases(
      alias = alias,
      secondaryAlias = alias,
      secondOrderField = model.dbNameOfIdField_!,
      args = args
    )
  }

  def orderByForScalarListField(alias: String, args: QueryArguments): Vector[SortField[AnyRef]] = {
    val (first, last)  = (args.first, args.last)
    val isReverseOrder = last.isDefined

    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()

    val nodeIdField   = field(name(alias, nodeIdFieldName))
    val positionField = field(name(alias, positionFieldName))

    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    isReverseOrder match {
      case true  => Vector(nodeIdField.desc, positionField.desc)
      case false => Vector(nodeIdField.asc, positionField.asc)
    }
  }

  def orderByForRelation(relation: Relation, alias: String, args: QueryArguments): Vector[SortField[AnyRef]] = {
    orderByInternalWithAliases(
      alias = alias,
      secondaryAlias = alias,
      secondOrderField = relation.columnForRelationSide(RelationSide.A),
      args = args
    )
  }

  def orderByInternalWithAliases(alias: String, secondaryAlias: String, secondOrderField: String, args: QueryArguments): Vector[SortField[AnyRef]] = {
    val firstField  = (orderBy: OrderBy) => field(name(alias, orderBy.field.dbName))
    val secondField = field(name(secondaryAlias, secondOrderField))

    orderByFields(firstField, secondField, args)
  }

  def orderByInternal(secondOrderField: String, args: QueryArguments): Vector[SortField[AnyRef]] = {
    val firstField                 = (orderBy: OrderBy) => field(name(orderBy.field.dbName))
    val secondField: Field[AnyRef] = field(name(secondOrderField))

    orderByFields(firstField, secondField, args)
  }

  private def orderByFields(firstField: OrderBy => Field[AnyRef], secondField: Field[AnyRef], args: QueryArguments): Vector[SortField[AnyRef]] = {
    val (first, last, orderBy) = (args.first, args.last, args.orderBy)
    val isReverseOrder         = last.isDefined
    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()
    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val defaultOrder = orderBy.map(_.sortOrder.toString).getOrElse("asc")

    //the secondary field is always ascending no matter what the primary field says. If we need to revert due to last being defined it is always descending.

    (orderBy, defaultOrder, isReverseOrder) match {
      case (Some(order), "asc", true) if order.field.dbName != secondField.getName   => Vector(firstField(order).desc(), secondField.desc())
      case (Some(order), "desc", true) if order.field.dbName != secondField.getName  => Vector(firstField(order).asc(), secondField.desc())
      case (Some(order), "asc", false) if order.field.dbName != secondField.getName  => Vector(firstField(order).asc(), secondField.asc())
      case (Some(order), "desc", false) if order.field.dbName != secondField.getName => Vector(firstField(order).desc(), secondField.asc())
      case (_, "asc", true)                                                          => Vector(secondField.desc())
      case (_, "desc", true)                                                         => Vector(secondField.asc())
      case (_, "asc", false)                                                         => Vector(secondField.asc())
      case (_, "desc", false)                                                        => Vector(secondField.desc())
      case x                                                                         => sys.error(s"$x is unhandled in this pattern match")
    }
  }
}
