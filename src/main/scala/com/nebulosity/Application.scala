package com.nebulosity

object Application {
  @main def hello: Unit =
    println("Hello world!")
    println(msg)
    println(SamlAssertion.generate())

  def msg = "I was compiled by Scala 3. :)"
}
