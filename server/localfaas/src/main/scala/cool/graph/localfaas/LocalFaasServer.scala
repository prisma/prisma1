package cool.graph.localfaas

import java.io._

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import better.files.Cmds._
import cool.graph.akkautil.http.Server
import cool.graph.localfaas.actors.MappingActor
import cool.graph.localfaas.actors.MappingActor.{GetHandler, SaveMapping}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{JsError, JsSuccess, Json}
import better.files.File
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.sys.process.{Process, _}
import scala.util.{Failure, Success, Try}

/**
  * TODOs:
  * - Prevent concurrent deployment of the same function.
  * - Support multiple node versions. nvm has good concepts for that.
  * - Have a notion of different langs.
  * - Cleanup in error cases.
  * - Jail the subprocesses to their deployment.
  * - Tests.
  */
case class BadRequestException(reason: String) extends Exception(reason)

case class FunctionRuntimeServer(prefix: String = "", workingDir: File)(implicit system: ActorSystem, materializer: ActorMaterializer)
    extends Server
    with PlayJsonSupport {
  import Conversions._
  import system.dispatcher

  val functionHandlerFile = (workingDir / "handlers.json").createIfNotExists() // persistence file for handlers
  val functionsDir        = (workingDir / "functions").createIfNotExists(asDirectory = true, createParents = true)
  val deploymentsDir      = (workingDir / "deployments").createIfNotExists(asDirectory = true, createParents = true)

  implicit val timeout = Timeout(5.seconds)

  val exceptionHandler = ExceptionHandler {
    case e: BadRequestException => println(e.getMessage); complete(StatusCodes.BadRequest          -> StatusResponse(success = false, Some(e.getMessage)))
    case e                      => println(e.getMessage); complete(StatusCodes.InternalServerError -> StatusResponse(success = false, Some(e.getMessage)))
  }

  // Actor responsible for persisting the mapping of functions to their handlers
  val mappingActor = system.actorOf(Props(MappingActor(functionHandlerFile)))

  val innerRoutes = handleExceptions(exceptionHandler) {
    ((put | post) & pathPrefix("files")) {
      withoutSizeLimit {
        extractRequest { req =>
          pathPrefix(Segment) { projectId =>
            pathPrefix(Segment) { deploymentId =>
              val deployDirForProject = (deploymentsDir / projectId / deploymentId).createIfNotExists(asDirectory = true, createParents = true).clear()
              val destFile            = deployDirForProject / s"$deploymentId.zip"

              println(s"Writing to ${destFile.path}")

              val sink        = FileIO.toPath(destFile.path)
              val writeResult = req.entity.dataBytes.runWith(sink)

              onSuccess(writeResult) { result =>
                result.status match {
                  case Success(_) =>
                    println(s"Wrote ${result.count} bytes to disk. Unzipping...")

                    Try {
                      Utils.unzip(destFile, deployDirForProject)
                    } match {
                      case Success(_) =>
                        Try { destFile.delete() }
                        println("Done unzipping.")

                      case Failure(e) =>
                        Try { deployDirForProject.clear() }
                        println(s"Error while unzipping: $e")
                        throw e
                    }

                    complete(StatusResponse(success = true))

                  case Failure(e) =>
                    throw e
                }
              }
            }
          }
        }
      }
    } ~
      post {
        pathPrefix("deploy") {
          pathPrefix(Segment) { projectId =>
            entity(as[DeploymentInput]) { input =>
              println(s"Deploying function ${input.functionName} for project $projectId...")

              // Extract deployment ID
              val segments = Uri(input.zipUrl).path.toString().stripPrefix("/").split("/")

              if (segments.length != 4 || segments.take(3).toSeq != Seq("functions", "files", projectId)) {
                throw BadRequestException(s"Invalid zip URL '${input.zipUrl}', expected path '/functions/files/$projectId/<deploymentID>'.")
              }

              val deploymentId      = segments.last
              val functionArtifacts = deploymentsDir / projectId / deploymentId

              if (!functionArtifacts.exists || functionArtifacts.isEmpty) {
                throw BadRequestException(
                  s"Deployment '$deploymentId' does not exist. Make sure to deploy the necessary files first before deploying the function.")
              }

              // Check handler validity - if there are windows backslashes, try converting and check again
              val inputHandler = input.handlerPath
              val handlerPath = ((functionArtifacts / inputHandler).exists, inputHandler.contains("\\")) match {
                case (true, _) =>
                  inputHandler

                case (false, true) =>
                  val convertedHandler = inputHandler.replaceAllLiterally("""\""", "/")
                  if ((functionArtifacts / convertedHandler).exists) {
                    convertedHandler
                  } else {
                    throw BadRequestException(s"Handler '$inputHandler' does not exist in the given archive.")
                  }

                case _ =>
                  throw BadRequestException(s"Handler '$inputHandler' does not exist in the given archive.")
              }

              println(s"Using handler '$handlerPath'...")

              val functionDeploymentPath = (functionsDir / projectId / input.functionName).createIfNotExists(asDirectory = true, createParents = true).clear()
              cp(functionArtifacts, functionDeploymentPath)

              mappingActor ! SaveMapping(projectId, input.functionName, handlerPath)

              println(s"Deploying function ${input.functionName} for project $projectId... Done.")
              complete(StatusResponse(success = true))
            }
          }
        } ~
          pathPrefix("invoke") {
            pathPrefix(Segment) { projectId =>
              entity(as[FunctionInvocation]) { invocation =>
                val input       = Json.parse(invocation.input).toString
                val handlerPath = mappingActor ? GetHandler(projectId, invocation.functionName)

                val invocationResult = handlerPath.mapTo[String].map { path =>
                  val handlerFile = functionsDir / projectId / invocation.functionName / path

                  if (path.isEmpty || !handlerFile.exists) {
                    throw BadRequestException(s"Function can not be invoked - no handler found. Function is likely not (fully) deployed.")
                  }

                  var stdout: String = ""
                  var stderr: String = ""

                  // todo set CWD to handler root? (somehow not required for node, but for other langs)
                  val io = new ProcessIO(
                    (out: OutputStream) => {
                      out.write(input.getBytes("UTF-8"))
                      out.flush()
                      out.close()
                    },
                    (in: InputStream) => {
                      stdout = scala.io.Source.fromInputStream(in).mkString
                      in.close()
                    },
                    (errIn: InputStream) => {
                      stderr = scala.io.Source.fromInputStream(errIn).mkString
                      errIn.close()
                    }
                  )

                  val startTime = System.currentTimeMillis()
                  val process   = Process("node", Seq(handlerFile.path.toString)).run(io)
                  val exitCode  = process.exitValue()
                  val duration  = System.currentTimeMillis() - startTime

                  // For now only the stdout of the wrapper process is really interesting.
                  val parsedResult = Json.parse(stdout).validate[FunctionInvocationResult] match {
                    case JsSuccess(res, _) => res
                    case JsError(e)        => println(e); FunctionInvocationResult(None, None, None, stdout, stderr)
                  }

                  println(stdout)

                  val error   = parsedResult.error
                  val success = (error.isEmpty || error.exists(e => e.isEmpty || e == "null" || e == "{}")) && exitCode == 0

                  parsedResult.printSummary(duration, success, projectId, invocation.functionName)
                  parsedResult.copy(
                    success = Some(success),
                    stdout = parsedResult.stdout.stripLineEnd.trim,
                    stderr = parsedResult.stderr.stripLineEnd.trim
                  )
                }

                complete(invocationResult)
              }
            }
          }
      } ~
      delete {
        pathPrefix(Segment) { projectId =>
          pathPrefix(Segment) { functionName =>
            // We currently have no undeploy concept in the backend, WIP
            complete("RIP")
          }
        }
      }
  }

  override def healthCheck = Future.successful(())
}
