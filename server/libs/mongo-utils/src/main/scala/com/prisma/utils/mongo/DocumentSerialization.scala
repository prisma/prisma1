package com.prisma.utils.mongo

import java.util.NoSuchElementException

import org.mongodb.scala.Document

trait DocumentWrites[-A] {
  def writes(o: A): Document
}

trait DocumentReads[A] {
  def reads(document: Document): DocumentReadResult[A]
}
sealed trait DocumentReadResult[+A] {
  def get: A
}
case class DocumentReadSuccess[A](value: A) extends DocumentReadResult[A] {
  def get = value
}
case class DocumentReadError(msg: String) extends DocumentReadResult[Nothing] {
  def get = throw new NoSuchElementException(msg)
}

trait DocumentFormat[A] extends DocumentReads[A] with DocumentWrites[A]
