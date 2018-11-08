package com.prisma.api.connector.mongo.extensions

import com.prisma.api.connector.mongo.database._
import org.mongodb.scala.MongoDatabase

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object SlickReplacement {
  def run[A](database: MongoDatabase, action: MongoAction[A]): Future[A] = {
    action match {
      case SuccessAction(value) =>
        Future.successful(value)

      case FailedAction(error) =>
        Future.failed(error)

      case SimpleMongoAction(fn) =>
        fn(database)

      case FlatMapAction(source, fn) =>
        for {
          result     <- run(database, source)
          nextResult <- run(database, fn(result))
        } yield nextResult

      case MapAction(source, fn) =>
        for {
          result <- run(database, source)
        } yield fn(result)

      case AsTryAction(action) =>
        run(database, action).transformWith(theTry => Future.successful(theTry))

      case SequenceAction(actions) =>
        sequence(database, actions)

    }
  }

  def sequence[A](database: MongoDatabase, actions: Vector[MongoAction[A]]): Future[Vector[A]] = {
    if (actions.isEmpty) {
      Future.successful(Vector.empty)
    } else {
      for {
        headResult  <- run(database, actions.head)
        nextResults <- sequence(database, actions.tail)
      } yield headResult +: nextResults
    }
  }

}
