import sbt._

object Dependencies {

  val zioVersion = "2.0.17"
  val quillVersion = "4.6.0.1"
  object opensaml {
    val version = "4.3.0"
    val samlImpl = "org.opensaml" % "opensaml-saml-impl" % version
  }
}
