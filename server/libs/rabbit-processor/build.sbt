organization := "cool.graph"
name := "rabbit-processor"

libraryDependencies ++= Seq(
  "com.rabbitmq"                     % "amqp-client"             % "4.1.0",
  "com.fasterxml.jackson.core"       % "jackson-databind"        % "2.8.4",
  "com.fasterxml.jackson.core"       % "jackson-annotations"     % "2.8.4",
  "com.fasterxml.jackson.core"       % "jackson-core"            % "2.8.4",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.8.4"
)
