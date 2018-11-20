package com.prisma.cache

import scala.concurrent.Future

trait Cache[K, V] {
  def get(key: K): Option[V]

  def put(key: K, value: V): Unit

  def remove(key: K): Unit

  def getOrUpdate(key: K, fn: () => V): V

  def getOrUpdateOpt(key: K, fn: () => Option[V]): Option[V]

  def removeAll(fn: K => Boolean): Unit
}

trait AsyncCache[K, V] {
  def get(key: K): Future[Option[V]]

  def put(key: K, value: Future[Option[V]]): Unit

  def remove(key: K): Unit

  def getOrUpdate(key: K, fn: () => Future[V]): Future[V]

  def getOrUpdateOpt(key: K, fn: () => Future[Option[V]]): Future[Option[V]]

  def removeAll(fn: K => Boolean): Unit
}
