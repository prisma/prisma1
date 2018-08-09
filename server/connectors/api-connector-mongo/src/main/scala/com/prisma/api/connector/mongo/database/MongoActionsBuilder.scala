package com.prisma.api.connector.mongo.database
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

// format: off
trait AllActions
  extends NodeActions
//    with RelationActions
//    with ScalarListActions
//    with ValidationActions
//    with RelayIdActions
//    with ImportActions
    with MiscActions

trait AllQueries
//  extends NodeSingleQueries
//    with NodeManyQueries
//    with RelationQueries
//    with ScalarListQueries
//    with MiscQueries
// format: on

case class MongoActionsBuilder(
    schemaName: String,
    client: MongoClient
)(implicit ec: ExecutionContext)
    extends AllActions
    with AllQueries {
  val database = client.getDatabase(schemaName)
}

//sealed trait MongoAction[+S, +A] {
//  def map[B](f: A => B): MongoAction[A, B] = MapAction(this, f)
//
//  def flatMap[B](f: A => MongoAction[_, B]): MongoAction[A, B] = FlatMapAction(this, f)
//}
//case class SuccessAction[A](value: A)                                                 extends MongoAction[Nothing, A]
//case class SimpleMongoAction[A](fn: MongoDatabase => Future[A])                       extends MongoAction[Nothing, A]
//case class MapAction[A, B](source: MongoAction[_, A], fn: A => B)                     extends MongoAction[A, B]
//case class FlatMapAction[A, B](source: MongoAction[_, A], fn: A => MongoAction[_, B]) extends MongoAction[A, B]

case class SimpleMongoAction[+A](fn: MongoDatabase => Future[A])
