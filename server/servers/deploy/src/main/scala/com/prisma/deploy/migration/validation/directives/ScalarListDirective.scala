package com.prisma.deploy.migration.validation.directives

import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.validation._
import com.prisma.shared.models.ConnectorCapability.{EmbeddedScalarListsCapability, NonEmbeddedScalarListCapability, ScalarListsCapability}
import com.prisma.shared.models.{ConnectorCapabilities, FieldBehaviour}
import com.prisma.shared.models.FieldBehaviour.{ScalarListBehaviour, ScalarListStrategy}
import sangria.ast._

object ScalarListDirective extends FieldDirective[ScalarListBehaviour] {
  override def name                                              = "scalarList"
  override def requiredArgs(capabilities: ConnectorCapabilities) = Vector.empty
  override def optionalArgs(capabilities: ConnectorCapabilities) = Vector(ScalarListStrategyArgument(capabilities))

  override def validate(
      document: Document,
      typeDef: ObjectTypeDefinition,
      fieldDef: FieldDefinition,
      directive: Directive,
      capabilities: ConnectorCapabilities
  ) = {
    val invalidTypeError = (!fieldDef.isList).toOption {
      DeployError(typeDef, fieldDef, s"Fields that are marked as `@scalarList` must be lists, e.g. `[${fieldDef.typeName}]`.")
    }
    val unsupportedError = (fieldDef.isScalarList && capabilities.hasNot(ScalarListsCapability)).toOption {
      DeployError(typeDef, fieldDef, s"This connector does not support scalar lists.")
    }
    invalidTypeError.toVector ++ unsupportedError
  }

  override def value(document: Document, typeDef: ObjectTypeDefinition, fieldDef: FieldDefinition, capabilities: ConnectorCapabilities) = {
    lazy val behaviour = fieldDef.directive(name) match {
      case Some(directive) =>
        val strategy = ScalarListStrategyArgument(capabilities).value(directive).get
        ScalarListBehaviour(strategy)
      case None =>
        ScalarListBehaviour(ScalarListStrategy.Embedded)
    }
    fieldDef.isScalarList.toOption {
      behaviour
    }
  }

  override def postValidate(
      dataModel: PrismaSdl,
      capabilities: ConnectorCapabilities
  ): Vector[DeployError] = {
    for {
      model <- dataModel.modelTypes
      field <- model.scalarFields ++ model.enumFields
      if field.isList
      error <- validateListField(field, capabilities)
    } yield error
  }

  private def validateListField(field: PrismaField, capabilities: ConnectorCapabilities): Option[DeployError] = {
    require(field.isList)
    val strategy = field match {
      case f: ScalarPrismaField => f.behaviour.collect { case ScalarListBehaviour(strategy) => strategy }.get
      case f: EnumPrismaField   => f.behaviour.collect { case ScalarListBehaviour(strategy) => strategy }.get
      case _                    => sys.error("Can't happen because we are not passing relation fields into this")
    }

    val error = if (capabilities.hasNot(ScalarListsCapability)) {
      Some(s"This connector does not support scalar lists.")
    } else {
      ScalarListStrategyArgument(capabilities).isValidStrategyArgument(strategy)
    }
    error.map(error => DeployError(field.tpe.name, field.name, error))
  }
}

case class ScalarListStrategyArgument(capabilities: ConnectorCapabilities) extends DirectiveArgument[ScalarListStrategy] {
  val embeddedValue = "EMBEDDED"
  val relationValue = "RELATION"

  override def name = "strategy"

  val validStrategyValues = (
    capabilities.has(EmbeddedScalarListsCapability).toOption(embeddedValue) ++
      capabilities.has(NonEmbeddedScalarListCapability).toOption(relationValue)
  ).toVector

  val invalidStrategyError = s"Valid values for the strategy argument of `@scalarList` are: ${validStrategyValues.mkString(", ")}."

  override def validate(value: Value) = {
    strategyForProvidedArgument(value) match {
      case None           => Some(invalidStrategyError)
      case Some(strategy) => isValidStrategyArgument(strategy)
    }
  }

  override def value(value: Value) = {
    strategyForProvidedArgument(value).getOrElse(sys.error(s"Unknown strategy $value"))
  }

  private def strategyForProvidedArgument(value: Value) = {
    value.asString match {
      case `relationValue` => Some(FieldBehaviour.ScalarListStrategy.Relation)
      case `embeddedValue` => Some(FieldBehaviour.ScalarListStrategy.Embedded)
      case x               => None
    }
  }

  def isValidStrategyArgument(strategy: ScalarListStrategy): Option[String] = {
    val requiredCapability = strategy match {
      case ScalarListStrategy.Relation => NonEmbeddedScalarListCapability
      case ScalarListStrategy.Embedded => EmbeddedScalarListsCapability
    }

    if (capabilities.has(requiredCapability)) {
      None
    } else {
      Some(invalidStrategyError)
    }
  }
}
