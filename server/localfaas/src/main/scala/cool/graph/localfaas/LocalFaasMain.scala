package cool.graph.localfaas

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import better.files.File.root
import cool.graph.akkautil.http.ServerExecutor

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object LocalFaasMain extends App {
  implicit val system       = ActorSystem("functions-runtime")
  implicit val materializer = ActorMaterializer()

  val port       = sys.env.getOrElse("FUNCTIONS_PORT", sys.error("FUNCTIONS_PORT env var required but not found.")).toInt
  val workingDir = (root / "var" / "faas").createIfNotExists(asDirectory = true, createParents = true)

  val executor = ServerExecutor(
    port = port,
    FunctionRuntimeServer("functions", workingDir)
  )

  executor.start
  Await.result(system.whenTerminated, Duration.Inf)
  executor.stop
}
