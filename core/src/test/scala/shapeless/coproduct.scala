/*
 * Copyright (c) 2013-14 Miles Sabin
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

import test._
import testutil._

import ops.coproduct._

class CoproductTests {
  type ISB = Int :+: String :+: Boolean :+: CNil
  type III = Int :+: Int :+: Int :+: CNil

  trait Fruit
  case class Apple() extends Fruit
  case class Pear() extends Fruit
  case class Banana() extends Fruit

  type APB = Apple :+: Pear :+: Banana :+: CNil

  object size extends Poly1 {
    implicit val caseInt     = at[Int](_ => 1)
    implicit val caseString  = at[String](_.length)
    implicit val caseBoolean = at[Boolean](_ => 1)
  }

  @Test
  def testInject {
    implicitly[Inject[Int :+: CNil, Int]]
    implicitly[Inject[Int :+: Int :+: CNil, Int]]
    implicitly[Inject[Int :+: Int :+: Int :+: CNil, Int]]
    implicitly[Inject[String :+: Int :+: CNil, Int]]
    implicitly[Inject[Int :+: String :+: CNil, Int]]

    val foo1 = Coproduct[ISB](23)
    val foo2 = Coproduct[ISB]("foo")
    val foo3 = Coproduct[ISB](true)

    illTyped("""
      val foo4 = Coproduct[ISB](1.0)
    """)
    illTyped("""
      val foo4 = Coproduct[ISB](CNil)
    """)
  }

  @Test
  def testMatch {
    def cpMatch(v: ISB) = v match {
      case Inl(x) =>
        typed[Int](x)
      case Inr(Inl(x)) =>
        typed[String](x)
      case Inr(Inr(Inl(x))) =>
        typed[Boolean](x)
      case Inr(Inr(Inr(_))) => ??? // This impossible case required for exhaustivity
    }

    val foo1 = Coproduct[ISB](23)
    val foo2 = Coproduct[ISB]("foo")
    val foo3 = Coproduct[ISB](true)

    cpMatch(foo1)
    cpMatch(foo2)
    cpMatch(foo3)
  }

  @Test
  def testSelect {
    val foo1 = Coproduct[ISB](23)
    val foo2 = Coproduct[ISB]("foo")
    val foo3 = Coproduct[ISB](true)

    val sel1a = foo1.select[Int]
    typed[Option[Int]](sel1a)
    assertEquals(Some(23), sel1a)

    val sel1b = foo1.select[String]
    typed[Option[String]](sel1b)
    assertEquals(None, sel1b)

    val sel1c = foo1.select[Boolean]
    typed[Option[Boolean]](sel1c)
    assertEquals(None, sel1c)

    illTyped("""
      foo1.select[Double]
    """)

    val sel2a = foo2.select[Int]
    typed[Option[Int]](sel2a)
    assertEquals(None, sel2a)

    val sel2b = foo2.select[String]
    typed[Option[String]](sel2b)
    assertEquals(Some("foo"), sel2b)

    val sel2c = foo2.select[Boolean]
    typed[Option[Boolean]](sel2c)
    assertEquals(None, sel2c)

    illTyped("""
      foo2.select[Double]
    """)

    val sel3a = foo3.select[Int]
    typed[Option[Int]](sel3a)
    assertEquals(None, sel3a)

    val sel3b = foo3.select[String]
    typed[Option[String]](sel3b)
    assertEquals(None, sel3b)

    val sel3c = foo3.select[Boolean]
    typed[Option[Boolean]](sel3c)
    assertEquals(Some(true), sel3c)

    illTyped("""
      foo3.select[Double]
    """)
  }

  @Test
  def testFlatMap {
    type S = String; type I = Int; type D = Double; type C = Char
    val in1 = Coproduct[I :+: CNil](1)
    val is = Coproduct[I :+: S :+: CNil](1)
    val isd = Coproduct[I :+: S :+: D :+: CNil](1)

    object coIdentity extends Poly1 {
      implicit def default[A] = at[A](a => Coproduct[A :+: CNil](a))
    }

    val r1 = in1.flatMap(coIdentity)
    assertTypedEquals[I :+: CNil](in1, r1)
    val r2 = is.flatMap(coIdentity)
    assertTypedEquals[I :+: S :+: CNil](is, r2)

    object coSquare extends Poly1 {
      implicit def default[A] = at[A](a => Coproduct[A :+: A :+: CNil](a))
    }

    val r3 = in1.flatMap(coSquare)
    assertTypedEquals[I :+: I :+: CNil](Coproduct[I :+:I :+: CNil](1), r3)

    val r4 = is.flatMap(coSquare)
    assertTypedEquals[I :+: I :+: S :+: S :+: CNil](
      Coproduct[I :+: I :+: S :+: S :+: CNil](1), r4)

    object complex extends Poly1 {
      implicit def caseInt    = at[Int](i => Coproduct[S :+: CNil](i.toString))
      implicit def caseString = at[String](s => Coproduct[C :+: D :+: CNil](s(0)))
      implicit def caseDouble = at[Double](d => Coproduct[I :+: S :+: CNil](d.toInt))
    }

    val r5 = isd.flatMap(complex)
    assertTypedEquals[S :+: C :+: D :+: I :+: S :+: CNil](
      Coproduct[S :+: C :+: D :+: I :+: S :+: CNil]("1"), r5)
  }

  @Test
  def testMap {
    val foo1 = Coproduct[ISB](23)
    val foo2 = Coproduct[ISB]("foo")
    val foo3 = Coproduct[ISB](true)

    val foo1b = foo1 map size
    typed[III](foo1b)
    assertEquals(Inl(1), foo1b)

    val foo2b = foo2 map size
    typed[III](foo2b)
    assertEquals(Inr(Inl(3)), foo2b)

    val foo3b = foo3 map size
    typed[III](foo3b)
    assertEquals(Inr(Inr(Inl(1))), foo3b)
  }

  @Test
  def testUnify {
    val foo1 = Coproduct[ISB](23)
    val foo2 = Coproduct[ISB]("foo")
    val foo3 = Coproduct[ISB](true)

    val foo1b = foo1 map size
    val foo2b = foo2 map size
    val foo3b = foo3 map size

    val foo1c = foo1b.unify
    typed[Int](foo1c)
    assertEquals(1, foo1c)

    val foo2c = foo2b.unify
    typed[Int](foo2c)
    assertEquals(3, foo2c)

    val foo3c = foo3b.unify
    typed[Int](foo3c)
    assertEquals(1, foo3c)

    val f1 = Coproduct[APB](Apple())
    val f2 = Coproduct[APB](Pear())
    val f3 = Coproduct[APB](Banana())

    val f1b = f1.unify
    typed[Fruit](f1b)

    val f2b = f2.unify
    typed[Fruit](f2b)

    val f3b = f3.unify
    typed[Fruit](f3b)
  }

  @Test
  def testFold {
    import poly.identity

    val foo1 = Coproduct[ISB](23)
    val foo2 = Coproduct[ISB]("foo")
    val foo3 = Coproduct[ISB](true)

    val foo1b = foo1 fold size
    val foo2b = foo2 fold size
    val foo3b = foo3 fold size

    typed[Int](foo1b)
    assertEquals(1, foo1b)

    typed[Int](foo2b)
    assertEquals(3, foo2b)

    typed[Int](foo3b)
    assertEquals(1, foo3b)

    val f1 = Coproduct[APB](Apple())
    val f2 = Coproduct[APB](Pear())
    val f3 = Coproduct[APB](Banana())

    val f1b = f1 fold identity
    typed[Fruit](f1b)

    val f2b = f2 fold identity
    typed[Fruit](f2b)

    val f3b = f3 fold identity
    typed[Fruit](f3b)
  }

  @Test
  def testWithKeys {
    import syntax.singleton._
    import record.RecordType
    import union._
    import ops.union._

    val uSchema = RecordType.like('i ->> 23 :: 's ->> "foo" :: 'b ->> true :: HNil)
    val cKeys = Keys[uSchema.Union].apply()

    val u1 = Coproduct[ISB](23).zipWithKeys(cKeys)
    val v1 = u1.get('i)
    typed[Option[Int]](v1)
    assertEquals(Some(23), v1)
    assertEquals(None, u1.get('s))

    val u2 = Coproduct[ISB]("foo").zipWithKeys(cKeys)
    val v2 = u2.get('s)
    typed[Option[String]](v2)
    assertEquals(Some("foo"), v2)
    assertEquals(None, u2.get('b))

    val u3 = Coproduct[ISB](true).zipWithKeys(cKeys)
    val v3 = u3.get('b)
    typed[Option[Boolean]](v3)
    assertEquals(Some(true), v3)
    assertEquals(None, u3.get('i))

    illTyped("v3.get('d)")

    // key/value lengths must match up
    illTyped("u1.zipWithKeys(uKeys.tail)")
  }

  @Test
  def testPartialOrdering {
    val (one, two, abc, xyz) =
      (Coproduct[ISB](1), Coproduct[ISB](2), Coproduct[ISB]("abc"), Coproduct[ISB]("xyz"))

    def assertPOEquals(expected: Option[Int], l: ISB, r: ISB)(implicit po: PartialOrdering[ISB]) =
      assertEquals(s"${l} ${r}", expected, po.tryCompare(l, r))

    assertPOEquals(Some(0),  one, one)
    assertPOEquals(Some(-1), one, two)
    assertPOEquals(Some(1),  two, one)

    assertPOEquals(Some(0),  abc, abc)
    assertPOEquals(Some(-23), abc, xyz)
    assertPOEquals(Some(23),  xyz, abc)

    assertPOEquals(None, one, abc)
    assertPOEquals(None, abc, one)
  }

  @Test
  def testLength {
    val r1 = Coproduct[Int :+: CNil](123).length
    assertTypedEquals[Nat._1](Nat._1, r1)

    val r2 = Coproduct[Int :+: String :+: CNil](123).length
    assertTypedEquals[Nat._2](Nat._2, r2)

    val r3 = Coproduct[Int :+: String :+: Double :+: CNil](123).length
    assertTypedEquals[Nat._3](Nat._3, r3)

    val r4 = Coproduct[Int :+: String :+: Double :+: Char :+: CNil](123).length
    assertTypedEquals[Nat._4](Nat._4, r4)
  }

  @Test
  def testExtendRight {
    type S = String; type I = Int; type D = Double; type C = Char
    type CoI    = I :+: CNil
    type CoIS   = I :+: S :+: CNil
    type CoISD  = I :+: S :+: D :+: CNil
    type CoISDC = I :+: S :+: D :+: C :+: CNil

    val r1 = Coproduct[CoI](1).extendRight[S]
    assertTypedEquals[CoIS](Coproduct[CoIS](1), r1)

    val r2 = Coproduct[CoIS](1).extendRight[D]
    assertTypedEquals[CoISD](Coproduct[CoISD](1), r2)

    val r3 = Coproduct[CoISD](1).extendRight[C]
    assertTypedEquals[CoISDC](Coproduct[CoISDC](1), r3)
  }

  @Test
  def testExtendLeft {
    type S = String; type I = Int; type D = Double; type C = Char
    type CoI    = I :+: CNil
    type CoSI   = S :+: I :+: CNil
    type CoDSI  = D :+: S :+: I :+: CNil
    type CoCDSI = C :+: D :+: S :+: I :+: CNil

    val r1 = Coproduct[CoI](1).extendLeft[S]
    assertTypedEquals[CoSI](Coproduct[CoSI](1), r1)

    val r2 = Coproduct[CoSI](1).extendLeft[D]
    assertTypedEquals[CoDSI](Coproduct[CoDSI](1), r2)

    val r3 = Coproduct[CoDSI](1).extendLeft[C]
    assertTypedEquals[CoCDSI](Coproduct[CoCDSI](1), r3)
  }

  @Test
  def testExtendLeftBy {
    type S = String; type I = Int; type D = Double; type C = Char
    type CoI    = I :+: CNil
    type CoSI   = S :+: I :+: CNil
    type CoDSI  = D :+: S :+: I :+: CNil
    type CoCDSI = C :+: D :+: S :+: I :+: CNil
    val coi = Coproduct[CoI](1)

    val r1 = coi.extendLeftBy[CNil]
    assertTypedEquals[CoI](coi, r1)

    val r2 = coi.extendLeftBy[S :+: CNil]
    assertTypedEquals[CoSI](Coproduct[CoSI](1), r2)

    val r3 = coi.extendLeftBy[D :+: S :+: CNil]
    assertTypedEquals[CoDSI](Coproduct[CoDSI](1), r3)

    val r4 = coi.extendLeftBy[C :+: D :+: S :+: CNil]
    assertTypedEquals[CoCDSI](Coproduct[CoCDSI](1), r4)
  }

  @Test
  def testExtendRightBy {
    type S = String; type I = Int; type D = Double; type C = Char
    type CoI    = I :+: CNil
    type CoIS   = I :+: S :+: CNil
    type CoISD  = I :+: S :+: D :+: CNil
    type CoISDC = I :+: S :+: D :+: C :+: CNil
    val coi = Coproduct[CoI](1)

    val r1 = coi.extendRightBy[CNil]
    assertTypedEquals[CoI](coi, r1)

    val r2 = coi.extendRightBy[S :+: CNil]
    assertTypedEquals[CoIS](Coproduct[CoIS](1), r2)

    val r3 = coi.extendRightBy[S :+: D :+: CNil]
    assertTypedEquals[CoISD](Coproduct[CoISD](1), r3)

    val r4 = coi.extendRightBy[S :+: D :+: C :+: CNil]
    assertTypedEquals[CoISDC](Coproduct[CoISDC](1), r4)
  }

  @Test
  def testRotateLeft {
    import Nat._
    type S = String; type I = Int; type D = Double; type C = Char
    val in1 = Coproduct[I :+: CNil](1)
    val in2 = Coproduct[I :+: S :+: CNil](1)
    val in3 = Coproduct[I :+: S :+: D :+: CNil](1)
    val in4 = Coproduct[I :+: S :+: D :+: C :+: CNil](1)

    { // rotateLeft[_0]
      val r1 = in1.rotateLeft[_0]
      assertTypedSame[I :+: CNil](in1, r1)
      val r2 = in2.rotateLeft[_0]
      assertTypedSame[I :+: S :+: CNil](in2, r2)
      val r3 = in3.rotateLeft[_0]
      assertTypedSame[I :+: S :+: D :+: CNil](in3, r3)
      val r4 = in4.rotateLeft[_0]
      assertTypedSame[I :+: S :+: D :+: C :+: CNil](in4, r4)
    }

    { // rotateLeft[N % Size == 0]
      val r1 = in1.rotateLeft[_1]
      assertTypedSame[I :+: CNil](in1, r1)
      val r2 = in1.rotateLeft[_2]
      assertTypedSame[I :+: CNil](in1, r2)
      val r3 = in2.rotateLeft[_2]
      assertTypedSame[I :+: S :+: CNil](in2, r3)
      val r4 = in2.rotateLeft[_4]
      assertTypedSame[I :+: S :+: CNil](in2, r4)
      val r5 = in3.rotateLeft[_3]
      assertTypedSame[I :+: S :+: D :+: CNil](in3, r5)
      val r6 = in3.rotateLeft[_6]
      assertTypedSame[I :+: S :+: D :+: CNil](in3, r6)
      val r7 = in4.rotateLeft[_4]
      assertTypedSame[I :+: S :+: D :+: C :+: CNil](in4, r7)
      val r8 = in4.rotateLeft[_8]
      assertTypedSame[I :+: S :+: D :+: C :+: CNil](in4, r8)
    }

    { // other
      val r1 = in2.rotateLeft[_1]
      assertTypedEquals[S :+: I :+: CNil](Coproduct[S :+: I :+: CNil](1), r1)

      val r2 = in3.rotateLeft[_1]
      assertTypedEquals[S :+: D :+: I :+: CNil](Coproduct[S :+: D :+: I :+: CNil](1), r2)

      val r3 = in4.rotateLeft[_1]
      assertTypedEquals[S :+: D :+: C :+: I :+: CNil](Coproduct[S :+: D :+: C :+: I :+: CNil](1), r3)

      val r4 = in4.rotateLeft[_2]
      assertTypedEquals[D :+: C :+: I :+: S :+: CNil](Coproduct[D :+: C :+: I :+: S :+: CNil](1), r4)

      val r5 = in4.rotateLeft[_3]
      assertTypedEquals[C :+: I :+: S :+: D :+: CNil](Coproduct[C :+: I :+: S :+: D :+: CNil](1), r5)

      val r6 = in4.rotateLeft[_5]
      assertTypedEquals[S :+: D :+: C :+: I :+: CNil](Coproduct[S :+: D :+: C :+: I :+: CNil](1), r6)

      val r7 = in4.rotateLeft[_6]
      assertTypedEquals[D :+: C :+: I :+: S :+: CNil](Coproduct[D :+: C :+: I :+: S :+: CNil](1), r7)
    }
  }

  @Test
  def testRotateRight {
    import Nat._
    type S = String; type I = Int; type D = Double; type C = Char
    val in1 = Coproduct[I :+: CNil](1)
    val in2 = Coproduct[I :+: S :+: CNil](1)
    val in3 = Coproduct[I :+: S :+: D :+: CNil](1)
    val in4 = Coproduct[I :+: S :+: D :+: C :+: CNil](1)

    { // rotateRight[_0]
      val r1 = in1.rotateRight[_0]
      assertTypedSame[I :+: CNil](in1, r1)
      val r2 = in2.rotateRight[_0]
      assertTypedSame[I :+: S :+: CNil](in2, r2)
      val r3 = in3.rotateRight[_0]
      assertTypedSame[I :+: S :+: D :+: CNil](in3, r3)
      val r4 = in4.rotateRight[_0]
      assertTypedSame[I :+: S :+: D :+: C :+: CNil](in4, r4)
    }

    { // rotateRight[N % Size == 0]
      val r1 = in1.rotateRight[_1]
      assertTypedSame[I :+: CNil](in1, r1)
      val r2 = in1.rotateRight[_2]
      assertTypedSame[I :+: CNil](in1, r2)
      val r3 = in2.rotateRight[_2]
      assertTypedSame[I :+: S :+: CNil](in2, r3)
      val r4 = in2.rotateRight[_4]
      assertTypedSame[I :+: S :+: CNil](in2, r4)
      val r5 = in3.rotateRight[_3]
      assertTypedSame[I :+: S :+: D :+: CNil](in3, r5)
      val r6 = in3.rotateRight[_6]
      assertTypedSame[I :+: S :+: D :+: CNil](in3, r6)
      val r7 = in4.rotateRight[_4]
      assertTypedSame[I :+: S :+: D :+: C :+: CNil](in4, r7)
      val r8 = in4.rotateRight[_8]
      assertTypedSame[I :+: S :+: D :+: C :+: CNil](in4, r8)
    }

    { // other
      val r1 = in2.rotateRight[_1]
      assertTypedEquals[S :+: I :+: CNil](Coproduct[S :+: I :+: CNil](1), r1)

      val r2 = in3.rotateRight[_1]
      assertTypedEquals[D :+: I :+: S :+: CNil](Coproduct[D :+: I :+: S :+: CNil](1), r2)

      val r3 = in4.rotateRight[_1]
      assertTypedEquals[C :+: I :+: S :+: D :+: CNil](Coproduct[C :+: I :+: S :+: D :+: CNil](1), r3)

      val r4 = in4.rotateRight[_2]
      assertTypedEquals[D :+: C :+: I :+: S :+: CNil](Coproduct[D :+: C :+: I :+: S :+: CNil](1), r4)

      val r5 = in4.rotateRight[_3]
      assertTypedEquals[S :+: D :+: C :+: I :+: CNil](Coproduct[S :+: D :+: C :+: I :+: CNil](1), r5)

      val r6 = in4.rotateRight[_5]
      assertTypedEquals[C :+: I :+: S :+: D :+: CNil](Coproduct[C :+: I :+: S :+: D :+: CNil](1), r6)

      val r7 = in4.rotateRight[_6]
      assertTypedEquals[D :+: C :+: I :+: S :+: CNil](Coproduct[D :+: C :+: I :+: S :+: CNil](1), r7)
    }
  }

  @Test
  def testHead {
    val r1 = Coproduct[Int :+: CNil](1).head
    assertTypedEquals[Option[Int]](Some(1), r1)

    val r2 = Coproduct[Int :+: String :+: CNil](1).head
    assertTypedEquals[Option[Int]](Some(1), r2)

    val r3 = Coproduct[Int :+: String :+: CNil]("foo").head
    assertTypedEquals[Option[Int]](None, r3)
  }

  @Test
  def testTail {
    val r1 = Coproduct[Int :+: CNil](1).tail
    assertTypedEquals[Option[CNil]](None, r1)

    val r2 = Coproduct[Int :+: String :+: CNil](1).tail
    assertTypedEquals[Option[String :+: CNil]](None, r2)

    val r3 = Coproduct[Int :+: String :+: CNil]("foo").tail
    assertTypedEquals[Option[String :+: CNil]](Some(Coproduct[String :+: CNil]("foo")), r3)
  }

  @Test
  def testReverse {
    type S = String; type I = Int; type D = Double; type C = Char
    type SI = S :+: I :+: CNil; type IS = I :+: S :+: CNil

    val r1 = Coproduct[I :+: CNil](1).reverse
    assertTypedEquals[I :+: CNil](Coproduct[I :+: CNil](1), r1)

    val r2 = Coproduct[IS](1).reverse
    assertTypedEquals[SI](Coproduct[SI](1), r2)

    val r3 = Coproduct[IS]("foo").reverse
    assertTypedEquals[SI](Coproduct[SI]("foo"), r3)
  }

  @Test
  def testInit {
    val r1 = Coproduct[Int :+: CNil](1).init
    assertTypedEquals[Option[CNil]](None, r1)

    val r2 = Coproduct[Int :+: String :+: CNil]("foo").init
    assertTypedEquals[Option[Int :+: CNil]](None, r2)

    val r3 = Coproduct[Int :+: String :+: CNil](1).init
    assertTypedEquals[Option[Int :+: CNil]](Some(Coproduct[Int :+: CNil](1)), r3)
  }

  @Test
  def testLast {
    val r1 = Coproduct[Int :+: CNil](1).last
    assertTypedEquals[Option[Int]](Some(1), r1)

    val r2 = Coproduct[Int :+: String :+: CNil]("foo").last
    assertTypedEquals[Option[String]](Some("foo"), r2)

    val r3 = Coproduct[Int :+: String :+: CNil](1).last
    assertTypedEquals[Option[String]](None, r3)
  }

  @Test
  def testAt {
    import Nat._
    type S = String; type I = Int; type D = Double; type C = Char
    val in1 = Coproduct[I :+: CNil](1)
    val in2 = Coproduct[I :+: S :+: CNil](1)
    val in3 = Coproduct[I :+: S :+: D :+: CNil](1)

    val r1 = in1.at[_0]
    assertTypedEquals[Option[I]](Some(1), r1)

    val r2 = in2.at[_0]
    assertTypedEquals[Option[I]](Some(1), r2)

    val r3 = in3.at[_0]
    assertTypedEquals[Option[I]](Some(1), r3)


    val r4 = in2.at[_1]
    assertTypedEquals[Option[S]](None, r4)

    val r5 = in3.at[_1]
    assertTypedEquals[Option[S]](None, r5)

    val r6 = in3.at[_2]
    assertTypedEquals[Option[D]](None, r6)
  }

  @Test
  def testPartition {
    type S = String; type I = Int; type D = Double; type C = Char
    val i   = Coproduct[I :+: CNil](1)
    val is   = Coproduct[I :+: S :+: CNil](1)

    val isdi: I :+: S :+: D :+: I :+: CNil =
      Inr[I, S :+: D :+: I :+: CNil](Coproduct[S :+: D :+: I :+: CNil](2))

    val r1 = i.partition[I]
    assertTypedEquals[Either[I :+: CNil, CNil]](Left(i), r1)
    val r2 = is.partition[I]
    assertTypedEquals[Either[I :+: CNil, S :+: CNil]](Left(i), r2)

    val r3 = i.partition[S]
    assertTypedEquals[Either[CNil, I :+: CNil]](Right(i), r3)
    val r4 = is.partition[S]
    assertTypedEquals[Either[S :+: CNil, I :+: CNil]](Right(i), r4)

    val r5 = i.partition[C]
    assertTypedEquals[Either[CNil, I :+: CNil]](Right(i), r5)
    val r6 = is.partition[C]
    assertTypedEquals[Either[CNil, I :+: S :+: CNil]](Right(is), r6)

    val r7 = isdi.partition[I]
    assertTypedEquals[Either[I :+: I :+: CNil, S :+: D :+: CNil]](Left(Inr[I, I :+: CNil](Inl[I, CNil](2))), r7)
  }

  @Test
  def testPartitionC {
    type S = String; type I = Int; type D = Double; type C = Char
    val i   = Coproduct[I :+: CNil](1)
    val is   = Coproduct[I :+: S :+: CNil](1)

    val isdi: I :+: S :+: D :+: I :+: CNil =
      Inr[I, S :+: D :+: I :+: CNil](Coproduct[S :+: D :+: I :+: CNil](2))

    val r1 = i.partitionC[I]
    assertTypedEquals[(I :+: CNil) :+: CNil :+: CNil](Inl(i), r1)
    val r2 = is.partitionC[I]
    assertTypedEquals[(I :+: CNil) :+: (S :+: CNil) :+: CNil](Inl(i), r2)

    val r3 = i.partitionC[S]
    assertTypedEquals[CNil :+: (I :+: CNil) :+: CNil](Inr(Inl(i)), r3)
    val r4 = is.partitionC[S]
    assertTypedEquals[(S :+: CNil) :+: (I :+: CNil) :+: CNil](Inr(Inl(i)), r4)

    val r5 = i.partitionC[C]
    assertTypedEquals[CNil :+: (I :+: CNil) :+: CNil](Inr(Inl(i)), r5)
    val r6 = is.partitionC[C]
    assertTypedEquals[CNil :+: (I :+: S :+: CNil) :+: CNil](Inr(Inl(is)), r6)

    val r7 = isdi.partitionC[I]
    assertTypedEquals[(I :+: I :+: CNil) :+: (S :+: D :+: CNil) :+: CNil](
      Inl(Inr[I, I :+: CNil](Inl[I, CNil](2))), r7)
  }

  @Test
  def testFilter {
    type S = String; type I = Int; type D = Double; type C = Char
    val i   = Coproduct[I :+: CNil](1)
    val is   = Coproduct[I :+: S :+: CNil](1)

    val isdi: I :+: S :+: D :+: I :+: CNil =
      Inr[I, S :+: D :+: I :+: CNil](Coproduct[S :+: D :+: I :+: CNil](2))

    val r1 = i.filter[I]
    assertTypedEquals[Option[I :+: CNil]](Some(i), r1)
    val r2 = is.filter[I]
    assertTypedEquals[Option[I :+: CNil]](Some(i), r2)

    val r3 = i.filter[S]
    assertTypedEquals[Option[CNil]](None, r3)
    val r4 = is.filter[S]
    assertTypedEquals[Option[S :+: CNil]](None, r4)

    val r5 = i.filter[C]
    assertTypedEquals[Option[CNil]](None, r5)
    val r6 = is.filter[C]
    assertTypedEquals[Option[CNil]](None, r6)

    val r7 = isdi.filter[I]
    assertTypedEquals[Option[I :+: I :+: CNil]](Some(Inr[I, I :+: CNil](Inl[I, CNil](2))), r7)
  }

  @Test
  def testFilterNot {
    type S = String; type I = Int; type D = Double; type C = Char
    val i     = Coproduct[I :+: CNil](1)
    val is    = Coproduct[I :+: S :+: CNil](1)

    val isdi: I :+: S :+: D :+: I :+: CNil =
      Inr[I, S :+: D :+: I :+: CNil](Coproduct[S :+: D :+: I :+: CNil](2))

    val r1 = i.filterNot[I]
    assertTypedEquals[Option[CNil]](None, r1)
    val r2 = is.filterNot[I]
    assertTypedEquals[Option[S :+: CNil]](None, r2)


    val r4 = i.filterNot[S]
    assertTypedEquals[Option[I :+: CNil]](Some(i), r4)
    val r5 = is.filterNot[S]
    assertTypedEquals[Option[I :+: CNil]](Some(i), r5)


    val r7 = i.filterNot[D]
    assertTypedEquals[Option[I :+: CNil]](Some(i), r7)
    val r8 = is.filterNot[D]
    assertTypedEquals[Option[I :+: S :+: CNil]](Some(is), r8)

    val r14 = isdi.filterNot[I]
    assertTypedEquals[Option[S :+: D :+: CNil]](None, r14)
  }

  @Test
  def testSplit {
    import Nat._
    type S = String; type I = Int; type D = Double; type C = Char
    val in1 = Coproduct[I :+: CNil](1)
    val is = Coproduct[I :+: S :+: CNil](1)
    val dc = Coproduct[D :+: C :+: CNil](2.0)
    val isd = Coproduct[I :+: S :+: D :+: CNil](1)
    val isdc = Coproduct[I :+: S :+: D :+: C :+: CNil](2.0)

    val r1 = in1.split[_0]
    assertTypedEquals[Either[CNil, I :+: CNil]](Right(in1), r1)

    val r2 = is.split[_0]
    assertTypedEquals[Either[CNil, I :+: S :+: CNil]](Right(is), r2)


    val r3 = in1.split[_1]
    assertTypedEquals[Either[I :+: CNil, CNil]](Left(in1), r3)

    val r4 = is.split[_1]
    assertTypedEquals[Either[I :+: CNil, S :+: CNil]](Left(in1), r4)

    val r5 = isd.split[_1]
    assertTypedEquals[Either[I :+: CNil, S :+: D :+: CNil]](Left(in1), r5)


    val r6 = in1.split[_2]
    assertTypedEquals[Either[CNil, I :+: CNil]](Right(in1), r6)

    val r7 = is.split[_2]
    assertTypedEquals[Either[I :+: S :+: CNil, CNil]](Left(is), r7)

    val r8 = isd.split[_2]
    assertTypedEquals[Either[I :+: S :+: CNil, D :+: CNil]](Left(is), r8)

    val r9 = isdc.split[_2]
    assertTypedEquals[Either[I :+: S :+: CNil, D :+: C :+: CNil]](Right(dc), r9)
  }

  @Test
  def testSplitC {
    import Nat._
    type S = String; type I = Int; type D = Double; type C = Char
    val in1 = Coproduct[I :+: CNil](1)
    val is = Coproduct[I :+: S :+: CNil](1)
    val dc = Coproduct[D :+: C :+: CNil](2.0)
    val isd = Coproduct[I :+: S :+: D :+: CNil](1)
    val isdc = Coproduct[I :+: S :+: D :+: C :+: CNil](2.0)

    val r1 = in1.splitC[_0]
    assertTypedEquals[CNil :+: (I :+: CNil) :+: CNil](
      Coproduct[CNil :+: (I :+: CNil) :+: CNil](in1), r1)

    val r2 = is.splitC[_0]
    assertTypedEquals[CNil :+: (I :+: S :+: CNil) :+: CNil](
      Coproduct[CNil :+: (I :+: S :+: CNil) :+: CNil](is), r2)


    val r3 = in1.splitC[_1]
    assertTypedEquals[(I :+: CNil) :+: CNil :+: CNil](
      Coproduct[(I :+: CNil) :+: CNil :+: CNil](in1), r3)

    val r4 = is.splitC[_1]
    assertTypedEquals[(I :+: CNil) :+: (S :+: CNil) :+: CNil](
      Coproduct[(I :+: CNil) :+: (S :+: CNil) :+: CNil](in1), r4)

    val r5 = isd.splitC[_1]
    assertTypedEquals[(I :+: CNil) :+: (S :+: D :+: CNil) :+: CNil](
      Coproduct[(I :+: CNil) :+: (S :+: D :+: CNil) :+: CNil](in1), r5)


    val r6 = in1.splitC[_2]
    assertTypedEquals[CNil :+: (I :+: CNil) :+: CNil](
      Coproduct[CNil :+: (I :+: CNil) :+: CNil](in1), r6)

    val r7 = is.splitC[_2]
    assertTypedEquals[(I :+: S :+: CNil) :+: CNil :+: CNil](
      Coproduct[(I :+: S :+: CNil) :+: CNil :+: CNil](is), r7)

    val r8 = isd.splitC[_2]
    assertTypedEquals[(I :+: S :+: CNil) :+: (D :+: CNil) :+: CNil](
      Coproduct[(I :+: S :+: CNil) :+: (D :+: CNil) :+: CNil](is), r8)

    val r9 = isdc.splitC[_2]
    assertTypedEquals[(I :+: S :+: CNil) :+: (D :+: C :+: CNil) :+: CNil](
      Coproduct[(I :+: S :+: CNil) :+: (D :+: C :+: CNil) :+: CNil](dc), r9)
  }

  @Test
  def testRemoveElem {
    type S = String; type I = Int; type D = Double; type C = Char
    val i = Coproduct[I :+: CNil](1)
    val is = Coproduct[I :+: S :+: CNil](1)

    val r1 = i.removeElemC[I]
    assertTypedEquals[I :+: CNil](i, r1)

    val r2 = i.removeElem[I]
    assertTypedEquals[Either[I, CNil]](Left(1), r2)

    val r3 = is.removeElemC[I]
    assertTypedEquals[I :+: S :+: CNil](is, r3)

    val r4 = is.removeElem[I]
    assertTypedEquals[Either[I, S :+: CNil]](Left(1), r4)

    val r5 = is.removeElemC[S]
    assertTypedEquals[S :+: I :+: CNil](Coproduct[S :+: I :+: CNil](1), r5)

    val r6 = is.removeElem[S]
    assertTypedEquals[Either[S, I :+: CNil]](Right(i), r6)
  }
}
