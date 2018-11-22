import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.prisma.metrics.MetricsFacade;
import com.prisma.metrics.MetricsRegistry;
import com.prisma.metrics.dummy.DummyMetricsRegistry$;

@TargetClass(value = MetricsFacade.class)
final class Target_com_prisma_metrics_MetricsFacade {
    @Substitute
    public MetricsRegistry manager()
    {
        return DummyMetricsRegistry$.MODULE$;
    }
}
