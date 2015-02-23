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

package shapeless

import org.junit.Test
import org.junit.Assert._

import ops.record._
import record._
import syntax.singleton._
import test._
import testutil._
import union._

object LabelledGenericTestsAux {
  case class Book(author: String, title: String, id: Int, price: Double)
  case class ExtendedBook(author: String, title: String, id: Int, price: Double, inPrint: Boolean)

  val tapl = Book("Benjamin Pierce", "Types and Programming Languages", 262162091, 44.11)
  val tapl2 = Book("Benjamin Pierce", "Types and Programming Languages (2nd Ed.)", 262162091, 46.11)
  val taplExt = ExtendedBook("Benjamin Pierce", "Types and Programming Languages", 262162091, 44.11, true)

  val taplRecord =
    ('author ->> "Benjamin Pierce") ::
    ('title  ->> "Types and Programming Languages") ::
    ('id     ->>  262162091) ::
    ('price  ->>  44.11) ::
    HNil

  type BookRec = Record.`'author -> String, 'title -> String, 'id -> Int, 'price -> Double`.T
  type BookKeys = Keys[BookRec]
  type BookValues = Values[BookRec]

  sealed trait Tree
  case class Node(left: Tree, right: Tree) extends Tree
  case class Leaf(value: Int) extends Tree

  sealed trait AbstractNonCC
  class NonCCA(val i: Int, val s: String) extends AbstractNonCC
  class NonCCB(val b: Boolean, val d: Double) extends AbstractNonCC

  class NonCCWithCompanion private (val i: Int, val s: String)
  object NonCCWithCompanion {
    def apply(i: Int, s: String) = new NonCCWithCompanion(i, s)
    def unapply(s: NonCCWithCompanion): Option[(Int, String)] = Some((s.i, s.s))
  }

  class NonCCLazy(prev0: => NonCCLazy, next0: => NonCCLazy) {
    lazy val prev = prev0
    lazy val next = next0
  }
}

class LabelledGenericTests {
  import LabelledGenericTestsAux._

  @Test
  def testProductBasics {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)
    typed[BookRec](b0)
    assertEquals(taplRecord, b0)
    
    val b1 = gen.from(b0)
    typed[Book](b1)
    assertEquals(tapl, b1)

    val keys = b0.keys
    assertEquals('author.narrow :: 'title.narrow :: 'id.narrow :: 'price.narrow :: HNil, keys)

    val values = b0.values
    assertEquals("Benjamin Pierce" :: "Types and Programming Languages" :: 262162091 :: 44.11 :: HNil, values)
  }

  @Test
  def testGet {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)
    
    val e1 = b0.get('author)
    typed[String](e1)
    assertEquals("Benjamin Pierce", e1)

    val e2 = b0.get('title)
    typed[String](e2)
    assertEquals( "Types and Programming Languages", e2)

    val e3 = b0.get('id)
    typed[Int](e3)
    assertEquals(262162091, e3)

    val e4 = b0.get('price)
    typed[Double](e4)
    assertEquals(44.11, e4, Double.MinPositiveValue)
  }

  @Test
  def testApply {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)
    
    val e1 = b0('author)
    typed[String](e1)
    assertEquals("Benjamin Pierce", e1)

    val e2 = b0('title)
    typed[String](e2)
    assertEquals( "Types and Programming Languages", e2)

    val e3 = b0('id)
    typed[Int](e3)
    assertEquals(262162091, e3)

    val e4 = b0('price)
    typed[Double](e4)
    assertEquals(44.11, e4, Double.MinPositiveValue)
  }

  @Test
  def testAt {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)
    
    val v1 = b0.at(0)
    typed[String](v1)
    assertEquals("Benjamin Pierce", v1)

    val v2 = b0.at(1)
    typed[String](v2)
    assertEquals( "Types and Programming Languages", v2)

    val v3 = b0.at(2)
    typed[Int](v3)
    assertEquals(262162091, v3)
    
    val v4 = b0.at(3)
    typed[Double](v4)
    assertEquals(44.11, v4, Double.MinPositiveValue)
  }

  @Test
  def testUpdated {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)

    val b1 = b0.updated('title, "Types and Programming Languages (2nd Ed.)")
    val b2 = b1.updated('price, 46.11)

    val updated = gen.from(b2)
    assertEquals(tapl2, updated)
  }

  @Test
  def testUpdateWith {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)

    val b1 = b0.updateWith('title)(_+" (2nd Ed.)")
    val b2 = b1.updateWith('price)(_+2.0)

    val updated = gen.from(b2)
    assertEquals(tapl2, updated)
  }

  @Test
  def testExtension {
    val gen = LabelledGeneric[Book]
    val gen2 = LabelledGeneric[ExtendedBook]

    val b0 = gen.to(tapl)
    val b1 = b0 + ('inPrint ->> true)

    val b2 = gen2.from(b1)
    typed[ExtendedBook](b2)
    assertEquals(taplExt, b2)
  }

  @Test
  def testCoproductBasics {
    type TreeUnion = Union.`'Leaf -> Leaf, 'Node -> Node`.T

    val gen = LabelledGeneric[Tree]

    val t = Node(Node(Leaf(1), Leaf(2)), Leaf(3))
    val gt = gen.to(t)
    typed[TreeUnion](gt)
  }

  @Test
  def testAbstractNonCC {
    val ncca = new NonCCA(23, "foo")
    val nccb = new NonCCB(true, 2.0)
    val ancc: AbstractNonCC = ncca

    type NonCCARec = Record.`'i -> Int, 's -> String`.T
    type NonCCBRec = Record.`'b -> Boolean, 'd -> Double`.T
    type AbsUnion = Union.`'NonCCA -> NonCCA, 'NonCCB -> NonCCB`.T

    val genA = LabelledGeneric[NonCCA]
    val genB = LabelledGeneric[NonCCB]
    val genAbs = LabelledGeneric[AbstractNonCC]

    val rA = genA.to(ncca)
    assertTypedEquals[NonCCARec]('i ->> 23 :: 's ->> "foo" :: HNil, rA)

    val rB = genB.to(nccb)
    assertTypedEquals[NonCCBRec]('b ->> true :: 'd ->> 2.0 :: HNil, rB)

    val rAbs = genAbs.to(ancc)
    val injA = Coproduct[AbsUnion]('NonCCA ->> ncca)
    assertTypedEquals[AbsUnion](injA, rAbs)

    val fA = genA.from('i ->> 13 :: 's ->> "bar" :: HNil)
    typed[NonCCA](fA)
    assertEquals(13, fA.i)
    assertEquals("bar", fA.s)

    val fB = genB.from('b ->> false :: 'd ->> 3.0 :: HNil)
    typed[NonCCB](fB)
    assertEquals(false, fB.b)
    assertEquals(3.0, fB.d, Double.MinPositiveValue)

    val injB = Coproduct[AbsUnion]('NonCCB ->> nccb)
    val fAbs = genAbs.from(injB)
    typed[AbstractNonCC](fAbs)
    assertTrue(fAbs.isInstanceOf[NonCCB])
    assertEquals(true, fAbs.asInstanceOf[NonCCB].b)
    assertEquals(2.0, fAbs.asInstanceOf[NonCCB].d, Double.MinPositiveValue)
  }

  @Test
  def testNonCCWithCompanion {
    val nccc = NonCCWithCompanion(23, "foo")

    val rec = ('i ->> 23) :: ('s ->> "foo") :: HNil
    type NonCCRec = Record.`'i -> Int, 's -> String`.T

    val gen = LabelledGeneric[NonCCWithCompanion]

    val r = gen.to(nccc)
    assertTypedEquals[NonCCRec](rec, r)

    val f = gen.from('i ->> 13 :: 's ->> "bar" :: HNil)
    typed[NonCCWithCompanion](f)
    assertEquals(13, f.i)
    assertEquals("bar", f.s)
  }

  @Test
  def testNonCCLazy {
    lazy val (a: NonCCLazy, b: NonCCLazy, c: NonCCLazy) =
      (new NonCCLazy(c, b), new NonCCLazy(a, c), new NonCCLazy(b, a))

    val rec = 'prev ->> a :: 'next ->> c :: HNil
    type LazyRec = Record.`'prev -> NonCCLazy, 'next -> NonCCLazy`.T

    val gen = LabelledGeneric[NonCCLazy]

    val rB = gen.to(b)
    assertTypedEquals[LazyRec](rec, rB)

    val fD = gen.from('prev ->> a :: 'next ->> c :: HNil)
    typed[NonCCLazy](fD)
    assertEquals(a, fD.prev)
    assertEquals(c, fD.next)
  }
}
