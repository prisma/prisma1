package com.prisma.metrics;

import akka.actor.ActorSystem;
import scala.Tuple2;

public class MetricsFacade {
    private MetricsRegistry manager = null;

//    public MetricsRegistry manager() {
//        return com.prisma.metrics.micrometer.MicrometerMetricsRegistry$.MODULE$;
//    }

    public void initialize(MetricsRegistry registryToUse, PrismaCloudSecretLoader secretLoader, ActorSystem system) {
        manager = registryToUse;
        manager.initialize(secretLoader, system);
    }

    public GaugeMetric defineGauge(String name, Tuple2<CustomTag, String> ... predefTags) {
        return manager.defineGauge(name, predefTags);
    }

    public CounterMetric defineCounter(String name, CustomTag ... customTags) {
        return manager.defineCounter(name, customTags);
    }

    public TimerMetric defineTimer(String name, CustomTag ... customTags) {
        return manager.defineTimer(name, customTags);
    }
}
