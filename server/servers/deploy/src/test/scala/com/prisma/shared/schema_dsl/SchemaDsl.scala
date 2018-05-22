package com.prisma.shared.schema_dsl

import com.prisma.deploy.connector.{DeployConnector, InferredTables}
import com.prisma.deploy.migration.inference.{SchemaInferrer, SchemaMapping}
import com.prisma.deploy.migration.validation.SchemaSyntaxValidator
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.Manifestations.{FieldManifestation, InlineRelationManifestation, ModelManifestation}
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils
import cool.graph.cuid.Cuid
import org.scalatest.Suite
import sangria.parser.QueryParser

object SchemaDsl extends AwaitUtils {

  import scala.collection.mutable.Buffer

  def apply() = SchemaBuilder()

  def fromBuilder(fn: SchemaBuilder => Unit)(implicit deployConnector: DeployConnector, suite: Suite) = {
    val schemaBuilder = SchemaBuilder()
    fn(schemaBuilder)
    val project = schemaBuilder.buildProject(id = projectId(suite))
    if (deployConnector.isPassive) {
      addManifestations(project)
    } else {
      project
    }
  }

  def fromString(id: String = TestIds.testProjectId)(sdlString: String)(implicit deployConnector: DeployConnector, suite: Suite): Project = {
    val project = fromString(
      id = projectId(suite),
      InferredTables.empty,
      isActive = deployConnector.isActive,
      shouldCheckAgainstInferredTables = false
    )(sdlString.stripMargin)
    if (deployConnector.isPassive) {
      addManifestations(project)
    } else {
      project
    }
  }

  private def projectId(suite: Suite): String = {
    // GetFieldFromSQLUniqueException blows up if we generate longer names, since we then exceed the postgres limits for constraint names
    // todo: actually fix GetFieldFromSQLUniqueException instead
    val nameThatMightBeTooLong = suite.getClass.getSimpleName
    nameThatMightBeTooLong.substring(0, Math.min(32, nameThatMightBeTooLong.length))
  }

  def fromPassiveConnectorSdl(
      id: String = TestIds.testProjectId,
      deployConnector: DeployConnector
  )(sdlString: String): Project = {
    val inferredTables = deployConnector.databaseIntrospectionInferrer(id).infer().await()
    fromString(id, inferredTables, isActive = false, shouldCheckAgainstInferredTables = true)(sdlString)
  }

  private def fromString(
      id: String,
      inferredTables: InferredTables,
      isActive: Boolean,
      shouldCheckAgainstInferredTables: Boolean
  )(sdlString: String): Project = {
    val emptyBaseSchema    = Schema()
    val emptySchemaMapping = SchemaMapping.empty
    val sqlDocument        = QueryParser.parse(sdlString.stripMargin).get
    val validator = SchemaSyntaxValidator(
      sdlString,
      SchemaSyntaxValidator.directiveRequirements,
      SchemaSyntaxValidator.reservedFieldsRequirementsForAllConnectors,
      SchemaSyntaxValidator.requiredReservedFields,
      true
    )

    val prismaSdl = validator.generateSDL

    val schema = SchemaInferrer(isActive, shouldCheckAgainstInferredTables).infer(emptyBaseSchema, emptySchemaMapping, prismaSdl, inferredTables)
    TestProject().copy(id = id, schema = schema)
  }

  private def addManifestations(project: Project): Project = {
    val schema = project.schema
    val newRelations = project.relations.map { relation =>
      if (relation.isManyToMany) {
        relation.template
      } else {
        val relationFields = Vector(relation.modelAField, relation.modelBField).flatten
        val fieldToRepresentAsInlineRelation = relationFields.find(_.isList) match {
          case Some(field) => field
          case None        => relationFields.head // happens for one to one relations
        }
        val modelToLinkTo          = fieldToRepresentAsInlineRelation.model
        val modelToPutRelationInto = fieldToRepresentAsInlineRelation.relatedModel_!
        val manifestation = InlineRelationManifestation(
          inTableOfModelId = modelToPutRelationInto.name,
          referencingColumn = s"${relation.name}_${modelToLinkTo.name.toLowerCase}_id"
        )
        relation.template.copy(manifestation = Some(manifestation))
      }
    }
    val newModels = project.models.map { model =>
      val newFields = model.fields.map { field =>
        val newRelation = field.relation.flatMap { relation =>
          newRelations.find(_.name == relation.name)
        }
        field.copy(relationName = newRelation.map(_.name), manifestation = Some(FieldManifestation(field.name + "_column")))
      }

      model.copy(fieldTemplates = newFields, manifestation = Some(ModelManifestation(model.name + "_Table")))
    }
    project.copy(schema = schema.copy(relationTemplates = newRelations, modelTemplates = newModels))
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

    private def build(): (Set[ModelTemplate], Set[RelationTemplate]) = {
      val models    = modelBuilders.map(_.build())
      val relations = modelBuilders.flatMap(_.relations)

      (models.toSet, relations.toSet)
    }

    def buildProject(id: String = TestIds.testProjectId): Project = {
      val (models, relations) = build()
      TestProject().copy(
        id = id,
        schema = Schema(
          modelTemplates = models.toList,
          relationTemplates = relations.toList,
          enums = enums.toList
        ),
        functions = functions.toList
      )
    }
  }

  case class ModelBuilder(
      name: String,
      fields: Buffer[FieldTemplate] = Buffer(idField),
      var withPermissions: Boolean = true,
      relations: Buffer[RelationTemplate] = Buffer.empty
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
      val relation = RelationTemplate(
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
      this.relations += relation

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
      val relation = RelationTemplate(
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
      this.relations += relation

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
      val relation = RelationTemplate(
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
      this.relations += relation

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
      val relation = RelationTemplate(
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
      this.relations += relation

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
      val relation = RelationTemplate(
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
      this.relations += relation

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
      val relation = RelationTemplate(
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
      this.relations += relation

      this
    }

    def build(): ModelTemplate = {
      ModelTemplate(
        name = name,
        stableIdentifier = Cuid.createCuid(),
        fieldTemplates = fields.toList,
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
                 constraints: List[FieldConstraint] = List.empty): FieldTemplate = {

    FieldTemplate(
      name = name,
      typeIdentifier = theType,
      isRequired = isRequired,
      enum = enum,
      defaultValue = defaultValue,
      // hardcoded values
      isList = isList,
      isUnique = isUnique,
      isReadonly = false,
      isHidden = isHidden,
      relationName = None,
      relationSide = None,
      constraints = constraints,
      manifestation = None
    )
  }

  def relationField(name: String,
                    from: ModelBuilder,
                    to: ModelBuilder,
                    relation: RelationTemplate,
                    isList: Boolean,
                    isBackward: Boolean,
                    isRequired: Boolean = false): FieldTemplate = {
    FieldTemplate(
      name = name,
      isList = isList,
      relationSide = Some {
        if (!isBackward) RelationSide.A else RelationSide.B
      },
      relationName = Some(relation.name),
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

  private val idField = FieldTemplate(
    name = "id",
    typeIdentifier = TypeIdentifier.GraphQLID,
    isRequired = true,
    isList = false,
    isUnique = true,
    isReadonly = true,
    enum = None,
    defaultValue = None,
    relationName = None,
    relationSide = None,
    manifestation = None
  )

  private val updatedAtField = FieldTemplate(
    name = "updatedAt",
    typeIdentifier = TypeIdentifier.DateTime,
    isRequired = true,
    isList = false,
    isUnique = false,
    isReadonly = true,
    enum = None,
    defaultValue = None,
    relationName = None,
    relationSide = None,
    manifestation = None
  )

  private val createdAtField = FieldTemplate(
    name = "createdAt",
    typeIdentifier = TypeIdentifier.DateTime,
    isRequired = true,
    isList = false,
    isUnique = true,
    isReadonly = true,
    enum = None,
    defaultValue = None,
    relationName = None,
    relationSide = None,
    manifestation = None
  )
}
