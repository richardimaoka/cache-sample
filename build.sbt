lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "cache-sample",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.5.11",
      "org.scalactic" %% "scalactic" % "3.0.5" % "test",
      "org.scalatest" %% "scalatest" % "3.0.5" % "test",
      "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test
    ),
    parallelExecution in Test := false
  )
