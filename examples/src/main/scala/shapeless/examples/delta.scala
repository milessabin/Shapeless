/*
 * Copyright (c) 2014 Stacy Curl
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

import shapeless._

object DeltaExamples extends App {
  // A pair of arbitrary case classes
  case class Foo(i : Int, s : String)
  case class Bar(b : Boolean, s : String, of: Option[Bar])

  import DeltaSyntax._

  assert(6 == 2.delta(8))
  assert(("foo", "bar") == "foo".delta("bar"))
  assert(6 :: ("foo", "bar") :: HNil == (2 :: "foo" :: HNil).delta(8 :: "bar" :: HNil))
  assert(6 :: ("foo", "bar") :: HNil == Foo(2, "foo").delta(Foo(8, "bar")))
  assert(
    Bar(true,  "foo",  Some(Bar(true, "bar",  None))).delta(Bar(false, "food", Some(Bar(true, "barf", None)))) ==
    false :: ("foo", "food") :: Inl(Some(true :: ("bar", "barf") :: Inl(None) :: HNil)) :: HNil
  )
}

trait Delta[In] {
  type Out

  def apply(before: In, after: In): Out
}

trait Delta0 {
  implicit def generic[F, G, O](
    implicit gen: Generic.Aux[F, G], genDelta: => Delta.Aux[G, O]
  ): Delta.Aux[F, O] = new Delta[F] {
    type Out = O

    def apply(before: F, after: F): Out = genDelta.apply(gen.to(before), gen.to(after))
  }
}

object Delta extends Delta0 {
  def apply[In](implicit delta: Delta[In]): Delta.Aux[In, delta.Out] = delta

  type Aux[In, Out0] = Delta[In] { type Out = Out0 }

  implicit val booleanDelta: Delta.Aux[Boolean, Boolean] = new Delta[Boolean] {
    type Out = Boolean

    def apply(before: Boolean, after: Boolean): Out = before == after
  }

  implicit val intDelta: Delta.Aux[Int, Int] = new Delta[Int] {
    type Out = Int

    def apply(before: Int, after: Int): Out = after - before
  }

  implicit def stringDelta: Delta.Aux[String, (String, String)] = new Delta[String] {
    type Out = (String, String)

    def apply(before: String, after: String): (String, String) = (before, after)
  }

  implicit def optionDelta[T](
    implicit deltaT: Delta[T]
  ): Delta.Aux[Option[T], Option[deltaT.Out] :+: T :+: T :+: CNil] = new Delta[Option[T]] {
    type Out = Option[deltaT.Out] :+: T :+: T :+: CNil

    def apply(before: Option[T], after: Option[T]): Out = (before, after) match {
      case (None, None)       => Inl(None)
      case (Some(b), Some(a)) => Inl(Some(deltaT(b, a)))
      case (Some(b), None)    => Inr(Inl(b))
      case (None, Some(a))    => Inr(Inr(Inl(a)))
    }
  }

  implicit def deriveHNil: Delta.Aux[HNil, HNil] = new Delta[HNil] {
    type Out = HNil

    def apply(before: HNil, after: HNil): HNil = HNil
  }

  implicit def deriveHCons[H, T <: HList, HO](
    implicit deltaH: => Delta.Aux[H, HO], deltaT: Delta[T] { type Out <: HList }
  ): Delta.Aux[H :: T, HO :: deltaT.Out] = new Delta[H :: T] {
    type Out = HO :: deltaT.Out

    def apply(before: H :: T, after: H :: T): Out =
      deltaH(before.head, after.head) :: deltaT(before.tail, after.tail)
  }
}

object DeltaSyntax {
  implicit class DeltaOps[In](val before: In) extends AnyVal {
    def delta(after: In)(implicit delta: Delta[In]): delta.Out = delta(before, after)
  }
}
