package com.prisma.subscriptions

import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.Project
import sangria.ast.Document

case class SubscriptionUserContext(
    nodeId: IdGCValue,
    project: Project,
    requestId: String,
    log: Function[String, Unit],
    queryAst: Option[Document] = None
)
