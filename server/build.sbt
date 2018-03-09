import com.typesafe.sbt.SbtGit.GitKeys
import com.typesafe.sbt.git.ConsoleGitRunner
import sbt._

name := "server"
Revolver.settings


import Dependencies._
import com.typesafe.sbt.SbtGit

// determine the version of our artifacts with sbt-git
lazy val versionSettings = SbtGit.versionWithGit ++ Seq(
  git.baseVersion := "0.8.0",
  git.gitUncommittedChanges := { // the default implementation of sbt-git uses JGit which somehow always returns true here, so we roll our own impl
    import sys.process._
    val gitStatusResult = "git status --porcelain".!!
    if (gitStatusResult.nonEmpty){
      println("Git has uncommitted changes!")
      println(gitStatusResult)
    }
    gitStatusResult.nonEmpty
  }
)

lazy val commonSettings = versionSettings ++ Seq(
  organization := "com.prisma",
  organizationName := "graphcool",
  scalaVersion := "2.12.3",
  parallelExecution in Test := false,
  publishArtifact in Test := true,
  // We should gradually introduce https://tpolecat.github.io/2014/04/11/scalac-flags.html
  // These needs to separately be configured in Idea
  scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings"),
  resolvers ++= Seq(
    "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
  ),
  libraryDependencies := common
)

lazy val commonServerSettings = commonSettings ++ Seq(libraryDependencies ++= commonServerDependencies)

def commonDockerImageSettings(imageName: String) = commonServerSettings ++ Seq(
  imageNames in docker := Seq(
    ImageName(s"prismagraphql/$imageName:latest")
  ),
  dockerfile in docker := {
    val appDir    = stage.value
    val targetDir = "/app"

    new Dockerfile {
      from("anapsix/alpine-java")
      entryPoint(s"$targetDir/bin/${executableScriptName.value}")
      env("COMMIT_SHA", sys.env.getOrElse("COMMIT_SHA", sys.error("Env var COMMIT_SHA required but not found.")))
      env("CLUSTER_VERSION", sys.env.getOrElse("CLUSTER_VERSION", sys.error("Env var CLUSTER_VERSION required but not found.")))
      copy(appDir, targetDir)
    }
  },
  javaOptions in Universal ++= Seq(
    // -J params will be added as jvm parameters
    "-J-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n",
    "-J-Dcom.sun.management.jmxremote=true",
    "-J-Dcom.sun.management.jmxremote.local.only=false",
    "-J-Dcom.sun.management.jmxremote.authenticate=false",
    "-J-Dcom.sun.management.jmxremote.ssl=false",
    "-J-Dcom.sun.management.jmxremote.port=3333",
    "-J-Dcom.sun.management.jmxremote.rmi.port=3333",
    "-J-Djava.rmi.server.hostname=localhost"
  )
)

def dockerImageProject(name: String, imageName: String): Project = {
  Project(id = name, base = file(s"./images/$name"))
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
    .settings(commonDockerImageSettings(imageName): _*)
}

def normalProject(name: String): Project = Project(id = name, base = file(s"./$name")).settings(commonSettings: _*)
def serverProject(name: String): Project = Project(id = name, base = file(s"./servers/$name")).settings(commonServerSettings: _*).dependsOn(scalaUtils)
def libProject(name: String): Project =  Project(id = name, base = file(s"./libs/$name")).settings(commonSettings: _*)
def connectorProject(name: String): Project =  Project(id = name, base = file(s"./connectors/$name")).settings(commonSettings: _*).dependsOn(scalaUtils)

// ####################
//       IMAGES
// ####################
lazy val prismaLocal = dockerImageProject("prisma-local", imageName = "prisma")
  .dependsOn(api% "compile")
  .dependsOn(deploy % "compile")
  .dependsOn(deployConnectorMySql % "compile")
  .dependsOn(subscriptions % "compile")
  .dependsOn(workers % "compile")
  .dependsOn(graphQlClient % "compile")

lazy val prismaProd = dockerImageProject("prisma-prod", imageName = "prisma-prod")
  .dependsOn(api% "compile")
  .dependsOn(deploy % "compile")
  .dependsOn(deployConnectorMySql % "compile")
  .dependsOn(subscriptions % "compile")
  .dependsOn(workers % "compile")
  .dependsOn(graphQlClient % "compile")

// ####################
//       SERVERS
// ####################

lazy val deploy = serverProject("deploy")
  .dependsOn(deployConnector % "compile")
  .dependsOn(deployConnectorMySql % "test->test")
  .dependsOn(sharedModels % "compile")
  .dependsOn(akkaUtils % "compile")
  .dependsOn(metrics % "compile")
  .dependsOn(jvmProfiler % "compile")
  .dependsOn(messageBus % "compile")
  .dependsOn(graphQlClient % "compile")
  .dependsOn(stubServer % "test")
  .dependsOn(sangriaUtils % "compile")
  .dependsOn(auth % "compile")

lazy val api = serverProject("api")
  .dependsOn(sharedModels % "compile")
  .dependsOn(deploy % "test->test")
  .dependsOn(messageBus % "compile")
  .dependsOn(akkaUtils % "compile")
  .dependsOn(metrics % "compile")
  .dependsOn(jvmProfiler % "compile")
  .dependsOn(cache % "compile")
  .dependsOn(auth % "compile")
  .dependsOn(sangriaUtils % "compile")
  .settings(
    libraryDependencies ++= slick ++ Seq(mariaDbClient)
  )

lazy val subscriptions = serverProject("subscriptions")
  .dependsOn(api % "compile;test->test")
  .dependsOn(stubServer % "compile")
  .settings(
    libraryDependencies ++= Seq(playStreams)
  )

lazy val workers = serverProject("workers")
  .dependsOn(errorReporting % "compile")
  .dependsOn(messageBus % "compile")
  .dependsOn(scalaUtils % "compile")
  .dependsOn(stubServer % "test")

// ####################
//       CONNECTORS
// ####################

lazy val deployConnector = connectorProject("deploy-connector")
  .dependsOn(sharedModels % "compile")

lazy val deployConnectorMySql = connectorProject("deploy-connector-mysql")
  .dependsOn(deployConnector % "compile")
  .dependsOn(scalaUtils % "compile")
  .settings(
    libraryDependencies ++= slick ++ Seq(mariaDbClient)
  )

// ####################
//       SHARED
// ####################

lazy val sharedModels = normalProject("shared-models")
  .dependsOn(gcValues % "compile")
  .dependsOn(jsonUtils % "compile")
  .settings(
  libraryDependencies ++= Seq(
    cuid
  ) ++ joda
)

// ####################
//       LIBS
// ####################

lazy val gcValues = libProject("gc-values")
  .settings(libraryDependencies ++= Seq(
    playJson
  ) ++ joda)

lazy val akkaUtils = libProject("akka-utils")
  .dependsOn(errorReporting % "compile")
  .dependsOn(scalaUtils % "compile")
  .dependsOn(stubServer % "test")
  .settings(libraryDependencies ++= Seq(
    akka,
    akkaContrib,
    akkaHttp,
    akkaTestKit,
    finagle,
    akkaHttpCors,
    playJson,
    specs2,
    caffeine
  ))
  .settings(scalacOptions := Seq("-deprecation", "-feature"))

lazy val metrics = libProject("metrics")
  .dependsOn(errorReporting % "compile")
  .dependsOn(akkaUtils % "compile")
  .settings(
    libraryDependencies ++= Seq(
      datadogStatsd,
      akkaHttp,
      finagle,
      akka,
      scalaTest,
      librato
    )
  )

lazy val rabbitProcessor = libProject("rabbit-processor")
  .settings(
    libraryDependencies ++= Seq(
      amqp
    ) ++ jackson
  )
  .dependsOn(errorReporting % "compile")

lazy val messageBus = libProject("message-bus")
  .dependsOn(errorReporting % "compile")
  .dependsOn(akkaUtils % "compile")
  .dependsOn(rabbitProcessor % "compile")
  .settings(libraryDependencies ++= Seq(
    akka,
    specs2,
    akkaTestKit,
    playJson
  ))


lazy val jvmProfiler = libProject("jvm-profiler")
  .settings(commonSettings: _*)
  .dependsOn(metrics % "compile")
  .settings(libraryDependencies += scalaTest)

lazy val graphQlClient = libProject("graphql-client")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    scalaTest,
    playJson,
    akkaHttp
  ))
  .dependsOn(stubServer % "test")
  .dependsOn(akkaUtils % "compile")


lazy val stubServer = libProject("stub-server")
    .settings(
      libraryDependencies ++= Seq(
        jettyServer,
        scalaUri,
        parserCombinators,
        scalajHttp,
        specs2
      )
    )

lazy val scalaUtils = libProject("scala-utils")


lazy val errorReporting = libProject("error-reporting")
    .settings(libraryDependencies ++= Seq(
      bugsnagClient,
      playJson
    ))

lazy val sangriaUtils = libProject("sangria-utils")
    .dependsOn(errorReporting % "compile")
    .settings(libraryDependencies ++= Seq(
      akkaHttp,
    ) ++ sangria)

lazy val jsonUtils = libProject("json-utils")
    .settings(libraryDependencies ++= Seq(
      playJson,
      scalaTest
    ) ++ joda)

lazy val cache = libProject("cache")
    .settings(libraryDependencies ++= Seq(
      caffeine,
      jsr305
    ))

lazy val auth = libProject("auth").settings(libraryDependencies ++= Seq(jwt))

val allDockerImageProjects = List(
  prismaLocal,
  prismaProd
)

val allServerProjects = List(
  api,
  deploy,
  subscriptions,
  workers
)

val allConnectorProjects = List(
  deployConnector,
  deployConnectorMySql
)

val allLibProjects = List(
  akkaUtils,
  metrics,
  rabbitProcessor,
  messageBus,
  jvmProfiler,
  graphQlClient,
  stubServer,
  scalaUtils,
  jsonUtils,
  cache,
  errorReporting,
  sangriaUtils
)

lazy val libs = (project in file("libs")).aggregate(allLibProjects.map(Project.projectToRef): _*)
lazy val images = (project in file("images")).aggregate(allDockerImageProjects.map(Project.projectToRef): _*)
lazy val connectors = (project in file("connectors")).aggregate(allConnectorProjects.map(Project.projectToRef): _*)
lazy val servers = (project in file("servers")).aggregate(allServerProjects.map(Project.projectToRef): _*)

lazy val root = (project in file("."))
  .aggregate((allServerProjects ++ allDockerImageProjects ++ allConnectorProjects).map(Project.projectToRef): _*)
  .settings(
    publish := { } // do not publish a JAR for the root project
  )
