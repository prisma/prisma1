package com.prisma.deploy.migration

import com.prisma.deploy.migration.mutactions.DeployMutaction
import com.prisma.shared.models.{MigrationStep, Schema}

trait MigrationStepMapper {
  def mutactionFor(previousSchema: Schema, nextSchema: Schema, step: MigrationStep): Option[DeployMutaction]
}
