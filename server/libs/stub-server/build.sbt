organization := "cool.graph"
name := """stub-server"""

scalaVersion := "2.11.6"

// Change this to another test framework if you prefer
libraryDependencies ++= Seq(
  "org.eclipse.jetty"      % "jetty-server"              % "9.3.0.v20150612",
  "com.netaporter"         %% "scala-uri"                % "0.4.16",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.scalaj"             %% "scalaj-http"              % "1.1.4" % "test",
  "org.scalatest"          %% "scalatest"                % "2.2.4" % "test",
  "org.specs2"             %% "specs2-core"              % "3.6.1" % "test"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

parallelExecution in Test := false

scalacOptions in Test ++= Seq("-Yrangepos")
