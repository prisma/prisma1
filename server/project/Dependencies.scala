import sbt._

object Dependencies {

  /**
    * Version locks for all libraries that share a version number from their parent project,
    * with akka being a good example.
    */
  object v {
    val sangria     = "1.3.3"
    val akka        = "2.5.8"
    val akkaHttp    = "10.0.10"
    val joda        = "2.9.4"
    val jodaConvert = "1.7"
    val cuid        = "0.1.1"
    val play        = "2.6.8"
    val scalactic   = "3.0.4"
    val scalaTest   = "3.0.4"
    val slick       = "3.2.0"
    val spray       = "1.3.3"
    val jackson     = "2.8.4"
  }

  val jodaTime    = "joda-time" % "joda-time" % v.joda
  val jodaConvert = "org.joda" % "joda-convert" % v.jodaConvert
  val joda        = Seq(jodaTime, jodaConvert)

  val cuid      = "cool.graph"    % "cuid-java"   % v.cuid
  val scalactic = "org.scalactic" %% "scalactic"  % v.scalactic
  val scalaTest = "org.scalatest" %% "scalatest"  % v.scalaTest % Test
  val spray     = "io.spray"      %% "spray-json" % v.spray

  val slickCore   = "com.typesafe.slick" %% "slick" % v.slick
  val slickHikari = "com.typesafe.slick" %% "slick-hikaricp" % v.slick
  val slickJoda   = "com.github.tototoshi" %% "slick-joda-mapper" % "2.3.0"
  val slick       = Seq(slickCore, slickHikari, slickJoda)

  val mariaDbClient = "org.mariadb.jdbc" % "mariadb-java-client" % "2.1.2"

  val playJson    = "com.typesafe.play" %% "play-json"    % v.play
  val playStreams = "com.typesafe.play" %% "play-streams" % v.play

  val akka              = "com.typesafe.akka" %% "akka-actor"           % v.akka
  val akkaClusterTools  = "com.typesafe.akka" %% "akka-cluster-tools"   % v.akka
  val akkaContrib       = "com.typesafe.akka" %% "akka-contrib"         % v.akka
  val akkaTestKit       = "com.typesafe.akka" %% "akka-testkit"         % v.akka
  val akkaHttp          = "com.typesafe.akka" %% "akka-http"            % v.akkaHttp
  val akkaHttpTestKit   = "com.typesafe.akka" %% "akka-http-testkit"    % v.akkaHttp
  val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % v.akkaHttp
  val akkaHttpCors      = "ch.megard"         %% "akka-http-cors"       % "0.2.2"
  val akkaHttpPlayJson = "de.heikoseeberger" %% "akka-http-play-json" % "1.18.0" excludeAll (
    ExclusionRule(organization = "com.typesafe.akka"),
    ExclusionRule(organization = "com.typesafe.play")
  )

  val jsr305        = "com.google.code.findbugs"      % "jsr305"                % "3.0.0"
  val caffeine      = "com.github.ben-manes.caffeine" % "caffeine"              % "2.5.5"
  val finagle       = "com.twitter"                   %% "finagle-http"         % "6.44.0"
  val guava         = "com.google.guava"              % "guava"                 % "19.0"
  val datadogStatsd = "com.datadoghq"                 % "java-dogstatsd-client" % "2.3"

  val sangriaGraphql   = "org.sangria-graphql" %% "sangria" % v.sangria
  val sangriaRelay     = "org.sangria-graphql" %% "sangria-relay" % v.sangria
  val sangriaSprayJson = "org.sangria-graphql" %% "sangria-spray-json" % "1.0.0"
  val sangriaPlayJson  = "org.sangria-graphql" %% "sangria-play-json" % "1.0.4"
  val sangria          = Seq(sangriaGraphql, sangriaRelay, sangriaSprayJson, sangriaPlayJson)

  val bugsnagClient = "com.bugsnag" % "bugsnag"      % "3.0.2"
  val specs2        = "org.specs2"  %% "specs2-core" % "3.8.8" % "test"

  val jacksonCore           = "com.fasterxml.jackson.core" % "jackson-core" % v.jackson
  val jacksonDatabind       = "com.fasterxml.jackson.core" % "jackson-databind" % v.jackson
  val jacksonAnnotation     = "com.fasterxml.jackson.core" % "jackson-annotations" % v.jackson
  val jacksonDataformatCbor = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % v.jackson
  val jackson               = Seq(jacksonCore, jacksonDatabind, jacksonAnnotation, jacksonDataformatCbor)

  val amqp         = "com.rabbitmq"               % "amqp-client"              % "4.1.0"
  val java8Compat  = "org.scala-lang.modules"     %% "scala-java8-compat"      % "0.8.0"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging"           % "3.7.0"
  val jwt          = "com.pauldijou"              %% "jwt-core"                % "0.14.1"
  val scalaj       = "org.scalaj"                 %% "scalaj-http"             % "2.3.0"
  val evoInflector = "org.atteo"                  % "evo-inflector"            % "1.2"
  val logBack      = "ch.qos.logback"             % "logback-classic"          % "1.1.7"
  val snakeYML     = "org.yaml"                   % "snakeyaml"                % "1.17"
  val moultingYML  = "net.jcazevedo"              %% "moultingyaml"            % "0.4.0"
  val logstash     = "net.logstash.logback"       % "logstash-logback-encoder" % "4.7"

  lazy val common: Seq[ModuleID] = sangria ++ slick ++ joda ++ Seq(
    guava,
    akkaTestKit,
    akkaHttp,
    akkaHttpSprayJson,
    akkaHttpCors,
    scalaj,
    scalaLogging,
    logBack,
    evoInflector,
    java8Compat,
    mariaDbClient,
    scalactic,
    cuid,
    akkaHttpPlayJson,
    finagle,
    scalaTest,
    snakeYML,
    logstash

    //    "io.spray"   %% "spray-json"  % "1.3.3",
    //    "org.scaldi"           %% "scaldi"            % "0.5.8",
    //    "org.scaldi"                 %% "scaldi-akka"       % "0.5.8",
    //    "software.amazon.awssdk" % "lambda"          % "2.0.0-preview-4",
    //    "software.amazon.awssdk" % "s3"                  % "2.0.0-preview-4",
    //    "com.github.t3hnar"      %% "scala-bcrypt"       % "2.6",
    //    "com.jsuereth" %% "scala-arm" % "2.0",
    //    "com.google.code.findbugs" % "jsr305"                   % "3.0.1",
    //    "com.stripe"               % "stripe-java"              % "3.9.0",
    //    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.8.4",
  )
}
