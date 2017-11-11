organization := "cool.graph"
name := "message-bus"
scalaVersion := "2.11.8"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"         % "2.4.8"   % "provided",
  "com.typesafe.akka" %% "akka-testkit"       % "2.4.8"   % "test",
  "org.specs2"        %% "specs2-core"        % "3.8.8"   % "test",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.17"
)
