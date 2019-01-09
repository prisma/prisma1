package com.prisma.api.schema

import com.prisma.api.connector.{OrderBy, SortOrder}
import com.prisma.shared.models
import com.prisma.shared.models.ConnectorCapability.{JoinRelationsFilterCapability, MongoJoinRelationLinksCapability}
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
    val filterObject = capabilities.has(MongoJoinRelationLinksCapability) match {
      case false => utils.filterObjectType
      case true  => utils.filterObjectTypeForMongo
    }

    val inputType = OptionInputType(filterObject)
    Argument(name, inputType, description = "")
  }

  def whereSubscriptionArgument(model: models.Model, project: models.Project, name: String = "where", capabilities: ConnectorCapabilities) = {
    val utils = FilterObjectTypeBuilder(model, project)
    val filterObject: InputObjectType[Any] = capabilities.has(MongoJoinRelationLinksCapability) match {
      case false => utils.subscriptionFilterObjectType
      case true  => utils.subscriptionFilterObjectTypeForMongo
    }
    Argument(name, OptionInputType(filterObject), description = "")
  }

  def internalWhereSubscriptionArgument(model: models.Model, project: models.Project, name: String = "where", capabilities: ConnectorCapabilities) = {
    val utils = FilterObjectTypeBuilder(model, project)
    val filterObject = capabilities.has(MongoJoinRelationLinksCapability) match {
      case false => utils.internalSubscriptionFilterObjectType
      case true  => utils.internalSubscriptionFilterObjectTypeForMongo
    }

    Argument(name, OptionInputType(filterObject), description = "")
  }
}
