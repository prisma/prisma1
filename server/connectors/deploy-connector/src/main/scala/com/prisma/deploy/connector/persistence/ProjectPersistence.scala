package com.prisma.deploy.connector.persistence

import com.prisma.shared.models.Project

import scala.concurrent.Future

trait ProjectPersistence {
  def load(id: String): Future[Option[Project]]
  def loadAll(): Future[Seq[Project]]
  def create(project: Project): Future[Unit]
  def update(project: Project): Future[_]
  def delete(project: String): Future[Unit]
}
