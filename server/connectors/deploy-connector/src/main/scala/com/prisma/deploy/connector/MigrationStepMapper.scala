package com.prisma.deploy.connector

import com.prisma.shared.models.{MigrationStep, Schema}

trait MigrationStepMapper {
  def mutactionFor(previousSchema: Schema, nextSchema: Schema, step: MigrationStep): Option[DeployMutaction]
}
