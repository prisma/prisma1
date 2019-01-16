package com.prisma.deploy.connector.persistence
import scala.concurrent.Future

case class InternalMigration(id: String)

trait InternalMigrationPersistence {
  def loadAll(): Future[Vector[InternalMigration]]
  def create(mig: InternalMigration): Future[Unit]
}

object EmptyInternalMigrationPersistence extends InternalMigrationPersistence {
  override def loadAll(): Future[Vector[InternalMigration]] = Future.successful(Vector.empty)
  override def create(mig: InternalMigration): Future[Unit] = Future.successful(())
}
