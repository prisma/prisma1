libraryDependencies ++= Seq(
  "com.typesafe.akka"             %% "akka-actor"   % "2.4.8" % "provided",
  "com.typesafe.akka"             %% "akka-contrib" % "2.4.8" % "provided",
  "com.typesafe.akka"             %% "akka-http"    % "10.0.5",
  "com.typesafe.akka"             %% "akka-testkit" % "2.4.8" % "test",
  "org.specs2"                    %% "specs2-core"  % "3.8.8" % "test",
  "com.github.ben-manes.caffeine" % "caffeine"      % "2.4.0",
  "com.twitter"                   %% "finagle-http" % "6.44.0"
)

fork in Test := true
