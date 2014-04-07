/*
 * Copyright (c) 2014 Miles Sabin 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package shapeless.examples

/**
 * LabelledGeneric examples.
 * 
 * @author Miles Sabin
 */
object LabelledGenericExamples extends App {
  import shapeless._
  import record._
  import ops.record._
  import syntax.singleton._

  case class Book(author: String, title: String, id: Int, price: Double)
  case class ExtendedBook(author: String, title: String, id: Int, price: Double, inPrint: Boolean)

  val bookGen = LabelledGeneric[Book]
  val bookExtGen = LabelledGeneric[ExtendedBook]

  val tapl = Book("Benjamin Pierce", "Types and Programming Languages", 262162091, 44.11)

  val rec = bookGen.to(tapl)

  // Read price field
  val currentPrice = rec('price)  // Static type is Double
  println("Current price is "+currentPrice)
  println

  // Update price field, relying on static type of currentPrice
  val updated = bookGen.from(rec.updateWith('price)(_+2.0))
  println(updated)
  println

  // Add a new field, map back into ExtendedBook
  val extended = bookExtGen.from(rec + ('inPrint ->> true)) // Static type is ExtendedBook
  println(extended)
  println

  // internationalization Shapeless style?
  case class Libro(autor: String, `título`: String, id: Int, precio: Double)

  val libroGen = LabelledGeneric[Libro]
  val libroKeys = Keys[libroGen.Repr]
  val libroRec = rec.values.zipWithKeys(libroKeys())
  val libro = libroGen.from(libroRec) // static type is Libro
  println(libro)
  println

}
