libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.5" % "provided",
  "com.typesafe.play" %% "play-json" % "2.5.12"
)

fork in Test := true
