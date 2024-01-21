import Dependencies.{quillVersion, zioVersion}

ThisBuild / scalaVersion := "3.3.1"
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
    libraryDependencies ++= Seq(
      "org.opensaml" % "opensaml-saml-impl" % "4.3.0",
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-json" % "0.6.2",
      "io.getquill" %% "quill-jdbc-zio" % quillVersion,
      "io.getquill" %% "quill-jasync-postgres" % quillVersion,
      "org.postgresql" % "postgresql" % "42.6.0"
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
    nativeImageGraalHome := file(
      sys.env.getOrElse("GRAAL_HOME", "/opt/graalvm-ce-java17-22.3.1/")
    ).toPath
  )

resolvers += "Shibboleth OSS Releases".at(
  "https://build.shibboleth.net/maven/releases/"
)
