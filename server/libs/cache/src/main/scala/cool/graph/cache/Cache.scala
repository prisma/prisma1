package cool.graph.cache

import scala.concurrent.{ExecutionContext, Future}

object Cache {
  def unbounded[K, V >: Null](): Cache[K, V] = {
    CaffeineImplForCache.unbounded[K, V]
  }

  def lfu[K, V >: Null](initialCapacity: Int, maxCapacity: Int): Cache[K, V] = {
    CaffeineImplForCache.lfu(initialCapacity = initialCapacity, maxCapacity = maxCapacity)
  }

  def lfuAsync[K, V >: Null](initialCapacity: Int, maxCapacity: Int)(implicit ec: ExecutionContext): AsyncCache[K, V] = {
    CaffeineImplForAsyncCache.lfu(initialCapacity = initialCapacity, maxCapacity = maxCapacity)
  }
}

trait Cache[K, V] {
  def get(key: K): Option[V]

  def put(key: K, value: V): Unit

  def remove(key: K): Unit

  def getOrUpdate(key: K, fn: () => V): V

  def getOrUpdateOpt(key: K, fn: () => Option[V]): Option[V]
}

trait AsyncCache[K, V] {
  def get(key: K): Future[Option[V]]

  def put(key: K, value: Future[Option[V]]): Unit

  def remove(key: K): Unit

  def getOrUpdate(key: K, fn: () => Future[V]): Future[V]

  def getOrUpdateOpt(key: K, fn: () => Future[Option[V]]): Future[Option[V]]
}
