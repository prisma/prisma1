package com.prisma.shared.schema_dsl

import com.prisma.deploy.connector.{DatabaseIntrospectionInferrer, DeployConnector, InferredTables}
import com.prisma.deploy.migration.inference.{SchemaInferrer, SchemaMapping}
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils
import cool.graph.cuid.Cuid
import sangria.parser.QueryParser

object SchemaDsl extends AwaitUtils {

  import scala.collection.mutable.Buffer

  def apply()  = schema()
  def schema() = SchemaBuilder()

  def fromString(id: String = TestIds.testProjectId)(sdlString: String): Project = {
    fromString(id, InferredTables.empty, isActive = true)(sdlString)
  }

  def fromPassiveConnectorSdl(
      id: String = TestIds.testProjectId,
      deployConnector: DeployConnector
  )(sdlString: String): Project = {
    val inferredTables = deployConnector.databaseIntrospectionInferrer(id).infer().await()
    fromString(id, inferredTables, isActive = false)(sdlString)
  }

  private def fromString(
      id: String,
      inferredTables: InferredTables,
      isActive: Boolean
  )(sdlString: String): Project = {
    val emptyBaseSchema    = Schema()
    val emptySchemaMapping = SchemaMapping.empty
    val sqlDocument        = QueryParser.parse(sdlString.stripMargin).get
    val schema             = SchemaInferrer(isActive).infer(emptyBaseSchema, emptySchemaMapping, sqlDocument, inferredTables).get
    TestProject().copy(id = id, schema = schema)
  }

  case class SchemaBuilder(
      modelBuilders: Buffer[ModelBuilder] = Buffer.empty,
      enums: Buffer[Enum] = Buffer.empty,
      functions: Buffer[com.prisma.shared.models.Function] = Buffer.empty
  ) {

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
      val newEnum = Enum(name, values)
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
        schema = Schema(
          models = models.toList,
          relations = relations.toList,
          enums = enums.toList
        ),
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

      val newField = plainField(
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
      val newField = plainField(
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
        fieldAName: String,
        fieldBName: String,
        modelB: ModelBuilder,
        relationName: Option[String] = None,
        includeFieldB: Boolean = true,
        modelAOnDelete: OnDelete.Value = OnDelete.SetNull,
        modelBOnDelete: OnDelete.Value = OnDelete.SetNull
    ): ModelBuilder = {
      val relation = Relation(
        name = relationName.getOrElse(s"${this.name}To${modelB.name}"),
        modelAId = this.id,
        modelBId = modelB.id,
        modelAOnDelete = modelAOnDelete,
        modelBOnDelete = modelBOnDelete,
        manifestation = None
      )
      val newField = relationField(fieldAName, this, modelB, relation, isList = false, isBackward = false)
      fields += newField

      if (includeFieldB) {
        val newBField = relationField(fieldBName, modelB, this, relation, isList = false, isBackward = true)
        modelB.fields += newBField
      }

      this
    }

    def oneToOneRelation_!(
        fieldAName: String,
        fieldBName: String,
        modelB: ModelBuilder,
        relationName: Option[String] = None,
        isRequiredOnFieldB: Boolean = true,
        includeFieldB: Boolean = true,
        modelAOnDelete: OnDelete.Value = OnDelete.SetNull,
        modelBOnDelete: OnDelete.Value = OnDelete.SetNull
    ): ModelBuilder = {
      val relation = Relation(
        name = relationName.getOrElse(s"${this.name}To${modelB.name}"),
        modelAId = this.id,
        modelBId = modelB.id,
        modelAOnDelete = modelAOnDelete,
        modelBOnDelete = modelBOnDelete,
        manifestation = None
      )

      val newField = relationField(fieldAName, this, modelB, relation, isList = false, isBackward = false, isRequired = true)
      fields += newField

      if (includeFieldB) {
        val newBField = relationField(fieldBName, modelB, this, relation, isList = false, isBackward = true, isRequired = isRequiredOnFieldB)
        modelB.fields += newBField
      }

      this
    }

    def oneToManyRelation_!(
        fieldAName: String,
        fieldBName: String,
        modelB: ModelBuilder,
        relationName: Option[String] = None,
        includeFieldB: Boolean = true,
        modelAOnDelete: OnDelete.Value = OnDelete.SetNull,
        modelBOnDelete: OnDelete.Value = OnDelete.SetNull
    ): ModelBuilder = {
      val relation = Relation(
        name = relationName.getOrElse(s"${this.name}To${modelB.name}"),
        modelAId = this.id,
        modelBId = modelB.id,
        modelAOnDelete = modelAOnDelete,
        modelBOnDelete = modelBOnDelete,
        manifestation = None
      )

      val newField = relationField(fieldAName, this, modelB, relation, isList = true, isBackward = false, isRequired = false)
      fields += newField

      if (includeFieldB) {
        val newBField = relationField(fieldBName, modelB, this, relation, isList = false, isBackward = true, isRequired = true)
        modelB.fields += newBField
      }

      this
    }

    def oneToManyRelation(
        fieldAName: String,
        fieldBName: String,
        modelB: ModelBuilder,
        relationName: Option[String] = None,
        includeFieldB: Boolean = true,
        modelAOnDelete: OnDelete.Value = OnDelete.SetNull,
        modelBOnDelete: OnDelete.Value = OnDelete.SetNull
    ): ModelBuilder = {
      val relation = Relation(
        name = relationName.getOrElse(s"${this.name}To${modelB.name}"),
        modelAId = this.id,
        modelBId = modelB.id,
        modelAOnDelete = modelAOnDelete,
        modelBOnDelete = modelBOnDelete,
        manifestation = None
      )
      val newField = relationField(fieldAName, this, modelB, relation, isList = true, isBackward = false)
      fields += newField

      if (includeFieldB) {
        val newBField = relationField(fieldBName, modelB, this, relation, isList = false, isBackward = true)
        modelB.fields += newBField
      }

      this
    }

    def manyToOneRelation(
        fieldAName: String,
        fieldBName: String,
        modelB: ModelBuilder,
        relationName: Option[String] = None,
        includeFieldB: Boolean = true,
        modelAOnDelete: OnDelete.Value = OnDelete.SetNull,
        modelBOnDelete: OnDelete.Value = OnDelete.SetNull
    ): ModelBuilder = {
      val relation = Relation(
        name = relationName.getOrElse(s"${this.name}To${modelB.name}"),
        modelAId = this.id,
        modelBId = modelB.id,
        modelAOnDelete = modelAOnDelete,
        modelBOnDelete = modelBOnDelete,
        manifestation = None
      )
      val newField = relationField(fieldAName, this, modelB, relation, isList = false, isBackward = false)
      fields += newField

      if (includeFieldB) {
        val newBField = relationField(fieldBName, modelB, this, relation, isList = true, isBackward = true)
        modelB.fields += newBField
      }

      this
    }

    def manyToManyRelation(
        fieldAName: String,
        fieldBName: String,
        modelB: ModelBuilder,
        relationName: Option[String] = None,
        includeFieldB: Boolean = true,
        modelAOnDelete: OnDelete.Value = OnDelete.SetNull,
        modelBOnDelete: OnDelete.Value = OnDelete.SetNull
    ): ModelBuilder = {
      val relation = Relation(
        name = relationName.getOrElse(s"${this.name}To${modelB.name}"),
        modelAId = this.id,
        modelBId = modelB.id,
        modelAOnDelete = modelAOnDelete,
        modelBOnDelete = modelBOnDelete,
        manifestation = None
      )
      val newField = relationField(fieldAName, from = this, to = modelB, relation, isList = true, isBackward = false)
      fields += newField

      if (includeFieldB) {
        val newBField = relationField(fieldBName, from = modelB, to = this, relation, isList = true, isBackward = true)
        modelB.fields += newBField
      }

      this
    }

    def build(): Model = {
      Model(
        name = name,
        stableIdentifier = Cuid.createCuid(),
        fields = fields.toList,
        manifestation = None
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
      constraints = constraints,
      manifestation = None
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
      enum = None,
      manifestation = None
    )
  }

  def newId(): Id = Cuid.createCuid()

  private val idField = Field(
    name = "id",
    typeIdentifier = TypeIdentifier.GraphQLID,
    isRequired = true,
    isList = false,
    isUnique = true,
    isReadonly = true,
    enum = None,
    defaultValue = None,
    relation = None,
    relationSide = None,
    manifestation = None
  )

  private val updatedAtField = Field(
    name = "updatedAt",
    typeIdentifier = TypeIdentifier.DateTime,
    isRequired = true,
    isList = false,
    isUnique = false,
    isReadonly = true,
    enum = None,
    defaultValue = None,
    relation = None,
    relationSide = None,
    manifestation = None
  )

  private val createdAtField = Field(
    name = "createdAt",
    typeIdentifier = TypeIdentifier.DateTime,
    isRequired = true,
    isList = false,
    isUnique = true,
    isReadonly = true,
    enum = None,
    defaultValue = None,
    relation = None,
    relationSide = None,
    manifestation = None
  )
}
