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

import labelled.FieldType

class UnionTests {
  import union._
  import syntax.singleton._
  import test._
  import testutil._

  val wI = Witness('i)
  type i = wI.T

  val wS = Witness('s)
  type s = wS.T

  val sB = Witness('b)
  type b = sB.T

  type U = Union.`'i -> Int, 's -> String, 'b -> Boolean`.T

  @Test
  def testGetLiterals {
    val u1 = Coproduct[U]('i ->> 23)
    val u2 = Coproduct[U]('s ->> "foo")
    val u3 = Coproduct[U]('b ->> true)

    val v1 = u1.get('i)
    typed[Option[Int]](v1)
    assertEquals(Some(23), v1)

    val v2 = u2.get('s)
    typed[Option[String]](v2)
    assertEquals(Some("foo"), v2)

    val v3 = u3.get('b)
    typed[Option[Boolean]](v3)
    assertEquals(Some(true), v3)

    illTyped("""
      u1.get('foo)
    """)
  }

  @Test
  def testSelectDynamic {
    val u1 = Coproduct[U]('i ->> 23).union
    val u2 = Coproduct[U]('s ->> "foo").union
    val u3 = Coproduct[U]('b ->> true).union

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

    /*
     * illTyped gives a false positive here, but `u1.foo` does in fact fail to compile
     * however, it fails in a weird way:
     *   Unknown type: <error>, <error> [class scala.reflect.internal.Types$ErrorType$,
     *   class scala.reflect.internal.Types$ErrorType$] TypeRef? false
     */
    //illTyped("u1.foo")
  }

  @Test
  def testUnionTypeSelector {
    type ii = FieldType[i, Int] :+: CNil
    typed[ii](Coproduct[Union.`'i -> Int`.T]('i ->> 23))

    type iiss = FieldType[i, Int] :+: FieldType[s, String] :+: CNil
    typed[iiss](Coproduct[Union.`'i -> Int, 's -> String`.T]('s ->> "foo"))

    type iissbb = FieldType[i, Int] :+: FieldType[s, String] :+: FieldType[b, Boolean] :+: CNil
    typed[iissbb](Coproduct[Union.`'i -> Int, 's -> String, 'b -> Boolean`.T]('b ->> true))
  }

  @Test
  def testNamedArgsInject {
    val u1 = Union[U](i = 23)
    val u2 = Union[U](s = "foo")
    val u3 = Union[U](b = true)

    val v1 = u1.get('i)
    typed[Option[Int]](v1)
    assertEquals(Some(23), v1)

    val v2 = u2.get('s)
    typed[Option[String]](v2)
    assertEquals(Some("foo"), v2)

    val v3 = u3.get('b)
    typed[Option[Boolean]](v3)
    assertEquals(Some(true), v3)

    illTyped("""
      u1.get('foo)
    """)
  }

  @Test
  def testToMap {
    val u1 = Union[U](i = 23)
    val u2 = Union[U](s = "foo")
    val u3 = Union[U](b = true)

    {
      val m1 = u1.toMap
      val m2 = u2.toMap
      val m3 = u3.toMap

      assertTypedEquals(Map[Symbol, Any]('i -> 23), m1)
      assertTypedEquals(Map[Symbol, Any]('s -> "foo"), m2)
      assertTypedEquals(Map[Symbol, Any]('b -> true), m3)
    }

    {
      val m1 = u1.toMap[Symbol, Any]
      val m2 = u2.toMap[Symbol, Any]
      val m3 = u3.toMap[Symbol, Any]

      assertTypedEquals(Map[Symbol, Any]('i -> 23), m1)
      assertTypedEquals(Map[Symbol, Any]('s -> "foo"), m2)
      assertTypedEquals(Map[Symbol, Any]('b -> true), m3)
    }

    type US = Union.`"first" -> Option[Int], "second" -> Option[Boolean], "third" -> Option[String]`.T
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
  def testMapValues {
    object f extends Poly1 {
      implicit def int = at[Int](i => i > 0)
      implicit def string = at[String](s => s"s: $s")
      implicit def boolean = at[Boolean](v => if (v) "Yup" else "Nope")
    }

    {
      val u1 = Union[U](i = 23)
      val u2 = Union[U](s = "foo")
      val u3 = Union[U](b = true)

      type R = Union.`'i -> Boolean, 's -> String, 'b -> String`.T

      val res1 = u1.mapValues(f)
      val res2 = u2.mapValues(f)
      val res3 = u3.mapValues(f)

      assertTypedEquals[R](Union[R](i = true), res1)
      assertTypedEquals[R](Union[R](s = "s: foo"), res2)
      assertTypedEquals[R](Union[R](b = "Yup"), res3)
    }

    {
      object toUpper extends Poly1 {
        implicit def stringToUpper = at[String](_.toUpperCase)
        implicit def otherTypes[X] = at[X](identity)
      }

      type U = Union.`"foo" -> String, "bar" -> Boolean, "baz" -> Double`.T
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
