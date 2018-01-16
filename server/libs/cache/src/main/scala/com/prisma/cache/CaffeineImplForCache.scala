package cool.graph.cache

import com.github.benmanes.caffeine.cache.{Caffeine, Cache => CaffeineCache}
import scala.compat.java8.FunctionConverters._

object CaffeineImplForCache {
  def unbounded[K, V >: Null](): CaffeineImplForCache[K, V] = {
    val caffeineCache = Caffeine.newBuilder().asInstanceOf[Caffeine[K, V]].build[K, V]()
    CaffeineImplForCache(caffeineCache)
  }

  def lfu[K, V >: Null](initialCapacity: Int, maxCapacity: Int): CaffeineImplForCache[K, V] = {
    val caffeineCache = Caffeine
      .newBuilder()
      .initialCapacity(initialCapacity)
      .maximumSize(maxCapacity)
      .asInstanceOf[Caffeine[K, V]]
      .build[K, V]()
    CaffeineImplForCache(caffeineCache)
  }
}

case class CaffeineImplForCache[K, V >: Null](underlying: CaffeineCache[K, V]) extends Cache[K, V] {

  override def get(key: K): Option[V] = Option(underlying.getIfPresent(key))

  override def put(key: K, value: V): Unit = underlying.put(key, value)

  override def remove(key: K): Unit = underlying.invalidate(key)

  override def getOrUpdate(key: K, fn: () => V): V = {
    val caffeineFunction = (_: K) => fn()
    underlying.get(key, asJavaFunction(caffeineFunction))
  }

  override def getOrUpdateOpt(key: K, fn: () => Option[V]): Option[V] = {
    val caffeineFunction = (_: K) => fn().orNull
    Option(underlying.get(key, asJavaFunction(caffeineFunction)))
  }
}
