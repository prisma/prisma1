unmanagedBase := baseDirectory.value / "libs"

libraryDependencies ++= Seq(
  "org.scalaj"        %% "scalaj-http" % "2.3.0",
  "com.typesafe.play" %% "play-json"   % "2.6.6"
)

addSbtPlugin("io.spray"          % "sbt-revolver"        % "0.7.2")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager" % "1.0.3")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker"          % "1.4.1")
addSbtPlugin("com.eed3si9n"      % "sbt-assembly"        % "0.14.5")

addSbtPlugin("io.get-coursier" % "sbt-coursier"       % "1.0.0-RC12")
addSbtPlugin("org.duhemm"      % "sbt-errors-summary" % "0.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")

addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.17")
