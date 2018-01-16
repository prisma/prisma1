package cool.graph.cache

import java.util.concurrent.{CompletableFuture, Executor}
import java.util.function.BiFunction

import com.github.benmanes.caffeine.cache.{AsyncCacheLoader, Caffeine, AsyncLoadingCache => AsyncCaffeineCache, Cache => CaffeineCache}
import scala.compat.java8.FunctionConverters._
import scala.compat.java8.FutureConverters._

import scala.concurrent.{ExecutionContext, Future}

object CaffeineImplForAsyncCache {
  def lfu[K, V >: Null](initialCapacity: Int, maxCapacity: Int)(implicit ec: ExecutionContext): AsyncCache[K, V] = {
    val caffeineCache = Caffeine
      .newBuilder()
      .initialCapacity(initialCapacity)
      .maximumSize(maxCapacity)
      .asInstanceOf[Caffeine[K, V]]
      .buildAsync[K, V](dummyLoader[K, V])
    CaffeineImplForAsyncCache(caffeineCache)
  }

  //LfuCache requires a loader function on creation - this will not be used.
  private def dummyLoader[K, V] = new AsyncCacheLoader[K, V] {
    def asyncLoad(k: K, e: Executor) =
      Future.failed[V](new RuntimeException("Dummy loader should not be used by LfuCache")).toJava.toCompletableFuture
  }
}

case class CaffeineImplForAsyncCache[K, V >: Null](underlying: AsyncCaffeineCache[K, V])(implicit ec: ExecutionContext) extends AsyncCache[K, V] {

  override def get(key: K): Future[Option[V]] = {
    val cacheEntry = underlying.getIfPresent(key)
    if (cacheEntry != null) {
      cacheEntry.toScala.map(Some(_))
    } else {
      Future.successful(None)
    }
  }

  override def put(key: K, value: Future[Option[V]]): Unit = {
    val asCompletableNullableFuture = value.map(_.orNull).toJava.toCompletableFuture
    underlying.put(key, asCompletableNullableFuture)
  }

  override def remove(key: K): Unit = underlying.synchronous().invalidate(key)

  override def getOrUpdate(key: K, fn: () => Future[V]): Future[V] = {
    val javaFn = toCaffeineMappingFunction[K, V](fn)
    underlying.get(key, javaFn).toScala
  }

  override def getOrUpdateOpt(key: K, fn: () => Future[Option[V]]): Future[Option[V]] = {
    val nullable: () => Future[V] = () => fn().map(_.orNull)
    val javaFn                    = toCaffeineMappingFunction[K, V](nullable)
    val cacheEntry                = underlying.get(key, javaFn)
    if (cacheEntry != null) {
      cacheEntry.toScala.map(Option(_))
    } else {
      Future.successful(None)
    }
  }

  private def toCaffeineMappingFunction[K, V](genValue: () ⇒ Future[V]): BiFunction[K, Executor, CompletableFuture[V]] = {
    asJavaBiFunction[K, Executor, CompletableFuture[V]]((_, _) ⇒ genValue().toJava.toCompletableFuture)
  }
}
