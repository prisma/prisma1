package cool.graph.javascriptEngine.lib

import java.io._
import java.util.concurrent.{AbstractExecutorService, TimeUnit}

import akka.actor._
import akka.contrib.process.StreamEvents.Ack
import akka.contrib.process._
import akka.pattern.AskTimeoutException
import cool.graph.javascriptEngine.lib.Engine.ExecuteJs
import io.apigee.trireme.core._
import io.apigee.trireme.kernel.streams.{NoCloseInputStream, NoCloseOutputStream}
import org.mozilla.javascript.RhinoException

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.blocking
import scala.concurrent.duration._
import scala.util.Try

/**
  * Declares an in-JVM Rhino based JavaScript engine supporting the Node API.
  * The <a href="https://github.com/apigee/trireme#trireme">Trireme</a> project provides this capability.
  * The actor is expected to be associated with a blocking dispatcher as its use of Jdk streams are blocking.
  */
class Trireme(
    stdArgs: immutable.Seq[String],
    stdEnvironment: Map[String, String],
    ioDispatcherId: String
) extends Engine(stdArgs, stdEnvironment) {

  // The main objective of this actor implementation is to establish actors for both the execution of
  // Trireme code (Trireme's execution is blocking), and actors for the source of stdio (which is also blocking).
  // This actor is then a conduit of the IO as a result of execution.

  val StdioTimeout = Engine.infiniteSchedulerTimeout(context.system.settings.config)

  def receive = {
    case ExecuteJs(source, args, timeout, timeoutExitValue, environment) =>
      val requester = sender()

      val stdinSink    = context.actorOf(BufferingSink.props(ioDispatcherId = ioDispatcherId), "stdin")
      val stdinIs      = new SourceStream(stdinSink, StdioTimeout)
      val stdoutSource = context.actorOf(ForwardingSource.props(self, ioDispatcherId = ioDispatcherId), "stdout")
      val stdoutOs     = new SinkStream(stdoutSource, StdioTimeout)
      val stderrSource = context.actorOf(ForwardingSource.props(self, ioDispatcherId = ioDispatcherId), "stderr")
      val stderrOs     = new SinkStream(stderrSource, StdioTimeout)

      try {
        context.become(
          engineIOHandler(
            stdinSink,
            stdoutSource,
            stderrSource,
            requester,
            Ack,
            timeout,
            timeoutExitValue
          ))

        context.actorOf(TriremeShell.props(
                          source,
                          stdArgs ++ args,
                          stdEnvironment ++ environment,
                          ioDispatcherId,
                          stdinIs,
                          stdoutOs,
                          stderrOs
                        ),
                        "trireme-shell") ! TriremeShell.Execute

      } finally {
        // We don't need stdin
        blocking(Try(stdinIs.close()))
      }
  }
}

object Trireme {

  /**
    * Give me a Trireme props.
    */
  def props(
      stdArgs: immutable.Seq[String] = Nil,
      stdEnvironment: Map[String, String] = Map.empty,
      ioDispatcherId: String = "blocking-process-io-dispatcher"
  ): Props = {
    Props(classOf[Trireme], stdArgs, stdEnvironment, ioDispatcherId)
      .withDispatcher(ioDispatcherId)
  }

}

/**
  * Manage the execution of the Trireme shell setting up its environment, running the main entry point
  * and sending its parent the exit code when we're done.
  */
class TriremeShell(
    source: String,
    args: immutable.Seq[String],
    environment: Map[String, String],
    ioDispatcherId: String,
    stdinIs: InputStream,
    stdoutOs: OutputStream,
    stderrOs: OutputStream
) extends Actor
    with ActorLogging {

  val AwaitTerminationTimeout = 1.second

  val blockingDispatcher = context.system.dispatchers.lookup(ioDispatcherId)
  val executorService = new AbstractExecutorService {
    def shutdown()                                    = throw new UnsupportedOperationException
    def isTerminated                                  = false
    def awaitTermination(l: Long, timeUnit: TimeUnit) = throw new UnsupportedOperationException
    def shutdownNow()                                 = throw new UnsupportedOperationException
    def isShutdown                                    = false
    def execute(runnable: Runnable)                   = blockingDispatcher.execute(runnable)
  }

  val env     = (sys.env ++ environment).asJava
  val sandbox = new Sandbox()
  sandbox.setAsyncThreadPool(executorService)
  val nodeEnv = new NodeEnvironment()
  nodeEnv.setSandbox(sandbox)
  sandbox.setStdin(new NoCloseInputStream(stdinIs))
  sandbox.setStdout(new NoCloseOutputStream(stdoutOs))
  sandbox.setStderr(new NoCloseOutputStream(stderrOs))

  def receive = {
    case TriremeShell.Execute =>
      if (log.isDebugEnabled) {
        log.debug("Invoking Trireme with {}", args)
      }

      val script = nodeEnv.createScript("thisIsAJsFile.js", source, args.toArray)
      script.setEnvironment(env)

      val senderSel = sender().path
      val senderSys = context.system
      script.execute.setListener(new ScriptStatusListener {
        def onComplete(script: NodeScript, status: ScriptStatus): Unit = {
          if (status.hasCause) {
            try {
              status.getCause match {
                case e: RhinoException =>
                  stderrOs.write(e.getLocalizedMessage.getBytes("UTF-8"))
                  stderrOs.write(e.getScriptStackTrace.getBytes("UTF-8"))
                case t =>
                  t.printStackTrace(new PrintStream(stderrOs))
              }
            } catch {
              case e: Throwable =>
                if (e.isInstanceOf[AskTimeoutException] || status.getCause.isInstanceOf[AskTimeoutException]) {
                  log.error(
                    e,
                    "Received a timeout probably because stdio sinks and sources were closed early given a timeout waiting for the JS to execute. Increase the timeout."
                  )
                } else {
                  log.error(status.getCause, "Problem completing Trireme. Throwing exception, meanwhile here's the Trireme problem")
                  throw e
                }
            }
          }
          // The script holds an NIO selector that needs to be closed, otherwise it leaks.
          script.close()
          stdoutOs.close()
          stderrOs.close()
          senderSys.actorSelection(senderSel) ! status.getExitCode
        }
      })
  }

  override def postStop() = {
    // The script pool is a cached thread pool so it should shut itself down, but it's better to clean up immediately,
    // and this means that our tests work.
    nodeEnv.getScriptPool.shutdown()
    nodeEnv.getScriptPool.awaitTermination(AwaitTerminationTimeout.toMillis, TimeUnit.MILLISECONDS)
  }
}

object TriremeShell {
  def props(
      moduleBase: String,
      args: immutable.Seq[String],
      environment: Map[String, String],
      ioDispatcherId: String = "blocking-process-io-dispatcher",
      stdinIs: InputStream,
      stdoutOs: OutputStream,
      stderrOs: OutputStream
  ): Props = {
    Props(classOf[TriremeShell], moduleBase, args, environment, ioDispatcherId, stdinIs, stdoutOs, stderrOs)
  }

  case object Execute

}
