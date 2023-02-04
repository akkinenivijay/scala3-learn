val scala3Version = "3.2.2"

scalafmtPrintDiff := true
scalafmtDetailedError := true
scalafmtFilter := "diff-dirty"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala3-learn",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )
