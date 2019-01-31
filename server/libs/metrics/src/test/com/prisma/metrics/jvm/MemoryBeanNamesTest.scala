package com.prisma.metrics.jvm

import java.lang.management.ManagementFactory

import org.scalatest.{FlatSpec, Matchers}

class MemoryBeanNamesTest extends FlatSpec with Matchers {

  /**
    * Serial GC: -XX:+UseSerialGC
    * Parallel GC: -XX:+UseParallelGC
    *              -XX:+UseParallelOldGC
    *
    * Concurrent Mark Sweep: -XX:+UseConcMarkSweepGC
    * G1: -XX:+UseG1GC
    */
  import scala.collection.JavaConverters._

  val gcBeans = ManagementFactory.getGarbageCollectorMXBeans

  println(s"There are ${gcBeans.size()} beans")
  asScalaBuffer(gcBeans).foreach { gcBean =>
    println("-" * 75)
    println(s"name: ${gcBean.getName}")
    println(s"ObjectName.canonicalName: ${gcBean.getObjectName.getCanonicalName}")
    println(s"ObjectName.domain: ${gcBean.getObjectName.getDomain}")
    println(s"ObjectName.canonicalKeyPropertyListString: ${gcBean.getObjectName.getCanonicalKeyPropertyListString}")
    println(s"ObjectName.getKeyPropertyList: ${mapAsScalaMap(gcBean.getObjectName.getKeyPropertyList)}")
    println(s"memory pool names: ${gcBean.getMemoryPoolNames.toVector.mkString(", ")}")
  }

  /**
    * Results:
    *
    * NO ARGUMENTS:
    *
    * There are 2 beans
---------------------------------------------------------------------------
name: PS Scavenge
ObjectName.canonicalName: java.lang:name=PS Scavenge,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=PS Scavenge,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> PS Scavenge, type -> GarbageCollector)
memory pool names: PS Eden Space, PS Survivor Space
---------------------------------------------------------------------------
name: PS MarkSweep
ObjectName.canonicalName: java.lang:name=PS MarkSweep,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=PS MarkSweep,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> PS MarkSweep, type -> GarbageCollector)
memory pool names: PS Eden Space, PS Survivor Space, PS Old Gen

    *
    * -XX:+UseSerialGC:
    * There are 2 beans
---------------------------------------------------------------------------
name: Copy
ObjectName.canonicalName: java.lang:name=Copy,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=Copy,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> Copy, type -> GarbageCollector)
memory pool names: Eden Space, Survivor Space
---------------------------------------------------------------------------
name: MarkSweepCompact
ObjectName.canonicalName: java.lang:name=MarkSweepCompact,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=MarkSweepCompact,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> MarkSweepCompact, type -> GarbageCollector)
memory pool names: Eden Space, Survivor Space, Tenured Gen

    *
    * -XX:+UseParallelGC:
    * There are 2 beans
---------------------------------------------------------------------------
name: PS Scavenge
ObjectName.canonicalName: java.lang:name=PS Scavenge,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=PS Scavenge,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> PS Scavenge, type -> GarbageCollector)
memory pool names: PS Eden Space, PS Survivor Space
---------------------------------------------------------------------------
name: PS MarkSweep
ObjectName.canonicalName: java.lang:name=PS MarkSweep,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=PS MarkSweep,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> PS MarkSweep, type -> GarbageCollector)
memory pool names: PS Eden Space, PS Survivor Space, PS Old Gen

    *
    * -XX:+UseParallelOldGC:
    * There are 2 beans
---------------------------------------------------------------------------
name: PS Scavenge
ObjectName.canonicalName: java.lang:name=PS Scavenge,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=PS Scavenge,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> PS Scavenge, type -> GarbageCollector)
memory pool names: PS Eden Space, PS Survivor Space
---------------------------------------------------------------------------
name: PS MarkSweep
ObjectName.canonicalName: java.lang:name=PS MarkSweep,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=PS MarkSweep,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> PS MarkSweep, type -> GarbageCollector)
memory pool names: PS Eden Space, PS Survivor Space, PS Old Gen


    * -XX:+UseConcMarkSweepGC:
    * There are 2 beans
---------------------------------------------------------------------------
name: ParNew
ObjectName.canonicalName: java.lang:name=ParNew,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=ParNew,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> ParNew, type -> GarbageCollector)
memory pool names: Par Eden Space, Par Survivor Space
---------------------------------------------------------------------------
name: ConcurrentMarkSweep
ObjectName.canonicalName: java.lang:name=ConcurrentMarkSweep,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=ConcurrentMarkSweep,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> ConcurrentMarkSweep, type -> GarbageCollector)
memory pool names: Par Eden Space, Par Survivor Space, CMS Old Gen

    * -XX:+UseG1GC:
    * ---------------------------------------------------------------------------
name: G1 Young Generation
ObjectName.canonicalName: java.lang:name=G1 Young Generation,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=G1 Young Generation,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> G1 Young Generation, type -> GarbageCollector)
memory pool names: G1 Eden Space, G1 Survivor Space
---------------------------------------------------------------------------
name: G1 Old Generation
ObjectName.canonicalName: java.lang:name=G1 Old Generation,type=GarbageCollector
ObjectName.domain: java.lang
ObjectName.canonicalKeyPropertyListString: name=G1 Old Generation,type=GarbageCollector
ObjectName.getKeyPropertyList: Map(name -> G1 Old Generation, type -> GarbageCollector)
memory pool names: G1 Eden Space, G1 Survivor Space, G1 Old Gen
    *
    *
    *
    *
    */
  "bla" should "bla" in {
    true should be(true)
  }
}
