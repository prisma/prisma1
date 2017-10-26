package cool.graph.shared.models

import cool.graph.DataItem
import cool.graph.Types.Id
import cool.graph.client.UserContext
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.cuid.Cuid
import cool.graph.shared.{TypeInfo, models}
import cool.graph.shared.errors.UserInputErrors.{InvalidSchema, SchemaExtensionParseError}
import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models.RequestPipelineOperation.RequestPipelineOperation
import cool.graph.subscriptions.schemas.QueryTransformer
import sangria.ast
import sangria.schema
import sangria.schema.{ListType, ObjectType, OptionType, OutputType}
import sangria.parser.QueryParser

object FunctionBinding extends Enumeration {
  type FunctionBinding = Value
  val CUSTOM_MUTATION: models.FunctionBinding.Value         = Value("CUSTOM_MUTATION")
  val CUSTOM_QUERY: models.FunctionBinding.Value            = Value("CUSTOM_QUERY")
  val SERVERSIDE_SUBSCRIPTION: models.FunctionBinding.Value = Value("SERVERSIDE_SUBSCRIPTION")
  val TRANSFORM_REQUEST: models.FunctionBinding.Value       = Value("TRANSFORM_REQUEST")
  val TRANSFORM_ARGUMENT: models.FunctionBinding.Value      = Value("TRANSFORM_ARGUMENT")
  val PRE_WRITE: models.FunctionBinding.Value               = Value("PRE_WRITE")
  val TRANSFORM_PAYLOAD: models.FunctionBinding.Value       = Value("TRANSFORM_PAYLOAD")
  val TRANSFORM_RESPONSE: models.FunctionBinding.Value      = Value("TRANSFORM_RESPONSE")
}

object FunctionType extends Enumeration {
  type FunctionType = Value
  val WEBHOOK: models.FunctionType.Value = Value("WEBHOOK")
  val CODE: models.FunctionType.Value    = Value("AUTH0")
}

sealed trait Function {
  def id: Id
  def name: String
  def isActive: Boolean
  def delivery: FunctionDelivery
  def binding: FunctionBinding
}

case class ServerSideSubscriptionFunction(
    id: Id,
    name: String,
    isActive: Boolean,
    query: String,
    queryFilePath: Option[String] = None,
    delivery: FunctionDelivery
) extends Function {
  def isServerSideSubscriptionFor(model: Model, mutationType: ModelMutationType): Boolean = {
    val queryDoc             = QueryParser.parse(query).get
    val modelNameInQuery     = QueryTransformer.getModelNameFromSubscription(queryDoc).get
    val mutationTypesInQuery = QueryTransformer.getMutationTypesFromSubscription(queryDoc)
    model.name == modelNameInQuery && mutationTypesInQuery.contains(mutationType)
  }

  def binding: models.FunctionBinding.Value = FunctionBinding.SERVERSIDE_SUBSCRIPTION
}

case class RequestPipelineFunction(
    id: Id,
    name: String,
    isActive: Boolean,
    binding: FunctionBinding,
    modelId: Id,
    operation: RequestPipelineOperation,
    delivery: FunctionDelivery
) extends Function

sealed trait FunctionDelivery {
  val functionType: FunctionType

  def update(headers: Option[Seq[(String, String)]],
             functionType: Option[FunctionType],
             webhookUrl: Option[String],
             inlineCode: Option[String],
             auth0Id: Option[String],
             codeFilePath: Option[String] = None): FunctionDelivery = {

    // FIXME: how could we do a proper validation before calling those .get()?
    (functionType.getOrElse(this.functionType), this) match {
      case (FunctionType.WEBHOOK, webhook: WebhookFunction) =>
        webhook.copy(url = webhookUrl.getOrElse(webhook.url), headers = headers.getOrElse(webhook.headers))

      case (FunctionType.WEBHOOK, _) =>
        WebhookFunction(url = webhookUrl.get, headers = headers.getOrElse(Seq.empty))

      case (FunctionType.CODE, auth0Fn: Auth0Function) =>
        auth0Fn.copy(
          code = inlineCode.getOrElse(auth0Fn.code),
          codeFilePath = codeFilePath.orElse(auth0Fn.codeFilePath),
          url = webhookUrl.getOrElse(auth0Fn.url),
          auth0Id = auth0Id.getOrElse(auth0Fn.auth0Id),
          headers = headers.getOrElse(auth0Fn.headers)
        )
      case (FunctionType.CODE, fn: ManagedFunction) =>
        fn

      case (FunctionType.CODE, _) =>
        Auth0Function(
          code = inlineCode.get,
          codeFilePath = codeFilePath,
          url = webhookUrl.get,
          auth0Id = auth0Id.get,
          headers = headers.getOrElse(Seq.empty)
        )

      case (_, _) =>
        sys.error("This clause is impossible to reach, but Scala Enumerations are stupid so the compiler cannot check it.")
    }
  }
}
sealed trait CodeFunction extends FunctionDelivery {
  val code: String
}

sealed trait HttpFunction extends FunctionDelivery {
  def headers: Seq[(String, String)]
  def url: String
}

//case class LambdaFunction(code: String, arn: String) extends CodeFunction {
//  override val functionType: FunctionType = FunctionType.LAMBDA
//}

case class Auth0Function(code: String, codeFilePath: Option[String] = None, auth0Id: String, url: String, headers: Seq[(String, String)])
    extends CodeFunction
    with HttpFunction {
  override val functionType: FunctionType = FunctionType.CODE
}

// Function to be deployed and invoked by the function runtime configured for the cluster
case class ManagedFunction(codeFilePath: Option[String] = None) extends FunctionDelivery {
  override val functionType = FunctionType.CODE
}

case class WebhookFunction(url: String, headers: Seq[(String, String)]) extends HttpFunction {
  override val functionType: FunctionType = FunctionType.WEBHOOK
}

case class FunctionDataItems(isNull: Boolean, dataItems: Vector[DataItem])

case class FreeType(
    name: String,
    isList: Boolean,
    isRequired: Boolean,
    fields: List[Field]
) {

  def createOutputType(modelObjectTypesBuilder: SchemaModelObjectTypesBuilder[_]): schema.ObjectType[UserContext, DataItem] = {
    ObjectType(
      name = this.name,
      description = Some(this.name),
      fieldsFn = () => { this.fields.map(modelObjectTypesBuilder.mapCustomMutationField) },
      interfaces = List(),
      instanceCheck = (value: Any, valClass: Class[_], tpe: ObjectType[UserContext, _]) =>
        value match {
          case DataItem(_, _, Some(tpe.name)) => true
          case DataItem(_, _, Some(_))        => false
          case _                              => valClass.isAssignableFrom(value.getClass)
      },
      astDirectives = Vector.empty
    )
  }

  def getFieldType(modelObjectTypesBuilder: SchemaModelObjectTypesBuilder[_]): OutputType[Equals] = {
    val fieldType = (this.isList, this.isRequired) match {
      case (false, false) => OptionType(createOutputType(modelObjectTypesBuilder))
      case (false, true)  => createOutputType(modelObjectTypesBuilder)
      case (true, false)  => OptionType(ListType(createOutputType(modelObjectTypesBuilder)))
      case (true, true)   => ListType(createOutputType(modelObjectTypesBuilder))
    }
    fieldType
  }

  def adjustResolveType(x: FunctionDataItems): Equals = {
    (this.isList, this.isRequired, x.isNull) match {
      case (_, _, true)          => None
      case (false, false, false) => x.dataItems.headOption
      case (false, true, false)  => x.dataItems.head
      case (true, false, false)  => Option(x.dataItems)
      case (true, true, false)   => x.dataItems
    }
  }
}

sealed trait SchemaExtensionFunction extends cool.graph.shared.models.Function {
  def schema: String
  def schemaFilePath: Option[String]
}

object SchemaExtensionFunction {
  def createFunction(
      id: Id,
      name: String,
      isActive: Boolean,
      schema: String,
      delivery: FunctionDelivery,
      schemaFilePath: Option[String] = None
  ): SchemaExtensionFunction = {
    FunctionSchemaParser.determineBinding(name, schema) match {
      case FunctionBinding.CUSTOM_QUERY =>
        CustomQueryFunction(
          id = id: Id,
          name = name: String,
          isActive = isActive: Boolean,
          schema = schema: String,
          schemaFilePath = schemaFilePath,
          delivery = delivery: FunctionDelivery
        )

      case FunctionBinding.CUSTOM_MUTATION =>
        CustomMutationFunction(
          id = id: Id,
          name = name: String,
          isActive = isActive: Boolean,
          schema = schema: String,
          schemaFilePath = schemaFilePath,
          delivery = delivery: FunctionDelivery
        )

      case _ =>
        throw SchemaExtensionParseError(name, "Schema did not contain a schema extension")
    }
  }
}

case class CustomMutationFunction(
    id: Id,
    name: String,
    isActive: Boolean,
    schema: String,
    schemaFilePath: Option[String] = None,
    delivery: FunctionDelivery,
    mutationName: String,
    arguments: List[Field],
    payloadType: FreeType
) extends cool.graph.shared.models.Function
    with SchemaExtensionFunction {
  def binding: models.FunctionBinding.Value = FunctionBinding.CUSTOM_MUTATION
}

object CustomMutationFunction {
  def apply(
      id: Id,
      name: String,
      isActive: Boolean,
      schema: String,
      schemaFilePath: Option[String],
      delivery: FunctionDelivery
  ): CustomMutationFunction = {
    val parsedSchema = FunctionSchemaParser.parse(
      functionName = name,
      schema = schema,
      definitionName = "Mutation",
      extendError = """Must extend Mutation: extend type Mutation { myMutation(arg1: Int): MyPayload }""",
      extendContentError = """Must contain a mutation: extend type Mutation { myMutation(arg1: Int): MyPayload }"""
    )
    new CustomMutationFunction(
      id = id,
      name = name,
      isActive = isActive,
      schema = schema,
      schemaFilePath = schemaFilePath,
      delivery = delivery,
      mutationName = parsedSchema.name,
      arguments = parsedSchema.args,
      payloadType = parsedSchema.payloadType
    )
  }
}

case class CustomQueryFunction(
    id: Id,
    name: String,
    isActive: Boolean,
    schema: String,
    schemaFilePath: Option[String] = None,
    delivery: FunctionDelivery,
    queryName: String,
    arguments: List[Field],
    payloadType: FreeType
) extends cool.graph.shared.models.Function
    with SchemaExtensionFunction {
  def binding: models.FunctionBinding.Value = FunctionBinding.CUSTOM_QUERY
}

object CustomQueryFunction {
  def apply(
      id: Id,
      name: String,
      isActive: Boolean,
      schema: String,
      schemaFilePath: Option[String],
      delivery: FunctionDelivery
  ): CustomQueryFunction = {
    val parsedSchema = FunctionSchemaParser.parse(
      functionName = name,
      schema,
      definitionName = "Query",
      extendError = """Must extend Query: extend type Query { myQuery(arg1: Int): MyPayload }""",
      extendContentError = """Must contain a query: extend type Query { myQuery(arg1: Int): MyPayload }"""
    )
    new CustomQueryFunction(
      id = id,
      name = name,
      isActive = isActive,
      schema = schema,
      schemaFilePath = schemaFilePath,
      delivery = delivery,
      queryName = parsedSchema.name,
      arguments = parsedSchema.args,
      payloadType = parsedSchema.payloadType
    )
  }
}

protected case class ParsedSchema(name: String, args: List[Field], payloadType: FreeType)

object FunctionSchemaParser {
  def parse(functionName: String, schema: String, definitionName: String, extendError: String, extendContentError: String): ParsedSchema = {
    val doc = sangria.parser.QueryParser.parse(schema).getOrElse(throw SchemaExtensionParseError(functionName, s"""Could not parse schema: $schema"""))

    val extensionDefinition = (doc.definitions collect {
      case x: ast.TypeExtensionDefinition if x.definition.name == definitionName => x.definition
    }).headOption.getOrElse(throw SchemaExtensionParseError(functionName, extendError))

    val actualOperationDef: ast.FieldDefinition =
      extensionDefinition.fields.headOption.getOrElse(throw SchemaExtensionParseError(functionName, extendContentError))
    val payloadTypeName = actualOperationDef.fieldType.namedType.name

    if (extensionDefinition.fields.length > 1)
      throw SchemaExtensionParseError(functionName, """Only one query or mutation can be added in a schema extension""")

    val args: List[Field] = actualOperationDef.arguments.map(x => mapInputValueDefinitionToField(functionName, x)).toList

    val payloadTypeDefinitions = doc.definitions.collect { case x: ast.ObjectTypeDefinition => x }

    if (payloadTypeDefinitions.isEmpty)
      throw SchemaExtensionParseError(functionName, """Must provide return type. For example: type MyPayload { someField: Boolean }""")

    if (payloadTypeDefinitions.length > 1)
      throw SchemaExtensionParseError(functionName, """Only one return type can be specified in a schema extension""")

    val selectedPayloadTypeDefinition = payloadTypeDefinitions
      .find(_.name == payloadTypeName)
      .getOrElse(throw SchemaExtensionParseError(
        functionName,
        s"""Return type must match a type in the schema extension. $payloadTypeName did not match any of ${payloadTypeDefinitions
          .map(_.name)
          .mkString(", ")}"""
      ))

    val typeFields = selectedPayloadTypeDefinition.fields.map(x => mapFieldDefinitionToField(functionName, x)).toList

    if (typeFields.exists(
          field => field.name.toLowerCase == "id" && field.typeIdentifier != TypeIdentifier.String && field.typeIdentifier != TypeIdentifier.GraphQLID))
      throw SchemaExtensionParseError(functionName, """The name id is reserved for fields with type ID or String.""")

    val mutationType = actualOperationDef.fieldType match {
      case ast.NamedType(name, _) =>
        FreeType(name = name, isList = false, isRequired = false, fields = typeFields)

      case ast.NotNullType(ast.NamedType(name, _), _) =>
        FreeType(name = name, isList = false, isRequired = true, fields = typeFields)

      case ast.ListType(ast.NotNullType(ast.NamedType(name, _), _), _) =>
        FreeType(name = name, isList = true, isRequired = false, fields = typeFields)

      case ast.NotNullType(ast.ListType(ast.NotNullType(ast.NamedType(name, _), _), _), _) =>
        FreeType(name = name, isList = true, isRequired = true, fields = typeFields)

      case _ =>
        throw InvalidSchema("Invalid field type definition detected. Valid field type formats: Int, Int!, [Int!], [Int!]! for example.")
    }

    ParsedSchema(actualOperationDef.name, args, mutationType)
  }

  def determineBinding(functionName: String, schema: String): FunctionBinding = {
    val doc                      = sangria.parser.QueryParser.parse(schema).getOrElse(throw SchemaExtensionParseError(functionName, s"""Could not parse schema: $schema"""))
    val typeExtensionDefinitions = doc.definitions collect { case x: ast.TypeExtensionDefinition => x }

    if (typeExtensionDefinitions.length > 1) throw SchemaExtensionParseError(functionName, "Schema must not contain more than one type extension")

    val extensionName = typeExtensionDefinitions.headOption
      .getOrElse(throw SchemaExtensionParseError(functionName, "Schema must contain a type extension"))
      .definition
      .name

    extensionName match {
      case "Mutation" => FunctionBinding.CUSTOM_MUTATION
      case "Query"    => FunctionBinding.CUSTOM_QUERY
      case x          => throw SchemaExtensionParseError(functionName, s"Must extend either Query or Mutation. Not '$x'")

    }
  }

  private def mapInputValueDefinitionToField(functionName: String, ivd: ast.InputValueDefinition): Field =
    typeInfoToField(functionName, ivd.name, TypeInfo.extract(f = ivd, allowNullsInScalarList = true))

  private def mapFieldDefinitionToField(functionName: String, fd: ast.FieldDefinition): Field =
    typeInfoToField(functionName, fd.name, TypeInfo.extract(fd, None, Seq.empty, allowNullsInScalarList = true))

  private def typeInfoToField(functionName: String, fieldName: String, typeInfo: TypeInfo) = {
    if (typeInfo.typeIdentifier == TypeIdentifier.Relation)
      throw SchemaExtensionParseError(functionName, s"Relations are currently not supported. Field '$fieldName'")

    Field(
      id = Cuid.createCuid(),
      name = fieldName,
      typeIdentifier = typeInfo.typeIdentifier,
      description = None,
      isRequired = typeInfo.isRequired,
      isList = typeInfo.isList,
      isUnique = false,
      isSystem = false,
      isReadonly = false
    )
  }
}
