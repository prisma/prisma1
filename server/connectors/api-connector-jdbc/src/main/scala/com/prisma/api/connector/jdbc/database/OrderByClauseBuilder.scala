package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.QueryArguments
import com.prisma.api.schema.APIErrors
import com.prisma.shared.models.{Model, Relation, RelationSide}
import org.jooq.SortField

trait OrderByClauseBuilder extends QueryBuilderConstants {
  import org.jooq.impl.DSL._

  def orderByForModel(model: Model, alias: String, args: Option[QueryArguments]): Vector[SortField[AnyRef]] = {
    orderByInternal(
      alias = alias,
      secondaryAlias = alias,
      secondOrderField = model.dbNameOfIdField_!,
      args = args
    )
  }

  def orderByForScalarListField(alias: String, args: Option[QueryArguments]): Vector[SortField[AnyRef]] = {
    val (first, last)  = (args.flatMap(_.first), args.flatMap(_.last))
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

  def orderByForRelation(relation: Relation, alias: String, args: Option[QueryArguments]): Vector[SortField[AnyRef]] = {
    orderByInternal(
      alias = alias,
      secondaryAlias = alias,
      secondOrderField = relation.columnForRelationSide(RelationSide.A),
      args = args
    )
  }

  def orderByInternal(alias: String, secondaryAlias: String, secondOrderField: String, args: Option[QueryArguments]): Vector[SortField[AnyRef]] = {
    val (first, last, orderBy) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.orderBy))
    val isReverseOrder         = last.isDefined
    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()
    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val defaultOrder = orderBy.map(_.sortOrder.toString).getOrElse("asc")
    val secondField  = field(name(secondaryAlias, secondOrderField))

    (orderBy, defaultOrder, isReverseOrder) match {
      case (Some(order), "asc", true) if order.field.dbName != secondOrderField   => Vector(field(name(alias, order.field.dbName)).desc(), secondField.desc())
      case (Some(order), "desc", true) if order.field.dbName != secondOrderField  => Vector(field(name(alias, order.field.dbName)).asc(), secondField.asc())
      case (Some(order), "asc", false) if order.field.dbName != secondOrderField  => Vector(field(name(alias, order.field.dbName)).asc(), secondField.asc())
      case (Some(order), "desc", false) if order.field.dbName != secondOrderField => Vector(field(name(alias, order.field.dbName)).desc(), secondField.desc())
      case (_, "asc", true)                                                       => Vector(secondField.desc())
      case (_, "desc", true)                                                      => Vector(secondField.asc())
      case (_, "asc", false)                                                      => Vector(secondField.asc())
      case (_, "desc", false)                                                     => Vector(secondField.desc())
      case _                                                                      => throw new IllegalArgumentException
    }
  }

  def orderByInternal2(secondOrderField: String, args: Option[QueryArguments]): Vector[SortField[AnyRef]] = {
    val (first, last, orderBy) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.orderBy))
    val isReverseOrder         = last.isDefined
    if (first.isDefined && last.isDefined) throw APIErrors.InvalidConnectionArguments()
    // The limit instruction only works from up to down. Therefore, we have to invert order when we use before.
    val defaultOrder = orderBy.map(_.sortOrder.toString).getOrElse("asc")
    val secondField  = field(name(secondOrderField))

    (orderBy, defaultOrder, isReverseOrder) match {
      case (Some(order), "asc", true) if order.field.dbName != secondOrderField   => Vector(field(name(order.field.dbName)).desc(), secondField.desc())
      case (Some(order), "desc", true) if order.field.dbName != secondOrderField  => Vector(field(name(order.field.dbName)).asc(), secondField.asc())
      case (Some(order), "asc", false) if order.field.dbName != secondOrderField  => Vector(field(name(order.field.dbName)).asc(), secondField.asc())
      case (Some(order), "desc", false) if order.field.dbName != secondOrderField => Vector(field(name(order.field.dbName)).desc(), secondField.desc())
      case (_, "asc", true)                                                       => Vector(secondField.desc())
      case (_, "desc", true)                                                      => Vector(secondField.asc())
      case (_, "asc", false)                                                      => Vector(secondField.asc())
      case (_, "desc", false)                                                     => Vector(secondField.desc())
      case _                                                                      => throw new IllegalArgumentException
    }
  }
}
