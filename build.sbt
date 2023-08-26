import Dependencies.*

ThisBuild / scalaVersion := "3.3.0"
ThisBuild / semanticdbEnabled := true

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / excludeLintKeys ++= Set(
  scalafmtFilter,
  mainClass
)

scalafmtPrintDiff := true
scalafmtDetailedError := true
scalafmtFilter := "diff-dirty"

// Test settings.
Test / fork := true

lazy val root = project
  .in(file("."))
  .enablePlugins(NativeImagePlugin)
  .settings(
    name := "scala3-learn",
    compileOrder := CompileOrder.JavaThenScala,
    libraryDependencies += "org.opensaml" % "opensaml-saml-impl" % "4.3.0",
    libraryDependencies += ("org.scalameta" %% "munit" % "0.7.29" % Test),
    libraryDependencies += "dev.zio" %% "zio" % "2.0.10",
    libraryDependencies += "dev.zio" %% "zio-streams" % "2.0.10",
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
    nativeImageGraalHome := file(
      sys.env.get("GRAAL_HOME").getOrElse("/opt/graalvm-ce-java17-22.3.1/")
    ).toPath()
  )

resolvers += "Shibboleth OSS Releases".at(
  "https://build.shibboleth.net/maven/releases/"
)
