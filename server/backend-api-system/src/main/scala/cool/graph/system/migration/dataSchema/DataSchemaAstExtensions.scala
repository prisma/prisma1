package cool.graph.system.migration.dataSchema

import sangria.ast._
import scala.collection.Seq

object DataSchemaAstExtensions {
  implicit class CoolDocument(val doc: Document) extends AnyVal {
    def typeNames: Vector[String]    = objectTypes.map(_.name)
    def oldTypeNames: Vector[String] = objectTypes.map(_.oldName)

    def enumNames: Vector[String]    = enumTypes.map(_.name)
    def oldEnumNames: Vector[String] = enumTypes.map(_.oldName)

    def containsRelation(relationName: String): Boolean = {
      val allFields = objectTypes.flatMap(_.fields)
      allFields.exists(fieldDef => fieldDef.oldRelationName.contains(relationName))
    }

    def isObjectOrEnumType(name: String): Boolean = objectType(name).isDefined || enumType(name).isDefined

    def objectType_!(name: String): ObjectTypeDefinition       = objectType(name).getOrElse(sys.error(s"Could not find the object type $name!"))
    def objectType(name: String): Option[ObjectTypeDefinition] = objectTypes.find(_.name == name)
    def objectTypes: Vector[ObjectTypeDefinition]              = doc.definitions.collect { case x: ObjectTypeDefinition => x }

    def enumType(name: String): Option[EnumTypeDefinition] = enumTypes.find(_.name == name)
    def enumTypes: Vector[EnumTypeDefinition]              = doc.definitions collect { case x: EnumTypeDefinition => x }
  }

  implicit class CoolObjectType(val objectType: ObjectTypeDefinition) extends AnyVal {
    def hasNoIdField: Boolean = field("id").isEmpty

    def oldName: String = {
      val nameBeforeRename = for {
        directive <- objectType.directive("rename")
        argument  <- directive.arguments.headOption
      } yield argument.value.asInstanceOf[StringValue].value

      nameBeforeRename.getOrElse(objectType.name)
    }

    def field_!(name: String): FieldDefinition       = field(name).getOrElse(sys.error(s"Could not find the field $name on the type ${objectType.name}"))
    def field(name: String): Option[FieldDefinition] = objectType.fields.find(_.name == name)

    def nonRelationFields: Vector[FieldDefinition] = objectType.fields.filter(_.isNoRelation)
    def relationFields: Vector[FieldDefinition]    = objectType.fields.filter(_.hasRelationDirective)

    def description: Option[String] = objectType.directiveArgumentAsString("description", "text")
  }

  implicit class CoolField(val fieldDefinition: FieldDefinition) extends AnyVal {

    def oldName: String = {
      val nameBeforeRename = fieldDefinition.directiveArgumentAsString("rename", "oldName")
      nameBeforeRename.getOrElse(fieldDefinition.name)
    }

    def isIdField: Boolean = fieldDefinition.name == "id"

    def isNotSystemField = {
      val name = fieldDefinition.name
      name != "id" && name != "updatedAt" && name != "createdAt"
    }

    def typeString: String = fieldDefinition.fieldType.renderPretty

    def typeName: String = fieldDefinition.fieldType.namedType.name

    def isUnique: Boolean = fieldDefinition.directive("isUnique").isDefined

    def isRequired: Boolean = fieldDefinition.fieldType.isRequired

    def isList: Boolean = fieldDefinition.fieldType match {
      case ListType(_, _)                  => true
      case NotNullType(ListType(__, _), _) => true
      case _                               => false
    }

    def isValidRelationType: Boolean = fieldDefinition.fieldType match {
      case NamedType(_,_)                                             => true
      case NotNullType(NamedType(_,_), _)                             => true
      case NotNullType(ListType(NotNullType(NamedType(_,_),_), _), _) => true
      case _                                                          => false
    }

    def isValidScalarType: Boolean = fieldDefinition.fieldType match {
      case NamedType(_,_)                                             => true
      case NotNullType(NamedType(_,_), _)                             => true
      case ListType(NotNullType(NamedType(_,_),_), _)                 => true
      case NotNullType(ListType(NotNullType(NamedType(_,_),_), _), _) => true
      case _                                                          => false
    }

    def isOneRelationField: Boolean     = hasRelationDirective && !isList
    def hasRelationDirective: Boolean   = relationName.isDefined
    def isNoRelation: Boolean           = !hasRelationDirective
    def description: Option[String]     = fieldDefinition.directiveArgumentAsString("description", "text")
    def defaultValue: Option[String]    = fieldDefinition.directiveArgumentAsString("defaultValue", "value")
    def migrationValue: Option[String]  = fieldDefinition.directiveArgumentAsString("migrationValue", "value")
    def relationName: Option[String]    = fieldDefinition.directiveArgumentAsString("relation", "name")
    def oldRelationName: Option[String] = fieldDefinition.directiveArgumentAsString("relation", "oldName").orElse(relationName)
  }

  implicit class CoolEnumType(val enumType: EnumTypeDefinition) extends AnyVal {
    def oldName: String = {
      val nameBeforeRename = enumType.directiveArgumentAsString("rename", "oldName")
      nameBeforeRename.getOrElse(enumType.name)
    }

    def migrationValue: Option[String] = enumType.directiveArgumentAsString("migrationValue", "value")
    def valuesAsStrings: Seq[String]   = enumType.values.map(_.name)
  }

  implicit class CoolWithDirectives(val withDirectives: WithDirectives) extends AnyVal {
    def directiveArgumentAsString(directiveName: String, argumentName: String): Option[String] = {
      for {
        directive <- directive(directiveName)
        argument <- directive.arguments.find { x =>
                     val isScalarOrEnum = x.value.isInstanceOf[ScalarValue] || x.value.isInstanceOf[EnumValue]
                     x.name == argumentName && isScalarOrEnum
                   }
      } yield {
        argument.value match {
          case value: EnumValue       => value.value
          case value: StringValue     => value.value
          case value: BigIntValue     => value.value.toString
          case value: BigDecimalValue => value.value.toString
          case value: IntValue        => value.value.toString
          case value: FloatValue      => value.value.toString
          case value: BooleanValue    => value.value.toString
          case _                      => sys.error("This clause is unreachable because of the instance checks above, but i did not know how to prove it to the compiler.")
        }
      }
    }

    def directive(name: String): Option[Directive] = withDirectives.directives.find(_.name == name)
    def directive_!(name: String): Directive       = directive(name).getOrElse(sys.error(s"Could not find the directive with name: $name!"))

  }

  implicit class CoolDirective(val directive: Directive) extends AnyVal {
    import shapeless._
    import syntax.typeable._

    def containsArgument(name: String, mustBeAString: Boolean): Boolean = {
      if (mustBeAString) {
        directive.arguments.find(_.name == name).flatMap(_.value.cast[StringValue]).isDefined
      } else {
        directive.arguments.exists(_.name == name)
      }
    }

    def argument(name: String): Option[Argument] = directive.arguments.find(_.name == name)
    def argument_!(name: String): Argument       = argument(name).getOrElse(sys.error(s"Could not find the argument with name: $name!"))
  }

  implicit class CoolType(val `type`: Type) extends AnyVal {

    /** Example
      * type Todo {
      *   tag: Tag!     <- we treat this as required; this is the only one we treat as required
      *   tags: [Tag!]! <- this is explicitly not required, because we don't allow many relation fields to be required
      * }
      */
    def isRequired = `type` match {
      case NotNullType(NamedType(_, _), _) => true
      case _                               => false
    }
  }

}
