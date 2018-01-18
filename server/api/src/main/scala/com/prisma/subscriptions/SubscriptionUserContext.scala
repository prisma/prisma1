package com.prisma.subscriptions

import com.prisma.shared.models.Project
import sangria.ast.Document

case class SubscriptionUserContext(
    nodeId: String,
    project: Project,
    requestId: String,
    log: Function[String, Unit],
    queryAst: Option[Document] = None
)
