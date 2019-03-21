unmanagedBase := baseDirectory.value / "libs"

addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager"  % "1.3.11")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker"           % "1.5.0")
addSbtPlugin("com.eed3si9n"      % "sbt-assembly"         % "0.14.6")
addSbtPlugin("io.get-coursier"   % "sbt-coursier"         % "1.0.0-RC12")
addSbtPlugin("com.typesafe.sbt"  % "sbt-git"              % "0.9.3")
addSbtPlugin("net.virtual-void"  % "sbt-dependency-graph" % "0.9.0")
addSbtPlugin("com.thesamet"      % "sbt-protoc"           % "0.99.19")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.8.2"
