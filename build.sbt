import Dependencies._

ThisBuild / organization := "mt.gamify"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "kakfa_test",
    libraryDependencies ++= deps,
    Test / fork := false
  )
