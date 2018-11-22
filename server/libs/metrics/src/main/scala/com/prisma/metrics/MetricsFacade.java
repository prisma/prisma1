package com.prisma.metrics;

import akka.actor.ActorSystem;
import com.prisma.metrics.micrometer.MicrometerMetricsRegistry$;
import scala.Tuple2;

public class MetricsFacade {
    public MetricsRegistry manager() {
        return MicrometerMetricsRegistry$.MODULE$;
    }

    public void initialize(PrismaCloudSecretLoader secretLoader, ActorSystem system) {
        System.out.println("Using manager: " + manager().getClass().getName());
        manager().initialize(secretLoader, system);
    }

    public GaugeMetric defineGauge(String name, Tuple2<CustomTag, String> ... predefTags) {
        return manager().defineGauge(name, predefTags);
    }

    public CounterMetric defineCounter(String name, CustomTag ... customTags) {
        return manager().defineCounter(name, customTags);
    }

    public TimerMetric defineTimer(String name, CustomTag ... customTags) {
        return manager().defineTimer(name, customTags);
    }
}