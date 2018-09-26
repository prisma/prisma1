package com.prisma.errors

import scala.collection.immutable.Seq

trait ErrorReporter {
  def report(t: Throwable, meta: ErrorMetadata*): Unit
}

trait ErrorMetadata

case class RequestMetadata(requestId: String, method: String, uri: String, headers: Seq[(String, String)]) extends ErrorMetadata
case class GraphQlMetadata(query: String, variables: String)                                               extends ErrorMetadata
case class ProjectMetadata(id: String)                                                                     extends ErrorMetadata
case class GenericMetadata(group: String, key: String, value: String)                                      extends ErrorMetadata
