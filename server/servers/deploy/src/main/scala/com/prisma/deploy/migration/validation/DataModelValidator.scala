package com.prisma.deploy.migration.validation

import com.prisma.deploy.connector.FieldRequirementsInterface
import com.prisma.shared.models.ConnectorCapability
import org.scalactic.Or

trait DataModelValidator {
  def validate(dataModel: String, fieldRequirements: FieldRequirementsInterface, capabilities: Set[ConnectorCapability]): PrismaSdl Or Vector[DeployError]
}
