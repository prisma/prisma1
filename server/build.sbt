import com.typesafe.sbt.SbtGit.GitKeys
import com.typesafe.sbt.git.ConsoleGitRunner
import sbt._

name := "server"
Revolver.settings

import Dependencies._
import com.typesafe.sbt.SbtGit

lazy val propagateVersionToOtherRepo = taskKey[Unit]("Propagates the version of this project to another github repo.")
lazy val actualBranch = settingKey[String]("the current branch of the git repo")

actualBranch := {
  val branch = sys.env.getOrElse("BRANCH", git.gitCurrentBranch.value)
  if(branch != "master"){
    sys.props += "project.version" -> s"$branch-SNAPSHOT"
  }
  branch
}


propagateVersionToOtherRepo := {
  val branch = actualBranch.value
  println(s"Will try to propagate the version to branch $branch in other repo.")
  val githubClient = GithubClient()
  githubClient.updateFile(
    owner = Env.read("OTHER_REPO_OWNER"),
    repo = Env.read("OTHER_REPO"),
    filePath = Env.read("OTHER_REPO_FILE"),
    branch = branch,
    newContent = version.value
  )
}



// determine the version of our artifacts with sbt-git
lazy val versionSettings = SbtGit.versionWithGit ++ Seq(
  git.baseVersion := "0.8.0",
  git.gitUncommittedChanges := { // the default implementation of sbt-git uses JGit which somehow always returns true here, so we roll our own impl
    import sys.process._
    val gitStatusResult = "git status --porcelain".!!
    if(gitStatusResult.nonEmpty){
      println("Git has uncommitted changes!")
      println(gitStatusResult)
    }
    gitStatusResult.nonEmpty
  }
)

lazy val deploySettings = overridePublishBothSettings ++ Seq(
  credentials += Credentials(
    realm = "packagecloud",
    host = "packagecloud.io",
    userName = "",
    passwd = sys.env.getOrElse("PACKAGECLOUD_PW", sys.error("PACKAGECLOUD_PW env var is not set."))
  ),
  publishTo := Some("packagecloud+https" at "packagecloud+https://packagecloud.io/graphcool/graphcool"),
  aether.AetherKeys.aetherWagons := Seq(aether.WagonWrapper("packagecloud+https", "io.packagecloud.maven.wagon.PackagecloudWagon"))
)

lazy val commonSettings = deploySettings ++ versionSettings ++ Seq(
  organization := "cool.graph",
  organizationName := "graphcool",
  scalaVersion := "2.11.8",
  parallelExecution in Test := false,
  publishArtifact in Test := true,
  // We should gradually introduce https://tpolecat.github.io/2014/04/11/scalac-flags.html
  // These needs to separately be configured in Idea
  scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings"),
  resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

lazy val commonBackendSettings = commonSettings ++ Seq(
  libraryDependencies ++= Dependencies.common,
  imageNames in docker := Seq(
    ImageName(s"graphcool/${name.value}:latest")
  ),
  dockerfile in docker := {
    val appDir    = stage.value
    val targetDir = "/app"

    new Dockerfile {
      from("anapsix/alpine-java")
      entryPoint(s"$targetDir/bin/${executableScriptName.value}")
      copy(appDir, targetDir)
      expose(8081)
      expose(8000)
      expose(3333)
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
    "-J-Djava.rmi.server.hostname=localhost",
    "-J-Xmx2560m"
  )
)

lazy val bugsnag = Project(id = "bugsnag", base = file("./libs/bugsnag"))
  .settings(commonSettings: _*)

lazy val akkaUtils = Project(id = "akka-utils", base = file("./libs/akka-utils"))
  .settings(commonSettings: _*)
  .dependsOn(bugsnag % "compile")
  .dependsOn(scalaUtils % "compile")
  .settings(libraryDependencies ++= Seq(
    "ch.megard"           %% "akka-http-cors"       % "0.2.1"
  ))

lazy val cloudwatch = Project(id = "cloudwatch", base = file("./libs/cloudwatch"))
  .settings(commonSettings: _*)

lazy val metrics = Project(id = "metrics", base = file("./libs/metrics"))
  .settings(commonSettings: _*)
  .dependsOn(bugsnag % "compile")
  .dependsOn(akkaUtils % "compile")
  .settings(
    libraryDependencies ++= Seq(
      "com.datadoghq"     % "java-dogstatsd-client" % "2.3",
      "com.typesafe.akka" %% "akka-http"          % "10.0.5",
      Dependencies.finagle,
      Dependencies.akka,
      Dependencies.scalaTest
    )
  )

lazy val rabbitProcessor = Project(id = "rabbit-processor", base = file("./libs/rabbit-processor"))
  .settings(commonSettings: _*)
  .dependsOn(bugsnag % "compile")

lazy val messageBus = Project(id = "message-bus", base = file("./libs/message-bus"))
  .settings(commonSettings: _*)
  .dependsOn(bugsnag % "compile")
  .dependsOn(akkaUtils % "compile")
  .dependsOn(rabbitProcessor % "compile")
  .settings(libraryDependencies ++= Seq(
    Dependencies.scalaTest,
    "com.typesafe.akka"   %% "akka-testkit" % "2.4.17" % "compile",
    "com.typesafe.play" %% "play-json" % "2.5.12"
  ))

lazy val jvmProfiler = Project(id = "jvm-profiler", base = file("./libs/jvm-profiler"))
  .settings(commonSettings: _*)
  .dependsOn(metrics % "compile")
  .settings(libraryDependencies += Dependencies.scalaTest)

lazy val graphQlClient = Project(id = "graphql-client", base = file("./libs/graphql-client"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies += Dependencies.scalaTest)
  .dependsOn(stubServer % "test")
  .dependsOn(akkaUtils % "compile")

lazy val javascriptEngine = Project(id = "javascript-engine", base = file("./libs/javascript-engine"))
  .settings(commonSettings: _*)

lazy val stubServer = Project(id = "stub-server", base = file("./libs/stub-server"))
  .settings(commonSettings: _*)

lazy val backendShared =
  Project(id = "backend-shared", base = file("./backend-shared"))
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
    .settings(commonBackendSettings: _*)
    .settings(unmanagedBase := baseDirectory.value / "self_built_libs")
    .dependsOn(bugsnag % "compile")
    .dependsOn(akkaUtils % "compile")
    .dependsOn(cloudwatch % "compile")
    .dependsOn(metrics % "compile")
    .dependsOn(jvmProfiler % "compile")
    .dependsOn(rabbitProcessor % "compile")
    .dependsOn(graphQlClient % "compile")
    .dependsOn(javascriptEngine % "compile")
    .dependsOn(stubServer % "test")
    .dependsOn(messageBus % "compile")
    .dependsOn(scalaUtils % "compile")
    .dependsOn(cache % "compile")

lazy val clientShared =
  Project(id = "client-shared", base = file("./client-shared"))
    .settings(commonSettings: _*)
    .dependsOn(backendShared % "compile")
    .settings(libraryDependencies ++= Dependencies.clientShared)

lazy val backendApiSystem =
  Project(id = "backend-api-system", base = file("./backend-api-system"))
    .dependsOn(backendShared % "compile")
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
    .settings(commonBackendSettings: _*)

lazy val backendApiSimple =
  Project(id = "backend-api-simple", base = file("./backend-api-simple"))
    .dependsOn(clientShared % "compile")
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
    .settings(commonBackendSettings: _*)
    .settings(libraryDependencies ++= Dependencies.apiServer)

lazy val backendApiRelay =
  Project(id = "backend-api-relay", base = file("./backend-api-relay"))
    .dependsOn(clientShared % "compile")
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
    .settings(commonBackendSettings: _*)
    .settings(libraryDependencies ++= Dependencies.apiServer)

lazy val backendApiSubscriptionsWebsocket =
  Project(id = "backend-api-subscriptions-websocket", base = file("./backend-api-subscriptions-websocket"))
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
    .settings(commonBackendSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json"           % "2.5.12",
      "de.heikoseeberger" %% "akka-http-play-json" % "1.14.0" excludeAll (
        ExclusionRule(organization = "com.typesafe.akka"),
        ExclusionRule(organization = "com.typesafe.play")
      )
    ))
    .dependsOn(cloudwatch % "compile")
    .dependsOn(metrics % "compile")
    .dependsOn(jvmProfiler % "compile")
    .dependsOn(akkaUtils % "compile")
    .dependsOn(rabbitProcessor % "compile")
    .dependsOn(bugsnag % "compile")
    .dependsOn(messageBus % "compile")

lazy val backendApiSimpleSubscriptions =
  Project(id = "backend-api-simple-subscriptions", base = file("./backend-api-simple-subscriptions"))
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
    .settings(commonBackendSettings: _*)
    .settings(libraryDependencies ++= Dependencies.apiServer)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json"           % "2.5.12",
      "de.heikoseeberger" %% "akka-http-play-json" % "1.14.0" excludeAll (
        ExclusionRule(organization = "com.typesafe.akka"),
        ExclusionRule(organization = "com.typesafe.play")
      )
    ))
    .dependsOn(clientShared % "compile")

lazy val backendApiFileupload =
  Project(id = "backend-api-fileupload", base = file("./backend-api-fileupload"))
    .dependsOn(clientShared % "compile")
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
    .settings(commonBackendSettings: _*)
    .settings(libraryDependencies ++= Dependencies.apiServer)

lazy val backendApiSchemaManager =
  Project(id = "backend-api-schema-manager", base = file("./backend-api-schema-manager"))
    .dependsOn(backendApiSystem % "compile")
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
    .settings(commonBackendSettings: _*)
    .settings(libraryDependencies ++= Dependencies.apiServer)

lazy val backendWorkers =
  Project(id = "backend-workers", base = file("./backend-workers"))
    .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
    .settings(commonSettings: _*)
    .dependsOn(bugsnag % "compile")
    .dependsOn(messageBus % "compile")
    .dependsOn(stubServer % "test")
    .dependsOn(scalaUtils % "compile")
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.play"                %% "play-json"              % "2.5.12",
      "com.typesafe.akka"                %% "akka-http"              % "10.0.5",
      "com.typesafe.slick"               %% "slick"                  % "3.2.0",
      "com.typesafe.slick"               %% "slick-hikaricp"         % "3.2.0",
      "com.typesafe.play"                %% "play-ahc-ws-standalone" % "1.0.7",
      "org.mariadb.jdbc"                 %  "mariadb-java-client"    % "1.5.8",
      "cool.graph"                       %  "cuid-java"              % "0.1.1",
      "org.scalatest"                    %% "scalatest"              % "2.2.6" % "test"
    ))
    .settings(
      imageNames in docker := Seq(
        ImageName(s"graphcool/${name.value}:latest")
      ),
      dockerfile in docker := {
        val appDir    = stage.value
        val targetDir = "/app"

        new Dockerfile {
          from("anapsix/alpine-java")
          entryPoint(s"$targetDir/bin/${executableScriptName.value}")
          copy(appDir, targetDir)
          runRaw("apk add --update mysql-client && rm -rf /var/cache/apk/*")
        }
      }
    )

lazy val scalaUtils =
  Project(id = "scala-utils", base = file("./libs/scala-utils"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      scalaTest
    ))

lazy val cache =
  Project(id = "cache", base = file("./libs/cache"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      scalaTest,
      caffeine,
      java8Compat,
      jsr305
    ))

lazy val singleServer = Project(id = "single-server", base = file("./single-server"))
  .settings(commonSettings: _*)
  .dependsOn(backendApiSystem % "compile")
  .dependsOn(backendWorkers % "compile")
  .dependsOn(backendApiSimple % "compile")
  .dependsOn(backendApiRelay % "compile")
  .dependsOn(backendApiSimpleSubscriptions % "compile")
  .dependsOn(backendApiSubscriptionsWebsocket % "compile")
  .dependsOn(backendApiFileupload % "compile")
  .dependsOn(backendApiSchemaManager % "compile")
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .settings(
    imageNames in docker := Seq(
      ImageName(s"graphcool/graphcool-dev:latest")
    ),
    dockerfile in docker := {
      val appDir    = stage.value
      val targetDir = "/app"

      new Dockerfile {
        from("anapsix/alpine-java")
        entryPoint(s"$targetDir/bin/${executableScriptName.value}")
        copy(appDir, targetDir)
      }
    }
  )

lazy val localFaas = Project(id = "localfaas", base = file("./localfaas"))
  .settings(commonSettings: _*)
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .dependsOn(akkaUtils % "compile")
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-http"           % "10.0.5",
      "com.github.pathikrit"  %% "better-files-akka"   % "2.17.1",
      "org.apache.commons"    %  "commons-compress"    % "1.14",
      "com.typesafe.play"     %% "play-json"           % "2.5.12",
      "de.heikoseeberger"     %% "akka-http-play-json" % "1.14.0" excludeAll (
        ExclusionRule(organization = "com.typesafe.akka"),
        ExclusionRule(organization = "com.typesafe.play")
      )
    ),
    imageNames in docker := Seq(
      ImageName(s"graphcool/localfaas:latest")
    ),
    dockerfile in docker := {
      val appDir    = stage.value
      val targetDir = "/app"

      new Dockerfile {
        from("openjdk:8-alpine")
        runRaw("apk add --update nodejs=6.10.3-r1 bash")
        entryPoint(s"$targetDir/bin/${executableScriptName.value}")
        copy(appDir, targetDir)
        runRaw("rm -rf /var/cache/apk/*")
      }
    }
  )

val allProjects = List(
  bugsnag,
  akkaUtils,
  cloudwatch,
  metrics,
  rabbitProcessor,
  messageBus,
  jvmProfiler,
  graphQlClient,
  javascriptEngine,
  stubServer,
  backendShared,
  clientShared,
  backendApiSystem,
  backendApiSimple,
  backendApiRelay,
  backendApiSubscriptionsWebsocket,
  backendApiSimpleSubscriptions,
  backendApiFileupload,
  backendApiSchemaManager,
  backendWorkers,
  scalaUtils,
  cache,
  singleServer,
  localFaas
)

val allLibProjects = allProjects.filter(_.base.getPath.startsWith("./libs/")).map(Project.projectToRef)
lazy val libs = (project in file("libs")).aggregate(allLibProjects: _*)

lazy val root = (project in file("."))
  .aggregate(allProjects.map(Project.projectToRef): _*)
  .settings(
    publish := { } // do not publish a JAR for the root project
  )