package com.prisma.api.subscriptions

case class Webhook(
    projectId: String,
    functionName: String,
    requestId: String,
    url: String,
    payload: String,
    id: String,
    headers: Map[String, String]
)
