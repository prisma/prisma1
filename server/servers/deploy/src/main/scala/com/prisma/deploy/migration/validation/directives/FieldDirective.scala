package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.validation.DeployError
import com.prisma.shared.models.ConnectorCapability
import com.prisma.utils.boolean.BooleanUtils
import sangria.ast.{Document, FieldDefinition, ObjectTypeDefinition}

trait FieldDirective[T] extends BooleanUtils with SharedDirectiveValidation { // could introduce a new interface for type level directives
  def name: String
  def requiredArgs: Vector[ArgumentRequirement]
  def optionalArgs: Vector[ArgumentRequirement]

  // gets called if the directive was found. Can return an error message
  def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: sangria.ast.Directive,
      capabilities: Set[ConnectorCapability]
  ): Option[DeployError]

  def value(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      capabilities: Set[ConnectorCapability]
  ): Option[T]
}

object FieldDirective {
  val behaviour = Vector(IdDirective, CreatedAtDirective, UpdatedAtDirective, ScalarListDirective)
  val all       = Vector(DefaultDirective, RelationDirective) ++ behaviour
}

case class ArgumentRequirement(name: String, validate: sangria.ast.Value => Option[String])
