/*
 * Copyright (c) 2015 Miles Sabin
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

import test.illTyped

trait CachedTC[T]
object CachedTC {
  implicit def mkTC[T] = new CachedTC[T] {}
  implicit val cached: CachedTC[String] = cachedImplicit
}

object CachedTest {
  implicit val i: CachedTC[Int] = cachedImplicit
}

class CachedTest {
  import CachedTest._

  @Test
  def testBasics {
    assertTrue(CachedTest.i != null)
  }

  trait Foo[A]
  object Foo {
    implicit def materialize[A]: Foo[A] = new Foo[A] {}
  }

  case class Bar(x: Int)
  object Bar {
    implicit val foo: Foo[Bar] = cachedImplicit
  }

  @Test
  def testCompanion {
    assertTrue(CachedTC.cached != null)
    assertTrue(Bar.foo != null)
  }

  @Test
  def testDivergent {
    illTyped(
      "cachedImplicit[math.Ordering[Ordered[Int]]]",
      "diverging implicit expansion for type .*"
    )
  }

  @Test
  def testAmbiguous {
    implicit val a = new Foo[String] { }
    implicit val b = new Foo[String] { }
    illTyped(
      "cachedImplicit[Foo[String]]",
      """ambiguous implicit values:.*|could not find an implicit.*"""
    )
  }

  @Test
  def testNotFound {
    trait T[X]
    illTyped(
      "cachedImplicit[T[String]]",
      "could not find an implicit.*"
    )
  }

  @Test
  def testCustomMessage {
    @annotation.implicitNotFound("custom message")
    trait T[X]
    illTyped(
      "cachedImplicit[T[String]]",
      "custom message"
    )
  }
}
