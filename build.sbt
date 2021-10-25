name := """json-validation-service"""
organization := "io.github.lmath"
maintainer := "lmathie@gmail.com"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.6"

libraryDependencies += guice
//libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % Test
libraryDependencies += "com.github.java-json-tools" % "json-schema-validator" % "2.2.14"
libraryDependencies += "org.typelevel" %% "cats-core" % "2.6.1"


val elastic4sVersion = "7.4.0"
libraryDependencies ++= Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % elastic4sVersion,
  "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elastic4sVersion % "test"
)


Global / onChangedBuildSource := ReloadOnSourceChanges
