import sbt._

object Dependencies {
  lazy val common = Seq(
    "org.sangria-graphql"        %% "sangria"                 % "1.2.3-SNAPSHOT",
    "org.sangria-graphql"        %% "sangria"                 % "1.2.2",
    "org.sangria-graphql"        %% "sangria-spray-json"      % "1.0.0",
    "org.sangria-graphql"        %% "sangria-relay"           % "1.2.2",
    "com.google.guava"           % "guava"                    % "19.0",
    "com.typesafe.akka"          %% "akka-http"               % "10.0.5",
    "com.typesafe.akka"          %% "akka-testkit"            % "2.4.17",
    "com.typesafe.akka"          %% "akka-http-testkit"       % "10.0.5",
    "com.typesafe.akka"          %% "akka-http-spray-json"    % "10.0.5",
    "com.typesafe.akka"          %% "akka-contrib"            % "2.4.17",
    "ch.megard"                  %% "akka-http-cors"          % "0.2.1",
    "com.typesafe.slick"         %% "slick"                   % "3.2.0",
    "com.typesafe.slick"         %% "slick-hikaricp"          % "3.2.0",
    "com.github.tototoshi"       %% "slick-joda-mapper"       % "2.3.0",
    "joda-time"                  % "joda-time"                % "2.9.4",
    "org.joda"                   % "joda-convert"             % "1.7",
    "org.scalaj"                 %% "scalaj-http"             % "2.3.0",
    "io.spray"                   %% "spray-json"              % "1.3.3",
    "org.scaldi"                 %% "scaldi"                  % "0.5.8",
    "org.scaldi"                 %% "scaldi-akka"             % "0.5.8",
    "com.typesafe.scala-logging" %% "scala-logging"           % "3.4.0",
    "ch.qos.logback"             % "logback-classic"          % "1.1.7",
    "org.atteo"                  % "evo-inflector"            % "1.2",
    "com.amazonaws"              % "aws-java-sdk-kinesis"     % "1.11.171",
    "com.amazonaws"              % "aws-java-sdk-s3"          % "1.11.171",
    "com.amazonaws"              % "aws-java-sdk-cloudwatch"  % "1.11.171",
    "com.amazonaws"              % "aws-java-sdk-sns"         % "1.11.171",
    "software.amazon.awssdk"     % "lambda"                   % "2.0.0-preview-4",
    "org.scala-lang.modules"     % "scala-java8-compat_2.11"  % "0.8.0",
    "software.amazon.awssdk"     % "s3"                       % "2.0.0-preview-4",
    "org.mariadb.jdbc"           % "mariadb-java-client"      % "2.1.2",
    "com.github.t3hnar"          %% "scala-bcrypt"            % "2.6",
    "org.scalactic"              %% "scalactic"               % "2.2.6",
    "com.pauldijou"              %% "jwt-core"                % "0.7.1",
    "cool.graph"                 % "cuid-java"                % "0.1.1",
    "com.jsuereth"               %% "scala-arm"               % "2.0",
    "com.google.code.findbugs"   % "jsr305"                   % "3.0.1",
    "com.stripe"                 % "stripe-java"              % "3.9.0",
    "org.yaml"                   % "snakeyaml"                % "1.17",
    "net.jcazevedo"              %% "moultingyaml"            % "0.4.0",
    "net.logstash.logback"       % "logstash-logback-encoder" % "4.7",
    "org.sangria-graphql"        %% "sangria-play-json"       % "1.0.3",
    "de.heikoseeberger"          %% "akka-http-play-json"     % "1.17.0",
    finagle,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.8.4",
    scalaTest
  )

  val akka      = "com.typesafe.akka" %% "akka-actor"   % "2.4.8"
  val finagle   = "com.twitter"       %% "finagle-http" % "6.44.0"
  val scalaTest = "org.scalatest"     %% "scalatest"    % "2.2.6" % Test

  val apiServer    = Seq.empty
  val clientShared = Seq(scalaTest)

  val caffeine    = "com.github.ben-manes.caffeine" % "caffeine"            % "2.5.5"
  val java8Compat = "org.scala-lang.modules"        %% "scala-java8-compat" % "0.7.0"
  val jsr305      = "com.google.code.findbugs"      % "jsr305"              % "3.0.0"
}
