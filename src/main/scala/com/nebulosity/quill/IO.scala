package com.nebulosity.quill

case class MyIO[A](unasfeRun: () => A):
  def map[B](f: A => B) = {
    MyIO(() => f(unasfeRun()))
  }

@main
def hello() = {

  val x = () => "MyString"
  println(x)
  val myIO = MyIO[String](x)
  println(myIO.unasfeRun())

  val intIO = myIO.map[Int]((input: String) => input.length())

  println(intIO.unasfeRun())

  def fullname(firstName: String, lastName: String)(using
      separator: String
  ): String = {
    s"$firstName $separator $lastName"
  }

  {
    given String = ":"
    println("Hello")
    println(fullname("Vijay ", "Akkineni"))
  }

  def convert(strings: Seq[String]): Seq[String] =
    strings.map(x => x.toString())

  println(convert(Seq("Hello")))

}
