package util

import java.io.ByteArrayInputStream

import play.api.libs.json._

case class TestDatabase() {
  def setup(project: Project): Unit = {
    val engine = MigrationEngine(project)
    engine.reset()
    engine.inferAndApply()
  }

  def truncateProjectTables(project: Project): Unit = {
    // FIXME: implement truncation instead of this stupid approach
    setup(project)
  }
}

case class MigrationEngine(project: Project) {
  implicit val inferMigrationStepsInputWrites  = Json.writes[InferMigrationStepsInput]
  implicit val applyMigrationInputWrites       = Json.writes[ApplyMigrationInput]
  implicit val dataModelWarningOrErrorReads    = Json.reads[DataModelWarningOrError]
  implicit val migrationStepsResultOutputReads = Json.reads[MigrationStepsResultOutput]
  implicit val rpcResultReads                  = Json.reads[RpcResult]

  val migrationId = "test_migration_id"

  def inferAndApply(): Unit = {
    val result = inferMigrationSteps()
    applyMigration(ApplyMigrationInput(migrationId, result.datamodelSteps, force = None))
  }

  def inferMigrationSteps(): MigrationStepsResultOutput = {
    val params = InferMigrationStepsInput(
      migrationId = migrationId,
      datamodel = project.dataModel,
      assumeToBeApplied = Vector.empty
    )
    sendRpcCall[InferMigrationStepsInput, MigrationStepsResultOutput]("inferMigrationSteps", params)
  }

  def applyMigration(input: ApplyMigrationInput): Unit = {
    val _: JsValue = sendRpcCall[ApplyMigrationInput, JsValue]("applyMigration", input)
    ()
  }

  def reset(): Unit = {
    sendRpcCallInternal[JsValue]("reset", Json.obj())
  }

  private def sendRpcCall[A, B](method: String, params: A)(implicit writes: OWrites[A], reads: Reads[B]): B = {
    sendRpcCallInternal[B](method, Json.toJsObject(params))
  }

  private def sendRpcCallInternal[B](method: String, params: JsObject)(implicit reads: Reads[B]): B = {
    val rpcCall = envelope(method, params)
//    println(s"sending to MigrationEngine: $rpcCall")
    val cmd         = List(EnvVars.migrationEngineBinaryPath, "-s", "-d", project.dataModelPath)
    val inputStream = new ByteArrayInputStream(rpcCall.toString.getBytes("UTF-8"))
    val output: String = {
      import scala.sys.process._
      (cmd #< inputStream).!!
    }
    val lastLine = output.lines.foldLeft("")((_, line) => line)
    Json.parse(lastLine).validate[RpcResult] match {
      case JsSuccess(rpcResult, _) => rpcResult.result.as[B]
      case e: JsError => {
        println(s"MigrationEngine responded: $output")
        sys.error(e.toString)
      }
    }
  }

  private def envelope(method: String, params: JsObject): JsValue = {
    val finalParams = params ++ Json.obj("sourceConfig" -> project.dataSourceConfig)
    Json.obj(
      "id"      -> "1",
      "jsonrpc" -> "2.0",
      "method"  -> method,
      "params"  -> finalParams
    )
  }
}

case class InferMigrationStepsInput(
    migrationId: String,
    datamodel: String,
    assumeToBeApplied: Vector[JsValue]
)

case class ApplyMigrationInput(
    migrationId: String,
    steps: Vector[JsValue],
    force: Option[Boolean]
)

case class RpcResult(
    id: String,
    result: JsValue
)

case class MigrationStepsResultOutput(
    datamodel: String,
    datamodelSteps: Vector[JsValue],
    databaseSteps: JsValue,
    warnings: Vector[DataModelWarningOrError],
    errors: Vector[DataModelWarningOrError],
    generalErrors: Vector[String]
)

case class DataModelWarningOrError(
    `type`: String,
    field: Option[String],
    message: String
)
