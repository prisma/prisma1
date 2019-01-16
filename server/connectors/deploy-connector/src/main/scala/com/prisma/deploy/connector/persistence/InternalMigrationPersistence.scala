package com.prisma.deploy.connector.persistence
import enumeratum.{Enum, EnumEntry}

import scala.concurrent.Future

sealed trait InternalMigration extends EnumEntry {
  def id: String = entryName
}

object InternalMigration extends Enum[InternalMigration] {
  def values = findValues

  object RemoveIdColumnFromRelationTables extends InternalMigration { override def entryName = "remove-id-column-from-relation-tables" }
}

trait InternalMigrationPersistence {
  def loadAll(): Future[Vector[InternalMigration]]
  def create(mig: InternalMigration): Future[Unit]
}

object EmptyInternalMigrationPersistence extends InternalMigrationPersistence {
  override def loadAll(): Future[Vector[InternalMigration]] = Future.successful(Vector.empty)
  override def create(mig: InternalMigration): Future[Unit] = Future.successful(())
}
