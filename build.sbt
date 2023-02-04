val scala3Version = "3.2.2"

scalafmtPrintDiff := true
scalafmtDetailedError := true

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala3-learn",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    semanticdbEnabled := true,
    semanticdbIncludeInJar := true,
    scalafixOnCompile := true,
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )
