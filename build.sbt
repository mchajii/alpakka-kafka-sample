
name := "alpakka-kafka-sample"

version := "0.1"

scalaVersion := "2.13.5"

// Enable the Lightbend Telemetry (Cinnamon) sbt plugin.
enablePlugins(Cinnamon)

// Add the Cinnamon Agent for run.
cinnamon in run := true

libraryDependencies ++= Seq(

  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "co.elastic.apm" % "apm-agent-attach" % "1.22.0",
  "co.elastic.apm" % "apm-agent-api" % "1.22.0",
  "co.elastic.apm" % "apm-opentracing" % "1.22.0",

  "io.circe" %% "circe-core" % "0.13.0",
  "io.circe" %% "circe-generic" % "0.13.0",
  "io.circe" %% "circe-parser" % "0.13.0",

  "com.typesafe.slick" %% "slick" % "3.3.3",
  "com.h2database"  %  "h2" % "1.4.200",

  "com.typesafe.akka" %% "akka-stream" % "2.6.13",
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.7",

  Cinnamon.library.cinnamonCHMetrics,
  Cinnamon.library.cinnamonAkkaStream,
  Cinnamon.library.cinnamonOpenTracing
)

