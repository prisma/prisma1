package com.prisma.deploy.migration.validation

import com.prisma.deploy.connector.FieldRequirementsInterface
import com.prisma.shared.models.ConnectorCapabilities
import org.scalactic.Or

trait DataModelValidator {
  def validate(dataModel: String, fieldRequirements: FieldRequirementsInterface, capabilities: ConnectorCapabilities): PrismaSdl Or Vector[DeployError]
}
