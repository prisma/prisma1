package com.prisma.cache.factory

import com.prisma.cache.{AsyncCache, Cache}

import scala.concurrent.ExecutionContext

trait CacheFactory {
  def unbounded[K, V >: Null](): Cache[K, V]

  def lfu[K, V >: Null](initialCapacity: Int, maxCapacity: Int): Cache[K, V]

  def lfuAsync[K, V >: Null](initialCapacity: Int, maxCapacity: Int)(implicit ec: ExecutionContext): AsyncCache[K, V]
}
