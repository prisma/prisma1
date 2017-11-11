package cool.graph.client.schema.simple

import cool.graph.DataItem
import cool.graph.client.UserContext
import cool.graph.shared.models
import sangria.schema._
import scaldi.Injector

class SimplePermissionModelObjectTypesBuilder(project: models.Project)(implicit inj: Injector) extends SimpleSchemaModelObjectTypeBuilder(project) {

  val leafField =
    Field(name = "__leaf__", fieldType = StringType, description = Some("Dummy"), arguments = List[Argument[Any]](), resolve = (context: Context[_, _]) => "")
      .asInstanceOf[Field[UserContext, DataItem]]

  override def modelToObjectType(model: models.Model): ObjectType[UserContext, DataItem] =
    ObjectType(
      model.name,
      description = model.description.getOrElse(model.name),
      fieldsFn = () =>
        model.fields
          .filter(x => !x.isScalar)
          .map(mapClientField(model)) :+ leafField
    )

}
