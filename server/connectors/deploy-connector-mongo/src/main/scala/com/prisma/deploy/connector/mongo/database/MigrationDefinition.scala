package com.prisma.deploy.connector.mongo.database

import com.prisma.shared.models.MigrationStatus
import com.prisma.shared.models.MigrationStatus._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api.libs.json.JsValue

object MigrationDefinition {
  def apply(projectId: String,
            revision: Int,
            schema: JsValue,
            functions: JsValue,
            status: MigrationStatus,
            applied: Int,
            rolledBack: Int,
            steps: JsValue,
            errors: JsValue,
            startedAt: Option[DateTime],
            finishedAt: Option[DateTime]): MigrationDefinition = {

    MigrationDefinition(_id = new ObjectId(),
                        projectId,
                        revision,
                        schema,
                        functions,
                        stringStatus(status),
                        applied,
                        rolledBack,
                        steps,
                        errors,
                        startedAt,
                        finishedAt)
  }

  def stringStatus(status: MigrationStatus) = status match {
    case MigrationStatus.InProgress      => "IN_PROGRESS"
    case MigrationStatus.Pending         => "PENDING"
    case MigrationStatus.Success         => "SUCCESS"
    case MigrationStatus.RollingBack     => "ROLLING_BACK"
    case MigrationStatus.RollbackSuccess => "ROLLBACK_SUCCESS"
    case MigrationStatus.RollbackFailure => "ROLLBACK_FAILURE"
  }
}

case class MigrationDefinition(
    _id: ObjectId,
    projectId: String,
    revision: Int,
    schema: JsValue,
    functions: JsValue,
    status: String,
    applied: Int,
    rolledBack: Int,
    steps: JsValue,
    errors: JsValue,
    startedAt: Option[DateTime],
    finishedAt: Option[DateTime]
)
