package com.prisma.cache.factory

import com.prisma.cache._

import scala.concurrent.ExecutionContext

class CaffeineCacheFactory extends CacheFactory {
  def unbounded[K, V >: Null](): Cache[K, V] = {
    CaffeineImplForCache.unbounded[K, V]()
  }

  def lfu[K, V >: Null](initialCapacity: Int, maxCapacity: Int): Cache[K, V] = {
    CaffeineImplForCache.lfu(initialCapacity = initialCapacity, maxCapacity = maxCapacity)
  }

  def lfuAsync[K, V >: Null](initialCapacity: Int, maxCapacity: Int)(implicit ec: ExecutionContext): AsyncCache[K, V] = {
    CaffeineImplForAsyncCache.lfu(initialCapacity = initialCapacity, maxCapacity = maxCapacity)
  }
}
