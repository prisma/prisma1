package com.prisma.deploy.connector.postgresql.database

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

case class CloudSecret(secret: String)

class CloudSecretTable(tag: Tag) extends Table[CloudSecret](tag, "CloudSecret") {
  def secret = column[String]("secret")
  def *      = secret <> (CloudSecret(_), CloudSecret.unapply)
}

object CloudSecretTable {
  def getSecret(implicit ec: ExecutionContext): DBIO[Option[String]] = Tables.CloudSecret.result.headOption.map(_.map(_.secret))
  def setSecret(secret: Option[String])(implicit ec: ExecutionContext): DBIO[Unit] = {
    secret match {
      case None    => deleteSecret()
      case Some(x) => upsertSecret(x)
    }
  }

  private def deleteSecret()(implicit ec: ExecutionContext) = Tables.CloudSecret.delete.map(_ => ())
  private def upsertSecret(secret: String)(implicit ec: ExecutionContext) = {
    for {
      existing <- getSecret
      _ <- if (existing.isDefined) {
            Tables.CloudSecret.update(CloudSecret(secret))
          } else {
            Tables.CloudSecret += CloudSecret(secret)
          }
    } yield ()
  }
}
