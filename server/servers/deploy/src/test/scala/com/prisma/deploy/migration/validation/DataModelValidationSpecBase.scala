package com.prisma.deploy.migration.validation

import com.prisma.deploy.connector.FieldRequirementsInterface
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability}
import org.scalactic.{Bad, Good, Or}

trait DataModelValidationSpecBase {
  def validateThatMustError(dataModel: String, capabilities: Set[ConnectorCapability] = Set.empty): Vector[DeployError] = {
    val result = validateInternal(dataModel, capabilities)
    result match {
      case Good(_)     => sys.error("The validation did not produce an error, which it should have.")
      case Bad(errors) => errors
    }
  }

  def validateThatMustWarn(dataModel: String, capabilities: Set[ConnectorCapability] = Set.empty): Vector[DeployWarning] = {
    val result = validateInternal(dataModel, capabilities)
    result match {
      case Good(result) =>
        if (result.warnings.isEmpty) {
          sys.error("The validation did not produce an warning, which it should have.")
        } else {
          result.warnings
        }
      case Bad(errors) =>
        sys.error {
          s"""The validation returned the following unexpected errors:
             |   ${errors.mkString("\n")}
        """.stripMargin
        }
    }
  }

  def validate(dataModel: String, capabilities: Set[ConnectorCapability] = Set.empty) = {
    val result = validateInternal(dataModel, capabilities)
    result match {
      case Good(result) => result.dataModel
      case Bad(errors) =>
        sys.error {
          s"""The validation returned the following unexpected errors:
             |   ${errors.mkString("\n")}
        """.stripMargin
        }
    }
  }

  private def validateInternal(dataModel: String, capabilities: Set[ConnectorCapability]): Or[DataModelValidationResult, Vector[DeployError]] = {
    DataModelValidatorImpl.validate(dataModel, FieldRequirementsInterface.empty, ConnectorCapabilities(capabilities))
  }
}
