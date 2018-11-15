package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.validation.{DeployError, PrismaSdl}
import com.prisma.shared.models.ConnectorCapability
import com.prisma.utils.boolean.BooleanUtils
import sangria.ast.{Document, FieldDefinition, ObjectTypeDefinition}

trait DirectiveBase extends BooleanUtils with SharedDirectiveValidation {
  def name: String

  def postValidate(dataModel: PrismaSdl, capabilities: Set[ConnectorCapability]): Vector[DeployError] = Vector.empty
}

object TypeDirective {
  val all = Vector(TypeDbDirective, EmbeddedDirective)
}

trait TypeDirective[T] extends DirectiveBase {
  def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      directive: sangria.ast.Directive,
      capabilities: Set[ConnectorCapability]
  ): Vector[DeployError]

  def value(
      document: Document,
      typeDef: ObjectTypeDefinition,
      capabilities: Set[ConnectorCapability]
  ): Option[T]
}

trait FieldDirective[T] extends DirectiveBase {
  def requiredArgs(capabilities: Set[ConnectorCapability]): Vector[ArgumentRequirement]
  def optionalArgs(capabilities: Set[ConnectorCapability]): Vector[ArgumentRequirement]

  // gets called if the directive was found. Can return an error message
  def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: sangria.ast.Directive,
      capabilities: Set[ConnectorCapability]
  ): Vector[DeployError]

  def value(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      capabilities: Set[ConnectorCapability]
  ): Option[T]
}

object FieldDirective {
  val behaviour = Vector(IdDirective, CreatedAtDirective, UpdatedAtDirective, ScalarListDirective)
  val all       = Vector(DefaultDirective, RelationDirective, UniqueDirective, FieldDbDirective) ++ behaviour
}

case class ArgumentRequirement(name: String, validate: sangria.ast.Value => Option[String])
