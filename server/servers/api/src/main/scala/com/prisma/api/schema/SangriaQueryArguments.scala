package com.prisma.api.schema

import com.prisma.api.connector.{OrderBy, SortOrder}
import com.prisma.shared.models
import com.prisma.shared.models.ConnectorCapability.JoinRelationsFilterCapability
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability, Model}
import sangria.schema.{EnumType, EnumValue, _}

object SangriaQueryArguments {

  import com.prisma.util.coolSangria.FromInputImplicit.DefaultScalaResultMarshaller

  def orderByArgument(model: Model, name: String = "orderBy") = {
    val values = for {
      field     <- model.scalarNonListFields
      sortOrder <- List("ASC", "DESC")
    } yield EnumValue(field.name + "_" + sortOrder, description = None, OrderBy(field, SortOrder.withName(sortOrder.toLowerCase())))

    Argument(name, OptionInputType(EnumType(s"${model.name}OrderByInput", None, values)))
  }

  def whereArgument(model: models.Model, project: models.Project, name: String = "where", capabilities: ConnectorCapabilities): Argument[Option[Any]] = {
    val utils = FilterObjectTypeBuilder(model, project)
//    val filterObject = capabilities.has(JoinRelationsFilterCapability) match {
//      case true  => utils.filterObjectType
//      case false => utils.filterObjectTypeWithOutJoinRelationFilters
//    }

    val filterObject = utils.filterObjectType

    val inputType = OptionInputType(filterObject)
    Argument(name, inputType, description = "")
  }

  def whereSubscriptionArgument(model: models.Model, project: models.Project, name: String = "where") = {
    val utils                              = FilterObjectTypeBuilder(model, project)
    val filterObject: InputObjectType[Any] = utils.subscriptionFilterObjectType
    Argument(name, OptionInputType(filterObject), description = "")
  }

  def internalWhereSubscriptionArgument(model: models.Model, project: models.Project, name: String = "where") = {
    val utils                              = FilterObjectTypeBuilder(model, project)
    val filterObject: InputObjectType[Any] = utils.internalSubscriptionFilterObjectType
    Argument(name, OptionInputType(filterObject), description = "")
  }
}
