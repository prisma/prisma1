package cool.graph.aws

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSAsyncClientBuilder}

object AwsInitializers {
  lazy val accessKeyId = sys.env.getOrElse("AWS_ACCESS_KEY_ID", "")
  lazy val accessKey   = sys.env.getOrElse("AWS_SECRET_ACCESS_KEY", "")
  lazy val credentials = new BasicAWSCredentials(accessKeyId, accessKey)

  def createKinesis(): AmazonKinesis = {
    AmazonKinesisClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("KINESIS_ENDPOINT"), sys.env("AWS_REGION")))
      .build()
  }

  def createSns(): AmazonSNS = {
    AmazonSNSAsyncClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("SNS_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  def createS3(): AmazonS3 = {
    AmazonS3ClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("FILEUPLOAD_S3_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  def createExportDataS3(): AmazonS3 = {
    AmazonS3ClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("DATA_EXPORT_S3_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }

  // This is still in the old SBS AWS account
  def createS3Fileupload(): AmazonS3 = {
    val credentials = new BasicAWSCredentials(
      sys.env("FILEUPLOAD_S3_AWS_ACCESS_KEY_ID"),
      sys.env("FILEUPLOAD_S3_AWS_SECRET_ACCESS_KEY")
    )

    AmazonS3ClientBuilder.standard
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withEndpointConfiguration(new EndpointConfiguration(sys.env("FILEUPLOAD_S3_ENDPOINT"), sys.env("AWS_REGION")))
      .build
  }
}
