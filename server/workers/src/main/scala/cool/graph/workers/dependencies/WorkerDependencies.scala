package cool.graph.workers.dependencies

import com.prisma.errors.ErrorReporter
import cool.graph.akkautil.http.SimpleHttpClient
import cool.graph.messagebus.QueueConsumer
import cool.graph.workers.payloads.Webhook

trait WorkerDependencies {
  def httpClient: SimpleHttpClient
  def webhooksConsumer: QueueConsumer[Webhook]

  implicit val reporter: ErrorReporter
}
