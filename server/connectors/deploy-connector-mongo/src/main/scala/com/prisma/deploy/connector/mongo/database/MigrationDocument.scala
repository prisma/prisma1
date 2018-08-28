package com.prisma.deploy.connector.mongo.database

import com.prisma.shared.models.MigrationStatus
import com.prisma.shared.models.MigrationStatus._
import org.joda.time.DateTime
import play.api.libs.json.JsValue

case class MigrationDocument(
    projectId: String,
    revision: Int,
    schema: JsValue,
    functions: JsValue,
    status: MigrationStatus,
    applied: Int,
    rolledBack: Int,
    steps: JsValue,
    errors: JsValue,
    startedAt: Option[DateTime],
    finishedAt: Option[DateTime]
)
