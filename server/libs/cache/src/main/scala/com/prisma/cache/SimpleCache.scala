package com.prisma.cache

import scala.concurrent.{ExecutionContext, Future}

case class SimpleCache[K, V >: Null]() extends Cache[K, V] {
  val cache = new java.util.concurrent.ConcurrentHashMap[K, V]

  override def get(key: K): Option[V]                                 = Option(cache.get(key))
  override def put(key: K, value: V): Unit                            = cache.put(key, value)
  override def remove(key: K): Unit                                   = cache.remove(key)
  override def getOrUpdate(key: K, fn: () => V): V                    = cache.computeIfAbsent(key, (_: K) => fn())
  override def removeAll(fn: K => Boolean): Unit                      = cache.entrySet().removeIf(e => fn(e.getKey))
  override def getOrUpdateOpt(key: K, fn: () => Option[V]): Option[V] = Option(cache.computeIfAbsent(key, (_: K) => fn().orNull))
}

case class SimpleAsyncCache[K, V >: Null]()(implicit val ec: ExecutionContext) extends AsyncCache[K, V] {
  private val _underlying = SimpleCache[K, V]()

  override def get(key: K): Future[Option[V]]    = Future { _underlying.get(key) }
  override def remove(key: K): Unit              = _underlying.remove(key)
  override def removeAll(fn: K => Boolean): Unit = _underlying.removeAll(fn)

  override def put(key: K, value: Future[Option[V]]): Unit = value.map { v =>
    _underlying.put(key, v.orNull)
  }

  override def getOrUpdate(key: K, fn: () => Future[V]): Future[V] = {
    _underlying.get(key) match {
      case Some(v) =>
        Future.successful(v)

      case None =>
        val cacheValue = fn()
        put(key, cacheValue.map(Some(_)))
        cacheValue
    }
  }

  override def getOrUpdateOpt(key: K, fn: () => Future[Option[V]]): Future[Option[V]] = {
    _underlying.get(key) match {
      case x @ Some(_) =>
        Future.successful(x)

      case None =>
        fn().map {
          case x @ Some(v) =>
            _underlying.put(key, v)
            x

          case None =>
            None
        }
    }
  }
}
