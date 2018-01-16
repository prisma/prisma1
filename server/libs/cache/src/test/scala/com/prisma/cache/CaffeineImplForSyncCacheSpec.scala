package com.prisma.cache

import org.scalatest.{FlatSpec, Matchers}

class CaffeineImplForSyncCacheSpec extends FlatSpec with Matchers {

  def newCache = CaffeineImplForCache.lfu[String, String](initialCapacity = 100, maxCapacity = 100)

  "it" should "handle None results correctly" in {
    val cache  = newCache
    val result = cache.getOrUpdateOpt("key", () => None)
    result should be(None)
    cache.underlying.estimatedSize() should be(0)

    val foo     = Some("foo")
    val result2 = cache.getOrUpdateOpt("key", () => foo)
    result2 should be(foo)
    cache.underlying.estimatedSize() should be(1)
  }
}
