import Dependencies.*

val scala3Version = "3.2.2"

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / excludeLintKeys ++= Set(
  scalafmtFilter,
  mainClass
)

scalafmtPrintDiff := true
scalafmtDetailedError := true
scalafmtFilter := "diff-dirty"

//wartremoverErrors ++= Warts.unsafe

// ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

console / tpolecatExcludeOptions ++= ScalacOptions.defaultConsoleExclude

inThisBuild(
  List(
    scalaVersion := scala3Version,
    semanticdbEnabled := true
  )
)

// Test settings.
Test / fork := true

lazy val root = project
  .in(file("."))
  .enablePlugins(NativeImagePlugin)
  .settings(
    name := "scala3-learn",
    compileOrder := CompileOrder.JavaThenScala,
    semanticdbIncludeInJar := true,
    // scalafixOnCompile := true,
    libraryDependencies += "org.opensaml" % "opensaml-saml-impl" % "4.3.0",
    libraryDependencies += ("org.scalameta" %% "munit" % "0.7.29" % Test),
    scalacOptions ++= Seq(
      "-print-lines"
    ),
    ThisBuild / assembly / mainClass := Some("com.nebulosity.Application"),
    ThisBuild / Compile / mainClass := Some("com.nebulosity.Application"),
    ThisBuild / Compile / run / mainClass := Some("com.nebulosity.Application"),
    ThisBuild / packageBin / mainClass := Some(
      "com.nebulosity.Application"
    ),
    // nativeImageJvm := "graalvm-java17",
    // nativeImageVersion := "22.3.1",
    nativeImageOptions += s"-H:ReflectionConfigurationFiles=${target.value / "native-image-configs" / "reflect-config.json"}",
    nativeImageOptions += s"-H:ConfigurationFileDirectories=${target.value / "native-image-configs"}",
    nativeImageOptions += "-H:+JNI",
    nativeImageInstalled := true,
    nativeImageGraalHome := file("/opt/graalvm-ce-java17-22.3.1/").toPath()
  )

resolvers += "Shibboleth OSS Releases".at(
  "https://build.shibboleth.net/maven/releases/"
)
