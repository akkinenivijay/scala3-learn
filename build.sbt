val scala3Version = "3.2.2"

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / excludeLintKeys ++= Set(
  scalafmtFilter
)

scalafmtPrintDiff := true
scalafmtDetailedError := true
scalafmtFilter := "diff-dirty"

wartremoverErrors ++= Warts.all

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

console / tpolecatExcludeOptions ++= ScalacOptions.defaultConsoleExclude

inThisBuild(
  List(
    scalaVersion := scala3Version,
    semanticdbEnabled := true
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala3-learn",
    version := "0.1.0-SNAPSHOT",
    semanticdbIncludeInJar := true,
    scalafixOnCompile := true,
    libraryDependencies += ("org.scalameta" %% "munit" % "0.7.29" % Test),
    scalacOptions ++= Seq(
      "-color",
      "-explain",
      "-from-tasty",
      "-new-syntax",
      "-indent",
      "-print-lines",
      "-print-tasty"
    )
  )
