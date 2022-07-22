import sbt._

object Dependencies {

  object Versions {
    val logback = "1.2.10"
    val scalalogging = "3.9.4"
    val scalactic = "3.2.12"
    val scalatest = "3.2.12"
    val kafkaClient = "3.2.0"
    val testContainers = "0.40.9"
  }

  lazy val commonDeps = Seq(
    "ch.qos.logback" % "logback-classic" % Versions.logback,
    "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalalogging,
    "org.scalactic" %% "scalactic" % Versions.scalactic,
    "org.scalatest" %% "scalatest" % Versions.scalatest % Test
  )

  lazy val kafka = Seq(
    "org.apache.kafka" % "kafka-clients" % Versions.kafkaClient
  )

  lazy val testContainers = Seq(
    "com.dimafeng" %% "testcontainers-scala-scalatest" % Versions.testContainers % Test,
    "com.dimafeng" %% "testcontainers-scala-kafka" % Versions.testContainers % Test
  )

  lazy val deps = commonDeps ++ kafka ++ testContainers
}
