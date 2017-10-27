package cool.graph.system.migration.dataSchema

import cool.graph.GCDataTypes.{GCSangriaValueConverter, GCStringConverter}
import cool.graph.shared.models
import cool.graph.shared.models.{Model, Project, TypeIdentifier}
import cool.graph.system.database.SystemFields
import cool.graph.system.migration.project.{DatabaseSchemaExport, FileContainer}
import sangria.ast._
import sangria.renderer.QueryRenderer

object SchemaExport {

  def renderSchema(project: Project): String = {
    renderDefinitions(buildObjectTypeDefinitions(project) ++ buildEnumDefinitions(project), project)
  }

  def renderTypeSchema(project: Project): String = {
    renderDefinitions(buildObjectTypeDefinitions(project), project)
  }

  def renderEnumSchema(project: Project): String = {
    renderDefinitions(buildEnumDefinitions(project), project)
  }

  def renderDefinitions(definitions: Vector[TypeDefinition], project: Project): String = {
    def positionOfTypeDef(typeDef: TypeDefinition): Option[Long] = {
      project.getModelByName(typeDef.name).orElse(project.getEnumByName(typeDef.name)).map(_.id) match {
        case Some(id) =>
          val index = project.typePositions.indexOf(id)
          if (index > -1) Some(index) else None
        case None =>
          None // this can't happen unless this method receives a type definition which we can't lookup correctly, e.g. we introduce interfaces to the project
      }
    }
    def positionOfFieldDef(modelName: String)(fieldDef: FieldDefinition): Option[Long] = {
      for {
        model <- project.getModelByName(modelName)
        field <- model.getFieldByName(fieldDef.name)
        tmp   = model.fieldPositions.indexOf(field.id)
        index <- if (tmp > -1) Some(tmp.toLong) else None
      } yield index
    }
    def sortFn[T](index: T => Option[Long], name: T => String)(element1: T, element2: T): Boolean = {
      (index(element1), index(element2)) match {
        case (Some(index1), Some(index2)) => index1 < index2
        case (Some(_), None)              => true
        case (None, Some(_))              => false
        case (None, None)                 => name(element1) < name(element2)
      }
    }

    val sortedDefinitions = definitions
      .sortWith(sortFn(positionOfTypeDef, _.name))
      .map {
        case obj: ObjectTypeDefinition =>
          val sortedFields = obj.fields.sortWith(sortFn(positionOfFieldDef(obj.name), _.name))
          obj.copy(fields = sortedFields)
        case x => x
      }

    QueryRenderer.render(new Document(definitions = sortedDefinitions))
  }

  def buildFieldDefinition(project: models.Project, model: models.Model, field: models.Field) = {
    val typeName: String = field.typeIdentifier match {
      case TypeIdentifier.Relation => field.relatedModel(project).get.name
      case TypeIdentifier.Enum     => field.enum.map(_.name).getOrElse(sys.error("Enum must be not null if the typeIdentifier is enum."))
      case t                       => TypeIdentifier.toSangriaScalarType(t).name
    }

    val fieldType = (field.isList, field.isRequired, field.isRelation) match {
      case (false, false, _)    => NamedType(typeName)
      case (false, true, _)     => NotNullType(NamedType(typeName))
      case (true, false, false) => ListType(NotNullType(NamedType(typeName)))
      case (true, _, _)         => NotNullType(ListType(NotNullType(NamedType(typeName))))
    }

    val relationDirective = field.relation.map { relation =>
      Directive(name = "relation", arguments = Vector(Argument(name = "name", value = StringValue(relation.name))))
    }

    val isUniqueDirective = if (field.isUnique) Some(Directive(name = "isUnique", arguments = Vector.empty)) else None

    val defaultValueDirective = field.defaultValue.map { dV =>
      val defaultValue = GCStringConverter(field.typeIdentifier, field.isList).fromGCValue(dV)
      val argumentValue = if (field.isList) {
        StringValue(defaultValue)
      } else {
        field.typeIdentifier match {
          case TypeIdentifier.Enum    => EnumValue(defaultValue)
          case TypeIdentifier.Boolean => BooleanValue(defaultValue.toBoolean)
          case TypeIdentifier.Int     => IntValue(defaultValue.toInt)
          case TypeIdentifier.Float   => FloatValue(defaultValue.toDouble)
          case _                      => StringValue(defaultValue)
        }
      }

      Directive(name = "defaultValue", arguments = Vector(Argument(name = "value", value = argumentValue)))
    }

    FieldDefinition(name = field.name,
                    fieldType = fieldType,
                    arguments = Vector.empty,
                    directives = Vector(relationDirective, isUniqueDirective, defaultValueDirective).flatten)
  }

  def buildObjectTypeDefinitions(project: Project): Vector[ObjectTypeDefinition] = {
    project.models
      .map { model =>
        val fields = model.fields
          .map { field =>
            buildFieldDefinition(project, model, field)
          }
          .sortBy(_.name)
          .toVector
        val atModel  = Directive(name = "model", arguments = Vector.empty)
        val comments = Vector()

        // just add directive to all that implement node?
        ObjectTypeDefinition(model.name, interfaces = Vector.empty, fields = fields, directives = Vector(atModel), comments = comments)

//        ObjectTypeDefinition(model.name, interfaces = Vector(NamedType("Node")), fields = fields, directives = directives, comments = comments)
      }
      // stable order is desirable
      .sortBy(_.name)
      .toVector
  }

  def buildEnumDefinitions(project: Project): Vector[EnumTypeDefinition] = {
    project.enums.map { enum =>
      EnumTypeDefinition(
        name = enum.name,
        values = enum.values.map(v => EnumValueDefinition(v)).toVector
      )
    }.toVector
  }

  def addSystemFields(model: Model): Model = {
    val missingFields = SystemFields.generateAll.filter(f => !model.fields.exists(_.name == f.name))

    model.copy(fields = model.fields ++ missingFields)
  }
}
