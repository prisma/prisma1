package com.prisma.deploy.migration.validation

import com.prisma.deploy.connector.FieldRequirementsInterface
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability}
import org.scalactic.{Bad, Good, Or}

trait DataModelValidationSpecBase {
  def validateThatMustError(dataModel: String, capabilities: Set[ConnectorCapability] = Set.empty): Vector[DeployError] = {
    val result = validateInternal(dataModel, capabilities)
    result match {
      case Good(dm)    => sys.error("The validation did not produce an error, which it should have.")
      case Bad(errors) => errors
    }
  }

  def validate(dataModel: String, capabilities: Set[ConnectorCapability] = Set.empty) = {
    val result = validateInternal(dataModel, capabilities)
    result match {
      case Good(dm) => dm
      case Bad(errors) =>
        sys.error {
          s"""The validation returned the following unexpected errors:
             |   ${errors.mkString("\n")}
        """.stripMargin
        }
    }
  }

  def validateInternal(dataModel: String, capabilities: Set[ConnectorCapability]): Or[PrismaSdl, Vector[DeployError]] = {
    DataModelValidatorImpl.validate(dataModel, FieldRequirementsInterface.empty, ConnectorCapabilities(capabilities))
  }
}
