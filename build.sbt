import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies ++= Seq(
      //"net.debasishg" %% "redisclient" % "3.5"
      "com.github.etaty" %% "rediscala" % "1.8.0",
      "com.typesafe.akka" %% "akka-actor" % "2.5.11",
    )
  )
