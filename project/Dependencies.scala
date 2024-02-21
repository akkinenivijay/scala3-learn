import sbt._

object Dependencies {

  val zioVersion = "2.0.21"
  val quillVersion = "4.8.0"
  object opensaml {
    val version = "4.3.0"
    val samlImpl = "org.opensaml" % "opensaml-saml-impl" % version
  }
}
