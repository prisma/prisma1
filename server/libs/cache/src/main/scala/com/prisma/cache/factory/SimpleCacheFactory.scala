package com.prisma.cache

import com.prisma.cache.factory.CacheFactory

import scala.concurrent.ExecutionContext

class SimpleCacheFactory extends CacheFactory {
  override def unbounded[K, V >: Null](): Cache[K, V] = SimpleCache[K, V]()

  override def lfu[K, V >: Null](initialCapacity: Int, maxCapacity: Int): Cache[K, V] = unbounded()

  override def lfuAsync[K, V >: Null](initialCapacity: Int, maxCapacity: Int)(implicit ec: ExecutionContext): AsyncCache[K, V] = SimpleAsyncCache[K, V]()
}
