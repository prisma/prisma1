package com.prisma.api.connector.mongo.extensions

import com.prisma.api.connector.mongo.database._
import org.mongodb.scala.{ClientSession, MongoDatabase}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object SlickReplacement {
  def run[A](database: MongoDatabase, action: MongoAction[A], session: ClientSession): Future[A] = {
    action match {
      case SuccessAction(value) =>
        Future.successful(value)

      case FailedAction(error) =>
        Future.failed(error)

      case SimpleMongoAction(fn) =>
        fn(database, session)

      case FlatMapAction(source, fn) =>
        for {
          result     <- run(database, source, session)
          nextResult <- run(database, fn(result), session)
        } yield nextResult

      case MapAction(source, fn) =>
        for {
          result <- run(database, source, session)
        } yield fn(result)

      case AsTryAction(action) =>
        run(database, action, session).transformWith(theTry => Future.successful(theTry))

      case SequenceAction(actions) =>
        sequence(database, actions, session)

    }
  }

  def sequence[A](database: MongoDatabase, actions: Vector[MongoAction[A]], session: ClientSession): Future[Vector[A]] = {
    if (actions.isEmpty) {
      Future.successful(Vector.empty)
    } else {
      for {
        headResult  <- run(database, actions.head, session)
        nextResults <- sequence(database, actions.tail, session)
      } yield headResult +: nextResults
    }
  }

}
