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

import labelled.{FieldType, ->>}
import union._
import syntax.singleton._
import test._
import testutil._

// making it method local causes weird compile error in Scala 2.10
import ops.union.UnzipFields

class UnionTests {

  type i = "i"
  type s = "s"
  type b = "b"

  type U = ("i" ->> Int) :+: ("s" ->> String) :+: ("b" ->> Boolean) :+: CNil

  @Test
  def testGetLiterals: Unit = {
    val u1 = Coproduct[U]("i" ->> 23)
    val u2 = Coproduct[U]("s" ->> "foo")
    val u3 = Coproduct[U]("b" ->> true)

    val v1 = u1.get("i")
    typed[Option[Int]](v1)
    assertEquals(Some(23), v1)

    val v2 = u2.get("s")
    typed[Option[String]](v2)
    assertEquals(Some("foo"), v2)

    val v3 = u3.get("b")
    typed[Option[Boolean]](v3)
    assertEquals(Some(true), v3)

    illTyped("""u1.get("foo")""")
  }

  @Test
  def testSelectDynamic: Unit = {
    val u1 = Coproduct[U]("i" ->> 23).union
    val u2 = Coproduct[U]("s" ->> "foo").union
    val u3 = Coproduct[U]("b" ->> true).union

    val v1 = u1.i
    typed[Option[Int]](v1)
    assertEquals(Some(23), v1)

    val n1 = u1.s
    typed[Option[String]](n1)
    assertEquals(None, n1)

    val v2 = u2.s
    typed[Option[String]](v2)
    assertEquals(Some("foo"), v2)

    val n2 = u2.b
    typed[Option[Boolean]](n2)
    assertEquals(None, n2)

    val v3 = u3.b
    typed[Option[Boolean]](v3)
    assertEquals(Some(true), v3)

    illTyped("u1.foo")
  }

  @Test
  def testUnionTypeSelector: Unit = {
    type ii = FieldType[i, Int] :+: CNil
    typed[ii](Coproduct[("i" ->> Int) :+: CNil]("i" ->> 23))

    type iiss = FieldType[i, Int] :+: FieldType[s, String] :+: CNil
    typed[iiss](Coproduct[("i" ->> Int) :+: ("s" ->> String) :+: CNil]("s" ->> "foo"))

    type iissbb = FieldType[i, Int] :+: FieldType[s, String] :+: FieldType[b, Boolean] :+: CNil
    typed[iissbb](Coproduct[("i" ->> Int) :+: ("s" ->> String) :+: ("b" ->> Boolean) :+: CNil]("b" ->> true))

    // Curiously, lines like
    //   typed[('i ->> Int) :: ('s ->> String) :: HNil](Inl('i ->> 23))
    // or
    //   val u: ('i ->> Int) :: ('s ->> String) :: HNil = Inl('i ->> 23)
    // don't compile as is. One has to tear apart the type and the value made of fields and Inl/Inr.

    {
      type U = ("i" ->> Int) :+: CNil

      val u = Inl("i" ->> 23)

      typed[U](u)
    }

    {
      type U = ("i" ->> Int) :+: ("s" ->> String) :+: CNil

      val u0 = Inl("i" ->> 23)
      val u1 = Inr(Inl("s" ->> "foo"))

      typed[U](u0)
      typed[U](u1)
    }

    {
      type U = ("i" ->> Int) :+: ("s" ->> String) :+: ("b" ->> Boolean) :+: CNil

      val u0 = Inl("i" ->> 23)
      val u1 = Inr(Inl("s" ->> "foo"))
      val u2 = Inr(Inr(Inl("b" ->> true)))

      typed[U](u0)
      typed[U](u1)
      typed[U](u2)
    }

    // Literal types

    {
      type U = ("i" ->> 2) :+: CNil

      val u = Inl("i" ->> 2.narrow)

      typed[U](u)
    }

    {
      type U = ("i" ->> 2) :+: ("s" ->> "a") :+: ("b" ->> true) :+: CNil

      val u0 = Inl("i" ->> 2.narrow)
      val u1 = Inr(Inl("s" ->> "a".narrow))
      val u2 = Inr(Inr(Inl("b" ->> true.narrow)))

      typed[U](u0)
      typed[U](u1)
      typed[U](u2)
    }

    {
      type U = ("i" ->> 2) :+: CNil

      val u = Inl("i" ->> 3.narrow)

      illTyped("typed[U](u)")
    }

    // Mix of standard and literal types

    {
      type U = ("i" ->> 2) :+: ("s" ->> String) :+: ("b" ->> true) :+: CNil

      val u0 = Inl("i" ->> 2.narrow)
      val u1 = Inr(Inl("s" ->> "a"))
      val u2 = Inr(Inr(Inl("b" ->> true.narrow)))

      typed[U](u0)
      typed[U](u1)
      typed[U](u2)
    }
  }

  @Test
  def testNamedArgsInject: Unit = {
    val u1 = Union[U](i = 23)
    val u2 = Union[U](s = "foo")
    val u3 = Union[U](b = true)

    val v1 = u1.get("i")
    typed[Option[Int]](v1)
    assertEquals(Some(23), v1)

    val v2 = u2.get("s")
    typed[Option[String]](v2)
    assertEquals(Some("foo"), v2)

    val v3 = u3.get("b")
    typed[Option[Boolean]](v3)
    assertEquals(Some(true), v3)

    illTyped("""u1.get("foo")""")
  }

  @Test
  def testFields: Unit = {
    val u1 = Union[U](i = 23)
    val u2 = Union[U](s = "foo")
    val u3 = Union[U](b = true)

    type UF = ("i", Int) :+: ("s", String) :+: ("b", Boolean) :+: CNil

    {
      val f1 = u1.fields
      val f2 = u2.fields
      val f3 = u3.fields

      assertTypedEquals(Coproduct[UF]("i".narrow -> 23), f1)
      assertTypedEquals(Coproduct[UF]("s".narrow -> "foo"), f2)
      assertTypedEquals(Coproduct[UF]("b".narrow -> true), f3)
    }

    type US = ("first" ->> Option[Int]) :+: ("second" ->> Option[Boolean]) :+: ("third" ->> Option[String]) :+: CNil
    val us1 = Coproduct[US]("first" ->> Option(2))
    val us2 = Coproduct[US]("second" ->> Option(true))
    val us3 = Coproduct[US]("third" ->> Option.empty[String])

    type USF = ("first", Option[Int]) :+: ("second", Option[Boolean]) :+: ("third", Option[String]) :+: CNil

    {
      val f1 = us1.fields
      val f2 = us2.fields
      val f3 = us3.fields

      assertTypedEquals(Coproduct[USF]("first".narrow -> Option(2)), f1)
      assertTypedEquals(Coproduct[USF]("second".narrow -> Option(true)), f2)
      assertTypedEquals(Coproduct[USF]("third".narrow -> Option.empty[String]), f3)
    }
  }

  @Test
  def testUnzipFields: Unit = {

    val u1 = Union[U](i = 23)
    val u2 = Union[U](s = "foo")
    val u3 = Union[U](b = true)

    {
      val uf = UnzipFields[U]

      type UV = Int :+: String :+: Boolean :+: CNil

      assertTypedEquals("i".narrow :: "s".narrow :: "b".narrow :: HNil, uf.keys)
      assertTypedEquals(Coproduct[UV](23), uf.values(u1))
      assertTypedEquals(Coproduct[UV]("foo"), uf.values(u2))
      assertTypedEquals(Coproduct[UV](true), uf.values(u3))
    }

    type US = ("first" ->> Option[Int]) :+: ("second" ->> Option[Boolean]) :+: ("third" ->> Option[String]) :+: CNil
    val us1 = Coproduct[US]("first" ->> Option(2))
    val us2 = Coproduct[US]("second" ->> Option(true))
    val us3 = Coproduct[US]("third" ->> Option.empty[String])

    {
      val uf = UnzipFields[US]

      type USV = Option[Int] :+: Option[Boolean] :+: Option[String] :+: CNil

      assertTypedEquals("first".narrow :: "second".narrow :: "third".narrow :: HNil, uf.keys)
      assertTypedEquals(Coproduct[USV](Option(2)), uf.values(us1))
      assertTypedEquals(Coproduct[USV](Option(true)), uf.values(us2))
      assertTypedEquals(Coproduct[USV](Option.empty[String]), uf.values(us3))
    }
  }

  @Test
  def testToMap: Unit = {
    val u1 = Union[U](i = 23)
    val u2 = Union[U](s = "foo")
    val u3 = Union[U](b = true)

    {
      val m1 = u1.toMap
      val m2 = u2.toMap
      val m3 = u3.toMap

      assertTypedEquals(Map[String, Any]("i" -> 23), m1)
      assertTypedEquals(Map[String, Any]("s" -> "foo"), m2)
      assertTypedEquals(Map[String, Any]("b" -> true), m3)
    }

    {
      val m1 = u1.toMap[String, Any]
      val m2 = u2.toMap[String, Any]
      val m3 = u3.toMap[String, Any]

      assertTypedEquals(Map[String, Any]("i" -> 23), m1)
      assertTypedEquals(Map[String, Any]("s" -> "foo"), m2)
      assertTypedEquals(Map[String, Any]("b" -> true), m3)
    }

    type US = ("first" ->> Option[Int]) :+: ("second" ->> Option[Boolean]) :+: ("third" ->> Option[String]) :+: CNil
    val us1 = Coproduct[US]("first" ->> Option(2))
    val us2 = Coproduct[US]("second" ->> Option(true))
    val us3 = Coproduct[US]("third" ->> Option.empty[String])

    {
      val m1 = us1.toMap
      val m2 = us2.toMap
      val m3 = us3.toMap

      assertTypedEquals(Map[String, Option[Any]]("first" -> Some(2)), m1)
      assertTypedEquals(Map[String, Option[Any]]("second" -> Some(true)), m2)
      assertTypedEquals(Map[String, Option[Any]]("third" -> Option.empty[String]), m3)
    }

    {
      val m1 = us1.toMap[String, Option[Any]]
      val m2 = us2.toMap[String, Option[Any]]
      val m3 = us3.toMap[String, Option[Any]]

      assertTypedEquals(Map[String, Option[Any]]("first" -> Some(2)), m1)
      assertTypedEquals(Map[String, Option[Any]]("second" -> Some(true)), m2)
      assertTypedEquals(Map[String, Option[Any]]("third" -> Option.empty[String]), m3)
    }
  }

  @Test
  def testMapValues: Unit = {
    object f extends Poly1 {
      implicit def int: Case.Aux[Int, Boolean] = at[Int](i => i > 0)
      implicit def string: Case.Aux[String, String] = at[String](s => s"s: $s")
      implicit def boolean: Case.Aux[Boolean, String] = at[Boolean](v => if (v) "Yup" else "Nope")
    }

    {
      val u1 = Union[U](i = 23)
      val u2 = Union[U](s = "foo")
      val u3 = Union[U](b = true)

      type R = ("i" ->> Boolean) :+: ("s" ->> String) :+: ("b" ->> String) :+: CNil

      val res1 = u1.mapValues(f)
      val res2 = u2.mapValues(f)
      val res3 = u3.mapValues(f)

      assertTypedEquals[R](Union[R](i = true), res1)
      assertTypedEquals[R](Union[R](s = "s: foo"), res2)
      assertTypedEquals[R](Union[R](b = "Yup"), res3)
    }

    {
      object toUpper extends Poly1 {
        implicit def stringToUpper: Case.Aux[String, String] = at[String](_.toUpperCase)
        implicit def otherTypes[X]: Case.Aux[X, X] = at[X](identity)
      }

      type U = ("foo" ->> String) :+: ("bar" ->> Boolean) :+: ("baz" ->> Double) :+: CNil
      val u1 = Coproduct[U]("foo" ->> "joe")
      val u2 = Coproduct[U]("bar" ->> true)
      val u3 = Coproduct[U]("baz" ->> 2.0)

      val r1 = u1 mapValues toUpper
      val r2 = u2 mapValues toUpper
      val r3 = u3 mapValues toUpper

      assertTypedEquals[U](Coproduct[U]("foo" ->> "JOE"), r1)
      assertTypedEquals[U](Coproduct[U]("bar" ->> true), r2)
      assertTypedEquals[U](Coproduct[U]("baz" ->> 2.0), r3)
    }
  }
}
