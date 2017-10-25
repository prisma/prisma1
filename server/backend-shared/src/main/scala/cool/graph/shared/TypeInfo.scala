package cool.graph.shared

import cool.graph.shared.errors.UserInputErrors.InvalidSchema
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models.{Field => GraphcoolField, _}
import sangria.ast._

import scala.collection.Seq

case class TypeInfo(typeIdentifier: TypeIdentifier, isList: Boolean, isRequired: Boolean, enumValues: List[String], typename: String, isUnique: Boolean)

object TypeInfo {
  def extract(f: FieldDefinition, relation: Option[Relation], enumTypeDefinitions: Seq[EnumTypeDefinition], allowNullsInScalarList: Boolean): TypeInfo = {
    val isUnique = f.directives.exists(_.name == "isUnique")

    if (allowNullsInScalarList) {
      extractWithNullListValues(f.fieldType, isUnique, relation, enumTypeDefinitions)
    } else {
      extract(f.fieldType, isUnique, relation, enumTypeDefinitions)
    }
  }

  def extract(f: InputValueDefinition, allowNullsInScalarList: Boolean): TypeInfo = {
    val isUnique = f.directives.exists(_.name == "isUnique")

    if (allowNullsInScalarList) {
      extractWithNullListValues(f.valueType, isUnique)
    } else {
      extract(f.valueType, isUnique)
    }
  }

  def extractWithNullListValues(tpe: Type,
                                isUnique: Boolean,
                                relation: Option[Relation] = None,
                                enumTypeDefinitions: Seq[EnumTypeDefinition] = Seq.empty): TypeInfo = tpe match {
    case NamedType(name, _) =>
      create(typeName = name, isList = false, isRequired = false, relation = relation, isUnique = isUnique, enumTypeDefinitions = enumTypeDefinitions)

    case NotNullType(NamedType(name, _), _) =>
      create(typeName = name, isList = false, isRequired = true, relation = relation, isUnique = isUnique, enumTypeDefinitions = enumTypeDefinitions)

    case ListType(NamedType(name, _), _) =>
      create(typeName = name, isList = true, isRequired = false, relation = relation, isUnique = isUnique, enumTypeDefinitions = enumTypeDefinitions)

    case ListType(NotNullType(NamedType(name, _), _), _) =>
      create(typeName = name, isList = true, isRequired = false, relation = relation, isUnique = isUnique, enumTypeDefinitions = enumTypeDefinitions)

    case NotNullType(ListType(NamedType(name, _), _), _) =>
      create(typeName = name, isList = true, isRequired = false, relation = relation, isUnique = isUnique, enumTypeDefinitions = enumTypeDefinitions)

    case NotNullType(ListType(NotNullType(NamedType(name, _), _), _), _) =>
      create(typeName = name, isList = true, isRequired = true, relation = relation, isUnique = isUnique, enumTypeDefinitions = enumTypeDefinitions)

    case x => throw InvalidSchema(s"Invalid field type definition detected. ${x.toString}")
  }

  def extract(tpe: Type, isUnique: Boolean, relation: Option[Relation] = None, enumTypeDefinitions: Seq[EnumTypeDefinition] = Seq.empty): TypeInfo = tpe match {
    case NamedType(name, _) =>
      create(typeName = name, isList = false, isRequired = false, relation = relation, isUnique = isUnique, enumTypeDefinitions = enumTypeDefinitions)

    case NotNullType(NamedType(name, _), _) =>
      create(typeName = name, isList = false, isRequired = true, relation = relation, isUnique = isUnique, enumTypeDefinitions = enumTypeDefinitions)

    case ListType(NotNullType(NamedType(name, _), _), _) =>
      create(typeName = name, isList = true, isRequired = false, relation = relation, isUnique = isUnique, enumTypeDefinitions = enumTypeDefinitions)

    case NotNullType(ListType(NotNullType(NamedType(name, _), _), _), _) =>
      create(typeName = name, isList = true, isRequired = true, relation = relation, isUnique = isUnique, enumTypeDefinitions = enumTypeDefinitions)

    case x => throw InvalidSchema("Invalid field type definition detected. Valid field type formats: Int, Int!, [Int!], [Int!]! for example.")  // add offending type and model/relation/field
  }

  private def create(typeName: String,
                     isList: Boolean,
                     isRequired: Boolean,
                     relation: Option[Relation],
                     isUnique: Boolean,
                     enumTypeDefinitions: Seq[EnumTypeDefinition]): TypeInfo = {
    val enum = enumTypeDefinitions.find(_.name == typeName)
    val typeIdentifier = enum match {
      case Some(_) => TypeIdentifier.Enum
      case None    => typeIdentifierFor(typeName)
    }

    val enumValues = enum match {
      case Some(enumType) => enumType.values.map(_.name).toList
      case None           => List.empty
    }

    TypeInfo(typeIdentifier, isList, relation.isEmpty && isRequired, enumValues, typeName, isUnique)
  }

  def typeIdentifierFor(name: String): TypeIdentifier.Value = {
    if (name == "ID") {
      TypeIdentifier.GraphQLID
    } else {
      TypeIdentifier.withNameOpt(name) match {
        case Some(t) => t
        case None    => TypeIdentifier.Relation
      }
    }
  }
}
