package cool.graph.shared.project_dsl

import cool.graph.gc_values.GCValue
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

    def apply(fn: SchemaBuilder => Unit): Project = {
      fn(this)
      this.buildProject()
    }

    def model(name: String): ModelBuilder = {
      modelBuilders.find(_.name == name).getOrElse {
        val newModelBuilder = ModelBuilder(name)
        modelBuilders += newModelBuilder
        newModelBuilder
      }
    }

    def enum(name: String, values: Vector[String]): Enum = {
      val id      = name
      val newEnum = Enum(id, name, values)
      enums += newEnum
      newEnum
    }

    private def build(): (Set[Model], Set[Relation]) = {
      val models = modelBuilders.map(_.build())
      val relations = for {
        model <- models
        field <- model.fields if field.isRelation
      } yield field.relation.get

      (models.toSet, relations.toSet)
    }

    def buildProject(id: String = TestIds.testProjectId): Project = {
      val (models, relations) = build()
      TestProject().copy(
        id = id,
        models = models.toList,
        relations = relations.toList,
        enums = enums.toList,
        functions = functions.toList
      )
    }
  }

  case class ModelBuilder(
      name: String,
      fields: Buffer[Field] = Buffer(idField),
      var withPermissions: Boolean = true
  ) {
    val id = name

    def field(name: String,
              theType: TypeIdentifier.type => TypeIdentifier.Value,
              enum: Option[Enum] = None,
              isList: Boolean = false,
              isUnique: Boolean = false,
              isHidden: Boolean = false,
              defaultValue: Option[GCValue] = None,
              constraints: List[FieldConstraint] = List.empty): ModelBuilder = {

      val newField =
        plainField(
          name,
          this,
          theType(TypeIdentifier),
          isRequired = false,
          isUnique = isUnique,
          isHidden = isHidden,
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
                isHidden: Boolean = false,
                defaultValue: Option[GCValue] = None): ModelBuilder = {
      val newField =
        plainField(
          name,
          this,
          theType(TypeIdentifier),
          isRequired = true,
          isUnique = isUnique,
          isHidden = isHidden,
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

    def oneToOneRelation(
        fieldName: String,
        otherFieldName: String,
        other: ModelBuilder,
        relationName: Option[String] = None,
        includeOtherField: Boolean = true
    ): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")
      val relation =
        Relation(
          id = _relationName.toLowerCase,
          name = _relationName,
          modelAId = this.id,
          modelBId = other.id
        )
      val newField = relationField(fieldName, this, other, relation, isList = false, isBackward = false)
      fields += newField

      if (includeOtherField) {
        val otherNewField = relationField(otherFieldName, other, this, relation, isList = false, isBackward = true)
        other.fields += otherNewField
      }

      this
    }

    def oneToOneRelation_!(
        fieldName: String,
        otherFieldName: String,
        other: ModelBuilder,
        relationName: Option[String] = None,
        isRequiredOnOtherField: Boolean = true,
        includeOtherField: Boolean = true
    ): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")

      val relation = Relation(
        id = _relationName.toLowerCase,
        name = _relationName,
        modelAId = this.id,
        modelBId = other.id
      )

      val newField = relationField(fieldName, this, other, relation, isList = false, isBackward = false, isRequired = true)
      fields += newField

      if (includeOtherField) {
        val otherNewField = relationField(otherFieldName, other, this, relation, isList = false, isBackward = true, isRequired = isRequiredOnOtherField)
        other.fields += otherNewField
      }

      this
    }

    def oneToManyRelation_!(
        fieldName: String,
        otherFieldName: String,
        other: ModelBuilder,
        relationName: Option[String] = None,
        includeOtherField: Boolean = true
    ): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")

      val relation = Relation(
        id = _relationName.toLowerCase,
        name = _relationName,
        modelAId = this.id,
        modelBId = other.id
      )

      val newField = relationField(fieldName, this, other, relation, isList = true, isBackward = false, isRequired = false)
      fields += newField

      if (includeOtherField) {
        val otherNewField = relationField(otherFieldName, other, this, relation, isList = false, isBackward = true, isRequired = true)
        other.fields += otherNewField
      }

      this
    }

    def oneToManyRelation(
        fieldName: String,
        otherFieldName: String,
        other: ModelBuilder,
        relationName: Option[String] = None,
        includeOtherField: Boolean = true
    ): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")

      val relation = Relation(
        id = _relationName.toLowerCase,
        name = _relationName,
        modelAId = this.id,
        modelBId = other.id
      )
      val newField = relationField(fieldName, this, other, relation, isList = true, isBackward = false)
      fields += newField

      if (includeOtherField) {
        val otherNewField = relationField(otherFieldName, other, this, relation, isList = false, isBackward = true)
        other.fields += otherNewField
      }

      this
    }

    def manyToOneRelation(
        fieldName: String,
        otherFieldName: String,
        other: ModelBuilder,
        relationName: Option[String] = None,
        includeOtherField: Boolean = true
    ): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")

      val relation = Relation(
        id = _relationName.toLowerCase,
        name = _relationName,
        modelAId = this.id,
        modelBId = other.id
      )
      val newField = relationField(fieldName, this, other, relation, isList = false, isBackward = false)
      fields += newField

      if (includeOtherField) {
        val otherNewField = relationField(otherFieldName, other, this, relation, isList = true, isBackward = true)
        other.fields += otherNewField
      }

      this
    }

    def manyToManyRelation(
        fieldName: String,
        otherFieldName: String,
        other: ModelBuilder,
        relationName: Option[String] = None,
        includeOtherField: Boolean = true
    ): ModelBuilder = {
      val _relationName = relationName.getOrElse(s"${this.name}To${other.name}")

      val relation = Relation(
        id = _relationName.toLowerCase,
        name = _relationName,
        modelAId = this.id,
        modelBId = other.id
      )
      val newField = relationField(fieldName, from = this, to = other, relation, isList = true, isBackward = false)
      fields += newField

      if (includeOtherField) {
        val otherNewField = relationField(otherFieldName, from = other, to = this, relation, isList = true, isBackward = true)
        other.fields += otherNewField
      }

      this
    }

    def build(): Model = {
      Model(
        name = name,
        id = id,
        fields = fields.toList
      )
    }
  }

  def plainField(name: String,
                 model: ModelBuilder,
                 theType: TypeIdentifier.Value,
                 isRequired: Boolean,
                 isUnique: Boolean,
                 isHidden: Boolean,
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
      isReadonly = false,
      isHidden = isHidden,
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
      isReadonly = false,
      defaultValue = None,
      enum = None
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
    isReadonly = true,
    enum = None,
    defaultValue = None,
    relation = None,
    relationSide = None
  )

  private val updatedAtField = Field(
    id = "updatedAt",
    name = "updatedAt",
    typeIdentifier = TypeIdentifier.DateTime,
    isRequired = true,
    isList = false,
    isUnique = false,
    isReadonly = true,
    enum = None,
    defaultValue = None,
    relation = None,
    relationSide = None
  )

  private val createdAtField = Field(
    id = "createdAt",
    name = "createdAt",
    typeIdentifier = TypeIdentifier.DateTime,
    isRequired = true,
    isList = false,
    isUnique = true,
    isReadonly = true,
    enum = None,
    defaultValue = None,
    relation = None,
    relationSide = None
  )
}
