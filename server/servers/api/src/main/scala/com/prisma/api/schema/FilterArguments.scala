package com.prisma.api.schema

import com.prisma.shared.models.{Field, Model, TypeIdentifier}

case class FieldFilterTuple(field: Option[Field], filterArg: FilterArgument)
case class FilterArgument(name: String, description: String, isList: Boolean = false)

class FilterArguments(model: Model, isSubscriptionFilter: Boolean = false) {

  private val index = model.fields
    .flatMap(field => {
      FilterArguments
        .getFieldFilters(field)
        .map(filter => { (field.name + filter.name, FieldFilterTuple(Some(field), filter)) })
    })
    .toMap

  def lookup(filter: String): FieldFilterTuple = filter match {
    case "AND" =>
      FieldFilterTuple(None, FilterArguments.ANDFilter)

    case "OR" =>
      FieldFilterTuple(None, FilterArguments.ORFilter)

    case "NOT" =>
      FieldFilterTuple(None, FilterArguments.NOTFilter)

    case "boolean" if isSubscriptionFilter =>
      FieldFilterTuple(None, FilterArguments.booleanFilter)

    case "node" if isSubscriptionFilter =>
      FieldFilterTuple(None, FilterArguments.nodeFilter)

    case _ =>
      index.get(filter) match {
        case None                   => throw new Exception(s""""No field for the filter "$filter" has been found.""")
        case Some(fieldFilterTuple) => fieldFilterTuple
      }
  }
}

object FilterArguments {

  val ANDFilter     = FilterArgument("AND", "Logical AND on all given filters.")
  val ORFilter      = FilterArgument("OR", "Logical OR on all given filters.")
  val NOTFilter     = FilterArgument("NOT", "Logical NOT on all given filters combined by AND.")
  val booleanFilter = FilterArgument("boolean", "")
  val nodeFilter    = FilterArgument("node", "")

  private val baseFilters = List(
    FilterArgument("", ""),
    FilterArgument("_not", "All values that are not equal to given value.")
  )

  private val inclusionFilters = List(
    FilterArgument("_in", "All values that are contained in given list.", isList = true),
    FilterArgument("_not_in", "All values that are not contained in given list.", isList = true)
  )

  private val alphanumericFilters = List(
    FilterArgument("_lt", "All values less than the given value."),
    FilterArgument("_lte", "All values less than or equal the given value."),
    FilterArgument("_gt", "All values greater than the given value."),
    FilterArgument("_gte", "All values greater than or equal the given value.")
  )

  private val stringFilters = List(
    FilterArgument("_contains", "All values containing the given string."),
    FilterArgument("_not_contains", "All values not containing the given string."),
    FilterArgument("_starts_with", "All values starting with the given string."),
    FilterArgument("_not_starts_with", "All values not starting with the given string."),
    FilterArgument("_ends_with", "All values ending with the given string."),
    FilterArgument("_not_ends_with", "All values not ending with the given string.")
  )

  private val multiRelationFilters = List(
    FilterArgument("_every", "All nodes where all nodes in the relation satisfy the given condition."),
    FilterArgument("_some", "All nodes that have at least one node in the relation satisfying the given condition."),
    FilterArgument("_none", "All nodes that have no node in the relation satisfying the given condition.")
  )

  private val oneRelationFilters = List(FilterArgument("", ""))

  def getFieldFilters(field: Field): List[FilterArgument] = {
    val filters =
      if (field.isList) {
        field.typeIdentifier match {
          case TypeIdentifier.Relation => List(multiRelationFilters)
          case _                       => List()
        }
      } else {
        field.typeIdentifier match {
          case TypeIdentifier.UUID     => List(baseFilters, inclusionFilters)
          case TypeIdentifier.Cuid     => List(baseFilters, inclusionFilters, alphanumericFilters, stringFilters)
          case TypeIdentifier.String   => List(baseFilters, inclusionFilters, alphanumericFilters, stringFilters)
          case TypeIdentifier.Int      => List(baseFilters, inclusionFilters, alphanumericFilters)
          case TypeIdentifier.Float    => List(baseFilters, inclusionFilters, alphanumericFilters)
          case TypeIdentifier.Boolean  => List(baseFilters)
          case TypeIdentifier.Enum     => List(baseFilters, inclusionFilters)
          case TypeIdentifier.DateTime => List(baseFilters, inclusionFilters, alphanumericFilters)
          case TypeIdentifier.Json     => List()
          case TypeIdentifier.Relation => List(oneRelationFilters)
          case _                       => List()
        }
      }

    filters.flatten
  }
}
