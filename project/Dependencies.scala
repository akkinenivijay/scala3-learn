import sbt._

object Dependencies {
  object opensaml{
    val version = "4.3.0"
    val samlImpl = "org.opensaml" % "opensaml-saml-impl" % version
  }
}
