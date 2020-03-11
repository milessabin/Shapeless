/*
 * Copyright (c) 2014-16 Miles Sabin
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
  case class BookWithMultipleAuthors(title: String, id: Int, authors: String*)

  case class Private1(private val a: Int)
  case class Private2(private val a: Int, b: Int)
  case class Private3(a: Int, private val b: String)
  case class Private4(private val a: Int, b: String)

  val tapl = Book("Benjamin Pierce", "Types and Programming Languages", 262162091, 44.11)
  val tapl2 = Book("Benjamin Pierce", "Types and Programming Languages (2nd Ed.)", 262162091, 46.11)
  val taplExt = ExtendedBook("Benjamin Pierce", "Types and Programming Languages", 262162091, 44.11, true)
  val dp = BookWithMultipleAuthors(
    "Design Patterns", 201633612,
    "Erich Gamma", "Richard Helm", "Ralph Johnson", "John Vlissides"
  )

  val taplRecord =
    (Symbol("author") ->> "Benjamin Pierce") ::
    (Symbol("title")  ->> "Types and Programming Languages") ::
    (Symbol("id")     ->>  262162091) ::
    (Symbol("price")  ->>  44.11) ::
    HNil

  val dpRecord =
    (Symbol("title")   ->> "Design Patterns") ::
    (Symbol("id")      ->> 201633612) ::
    (Symbol("authors") ->> Seq("Erich Gamma", "Richard Helm", "Ralph Johnson", "John Vlissides")) ::
    HNil

  type BookRec = Record.`'author -> String, 'title -> String, 'id -> Int, 'price -> Double`.T
  type BookKeys = Keys[BookRec]
  type BookValues = Values[BookRec]

  type BookWithMultipleAuthorsRec = Record.`'title -> String, 'id -> Int, 'authors -> Seq[String]`.T


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

object ShapelessTaggedAux {
  import tag.@@

  trait CustomTag
  case class Dummy(i: Int @@ CustomTag)
}

object ScalazTaggedAux {
  import labelled.FieldType

  type Tagged[A, T] = { type Tag = T; type Self = A }
  type @@[T, Tag] = Tagged[T, Tag]

  trait CustomTag
  case class Dummy(i: Int @@ CustomTag)
  case class DummyTagged(b: Boolean, i: Int @@ CustomTag)

  trait TC[T] {
    def apply(): String
  }

  object TC extends TCLowPriority {
    implicit val intTC: TC[Int] =
      new TC[Int] {
        def apply() = "Int"
      }

    implicit val booleanTC: TC[Boolean] =
      new TC[Boolean] {
        def apply() = "Boolean"
      }

    implicit val taggedIntTC: TC[Int @@ CustomTag] =
      new TC[Int @@ CustomTag] {
        def apply() = s"TaggedInt"
      }

    implicit val hnilTC: TC[HNil] =
      new TC[HNil] {
        def apply() = "HNil"
      }

    implicit def hconsTC[K <: Symbol, H, T <: HList](implicit
      key: Witness.Aux[K],
      headTC: Lazy[TC[H]],
      tailTC: TC[T]
    ): TC[FieldType[K, H] :: T] =
      new TC[FieldType[K, H] :: T] {
        def apply() = s"${key.value.name}: ${headTC.value()} :: ${tailTC()}"
      }

    implicit def projectTC[F, G](implicit
      lgen: LabelledGeneric.Aux[F, G],
      tc: Lazy[TC[G]]
    ): TC[F] =
      new TC[F] {
        def apply() = s"Proj(${tc.value()})"
      }
  }

  abstract class TCLowPriority {
    // FIXME: Workaround #309
    implicit def hconsTCTagged[K <: Symbol, H, HT, T <: HList](implicit
      key: Witness.Aux[K],
      headTC: Lazy[TC[H @@ HT]],
      tailTC: TC[T]
    ): TC[FieldType[K, H @@ HT] :: T] =
      new TC[FieldType[K, H @@ HT] :: T] {
        def apply() = s"${key.value.name}: ${headTC.value()} :: ${tailTC()}"
      }
  }
}

class LabelledGenericTests {
  import LabelledGenericTestsAux._

  @Test
  def testProductBasics: Unit = {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)
    typed[BookRec](b0)
    assertEquals(taplRecord, b0)

    val b1 = gen.from(b0)
    typed[Book](b1)
    assertEquals(tapl, b1)

    val keys = b0.keys
    assertEquals(Symbol("author").narrow :: Symbol("title").narrow :: Symbol("id").narrow :: Symbol("price").narrow :: HNil, keys)

    val values = b0.values
    assertEquals("Benjamin Pierce" :: "Types and Programming Languages" :: 262162091 :: 44.11 :: HNil, values)
  }

  @Test
  def testPrivateFields: Unit = {
    val gen1 = LabelledGeneric[Private1]
    val gen2 = LabelledGeneric[Private2]
    val gen3 = LabelledGeneric[Private3]
    val gen4 = LabelledGeneric[Private4]
    val ab = Symbol("a").narrow :: Symbol("b").narrow :: HNil

    val p1 = Private1(1)
    val r1 = gen1.to(p1)
    assertTypedEquals(Symbol("a").narrow :: HNil, r1.keys)
    assertTypedEquals(1 :: HNil, r1.values)
    assertEquals(p1, gen1.from(r1))

    val p2 = Private2(2, 12)
    val r2 = gen2.to(p2)
    assertTypedEquals(ab, r2.keys)
    assertTypedEquals(2 :: 12 :: HNil, r2.values)
    assertEquals(p2, gen2.from(r2))

    val p3 = Private3(3, "p3")
    val r3 = gen3.to(p3)
    assertTypedEquals(ab, r3.keys)
    assertTypedEquals(3 :: "p3" :: HNil, r3.values)
    assertEquals(p3, gen3.from(r3))

    val p4 = Private4(4, "p4")
    val r4 = gen4.to(p4)
    assertTypedEquals(ab, r4.keys)
    assertTypedEquals(4 :: "p4" :: HNil, r4.values)
    assertEquals(p4, gen4.from(r4))
  }

  @Test
  def testProductWithVarargBasics: Unit = {
    val gen = LabelledGeneric[BookWithMultipleAuthors]

    val b0 = gen.to(dp)
    typed[BookWithMultipleAuthorsRec](b0)
    assertEquals(dpRecord, b0)

    val keys = b0.keys
    assertEquals(Symbol("title").narrow :: Symbol("id").narrow :: Symbol("authors").narrow :: HNil, keys)

    val values = b0.values
    assertEquals(
      "Design Patterns" :: 201633612 :: Seq("Erich Gamma", "Richard Helm", "Ralph Johnson", "John Vlissides") :: HNil,
      values
    )
  }

  @Test
  def testGet: Unit = {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)

    val e1 = b0.get(Symbol("author"))
    typed[String](e1)
    assertEquals("Benjamin Pierce", e1)

    val e2 = b0.get(Symbol("title"))
    typed[String](e2)
    assertEquals( "Types and Programming Languages", e2)

    val e3 = b0.get(Symbol("id"))
    typed[Int](e3)
    assertEquals(262162091, e3)

    val e4 = b0.get(Symbol("price"))
    typed[Double](e4)
    assertEquals(44.11, e4, Double.MinPositiveValue)
  }

  @Test
  def testApply: Unit = {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)

    val e1 = b0(Symbol("author"))
    typed[String](e1)
    assertEquals("Benjamin Pierce", e1)

    val e2 = b0(Symbol("title"))
    typed[String](e2)
    assertEquals( "Types and Programming Languages", e2)

    val e3 = b0(Symbol("id"))
    typed[Int](e3)
    assertEquals(262162091, e3)

    val e4 = b0(Symbol("price"))
    typed[Double](e4)
    assertEquals(44.11, e4, Double.MinPositiveValue)
  }

  @Test
  def testAt: Unit = {
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
  def testUpdated: Unit = {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)

    val b1 = b0.updated(Symbol("title"), "Types and Programming Languages (2nd Ed.)")
    val b2 = b1.updated(Symbol("price"), 46.11)

    val updated = gen.from(b2)
    assertEquals(tapl2, updated)
  }

  @Test
  def testUpdateWith: Unit = {
    val gen = LabelledGeneric[Book]

    val b0 = gen.to(tapl)

    val b1 = b0.updateWith(Symbol("title"))(_+" (2nd Ed.)")
    val b2 = b1.updateWith(Symbol("price"))(_+2.0)

    val updated = gen.from(b2)
    assertEquals(tapl2, updated)
  }

  @Test
  def testExtension: Unit = {
    val gen = LabelledGeneric[Book]
    val gen2 = LabelledGeneric[ExtendedBook]

    val b0 = gen.to(tapl)
    val b1 = b0 + (Symbol("inPrint") ->> true)

    val b2 = gen2.from(b1)
    typed[ExtendedBook](b2)
    assertEquals(taplExt, b2)
  }

  @Test
  def testCoproductBasics: Unit = {
    type TreeUnion = Union.`'Leaf -> Leaf, 'Node -> Node`.T

    val gen = LabelledGeneric[Tree]

    val t = Node(Node(Leaf(1), Leaf(2)), Leaf(3))
    val gt = gen.to(t)
    typed[TreeUnion](gt)
  }

  @Test
  def testAbstractNonCC: Unit = {
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
    assertTypedEquals[NonCCARec](Symbol("i") ->> 23 :: Symbol("s") ->> "foo" :: HNil, rA)

    val rB = genB.to(nccb)
    assertTypedEquals[NonCCBRec](Symbol("b") ->> true :: Symbol("d") ->> 2.0 :: HNil, rB)

    val rAbs = genAbs.to(ancc)
    val injA = Coproduct[AbsUnion](Symbol("NonCCA") ->> ncca)
    assertTypedEquals[AbsUnion](injA, rAbs)

    val fA = genA.from(Symbol("i") ->> 13 :: Symbol("s") ->> "bar" :: HNil)
    typed[NonCCA](fA)
    assertEquals(13, fA.i)
    assertEquals("bar", fA.s)

    val fB = genB.from(Symbol("b") ->> false :: Symbol("d") ->> 3.0 :: HNil)
    typed[NonCCB](fB)
    assertEquals(false, fB.b)
    assertEquals(3.0, fB.d, Double.MinPositiveValue)

    val injB = Coproduct[AbsUnion](Symbol("NonCCB") ->> nccb)
    val fAbs = genAbs.from(injB)
    typed[AbstractNonCC](fAbs)
    assertTrue(fAbs.isInstanceOf[NonCCB])
    assertEquals(true, fAbs.asInstanceOf[NonCCB].b)
    assertEquals(2.0, fAbs.asInstanceOf[NonCCB].d, Double.MinPositiveValue)
  }

  @Test
  def testNonCCWithCompanion: Unit = {
    val nccc = NonCCWithCompanion(23, "foo")

    val rec = (Symbol("i") ->> 23) :: (Symbol("s") ->> "foo") :: HNil
    type NonCCRec = Record.`'i -> Int, 's -> String`.T

    val gen = LabelledGeneric[NonCCWithCompanion]

    val r = gen.to(nccc)
    assertTypedEquals[NonCCRec](rec, r)

    val f = gen.from(Symbol("i") ->> 13 :: Symbol("s") ->> "bar" :: HNil)
    typed[NonCCWithCompanion](f)
    assertEquals(13, f.i)
    assertEquals("bar", f.s)
  }

  @Test
  def testNonCCLazy: Unit = {
    lazy val (a: NonCCLazy, b: NonCCLazy, c: NonCCLazy) =
      (new NonCCLazy(c, b), new NonCCLazy(a, c), new NonCCLazy(b, a))

    val rec = Symbol("prev") ->> a :: Symbol("next") ->> c :: HNil
    type LazyRec = Record.`'prev -> NonCCLazy, 'next -> NonCCLazy`.T

    val gen = LabelledGeneric[NonCCLazy]

    val rB = gen.to(b)
    assertTypedEquals[LazyRec](rec, rB)

    val fD = gen.from(Symbol("prev") ->> a :: Symbol("next") ->> c :: HNil)
    typed[NonCCLazy](fD)
    assertEquals(a, fD.prev)
    assertEquals(c, fD.next)
  }

  @Test
  def testShapelessTagged: Unit = {
    import ShapelessTaggedAux._

    val lgen = LabelledGeneric[Dummy]
    val s = s"${lgen from Record(i=tag[CustomTag](0))}"
    assertEquals(s, "Dummy(0)")
  }


  @Test
  def testScalazTagged: Unit = {
    import ScalazTaggedAux._

    implicitly[TC[Int @@ CustomTag]]
    implicitly[TC[Boolean]]

    implicitly[Generic[Dummy]]
    implicitly[LabelledGeneric[Dummy]]

    implicitly[TC[Dummy]]

    implicitly[TC[DummyTagged]]

    type R = Record.`'i -> Int @@ CustomTag`.T
    val lgen = LabelledGeneric[Dummy]
    implicitly[lgen.Repr =:= R]
    implicitly[TC[R]]

    type RT = Record.`'b -> Boolean, 'i -> Int @@ CustomTag`.T
    val lgent = LabelledGeneric[DummyTagged]
    implicitly[lgent.Repr =:= RT]
    implicitly[TC[RT]]
  }
}
