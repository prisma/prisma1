package com.prisma.cache

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future

class CaffeineImplForAsyncCacheSpec extends FlatSpec with Matchers with AwaitUtil {
  import scala.concurrent.ExecutionContext.Implicits.global

  def newCache = CaffeineImplForAsyncCache.lfu[String, String](initialCapacity = 100, maxCapacity = 100)

  //this test is flaky

  "it" should "handle None results correctly" in {
    val cache  = newCache
    val result = await(cache.getOrUpdateOpt("key", () => Future.successful(None)))
    result should be(None)

    val foo     = Some("foo")
    val result2 = await(cache.getOrUpdateOpt("key", () => Future.successful(foo)))
    result2 should be(foo)
  }
}
