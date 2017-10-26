libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"  % "2.4.8" % "provided",
  "org.specs2"        %% "specs2-core" % "3.8.8" % "test",
  "com.typesafe"      % "jse_2.11"     % "1.2.0",
  "cool.graph"        % "cuid-java"    % "0.1.1",
  "org.scalatest"     %% "scalatest"   % "2.2.6" % "test"
)

fork in Test := true
