package cool.graph.shared.functions.lambda

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.{CompletableFuture, CompletionException}

import com.amazonaws.HttpMethod
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import cool.graph.cuid.Cuid
import cool.graph.shared.functions._
import cool.graph.shared.models.Project
import software.amazon.awssdk.auth.{AwsCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import software.amazon.awssdk.services.lambda.model.{
  CreateFunctionRequest,
  FunctionCode,
  InvocationType,
  InvokeRequest,
  LogType,
  ResourceConflictException,
  Runtime,
  UpdateFunctionCodeRequest,
  UpdateFunctionCodeResponse,
  UpdateFunctionConfigurationRequest
}
import spray.json.{JsArray, JsObject, JsString}

import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http.Base64

object LambdaFunctionEnvironment {
  def parseLambdaLogs(logs: String): Vector[JsObject] = {
    val lines = logs.split("\\n").filter(line => !line.isEmpty && !line.startsWith("START") && !line.startsWith("END") && !line.startsWith("REPORT"))

    val groupings = lines.foldLeft(Vector.empty[Vector[String]])((acc: Vector[Vector[String]], next: String) => {
      if (next.matches("\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d\\.\\d+.*")) {
        acc :+ Vector(next)
      } else {
        acc.dropRight(1) :+ (acc.last :+ next)
      }
    })

    groupings.map(lineGroup => {
      val segments  = lineGroup.head.split("[\\t]", -1)
      val timeStamp = segments.head

      JsObject(timeStamp -> JsString((Vector(segments.last) ++ lineGroup.tail).mkString("\n").stripLineEnd.trim))
    })
  }
}

case class LambdaFunctionEnvironment(accessKeyId: String, secretAccessKey: String) extends FunctionEnvironment {
  val lambdaCredentials = new StaticCredentialsProvider(new AwsCredentials(accessKeyId, secretAccessKey))

  def lambdaClient(project: Project): LambdaAsyncClient =
    LambdaAsyncClient
      .builder()
      .region(awsRegion(project))
      .credentialsProvider(lambdaCredentials)
      .build()

  val s3Credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey)

  def s3Client(project: Project) = {
    val region = awsRegion(project).toString

    AmazonS3ClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(s3Credentials))
      .withEndpointConfiguration(new EndpointConfiguration(s"s3-$region.amazonaws.com", region))
      .build
  }

  val deployBuckets = Map(
    cool.graph.shared.models.Region.EU_WEST_1      -> "graphcool-lambda-deploy-eu-west-1",
    cool.graph.shared.models.Region.US_WEST_2      -> "graphcool-lambda-deploy-us-west-2",
    cool.graph.shared.models.Region.AP_NORTHEAST_1 -> "graphcool-lambda-deploy-ap-northeast-1"
  )

  def awsRegion(project: Project) = project.region match {
    case cool.graph.shared.models.Region.EU_WEST_1      => Region.EU_WEST_1
    case cool.graph.shared.models.Region.US_WEST_2      => Region.US_WEST_2
    case cool.graph.shared.models.Region.AP_NORTHEAST_1 => Region.AP_NORTHEAST_1
    case _                                              => Region.EU_WEST_1
  }

  def getTemporaryUploadUrl(project: Project): Future[String] = {
    val expiration     = new java.util.Date()
    val oneHourFromNow = expiration.getTime + 1000 * 60 * 60

    expiration.setTime(oneHourFromNow)

    val generatePresignedUrlRequest = new GeneratePresignedUrlRequest(deployBuckets(project.region), Cuid.createCuid())

    generatePresignedUrlRequest.setMethod(HttpMethod.PUT)
    generatePresignedUrlRequest.setExpiration(expiration)

    Future.successful(s3Client(project).generatePresignedUrl(generatePresignedUrlRequest).toString)
  }

  def deploy(project: Project, externalFile: ExternalFile, name: String): Future[DeployResponse] = {
    val key = externalFile.url.split("\\?").head.split("/").last

    def create =
      lambdaClient(project)
        .createFunction(
          CreateFunctionRequest.builder
            .code(FunctionCode.builder().s3Bucket(deployBuckets(project.region)).s3Key(key).build())
            .functionName(lambdaFunctionName(project, name))
            .handler(externalFile.lambdaHandler)
            .role("arn:aws:iam::484631947980:role/service-role/defaultLambdaFunctionRole")
            .timeout(15)
            .memorySize(512)
            .runtime(Runtime.Nodejs610)
            .build())
        .toScala
        .map(_ => DeploySuccess())

    def update = {
      val updateCode: CompletableFuture[UpdateFunctionCodeResponse] = lambdaClient(project)
        .updateFunctionCode(
          UpdateFunctionCodeRequest.builder
            .s3Bucket(deployBuckets(project.region))
            .s3Key(key)
            .functionName(lambdaFunctionName(project, name))
            .build()
        )

      lazy val updateConfiguration = lambdaClient(project)
        .updateFunctionConfiguration(
          UpdateFunctionConfigurationRequest.builder
            .functionName(lambdaFunctionName(project, name))
            .handler(externalFile.lambdaHandler)
            .build()
        )

      for {
        _ <- updateCode.toScala
        _ <- updateConfiguration.toScala
      } yield DeploySuccess()
    }

    create.recoverWith {
      case e: CompletionException if e.getCause.isInstanceOf[ResourceConflictException] => update.recover { case e: Throwable => DeployFailure(e) }
      case e: Throwable                                                                 => Future.successful(DeployFailure(e))
    }
  }

  def invoke(project: Project, name: String, event: String): Future[InvokeResponse] = {
    lambdaClient(project)
      .invoke(
        InvokeRequest.builder
          .functionName(lambdaFunctionName(project, name))
          .invocationType(InvocationType.RequestResponse)
          .logType(LogType.Tail) // return last 4kb of function logs
          .payload(ByteBuffer.wrap(event.getBytes("utf-8")))
          .build()
      )
      .toScala
      .map(response =>
        if (response.statusCode() == 200) {
          val returnValue                = StandardCharsets.UTF_8.decode(response.payload()).toString
          val logMessage                 = Base64.decodeString(response.logResult())
          val logLines                   = LambdaFunctionEnvironment.parseLambdaLogs(logMessage)
          val returnValueWithLogEnvelope = s"""{"logs":${JsArray(logLines).compactPrint}, "response": $returnValue}"""

          InvokeSuccess(returnValue = returnValueWithLogEnvelope)
        } else {
          InvokeFailure(sys.error(s"statusCode was ${response.statusCode()}"))
      })
      .recover { case e: Throwable => InvokeFailure(e) }
  }

  private def lambdaFunctionName(project: Project, functionName: String) = s"${project.id}-$functionName"
}
