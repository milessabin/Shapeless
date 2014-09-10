/*
 * Copyright (c) 2013 Miles Sabin 
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
package ops

import poly._

object coproduct {
  trait Inject[C <: Coproduct, I] {
    def apply(i: I): C
  }

  object Inject {
    def apply[C <: Coproduct, I](implicit inject: Inject[C, I]): Inject[C, I] = inject

    implicit def tlInject[H, T <: Coproduct, I](implicit tlInj : Inject[T, I]): Inject[H :+: T, I] = new Inject[H :+: T, I] {
      def apply(i: I): H :+: T = Inr(tlInj(i))
    }

    implicit def hdInject[H, T <: Coproduct]: Inject[H :+: T, H] = new Inject[H :+: T, H] {
      def apply(i: H): H :+: T = Inl(i)
    }
  }

  trait Selector[C <: Coproduct, T] {
    def apply(c: C): Option[T]
  }

  object Selector {
    def apply[C <: Coproduct, T](implicit select: Selector[C, T]): Selector[C, T] = select

    implicit def tlSelector1[H, T <: Coproduct, S](implicit st: Selector[T, S]): Selector[H :+: T, S] = new Selector[H :+: T, S] {
      def apply(c: H :+: T): Option[S] = c match {
        case Inl(h) => None
        case Inr(t) => st(t)
      }
    }

    implicit def hdSelector[H, T <: Coproduct](implicit st: Selector[T, H] = null): Selector[H :+: T, H] = new Selector[H :+: T, H] {
      def apply(c: H :+: T): Option[H] = c match {
        case Inl(h) => Some(h)
        case Inr(t) => if (st != null) st(t) else None
      }
    }
  }

  trait At[C <: Coproduct, N <: Nat] extends DepFn1[C] {
    type A
    type Out = Option[A]
  }

  object At {
    def apply[C <: Coproduct, N <: Nat](implicit at: At[C, N]): Aux[C, N, at.A] = at

    type Aux[C <: Coproduct, N <: Nat, A0] = At[C, N] { type A = A0 }

    implicit def coproductAt0[H, T <: Coproduct]: Aux[H :+: T, Nat._0, H] = new At[H :+: T, Nat._0] {
      type A = H

      def apply(c: H :+: T): Out = c match {
        case Inl(h) => Some(h)
        case _      => None
      }
    }

    implicit def coproductAtN[H, T <: Coproduct, N <: Nat](
      implicit att: At[T, N]
    ): Aux[H :+: T, Succ[N], att.A] = new At[H :+: T, Succ[N]] {
      type A = att.A

      def apply(c: H :+: T): Out = c match {
        case Inl(_)    => None
        case Inr(tail) => att(tail)
      }
    }
  }

  trait Filter[C <: Coproduct, U] extends DepFn1[C] {
    type A <: Coproduct
    type Out = Option[A]
  }

  object Filter {
    def apply[C <: Coproduct, U](implicit filter: Filter[C, U]): Aux[C, U, filter.A] = filter

    type Aux[C <: Coproduct, U, A0 <: Coproduct] = Filter[C, U] { type A = A0 }

    implicit def cnilFilter[U]: Aux[CNil, U, CNil] = new Filter[CNil, U] {
      type A = CNil

      def apply(c: CNil): Option[A] = Some(c)
    }

    implicit def coproductFilter_Match[H, T <: Coproduct, FilterT <: Coproduct](
      implicit filter: Aux[T, H, FilterT], inject: Inject[H :+: FilterT, H]
    ): Aux[H :+: T, H, H :+: FilterT] = new Filter[H :+: T, H] {
      type A = H :+: FilterT

      def apply(c: H :+: T): Option[A] = c match {
        case Inl(h) => Some(inject(h))
        case Inr(t) => filter(t).map(Inr[H, FilterT](_))
      }
    }

    implicit def coproductFilter_NonMatch[H, T <: Coproduct, FilterT <: Coproduct, U](
      implicit filter: Aux[T, U, FilterT], e: U =:!= H
    ): Aux[H :+: T, U, FilterT] = new Filter[H :+: T, U] {
      type A = FilterT

      def apply(c: H :+: T): Option[A] = c match {
        case Inr(t) => filter(t)
        case _      => None
      }
    }
  }

  trait FilterNot[C <: Coproduct, U] extends DepFn1[C] {
    type A <: Coproduct
    type Out = Option[A]
  }

  object FilterNot {
    def apply[C <: Coproduct, U](implicit filterNot: FilterNot[C, U]): Aux[C, U, filterNot.A] = filterNot

    type Aux[C <: Coproduct, U, A0 <: Coproduct] = FilterNot[C, U] { type A = A0 }

    implicit def cnilFilterNot[U]: Aux[CNil, U, CNil] = new FilterNot[CNil, U] {
      type A = CNil

      def apply(c: CNil): Option[A] = Some(c)
    }

    implicit def coproductFilterNot_Match[H, T <: Coproduct, TFilterNotH <: Coproduct](
      implicit filterNot: Aux[T, H, TFilterNotH]
    ): Aux[H :+: T, H, TFilterNotH] = new FilterNot[H :+: T, H] {
      type A = TFilterNotH

      def apply(c: H :+: T): Option[A] = c match {
        case Inr(t) => filterNot(t)
        case _      => None
      }
    }

    implicit def coproductFilterNot_NonMatch[H, T <: Coproduct, TFilterNotU <: Coproduct, U](
      implicit filterNot: Aux[T, U, TFilterNotU], inject: Inject[H :+: TFilterNotU, H], e: U =:!= H
    ): Aux[H :+: T, U, H :+: TFilterNotU] = new FilterNot[H :+: T, U] {
      type A = H :+: TFilterNotU

      def apply(c: H :+: T): Option[A] = c match {
        case Inl(h) => Some(inject(h))
        case Inr(t) => filterNot(t).map(Inr[H, TFilterNotU](_))
      }
    }
  }

  trait RemoveElem[C <: Coproduct, U] extends DepFn1[C] {
    type Rest <: Coproduct
    type Out = U :+: Rest

    def either(c: C): Either[U, Rest] = apply(c) match {
      case Inl(u) => Left(u)
      case Inr(r) => Right(r)
    }
  }

  object RemoveElem {
    def apply[C <: Coproduct, U]
      (implicit removeElem: RemoveElem[C, U]): Aux[C, U, removeElem.Rest] = removeElem

    type Aux[C <: Coproduct, U, Rest0 <: Coproduct] = RemoveElem[C, U] { type Rest = Rest0 }

    implicit def removeElemHead[H, T <: Coproduct]: Aux[H :+: T, H, T] = new RemoveElem[H :+: T, H] {
      type Rest = T

      def apply(c: H :+: T): Out = c
    }

    implicit def removeElemTail[H, T <: Coproduct, U, TRest <: Coproduct](
      implicit removeElem: Aux[T, U, TRest]
    ): Aux[H :+: T, U, H :+: TRest] = new RemoveElem[H :+: T, U] {
      type Rest = H :+: TRest

      def apply(c: H :+: T): Out = c match {
        case Inl(h) => Inr[U, H :+: TRest](Inl[H, TRest](h))
        case Inr(t) => removeElem(t) match {
          case Inl(u) => Inl[U, H :+: TRest](u)
          case Inr(r) => Inr[U, H :+: TRest](Inr[H, TRest](r))
        }
      }
    }
  }

  trait FlatMap[C <: Coproduct, F <: Poly] extends DepFn1[C] { type Out <: Coproduct }

  object FlatMap {
    def apply[C <: Coproduct, F <: Poly](implicit folder: FlatMap[C, F]): Aux[C, F, folder.Out] = folder

    type Aux[C <: Coproduct, F <: Poly, Out0 <: Coproduct] = FlatMap[C, F] { type Out = Out0 }

    implicit def cnilFlatMap[F <: Poly]: Aux[CNil, F, CNil] = new FlatMap[CNil, F] {
      type Out = CNil

      def apply(c: CNil): Out = c
    }

    implicit def cpFlatMap[H, T <: Coproduct, F <: Poly, OutH <: Coproduct, OutT <: Coproduct](
      implicit
       fh: Case1.Aux[F, H, OutH],
       ft: FlatMap.Aux[T, F, OutT],
       extendBy: ExtendBy[OutH, OutT]
    ): Aux[H :+: T, F, extendBy.Out] = new FlatMap[H :+: T, F] {
      type Out = extendBy.Out

      def apply(c: H :+: T): Out = c match {
        case Inl(h) => extendBy.right(fh(h))
        case Inr(t) => extendBy.left(ft(t))
      }
    }

  }

  trait Mapper[F <: Poly, C <: Coproduct] extends DepFn1[C] { type Out <: Coproduct }

  object Mapper {
    def apply[F <: Poly, C <: Coproduct](implicit mapper: Mapper[F, C]): Aux[F, C, mapper.Out] = mapper
    def apply[C <: Coproduct](f: Poly)(implicit mapper: Mapper[f.type, C]): Aux[f.type, C, mapper.Out] = mapper

    type Aux[F <: Poly, C <: Coproduct, Out0 <: Coproduct] = Mapper[F, C] { type Out = Out0 }

    implicit def cnilMapper[F <: Poly]: Aux[F, CNil, CNil] = new Mapper[F, CNil] {
      type Out = CNil
      def apply(t: CNil): Out = t
    }

    implicit def cpMapper[F <: Poly, H, OutH, T <: Coproduct]
      (implicit fh: Case1.Aux[F, H, OutH], mt: Mapper[F, T]): Aux[F, H :+: T, OutH :+: mt.Out] =
        new Mapper[F, H :+: T] {
          type Out = OutH :+: mt.Out
          def apply(c: H :+: T): Out = c match {
            case Inl(h) => Inl(fh(h))
            case Inr(t) => Inr(mt(t))
          }
        }
  }

  trait Unifier[C <: Coproduct] extends DepFn1[C]

  object Unifier {
    def apply[C <: Coproduct](implicit unifier: Unifier[C]): Aux[C, unifier.Out] = unifier

    type Aux[C <: Coproduct, Out0] = Unifier[C] { type Out = Out0 }

    implicit def lstUnifier[H]: Aux[H :+: CNil, H] =
      new Unifier[H :+: CNil] {
        type Out = H
        def apply(c: H :+: CNil): Out = (c: @unchecked) match {
          case Inl(h) => h
        }
      }
    
    implicit def cpUnifier[H1, H2, T <: Coproduct, TL, L, Out0 >: L]
      (implicit u: Lub[H1, H2, L], lt: Aux[L :+: T, Out0]): Aux[H1 :+: H2 :+: T, Out0] =
        new Unifier[H1 :+: H2 :+: T] {
          type Out = Out0
          def apply(c: H1 :+: H2 :+: T): Out = c match {
            case Inl(h1) => u.left(h1)
            case Inr(Inl(h2)) => u.right(h2)
            case Inr(Inr(t)) => lt(Inr(t))
          }
        }
  }

  trait Folder[F <: Poly, C <: Coproduct] extends DepFn1[C]

  object Folder {
    def apply[F <: Poly, C <: Coproduct](implicit folder: Folder[F, C]): Aux[F, C, folder.Out] = folder
    def apply[C <: Coproduct](f: Poly)(implicit folder: Folder[f.type, C]): Aux[f.type, C, folder.Out] = folder

    type Aux[F <: Poly, C <: Coproduct, Out0] = Folder[F, C] { type Out = Out0 }

    implicit def mkFolder[F <: Poly, C <: Coproduct, M <: Coproduct, Out0]
      (implicit mapper: Mapper.Aux[F, C, M], unifier: Unifier.Aux[M, Out0]): Aux[F, C, Out0] =
        new Folder[F, C] {
          type Out = Out0
          def apply(c: C): Out = unifier(mapper(c))
        }
  }

  trait ZipWithKeys[K <: HList, V <: Coproduct] extends DepFn2[K, V] { type Out <: Coproduct }

  object ZipWithKeys {
    import shapeless.record._

    def apply[K <: HList, V <: Coproduct]
      (implicit zipWithKeys: ZipWithKeys[K, V]): Aux[K, V, zipWithKeys.Out] = zipWithKeys

    type Aux[K <: HList, V <: Coproduct, Out0 <: Coproduct] = ZipWithKeys[K, V] { type Out = Out0 }

    implicit val cnilZipWithKeys: Aux[HNil, CNil, CNil] = new ZipWithKeys[HNil, CNil] {
      type Out = CNil
      def apply(k: HNil, v: CNil) = v
    }

    implicit def cpZipWithKeys[KH, VH, KT <: HList, VT <: Coproduct] (implicit zipWithKeys: ZipWithKeys[KT, VT], wkh: Witness.Aux[KH])
        : Aux[KH :: KT, VH :+: VT, FieldType[KH, VH] :+: zipWithKeys.Out] =
          new ZipWithKeys[KH :: KT, VH :+: VT] {
            type Out = FieldType[KH, VH] :+: zipWithKeys.Out
            def apply(k: KH :: KT, v: VH :+: VT): Out = v match {
              case Inl(vh) => Inl(field[wkh.T](vh))
              case Inr(vt) => Inr(zipWithKeys(k.tail, vt))
            }
          }
  }

  /**
   * Type class supporting computing the type-level Nat corresponding to the length of this `Coproduct'.
   *
   * @author Stacy Curl
   */
  trait Length[C <: Coproduct] extends DepFn0 { type Out <: Nat }

  object Length {
    def apply[C <: Coproduct](implicit length: Length[C]): Aux[C, length.Out] = length

    type Aux[C <: Coproduct, Out0 <: Nat] = Length[C] { type Out = Out0 }

    implicit def cnilLength: Aux[CNil, Nat._0] = new Length[CNil] {
      type Out = Nat._0

      def apply(): Out = Nat._0
    }

    implicit def coproductLength[H, T <: Coproduct, N <: Nat]
      (implicit lt: Aux[T, N], sn: Witness.Aux[Succ[N]]): Aux[H :+: T, Succ[N]] = new Length[H :+: T] {
        type Out = Succ[N]

        def apply(): Out = sn.value
      }

  }

  /**
   * Type class supporting extending a coproduct on the right
   *
   * @author Stacy Curl
   */
  trait ExtendRight[C <: Coproduct, T] extends DepFn1[C] { type Out <: Coproduct }

  object ExtendRight {
    def apply[C <: Coproduct, T]
      (implicit extendRight: ExtendRight[C, T]): Aux[C, T, extendRight.Out] = extendRight

    type Aux[C <: Coproduct, T, Out0 <: Coproduct] = ExtendRight[C, T] { type Out = Out0 }

    implicit def extendRightSingleton[H, A]: Aux[H :+: CNil, A, H :+: A :+: CNil] =
      new ExtendRight[H :+: CNil, A] {
        type Out = H :+: A :+: CNil

        def apply(c: H :+: CNil): Out = c match {
          case Inl(h) => Inl(h)
          case Inr(t) => Inr(Inr(t))
        }
      }

    implicit def extendRightCoproduct[H, T <: Coproduct, A, AT <: Coproduct]
      (implicit extendRight: Aux[T, A, AT]): Aux[H :+: T, A, H :+: AT] =
        new ExtendRight[H :+: T, A] {
          type Out = H :+: AT

          def apply(c: H :+: T) = c match {
            case Inl(h) => Inl(h)
            case Inr(t) => Inr(extendRight(t))
          }
        }
  }

  trait ExtendBy[L <: Coproduct, R <: Coproduct] {
    type Out <: Coproduct

    def right(l: L): Out
    def left(r: R): Out
  }

  object ExtendBy {
    def apply[L <: Coproduct, R <: Coproduct]
      (implicit extendBy: ExtendBy[L, R]): Aux[L, R, extendBy.Out] = extendBy

    type Aux[L <: Coproduct, R <: Coproduct, Out0 <: Coproduct] = ExtendBy[L, R] { type Out = Out0 }

    implicit def extendBy[L <: Coproduct, R <: Coproduct, Out0 <: Coproduct](
      implicit extendLeftBy: ExtendLeftBy.Aux[L, R, Out0], extendRightBy: ExtendRightBy.Aux[L, R, Out0]
    ): ExtendBy.Aux[L, R, Out0] = new ExtendBy[L, R] {
      type Out = Out0

      def right(l: L): Out = extendRightBy(l)
      def left(r: R): Out = extendLeftBy(r)
    }
  }

  trait ExtendLeftBy[L <: Coproduct, R <: Coproduct] extends DepFn1[R] { type Out <: Coproduct }

  object ExtendLeftBy {
    def apply[L <: Coproduct, R <: Coproduct]
      (implicit extendLeftBy: ExtendLeftBy[L, R]): Aux[L, R, extendLeftBy.Out] = extendLeftBy

    type Aux[L <: Coproduct, R <: Coproduct, Out0 <: Coproduct] = ExtendLeftBy[L, R] { type Out = Out0 }

    implicit def extendLeftByCoproduct[L <: Coproduct, R <: Coproduct, RevL <: Coproduct](
      implicit reverseL: Reverse.Aux[L, RevL], impl: Impl[RevL, R]
    ): Aux[L, R, impl.Out] = new ExtendLeftBy[L, R] {
      type Out = impl.Out

      def apply(r: R): Out = impl(r)
    }

    trait Impl[RevL <: Coproduct, R <: Coproduct] extends DepFn1[R] { type Out <: Coproduct }

    object Impl {
      type Aux[RevL <: Coproduct, R <: Coproduct, Out0 <: Coproduct] = Impl[RevL, R] { type Out = Out0 }

      implicit def extendLeftByCNilImpl[R <: Coproduct]: Aux[CNil, R, R] = new Impl[CNil, R] {
        type Out = R

        def apply(r: R): Out = r
      }

      implicit def extendLeftByCoproductImpl[H, T <: Coproduct, R <: Coproduct](
        implicit extendLeftBy: Impl[T, H :+: R]
      ): Aux[H :+: T, R, extendLeftBy.Out] = new Impl[H :+: T, R] {
        type Out = extendLeftBy.Out

        def apply(r: R): Out = extendLeftBy(Inr[H, R](r))
      }
    }
  }

  trait ExtendRightBy[L <: Coproduct, R <: Coproduct] extends DepFn1[L] { type Out <: Coproduct }

  object ExtendRightBy {
    def apply[L <: Coproduct, R <: Coproduct]
      (implicit extendRightBy: ExtendRightBy[L, R]): Aux[L, R, extendRightBy.Out] = extendRightBy

    type Aux[L <: Coproduct, R <: Coproduct, Out0 <: Coproduct] = ExtendRightBy[L, R] { type Out = Out0 }

    implicit def extendRightByCNil[L <: Coproduct]: Aux[L, CNil, L] = new ExtendRightBy[L, CNil] {
      type Out = L

      def apply(l: L): Out = l
    }

    implicit def extendRightByCoproduct[L <: Coproduct, H, LH <: Coproduct, T <: Coproduct](
      implicit extendRight: ExtendRight.Aux[L, H, LH], extendRightBy: ExtendRightBy[LH, T]
    ): Aux[L, H :+: T, extendRightBy.Out] = new ExtendRightBy[L, H :+: T] {
      type Out = extendRightBy.Out

      def apply(l: L): Out = extendRightBy(extendRight(l))
    }
  }

  /**
   * Type class supporting rotating a Coproduct left
   *
   * @author Stacy Curl
   */
  trait RotateLeft[C <: Coproduct, N <: Nat] extends DepFn1[C] { type Out <: Coproduct }

  object RotateLeft extends LowPriorityRotateLeft {
    def apply[C <: Coproduct, N <: Nat]
      (implicit rotateLeft: RotateLeft[C, N]): Aux[C, N, rotateLeft.Out] = rotateLeft

    implicit def implToRotateLeft[C <: Coproduct, N <: Nat, Size <: Nat, NModSize <: Succ[_]]
      (implicit
       length: Length.Aux[C, Size],
       mod: nat.Mod.Aux[N, Size, NModSize],
       impl: Impl[C, NModSize]
      ): Aux[C, N, impl.Out] = new RotateLeft[C, N] {
        type Out = impl.Out

        def apply(c: C): Out = impl(c)
      }

    trait Impl[C <: Coproduct, N <: Nat] extends DepFn1[C] { type Out <: Coproduct }

    object Impl {
      type Aux[C <: Coproduct, N <: Nat, Out0 <: Coproduct] = Impl[C, N] { type Out = Out0 }

      implicit def rotateCoproductOne[H, T <: Coproduct, TH <: Coproduct]
        (implicit extendRight: ExtendRight.Aux[T, H, TH], inject: Inject[TH, H]): Aux[H :+: T, Nat._1, TH] =
         new Impl[H :+: T, Nat._1] {
           type Out = TH

           def apply(c: H :+: T): Out = c match {
             case Inl(a)    => inject(a)
             case Inr(tail) => extendRight(tail)
           }
         }

      implicit def rotateCoproductN[C <: Coproduct, N <: Nat, CN <: Coproduct, CSN <: Coproduct]
        (implicit rotateN: Aux[C, N, CN], rotate1: Aux[CN, Nat._1, CSN]): Aux[C, Succ[N], CSN] =
          new Impl[C, Succ[N]] {
            type Out = CSN

            def apply(c: C): Out = rotate1(rotateN(c))
          }
    }
  }

  trait LowPriorityRotateLeft {
    type Aux[C <: Coproduct, N <: Nat, Out0 <: Coproduct] = RotateLeft[C, N] { type Out = Out0 }

    implicit def noopRotateLeftImpl[C <: Coproduct, N <: Nat]: Aux[C, N, C] = new RotateLeft[C, N] {
      type Out = C

      def apply(c: C): Out = c
    }
  }

  /**
   * Type class supporting rotating a Coproduct right
   *
   * @author Stacy Curl
   */
  trait RotateRight[C <: Coproduct, N <: Nat] extends DepFn1[C] { type Out <: Coproduct }

  object RotateRight extends LowPriorityRotateRight {
    def apply[C <: Coproduct, N <: Nat]
      (implicit rotateRight: RotateRight[C, N]): Aux[C, N, rotateRight.Out] = rotateRight

    implicit def hlistRotateRightt[
      C <: Coproduct, N <: Nat, Size <: Nat, NModSize <: Succ[_], Size_Diff_NModSize <: Nat
    ](implicit
      length: Length.Aux[C, Size],
      mod: nat.Mod.Aux[N, Size, NModSize],
      diff: nat.Diff.Aux[Size, NModSize, Size_Diff_NModSize],
      rotateLeft: RotateLeft.Impl[C, Size_Diff_NModSize]
    ): Aux[C, N, rotateLeft.Out] = new RotateRight[C, N] {
      type Out = rotateLeft.Out

      def apply(c: C): Out = rotateLeft(c)
    }
  }

  trait LowPriorityRotateRight {
    type Aux[C <: Coproduct, N <: Nat, Out0 <: Coproduct] = RotateRight[C, N] { type Out = Out0 }

    implicit def noopRotateRight[C <: Coproduct, N <: Nat]: Aux[C, N, C] = new RotateRight[C, N] {
      type Out = C

      def apply(c: C): Out = c
    }
  }

  /**
   * Type class providing access to head and tail of a Coproduct
   *
   * @author Stacy Curl
   */
  trait IsCCons[C <: Coproduct] extends DepFn1[C] {
    type Prefix
    type Suffix <: Coproduct
    type Out = Either[Prefix, Suffix]

    def head(c: C): Option[Prefix] = apply(c).left.toOption
    def tail(c: C): Option[Suffix] = apply(c).right.toOption
    def apply(c: C): Out = toEither(coproduct(c))
    def coproduct(c: C): Prefix :+: Suffix :+: CNil
  }

  object IsCCons {
    def apply[C <: Coproduct](implicit isCCons: IsCCons[C]): Aux[C, isCCons.Prefix, isCCons.Suffix] = isCCons

    type Aux[C <: Coproduct, H0, T0 <: Coproduct] = IsCCons[C] { type Prefix = H0; type Suffix = T0 }

    implicit def coproductCCons[H0, T0 <: Coproduct]: Aux[H0 :+: T0, H0, T0] = new IsCCons[H0 :+: T0] {
      type Prefix = H0
      type Suffix = T0

      def coproduct(c: H0 :+: T0): H0 :+: T0 :+: CNil = c match {
        case Inl(h) => Inl(h)
        case Inr(t) => Inr(Inl(t))
        case _      => sys.error("Impossible")
      }
    }
  }
  /**
   * Type class supporting splitting this `Coproduct` at the ''nth'' element returning prefix and suffix as a coproduct
   *
   * @author Stacy Curl
   */
  trait Split[C <: Coproduct, N <: Nat] extends DepFn1[C] {
    type Left  <: Coproduct
    type Right <: Coproduct
    type Out = Either[Left, Right]

    def coproduct(c: C): Left :+: Right :+: CNil
  }

  object Split {
    def apply[C <: Coproduct, N <: Nat](implicit split: Split[C, N]): Aux[C, N, split.Left, split.Right] = split

    type Aux[C <: Coproduct, N <: Nat, L <: Coproduct, R <: Coproduct] =
      Split[C, N] { type Left = L; type Right = R }

    trait Impl[C <: Coproduct, N <: Nat] extends DepFn1[C] {
      type Left  <: Coproduct
      type Right <: Coproduct
      type Out = Either[Left, Right]

      def apply(c: C): Out = coproduct(c) match {
        case Inl(left)       => Left(left)
        case Inr(Inl(right)) => Right(right)
        case _               => sys.error("Impossible")
      }

      def coproduct(c: C): Left :+: Right :+: CNil

      protected def left(l: Left)   = Inl[Left, Right :+: CNil](l)
      protected def right(r: Right) = Inr[Left, Right :+: CNil](Inl[Right, CNil](r))
    }

    implicit def coproductSplit[C <: Coproduct, N <: Nat, Size <: Nat, NModSize <: Nat](
      implicit
      length: Length.Aux[C, Size],
      mod: nat.Mod.Aux[N, Succ[Size], NModSize],
      impl: Impl[C, NModSize]
    ): Aux[C, N, impl.Left, impl.Right] = new Split[C, N] {
      type Left = impl.Left
      type Right = impl.Right

      def apply(c: C): Either[Left, Right]         = impl(c)
      def coproduct(c: C): Left :+: Right :+: CNil = impl.coproduct(c)
    }

    object Impl {
      type Aux[C <: Coproduct, N <: Nat, L <: Coproduct, R <: Coproduct] =
        Impl[C, N] { type Left = L; type Right = R }

      implicit def splitZero[C <: Coproduct]: Aux[C, Nat._0, CNil, C] = new Impl[C, Nat._0] {
        type Left  = CNil
        type Right = C

        def coproduct(c: C): Left :+: Right :+: CNil = right(c)
      }

      implicit def splitOne[H1, T <: Coproduct]
        : Aux[H1 :+: T, Nat._1, H1 :+: CNil, T] = new Impl[H1 :+: T, Nat._1] {

        type Left  = H1 :+: CNil
        type Right = T

        def coproduct(c: H1 :+: T): Left :+: Right :+: CNil = c match {
          case Inl(h1) => left(Inl[H1, CNil](h1))
          case Inr(t)  => right(t)
        }
      }

      implicit def coproductImpl[H, T <: Coproduct, N <: Nat, L0 <: Coproduct, R0 <: Coproduct](
        implicit splitN: Aux[T, N, L0, R0]
      ): Aux[H :+: T, Succ[N], H :+: L0, R0] = new Impl[H :+: T, Succ[N]] {
        type Left  = H :+: L0
        type Right = R0

        def coproduct(c: H :+: T): Left :+: Right :+: CNil = c match {
          case Inl(h) => left(Inl[H, L0](h))
          case Inr(t) => splitN.coproduct(t) match {
            case Inl(l0) => left(Inr[H, L0](l0))
            case Inr(Inl(r0)) => right(r0)
            case other        => sys.error("unreachable: " + other)
          }
        }
      }
    }
  }

  /**
   * Type class supporting reversing a Coproduct
   *
   * @author Stacy Curl
   */
  trait Reverse[C <: Coproduct] extends DepFn1[C] { type Out <: Coproduct }

  object Reverse {
    def apply[C <: Coproduct](implicit reverse: Reverse[C]): Aux[C, reverse.Out] = reverse

    type Aux[C <: Coproduct, Out0 <: Coproduct] = Reverse[C] { type Out = Out0 }

    implicit val reverseCNil: Aux[CNil, CNil] = new Reverse[CNil] {
      type Out = CNil

      def apply(c: CNil): Out = c
    }

    implicit def reverseCoproduct[
      H, T <: Coproduct, ReverseT <: Coproduct, RotateL_HReverseT <: Coproduct
    ](
      implicit
      reverse: Aux[T, ReverseT],
      rotateLeft: RotateLeft.Aux[H :+: ReverseT, Nat._1, RotateL_HReverseT],
      inject: Inject[RotateL_HReverseT, H]
    ): Aux[H :+: T, RotateL_HReverseT] = new Reverse[H :+: T] {
      type Out = RotateL_HReverseT

      def apply(c: H :+: T): Out = c match {
        case Inl(h) => inject(h)
        case Inr(t) => rotateLeft(Inr[H, ReverseT](reverse(t)))
      }
    }
  }

  /**
   * Type class providing access to init and last of a Coproduct
   *
   * @author Stacy Curl
   */
  trait InitLast[C <: Coproduct] extends DepFn1[C] {
    type Prefix <: Coproduct
    type Suffix
    type Out = Either[Prefix, Suffix]

    def init(c: C): Option[Prefix] = apply(c).left.toOption
    def last(c: C): Option[Suffix] = apply(c).right.toOption
    def apply(c: C): Out = toEither(coproduct(c))
    def coproduct(c: C): Prefix :+: Suffix :+: CNil
  }

  object InitLast {
    def apply[C <: Coproduct](implicit initLast: InitLast[C]): Aux[C, initLast.Prefix, initLast.Suffix] = initLast

    type Aux[C <: Coproduct, I0 <: Coproduct, L0] = InitLast[C] { type Prefix = I0; type Suffix = L0 }

    implicit def initLastCoproduct[C <: Coproduct, ReverseC <: Coproduct, H, T <: Coproduct](
      implicit reverse: Reverse.Aux[C, ReverseC], isCCons: IsCCons.Aux[ReverseC, H, T]
    ): Aux[C, T, H] = new InitLast[C] {
      type Prefix = T
      type Suffix = H

      def coproduct(c: C): Prefix :+: Suffix :+: CNil = isCCons.coproduct(reverse(c)) match {
        case Inl(suffix)      => Inr(Inl(suffix))
        case Inr(Inl(prefix)) => Inl(prefix)
        case _                => sys.error("Impossible")
      }
    }
  }

  implicit object cnilOrdering extends Ordering[CNil] {
    def compare(x: CNil, y: CNil) = 0
  }

  implicit def coproductPartialOrdering[H, T <: Coproduct]
    (implicit ordering: Ordering[H], partialOrdering: PartialOrdering[T]): PartialOrdering[H :+: T] =
      new PartialOrdering[H :+: T] {
        def lteq(x: H :+: T, y: H :+: T): Boolean = (x, y) match {
          case (Inl(xh), Inl(yh)) => ordering.compare(xh, yh) <= 0
          case (Inr(xt), Inr(yt)) => partialOrdering.tryCompare(xt, yt).fold(false)(_ <= 0)
          case _                  => false
        }

        def tryCompare(x: H :+: T, y: H :+: T): Option[Int] = (x, y) match {
          case (Inl(xh), Inl(yh)) => Some(ordering.compare(xh, yh))
          case (Inr(xt), Inr(yt)) => partialOrdering.tryCompare(xt, yt)
          case _                  => None
        }
      }

  private def toEither[Prefix, Suffix](c: Prefix :+: Suffix :+: CNil): Either[Prefix, Suffix] = c match {
    case Inl(left)       => Left(left)
    case Inr(Inl(right)) => Right(right)
    case _               => sys.error("Impossible")
  }
}
