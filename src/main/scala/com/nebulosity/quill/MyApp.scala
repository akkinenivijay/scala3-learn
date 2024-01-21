package com.nebulosity.quill

import io.getquill.*

object MyApp {
  private case class Person(firstName: String, lastName: String, age: Int)

  // SnakeCase turns firstName -> first_name
  val ctx = new PostgresJdbcContext(SnakeCase, "ctx")
  import ctx.*

  def main(args: Array[String]): Unit = {
    inline def people = quote {
      query[Person]
    }

    inline def joes = quote {
      people.filter(p => p.firstName == "Joe")
    }
    val x = run(joes)
    println(x)
  }
}
