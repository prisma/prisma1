package com.prisma.utils.mongo

import org.mongodb.scala.Document

object MongoExtensions extends MongoExtensions
trait MongoExtensions {
  implicit def documentExtensions(doc: Document) = new DocumentExtensions(doc)
}

class DocumentExtensions(val doc: Document) extends AnyVal {
  def readAs[A](implicit reads: DocumentReads[A]): DocumentReadResult[A] = reads.reads(doc)

  def as[A](implicit reads: DocumentReads[A]): A = readAs[A].get
}
