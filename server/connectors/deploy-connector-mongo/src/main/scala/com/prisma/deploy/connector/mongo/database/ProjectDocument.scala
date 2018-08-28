package com.prisma.deploy.connector.mongo.database

import play.api.libs.json.JsValue

case class ProjectDocument(
    id: String,
    secrets: JsValue,
    allowQueries: Boolean,
    allowMutations: Boolean,
    functions: JsValue
)
