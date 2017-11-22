package cool.graph.shared.project_dsl

import cool.graph.shared.gc_values.GCValue
import cool.graph.cuid.Cuid
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models._

object SchemaDsl {

  import scala.collection.mutable.Buffer

  def apply()  = schema()
  def schema() = SchemaBuilder()

  case class SchemaBuilder(modelBuilders: Buffer[ModelBuilder] = Buffer.empty,
                           enums: Buffer[Enum] = Buffer.empty,
                           functions: Buffer[cool.graph.shared.models.Function] = Buffer.empty) {
    def model(name: String): ModelBuilder = {
      modelBuilders.find(_.name == name).getOrElse {
        val newModelBuilder = ModelBuilder(name)
        modelBuilders += newModelBuilder
        newModelBuilder
      }
    }

    def enum(name: String, values: Seq[String]): Enum = {
      val id      = name.toLowerCase
      val newEnum = Enum(id, name, values)
      enums += newEnum
      newEnum
    }

    def build(): (Set[Model], Set[Relation]) = {
      val models = modelBuilders.map(_.build())
      val relations = for {
        model <- models
        field <- model.fields if field.isRelation
      } yield field.relation.get

      (models.toSet, relations.toSet)
    }

    def buildClientAndProject(id: String = TestIds.testProjectId, isEjected: Boolean = false): (Client, Project) = {
      val project = buildProject(id)
      val client  = TestClient(project)
      (client, project.copy(isEjected = isEjected))
    }

    def buildProject(id: String = TestIds.testProjectId): Project = {
      val (models, relations) = build()
      val projectAlias        = if (id == TestIds.testProjectId) Some(TestIds.testProjectAlias) else None
      TestProject().copy(
        id = id,
        alias = projectAlias,
        models = models.toList,
        relations = relations.toList,
        enums = enums.toList,
        functions = functions.toList
      )
    }

    def buildEmptyClientAndProject(isEjected: Boolean = false): (Client, Project) = {
      val (models, relations) = build()
      val project             = TestProject.empty
      val client              = TestClient(project)
      (client, project.copy(isEjected = isEjected))
    }
  }

  case class ModelBuilder(
      name: String,
      fields: Buffer[Field] = Buffer(idField),
      permissions: Buffer[ModelPermission] = Buffer.empty,
      var withPermissions: Boolean = true,
      var isSystem: Boolean = false
  ) {
    val id = name

    def field(name: String,
              theType: TypeIdentifier.type => TypeIdentifier.Value,
              enum: Option[Enum] = None,
              isList: Boolean = false,
              isUnique: Boolean = false,
              isSystem: Boolean = false,
              defaultValue: Option[GCValue] = None,
              constraints: List[FieldConstraint] = List.empty): ModelBuilder = {

      val newField =
        plainField(
          name,
          this,
          theType(TypeIdentifier),
          isRequired = false,
          isUnique = isUnique,
          isSystem = isSystem,
          enum = enum,
          isList = isList,
          defaultValue = defaultValue,
          constraints = constraints
        )

      fields += newField
      this
    }

    def field_!(name: String,
                theType: TypeIdentifier.type => TypeIdentifier.Value,
                enumValues: List[String] = List.empty,
                enum: Option[Enum] = None,
                isList: Boolean = false,
                isUnique: Boolean = false,
                isSystem: Boolean = false,
                defaultValue: Option[GCValue] = None): ModelBuilder = {
      val newField =
        plainField(
          name,
          this,
          theType(TypeIdentifier),
          isRequired = true,
          isUnique = isUnique,
          isSystem = isSystem,
          enum = enum,
          isList = isList,
          defaultValue = defaultValue
        )
      fields += newField
      this
    }

    def withTimeStamps: ModelBuilder = {
      fields += createdAtField += updatedAtField
      this
    }

    def oneToOneRelation(fieldName: String,
                         otherFieldName: String,
                         other: ModelBuilder,
                         relationName: Option[String] = None,
                         permissions: Option[List[RelationPermission]] = None): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")
      val relation =
        Relation(
          id = _relationName.toLowerCase,
          name = _relationName,
          modelAId = this.id,
          modelBId = other.id,
          permissions = permissions.getOrElse(RelationPermission.publicPermissions)
        )
      val newField = relationField(fieldName, this, other, relation, isList = false, isBackward = false)
      fields += newField

      val otherNewField = relationField(otherFieldName, other, this, relation, isList = false, isBackward = true)
      other.fields += otherNewField // also add the backwards relation

      this
    }

    def oneToOneRelation_!(fieldName: String,
                           otherFieldName: String,
                           other: ModelBuilder,
                           relationName: Option[String] = None,
                           isRequiredOnOtherField: Boolean = true,
                           permissions: Option[List[RelationPermission]] = None): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")

      val relation = Relation(
        id = _relationName.toLowerCase,
        name = _relationName,
        modelAId = this.id,
        modelBId = other.id,
        permissions = permissions.getOrElse(RelationPermission.publicPermissions)
      )

      val newField = relationField(fieldName, this, other, relation, isList = false, isBackward = false, isRequired = true)
      fields += newField

      val otherNewField = relationField(otherFieldName, other, this, relation, isList = false, isBackward = true, isRequired = isRequiredOnOtherField)
      other.fields += otherNewField // also add the backwards relation

      this
    }

    def oneToManyRelation_!(fieldName: String,
                            otherFieldName: String,
                            other: ModelBuilder,
                            relationName: Option[String] = None,
                            permissions: Option[List[RelationPermission]] = None): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")

      val relation = Relation(
        id = _relationName.toLowerCase,
        name = _relationName,
        modelAId = this.id,
        modelBId = other.id,
        permissions = permissions.getOrElse(RelationPermission.publicPermissions)
      )

      val newField =
        relationField(fieldName, this, other, relation, isList = true, isBackward = false, isRequired = false)
      fields += newField

      val otherNewField =
        relationField(otherFieldName, other, this, relation, isList = false, isBackward = true, isRequired = true)

      other.fields += otherNewField // also add the backwards relation

      this
    }

    def oneToManyRelation(fieldName: String,
                          otherFieldName: String,
                          other: ModelBuilder,
                          relationName: Option[String] = None,
                          permissions: Option[List[RelationPermission]] = None): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")
      val relation =
        Relation(
          id = _relationName.toLowerCase,
          name = _relationName,
          modelAId = this.id,
          modelBId = other.id,
          permissions = permissions.getOrElse(RelationPermission.publicPermissions)
        )
      val newField = relationField(fieldName, this, other, relation, isList = true, isBackward = false)
      fields += newField

      val otherNewField = relationField(otherFieldName, other, this, relation, isList = false, isBackward = true)
      other.fields += otherNewField // also add the backwards relation

      this
    }

    def manyToOneRelation(fieldName: String,
                          otherFieldName: String,
                          other: ModelBuilder,
                          relationName: Option[String] = None,
                          permissions: Option[List[RelationPermission]] = None): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")
      val relation =
        Relation(
          id = _relationName.toLowerCase,
          name = _relationName,
          modelAId = this.id,
          modelBId = other.id,
          permissions = permissions.getOrElse(RelationPermission.publicPermissions)
        )
      val newField = relationField(fieldName, this, other, relation, isList = false, isBackward = false)
      fields += newField

      val otherNewField = relationField(otherFieldName, other, this, relation, isList = true, isBackward = true)
      other.fields += otherNewField // also add the backwards relation

      this
    }

    def manyToManyRelation(fieldName: String,
                           otherFieldName: String,
                           other: ModelBuilder,
                           relationName: Option[String] = None,
                           permissions: Option[List[RelationPermission]] = None): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")
      val relation =
        Relation(
          id = _relationName.toLowerCase,
          name = _relationName,
          modelAId = this.id,
          modelBId = other.id,
          permissions = permissions.getOrElse(RelationPermission.publicPermissions)
        )
      val newField = relationField(fieldName, from = this, to = other, relation, isList = true, isBackward = false)
      fields += newField

      val otherNewField =
        relationField(otherFieldName, from = other, to = this, relation, isList = true, isBackward = true)
      other.fields += otherNewField // also add the backwards relation

      this
    }

    def permission(operation: ModelOperation.type => ModelOperation.Value,
                   userType: UserType.type => UserType.Value,
                   fields: List[String] = List.empty,
                   query: Option[String] = None,
                   queryFilePath: Option[String] = None,
                   description: Option[String] = None,
                   isActive: Boolean = true,
                   ruleName: Option[String] = None): ModelBuilder = {
      val fieldIds = fields.map(name => s"${this.id}.$name")

      this.permissions += ModelPermission(
        id = newId(),
        operation = operation(ModelOperation),
        userType = userType(UserType),
        fieldIds = fieldIds,
        applyToWholeModel = fields.isEmpty,
        isActive = isActive,
        rule = query.map(_ => CustomRule.Graph).getOrElse(CustomRule.None),
        ruleGraphQuery = query,
        ruleGraphQueryFilePath = queryFilePath,
        description = description,
        ruleName = ruleName
      )
      this
    }

    def withOutPermissions: ModelBuilder = {
      this.withPermissions = false
      this
    }

    def build(): Model = {
      Model(
        name = name,
        id = id,
        isSystem = isSystem,
        fields = fields.toList,
        permissions = this.permissions.toList
      )
    }
  }

  def plainField(name: String,
                 model: ModelBuilder,
                 theType: TypeIdentifier.Value,
                 isRequired: Boolean,
                 isUnique: Boolean,
                 isSystem: Boolean,
                 enum: Option[Enum],
                 isList: Boolean,
                 defaultValue: Option[GCValue] = None,
                 constraints: List[FieldConstraint] = List.empty): Field = {

    Field(
      name = name,
      id = name,
      typeIdentifier = theType,
      isRequired = isRequired,
      enum = enum,
      defaultValue = defaultValue,
      // hardcoded values
      description = None,
      isList = isList,
      isUnique = isUnique,
      isSystem = isSystem,
      isReadonly = false,
      relation = None,
      relationSide = None,
      constraints = constraints
    )
  }

  def relationField(name: String,
                    from: ModelBuilder,
                    to: ModelBuilder,
                    relation: Relation,
                    isList: Boolean,
                    isBackward: Boolean,
                    isRequired: Boolean = false): Field = {
    Field(
      name = name,
      id = s"${from.id}.$name",
      isList = isList,
      relationSide = Some {
        if (!isBackward) RelationSide.A else RelationSide.B
      },
      relation = Some(relation),
      // hardcoded values
      typeIdentifier = TypeIdentifier.Relation,
      isRequired = isRequired,
      isUnique = false,
      isSystem = false,
      isReadonly = false,
      defaultValue = None
    )
  }

  def newId(): Id = Cuid.createCuid()

  private val idField = Field(
    id = "id",
    name = "id",
    typeIdentifier = TypeIdentifier.GraphQLID,
    isRequired = true,
    isList = false,
    isUnique = true,
    isSystem = true,
    isReadonly = true
  )

  private val updatedAtField = Field(
    id = "updatedAt",
    name = "updatedAt",
    typeIdentifier = TypeIdentifier.DateTime,
    isRequired = true,
    isList = false,
    isUnique = false,
    isSystem = true,
    isReadonly = true
  )

  private val createdAtField = Field(
    id = "createdAt",
    name = "createdAt",
    typeIdentifier = TypeIdentifier.DateTime,
    isRequired = true,
    isList = false,
    isUnique = true,
    isSystem = true,
    isReadonly = true
  )
}
