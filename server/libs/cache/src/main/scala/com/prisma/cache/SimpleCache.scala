package com.prisma.cache

import scala.util.Try

case class SimpleCache[K, V]() extends Cache[K, V] {
  val cache = new java.util.concurrent.ConcurrentHashMap[K, V]

  override def get(key: K): Option[V]              = Try { cache.get(key) }.toOption
  override def put(key: K, value: V): Unit         = cache.put(key, value)
  override def remove(key: K): Unit                = cache.remove(key)
  override def getOrUpdate(key: K, fn: () => V): V = cache.computeIfAbsent(key, (_: K) => fn())
  override def removeAll(fn: K => Boolean): Unit   = cache.entrySet().removeIf(e => fn(e.getKey))

  override def getOrUpdateOpt(key: K, fn: () => Option[V]): Option[V] = ???
}
