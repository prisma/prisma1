package cool.graph.worker.utils

object Env {
  val clusterLocalRabbitUri = sys.env("RABBITMQ_URI")
  val bugsangApiKey         = sys.env("BUGSNAG_API_KEY")
}
