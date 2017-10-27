 # JVM Profiler
 
 This lib aims to provide easy to use profilers for the JVM. It currently only contains the `MemoryProfiler` that captures statistics about heap and off heap memory.
 Additionally it collects informations about Garbage Collection times.
 
 The MemoryProfiler is built on top of our `metrics` lib. In order to schedule the `MemoryProfiler` you have to pass a `MetricsManager`. This `MetricsManager` is used to define the metrics the profiler will use. It will also use the underlying scheduler of the underlying `ActorSytem` of the manager to schedule the profiling.
 
 Here's an how to use it inside a MetricsManager:
 ```
import cool.graph.profiling.MemoryProfiler

object MyMetrics extends MetricsManager {
  override def serviceName: String = "MyMetrics"
  
  // use defaults for timings
  MemoryProfiler.schedule(this)
  
  // or use custom timings
  import scala.concurrent.duration._
  MemoryProfiler.schedule(this, initialDelay = 10.seconds, interval = 2.seconds)
}
```