import com.typesafe.sbt.SbtGit.GitKeys
import com.typesafe.sbt.git.ConsoleGitRunner
import sbt.Keys.name
import sbt._

import scala.io.Source

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
lazy val prerunHookFile = new java.io.File(sys.props("user.dir") + "/prerun_hook.sh")

def commonDockerImageSettings(imageName: String) = commonServerSettings ++ Seq(
  imageNames in docker := Seq(
    ImageName(s"prismagraphql/$imageName:latest")
  ),
  dockerfile in docker := {
    val appDir    = stage.value
    val targetDir = "/app"

    new Dockerfile {
      from("anapsix/alpine-java")
      copy(appDir, targetDir)
      copy(prerunHookFile , s"$targetDir/prerun_hook.sh")
      runShell(s"touch", s"$targetDir/start.sh")
      runShell("echo", "#!/usr/bin/env bash", ">>", s"$targetDir/start.sh")
      runShell("echo", "set -e", ">>", s"$targetDir/start.sh")
      runShell("echo", s".$targetDir/prerun_hook.sh", ">>", s"$targetDir/start.sh")
      runShell("echo", s".$targetDir/bin/${executableScriptName.value}", ">>" ,s"$targetDir/start.sh")
      runShell(s"chmod", "+x", s"$targetDir/start.sh")
      env("COMMIT_SHA", sys.env.getOrElse("COMMIT_SHA", sys.error("Env var COMMIT_SHA required but not found.")))
      env("CLUSTER_VERSION", sys.env.getOrElse("CLUSTER_VERSION", sys.error("Env var CLUSTER_VERSION required but not found.")))
      entryPointShell(s"$targetDir/start.sh")
    }
  }
)

def imageProject(name: String, imageName: String): Project = imageProject(name).enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging).settings(commonDockerImageSettings(imageName): _*)
def imageProject(name: String): Project = Project(id = name, base = file(s"./images/$name"))
def serverProject(name: String): Project = Project(id = name, base = file(s"./servers/$name")).settings(commonServerSettings: _*).dependsOn(scalaUtils)
def connectorProject(name: String): Project =  Project(id = name, base = file(s"./connectors/$name")).settings(commonSettings: _*).dependsOn(scalaUtils)
def integrationTestProject(name: String): Project =  Project(id = name, base = file(s"./integration-tests/$name")).settings(commonSettings: _*)
def libProject(name: String): Project =  Project(id = name, base = file(s"./libs/$name")).settings(commonSettings: _*)
def normalProject(name: String): Project = Project(id = name, base = file(s"./$name")).settings(commonSettings: _*)

// ####################
//       IMAGES
// ####################
lazy val prismaLocal = imageProject("prisma-local", imageName = "prisma").dependsOn(prismaImageShared % "compile")
lazy val prismaProd = imageProject("prisma-prod", imageName = "prisma-prod").dependsOn(prismaImageShared % "compile")

lazy val prismaImageShared = imageProject("prisma-image-shared")
  .dependsOn(api% "compile")
  .dependsOn(deploy % "compile")
  .dependsOn(deployConnectorMySql % "compile")
  .dependsOn(apiConnectorMySql % "compile")
  .dependsOn(subscriptions % "compile")
  .dependsOn(workers % "compile")
  .dependsOn(graphQlClient % "compile")

// ####################
//       SERVERS
// ####################

lazy val deploy = serverProject("deploy")
  .dependsOn(deployConnector % "compile")
  .dependsOn(deployConnectorMySql % "test->test")
  .dependsOn(deployConnectorPostGreSql % "test->test")
  .dependsOn(akkaUtils % "compile")
  .dependsOn(metrics % "compile")
  .dependsOn(jvmProfiler % "compile")
  .dependsOn(messageBus % "compile")
  .dependsOn(graphQlClient % "compile")
  .dependsOn(stubServer % "test")
  .dependsOn(sangriaUtils % "compile")
  .dependsOn(auth % "compile")

lazy val api = serverProject("api")
  .dependsOn(apiConnector % "compile")
  .dependsOn(apiConnectorMySql % "test->test")
  .dependsOn(apiConnectorPostGreSql % "test->test")
  .dependsOn(deploy % "test->test")
  .dependsOn(messageBus % "compile")
  .dependsOn(akkaUtils % "compile")
  .dependsOn(metrics % "compile")
  .dependsOn(jvmProfiler % "compile")
  .dependsOn(cache % "compile")
  .dependsOn(auth % "compile")
  .dependsOn(sangriaUtils % "compile")

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

lazy val deployConnectorPostGreSql = connectorProject("deploy-connector-postgresql")
  .dependsOn(deployConnector % "compile")
  .dependsOn(scalaUtils % "compile")
  .settings(
    libraryDependencies ++= slick ++ Seq(postgresClient)
  )

lazy val apiConnector = connectorProject("api-connector")
  .dependsOn(sharedModels % "compile")
  .dependsOn(gcValues % "compile")
  .settings(
    libraryDependencies ++= Seq(apacheCommons)
  )

lazy val apiConnectorMySql = connectorProject("api-connector-mysql")
  .dependsOn(apiConnector % "compile")
  .dependsOn(scalaUtils % "compile")
  .dependsOn(metrics % "compile")
  .settings(
    libraryDependencies ++= slick ++ Seq(mariaDbClient)
  )

lazy val apiConnectorPostGreSql = connectorProject("api-connector-postgresql")
  .dependsOn(apiConnector % "compile")
  .dependsOn(scalaUtils % "compile")
  .dependsOn(metrics % "compile")
  .settings(
    libraryDependencies ++= slick ++ Seq(postgresClient)
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
//   INTEGRATION TESTS
// ####################

lazy val integrationTestsMySql = integrationTestProject("integration-tests-mysql")
  .dependsOn(deploy % "compile;test->test")
  .dependsOn(api % "compile;test->test")
  .dependsOn(deployConnectorMySql)

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
  deployConnectorMySql,
  deployConnectorPostGreSql,
  apiConnector,
  apiConnectorMySql,
  apiConnectorPostGreSql
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

val allIntegrationTestProjects = List(
  integrationTestsMySql
)

lazy val images = (project in file("images")).aggregate(allDockerImageProjects.map(Project.projectToRef): _*)
lazy val servers = (project in file("servers")).aggregate(allServerProjects.map(Project.projectToRef): _*)
lazy val connectors = (project in file("connectors")).aggregate(allConnectorProjects.map(Project.projectToRef): _*)
lazy val integrationTests = (project in file("integration-tests")).aggregate(allConnectorProjects.map(Project.projectToRef): _*)
lazy val libs = (project in file("libs")).aggregate(allLibProjects.map(Project.projectToRef): _*)

lazy val root = (project in file("."))
  .aggregate((allServerProjects ++ allDockerImageProjects ++ allConnectorProjects ++ allIntegrationTestProjects).map(Project.projectToRef): _*)
  .settings(
    publish := { } // do not publish a JAR for the root project
  )
