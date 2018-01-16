package com.prisma.workers.payloads

case class Webhook(
    projectId: String,
    functionId: String,
    requestId: String,
    url: String,
    payload: String,
    id: String,
    headers: Map[String, String]
)
