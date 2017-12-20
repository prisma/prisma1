package cool.graph.api.schema

import cool.graph.shared.models
import cool.graph.shared.models.Model
import cool.graph.api.database.Types.DataItemFilterCollection
import cool.graph.api.database.{OrderBy, QueryArguments, SortOrder}
import sangria.schema.{EnumType, EnumValue, _}

object SangriaQueryArguments {

  import cool.graph.util.coolSangria.FromInputImplicit.DefaultScalaResultMarshaller

  def orderByArgument(model: Model, name: String = "orderBy") = {
    val values = for {
      field     <- model.scalarFields.filter(!_.isList)
      sortOrder <- List("ASC", "DESC")
    } yield EnumValue(field.name + "_" + sortOrder, description = None, OrderBy(field, SortOrder.withName(sortOrder.toLowerCase())))

    Argument(name, OptionInputType(EnumType(s"${model.name}OrderByInput", None, values)))
  }

  def whereArgument(model: models.Model, project: models.Project, name: String = "where"): Argument[Option[Any]] = {
    val utils                              = new FilterObjectTypeBuilder(model, project)
    val filterObject: InputObjectType[Any] = utils.filterObjectType
    Argument(name, OptionInputType(filterObject), description = "")
  }

  def whereSubscriptionArgument(model: models.Model, project: models.Project, name: String = "where") = {
    val utils                              = new FilterObjectTypeBuilder(model, project)
    val filterObject: InputObjectType[Any] = utils.subscriptionFilterObjectType
    Argument(name, OptionInputType(filterObject), description = "")
  }

  def internalWhereSubscriptionArgument(model: models.Model, project: models.Project, name: String = "where") = {
    val utils                              = new FilterObjectTypeBuilder(model, project)
    val filterObject: InputObjectType[Any] = utils.internalSubscriptionFilterObjectType
    Argument(name, OptionInputType(filterObject), description = "")
  }

  // use given arguments if they exist or use sensible default values
  def createSimpleQueryArguments(skipOpt: Option[Int],
                                 after: Option[String],
                                 first: Option[Int],
                                 before: Option[String],
                                 last: Option[Int],
                                 filterOpt: Option[DataItemFilterCollection],
                                 orderByOpt: Option[OrderBy]) = {
    QueryArguments(skipOpt, after, first, before, last, filterOpt, orderByOpt)
  }
}
