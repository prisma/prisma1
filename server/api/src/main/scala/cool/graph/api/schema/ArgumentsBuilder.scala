package cool.graph.api.schema

import cool.graph.shared.models.{Model, Project}
import cool.graph.util.coolSangria.FromInputImplicit
import sangria.schema.{InputObjectType, _}

case class ArgumentsBuilder(project: Project) {

  val inputTypesBuilder: InputTypesBuilder = CachedInputTypesBuilder(project)

  private val oneRelationIdFieldType   = OptionInputType(IDType)
  private val manyRelationIdsFieldType = OptionInputType(ListInputType(IDType))

  implicit val anyFromInput = FromInputImplicit.CoercedResultMarshaller

  def getSangriaArgumentsForCreate(model: Model): List[Argument[Any]] = {
    val inputObjectType = inputTypesBuilder.inputObjectTypeForCreate(model)
    List(Argument[Any]("data", inputObjectType))
  }

  def getSangriaArgumentsForUpdate(model: Model): List[Argument[Any]] = {
    val inputObjectType = inputTypesBuilder.inputObjectTypeForUpdate(model)
    List(Argument[Any]("data", inputObjectType), getWhereArgument(model))
  }

  def getSangriaArgumentsForUpdateOrCreate(model: Model): List[Argument[Any]] = {
    List(
      Argument[Any]("create", inputTypesBuilder.inputObjectTypeForCreate(model)),
      Argument[Any]("update", inputTypesBuilder.inputObjectTypeForUpdate(model)),
      Argument[Any]("where", ???)
    )
  }

  def getSangriaArgumentsForDelete(model: Model): List[Argument[Any]] = {
    List(getWhereArgument(model))
  }

  def getWhereArgument(model: Model) = {
    Argument[Any](
      name = "where",
      argumentType = InputObjectType(
        name = s"${model.name}WhereUniqueInput",
        fields = model.fields.filter(_.isUnique).map(field => InputField(name = field.name, fieldType = SchemaBuilderUtils.mapToOptionalInputType(field)))
      )
    )
  }
}
