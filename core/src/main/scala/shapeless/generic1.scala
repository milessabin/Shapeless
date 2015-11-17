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

import scala.language.existentials
import scala.language.experimental.macros

import scala.annotation.{ StaticAnnotation, tailrec }
import scala.reflect.api.Universe
import scala.reflect.macros.Context

import ops.{ hlist, coproduct }

trait Generic1[F[_], FR[_[_]]] extends Serializable {
  type R[t]

  lazy val fr: FR[R] = mkFrr

  def to[T](ft: F[T]): R[T]
  def from[T](rt: R[T]): F[T]

  def mkFrr: FR[R]
}

object Generic1 extends Generic10 {
  type Aux[F[_], FR[_[_]], R0[_]] = Generic1[F, FR] { type R[t] = R0[t] }

  implicit def apply[T[_], FR[_[_]]]: Generic1[T, FR] = macro Generic1Macros.mkGeneric1Impl[T, FR, Generic1[T, FR]]
}

trait Generic10 {
  implicit def mkGeneric10[T[_], U[_], FR[_[_], _[_]]]: Generic1[T, ({ type λ[t[_]] = FR[t, U] })#λ] =
    macro Generic1Macros.mkGeneric1Impl[T, ({ type λ[t[_]] = FR[t, U] })#λ, Generic1[T, ({ type λ[t[_]] = FR[t, U] })#λ]]

  implicit def mkGeneric11[T[_], U[_], FR[_[_], _[_]]]: Generic1[T, ({ type λ[t[_]] = FR[U, t] })#λ] =
    macro Generic1Macros.mkGeneric1Impl[T, ({ type λ[t[_]] = FR[U, t] })#λ, Generic1[T, ({ type λ[t[_]] = FR[U, t] })#λ]]
}

trait IsHCons1[L[_], FH[_[_]], FT[_[_]]] extends Serializable {
  type H[_]
  type T[_] <: HList

  lazy val fh: FH[H] = mkFhh
  lazy val ft: FT[T] = mkFtt

  def pack[A](u: (H[A], T[A])): L[A]
  def unpack[A](p: L[A]): (H[A], T[A])

  def mkFhh: FH[H]
  def mkFtt: FT[T]
}

object IsHCons1 extends IsHCons10 {
  type Aux[L[_], FH[_[_]], FT[_[_]], H0[_], T0[_] <: HList] = IsHCons1[L, FH, FT] { type H[t] = H0[t] ; type T[t] = T0[t] }

  implicit def apply[L[_], FH[_[_]], FT[_[_]]]: IsHCons1[L, FH, FT] = macro IsHCons1Macros.mkIsHCons1Impl[L, FH, FT, IsHCons1[L, FH, FT]]
}

trait IsHCons10 {
  implicit def mkIsHCons10[L[_], FH[_[_], _[_]], U[_], FT[_[_]]]: IsHCons1[L, ({ type λ[t[_]] = FH[t, U] })#λ, FT] =
    macro IsHCons1Macros.mkIsHCons1Impl[L, ({ type λ[t[_]] = FH[t, U] })#λ, FT, IsHCons1[L, ({ type λ[t[_]] = FH[t, U] })#λ, FT]]

  implicit def mkIsHCons11[L[_], FH[_[_], _[_]], U[_], FT[_[_]]]: IsHCons1[L, ({ type λ[t[_]] = FH[U, t] })#λ, FT] =
    macro IsHCons1Macros.mkIsHCons1Impl[L, ({ type λ[t[_]] = FH[U, t] })#λ, FT, IsHCons1[L, ({ type λ[t[_]] = FH[U, t] })#λ, FT]]

  implicit def mkIsHCons12[L[_], FH[_[_]], FT[_[_], _[_]], U[_]]: IsHCons1[L, FH, ({ type λ[t[_]] = FT[t, U] })#λ] =
    macro IsHCons1Macros.mkIsHCons1Impl[L, FH, ({ type λ[t[_]] = FT[t, U] })#λ, IsHCons1[L, FH, ({ type λ[t[_]] = FT[t, U] })#λ]]

  implicit def mkIsHCons13[L[_], FH[_[_]], FT[_[_], _[_]], U[_]]: IsHCons1[L, FH, ({ type λ[t[_]] = FT[U, t] })#λ] =
    macro IsHCons1Macros.mkIsHCons1Impl[L, FH, ({ type λ[t[_]] = FT[U, t] })#λ, IsHCons1[L, FH, ({ type λ[t[_]] = FT[U, t] })#λ]]
}

trait IsCCons1[L[_], FH[_[_]], FT[_[_]]] extends Serializable {
  type H[_]
  type T[_] <: Coproduct

  lazy val fh: FH[H] = mkFhh
  lazy val ft: FT[T] = mkFtt

  def pack[A](u: Either[H[A], T[A]]): L[A]
  def unpack[A](p: L[A]): Either[H[A], T[A]]

  def mkFhh: FH[H]
  def mkFtt: FT[T]
}

object IsCCons1 extends IsCCons10 {
  type Aux[L[_], FH[_[_]], FT[_[_]], H0[_], T0[_] <: Coproduct] = IsCCons1[L, FH, FT] { type H[t] = H0[t] ; type T[t] = T0[t] }

  implicit def apply[L[_], FH[_[_]], FT[_[_]]]: IsCCons1[L, FH, FT] = macro IsCCons1Macros.mkIsCCons1Impl[L, FH, FT, IsCCons1[L, FH, FT]]
}

trait IsCCons10 {
  implicit def mkIsCCons10[L[_], FH[_[_], _[_]], U[_], FT[_[_]]]: IsCCons1[L, ({ type λ[t[_]] = FH[t, U] })#λ, FT] =
    macro IsCCons1Macros.mkIsCCons1Impl[L, ({ type λ[t[_]] = FH[t, U] })#λ, FT, IsCCons1[L, ({ type λ[t[_]] = FH[t, U] })#λ, FT]]

  implicit def mkIsCCons11[L[_], FH[_[_], _[_]], U[_], FT[_[_]]]: IsCCons1[L, ({ type λ[t[_]] = FH[U, t] })#λ, FT] =
    macro IsCCons1Macros.mkIsCCons1Impl[L, ({ type λ[t[_]] = FH[U, t] })#λ, FT, IsCCons1[L, ({ type λ[t[_]] = FH[U, t] })#λ, FT]]

  implicit def mkIsCCons12[L[_], FH[_[_]], FT[_[_], _[_]], U[_]]: IsCCons1[L, FH, ({ type λ[t[_]] = FT[t, U] })#λ] =
    macro IsCCons1Macros.mkIsCCons1Impl[L, FH, ({ type λ[t[_]] = FT[t, U] })#λ, IsCCons1[L, FH, ({ type λ[t[_]] = FT[t, U] })#λ]]

  implicit def mkIsCCons13[L[_], FH[_[_]], FT[_[_], _[_]], U[_]]: IsCCons1[L, FH, ({ type λ[t[_]] = FT[U, t] })#λ] =
    macro IsCCons1Macros.mkIsCCons1Impl[L, FH, ({ type λ[t[_]] = FT[U, t] })#λ, IsCCons1[L, FH, ({ type λ[t[_]] = FT[U, t] })#λ]]
}

trait Split1[L[_], FO[_[_]], FI[_[_]]] extends Serializable {
  type O[_]
  type I[_]

  lazy val fo: FO[O] = mkFoo
  lazy val fi: FI[I] = mkFii

  def pack[T](u: O[I[T]]): L[T]
  def unpack[T](p: L[T]): O[I[T]]

  def mkFoo: FO[O]
  def mkFii: FI[I]
}

object Split1 extends Split10 {
  type Aux[L[_], FO[_[_]], FI[_[_]], O0[_], I0[_]] = Split1[L, FO, FI] { type O[T] = O0[T] ; type I[T] = I0[T] }

  implicit def apply[L[_], FO[_[_]], FI[_[_]]]: Split1[L, FO, FI] = macro Split1Macros.mkSplit1Impl[L, FO, FI, Split1[L, FO, FI]]
}

trait Split10 {
  implicit def mkSplit10[L[_], FO[_[_], _[_]], U[_], FI[_[_]]]: Split1[L, ({ type λ[t[_]] = FO[t, U] })#λ, FI] =
    macro Split1Macros.mkSplit1Impl[L, ({ type λ[t[_]] = FO[t, U] })#λ, FI, Split1[L, ({ type λ[t[_]] = FO[t, U] })#λ, FI]]

  implicit def mkSplit11[L[_], FO[_[_], _[_]], U[_], FI[_[_]]]: Split1[L, ({ type λ[t[_]] = FO[U, t] })#λ, FI] =
    macro Split1Macros.mkSplit1Impl[L, ({ type λ[t[_]] = FO[U, t] })#λ, FI, Split1[L, ({ type λ[t[_]] = FO[U, t] })#λ, FI]]

  implicit def mkSplit12[L[_], FO[_[_]], FI[_[_], _[_]], U[_]]: Split1[L, FO, ({ type λ[t[_]] = FI[t, U] })#λ] =
    macro Split1Macros.mkSplit1Impl[L, FO, ({ type λ[t[_]] = FI[t, U] })#λ, Split1[L, FO, ({ type λ[t[_]] = FI[t, U] })#λ]]

  implicit def mkSplit13[L[_], FO[_[_]], FI[_[_], _[_]], U[_]]: Split1[L, FO, ({ type λ[t[_]] = FI[U, t] })#λ] =
    macro Split1Macros.mkSplit1Impl[L, FO, ({ type λ[t[_]] = FI[U, t] })#λ, Split1[L, FO, ({ type λ[t[_]] = FI[U, t] })#λ]]
}

class Generic1Macros[C <: Context](val c: C) extends CaseClassMacros {
  import c.universe._
  import Flag._

  def mkGeneric1Impl[T[_], FR[_[_]]](implicit tTag: WeakTypeTag[T[_]], frTag: WeakTypeTag[FR[Any]]): Tree = {
    val tpe = tTag.tpe

    val frTpe =
      c.openImplicits.headOption match {
        case Some((TypeRef(_, _, List(_, tpe)), _)) => tpe
        case other => frTag.tpe.typeConstructor
      }

    if(isReprType1(tpe))
      abort("No Generic1 instance available for HList or Coproduct")

    if(isProduct1(tpe))
      mkProductGeneric1(tpe, frTpe)
    else
      mkCoproductGeneric1(tpe, frTpe)
  }

  def mkProductGeneric1(tpe: Type, frTpe: Type): Tree = {
    def mkProductCases: (CaseDef, CaseDef) = {
      if(appliedType(tpe, List(typeOf[Any])).normalize =:= typeOf[Unit])
        (
          cq"() => _root_.shapeless.HNil",
          cq"_root_.shapeless.HNil => ()"
        )
      else if(isCaseObjectLike(tpe.typeSymbol.asClass)) {
        val singleton =
          appliedType(tpe, List(typeOf[Any])).normalize match {
            case SingleType(pre, sym) =>
              mkAttributedRef(pre, sym)
            case TypeRef(pre, sym, List()) if sym.isModule =>
              mkAttributedRef(pre, sym.asModule)
            case TypeRef(pre, sym, List()) if sym.isModuleClass =>
              mkAttributedRef(pre, sym.asClass.module)
            case other =>
              abort(s"Bad case object-like type $tpe")
          }

        (
          cq"x if x eq $singleton => _root_.shapeless.HNil",
          cq"_root_.shapeless.HNil => $singleton"
        )
      } else {
        val sym = tpe.typeSymbol
        val isCaseClass = sym.asClass.isCaseClass
        def hasNonGenericCompanionMember(name: String): Boolean = {
          val mSym = sym.companionSymbol.typeSignature.member(newTermName(name))
          mSym != NoSymbol && !isNonGeneric(mSym)
        }

        val binders = fieldsOf(tpe).map { case (name, tpe) => (newTermName(c.fresh("pat")), name, tpe) }

        val to =
          if(isCaseClass || hasNonGenericCompanionMember("unapply")) {
            val lhs = pq"${companionRef(tpe)}(..${binders.map(x => pq"${x._1}")})"
            val rhs =
              binders.foldRight(q"_root_.shapeless.HNil": Tree) {
                case ((bound, name, tpe), acc) => q"_root_.shapeless.::($bound, $acc)"
              }
            cq"$lhs => $rhs"
          } else {
            val lhs = newTermName(c.fresh("pat"))
            val rhs =
              fieldsOf(tpe).foldRight(q"_root_.shapeless.HNil": Tree) {
                case ((name, tpe), acc) => q"_root_.shapeless.::($lhs.$name, $acc)"
              }
            cq"$lhs => $rhs"
          }

        val from = {
          val lhs =
            binders.foldRight(q"_root_.shapeless.HNil": Tree) {
              case ((bound, _, _), acc) => pq"_root_.shapeless.::($bound, $acc)"
            }

          val rhs = {
            val ctorArgs = binders.map { case (bound, name, tpe) => Ident(bound) }
            if(isCaseClass || hasNonGenericCompanionMember("apply"))
              q"${companionRef(tpe)}(..$ctorArgs)"
            else
              q"new $tpe(..$ctorArgs)"
          }

          cq"$lhs => $rhs"
        }

        (to, from)
      }
    }

    val (toCases, fromCases) = {
      val (to, from) = mkProductCases
      (List(to), List(from))
    }

    val nme = newTypeName(c.fresh)
    val reprTpt = reprTypTree1(tpe, nme)
    val rnme = newTypeName(c.fresh)

    val clsName = newTypeName(c.fresh("anon$"))
    q"""
      type Apply0[F[_], T] = F[T]
      type Apply1[F[_[_]], T[_]] = F[T]

      final class $clsName extends _root_.shapeless.Generic1[$tpe, $frTpe] {
        type R[$nme] = $reprTpt

        def mkFrr: Apply1[$frTpe, R] = _root_.shapeless.lazily[Apply1[$frTpe, R]]

        def to[$nme](ft: Apply0[$tpe, $nme]): R[$nme] = ft match { case ..$toCases }
        def from[$nme](rt: R[$nme]): Apply0[$tpe, $nme] = rt match { case ..$fromCases }
      }
      type $rnme[$nme] = $reprTpt
      new $clsName(): _root_.shapeless.Generic1.Aux[$tpe, $frTpe, $rnme]
    """
  }

  def mkCoproductGeneric1(tpe: Type, frTpe: Type): Tree = {
    def mkCoproductCases(tpe: Type, index: Int): CaseDef = {
      val name = newTermName(c.fresh("pat"))

      val tc = tpe.typeConstructor
      val TypeRef(_, tcSym, _) = tc
      val params = tcSym.asType.typeParams.map { _ => Bind(c.universe.nme.WILDCARD, EmptyTree) }
      val tpeTpt = AppliedTypeTree(mkAttributedRef(tc), params)

      cq"$name: $tpeTpt => $index"
    }

    val nme = newTypeName(c.fresh)
    val reprTpt = reprTypTree1(tpe, nme)
    val rnme = newTypeName(c.fresh)

    val to = {
      val toCases = ctorsOf1(tpe) zip (Stream from 0) map (mkCoproductCases _).tupled
      q"""_root_.shapeless.Coproduct.unsafeMkCoproduct((ft: Any) match { case ..$toCases }, ft).asInstanceOf[R[$nme]]"""
    }

    val clsName = newTypeName(c.fresh("anon$"))
    q"""
      type Apply0[F[_], T] = F[T]
      type Apply1[F[_[_]], T[_]] = F[T]

      final class $clsName extends _root_.shapeless.Generic1[$tpe, $frTpe] {
        type R[$nme] = $reprTpt

        def mkFrr: Apply1[$frTpe, R] = _root_.shapeless.lazily[Apply1[$frTpe, R]]

        def to[$nme](ft: Apply0[$tpe, $nme]): R[$nme] = $to
        def from[$nme](rt: R[$nme]): Apply0[$tpe, $nme] = _root_.shapeless.Coproduct.unsafeGet(rt).asInstanceOf[Apply0[$tpe, $nme]]
      }
      type $rnme[$nme] = $reprTpt
      new $clsName(): _root_.shapeless.Generic1.Aux[$tpe, $frTpe, $rnme]
    """
  }
}

object Generic1Macros {
  def inst(c: Context) = new Generic1Macros[c.type](c)

  def mkGeneric1Impl[T[_], FR[_[_]], R](c: Context)
    (implicit tTag: c.WeakTypeTag[T[_]], frTag: c.WeakTypeTag[FR[Any]]): c.Expr[R] =
      c.Expr[R](inst(c).mkGeneric1Impl[T, FR])
}

class IsHCons1Macros[C <: Context](val c: C) extends IsCons1Macros {
  import c.universe._

  def mkIsHCons1Impl[L[_], FH[_[_]], FT[_[_]]]
    (implicit lTag: WeakTypeTag[L[_]], fhTag: WeakTypeTag[FH[Any]], ftTag: WeakTypeTag[FT[Any]]): Tree =
      mkIsCons1(lTag.tpe, fhTag.tpe.typeConstructor, ftTag.tpe.typeConstructor)

  val isCons1TC: Tree = tq"_root_.shapeless.IsHCons1"
  val consTpe: Type = hconsTpe

  def mkPackUnpack(nme: TypeName, lTpt: Tree, hdTpt: Tree, tlTpt: Tree): (Tree, Tree) =
    (
      q"""
        def pack[$nme](u: ($hdTpt, $tlTpt)): $lTpt = _root_.shapeless.::(u._1, u._2)
      """,
      q"""
        def unpack[$nme](p: $lTpt): ($hdTpt, $tlTpt) = (p.head, p.tail)
      """
    )
}

object IsHCons1Macros {
  def inst(c: Context) = new IsHCons1Macros[c.type](c)

  def mkIsHCons1Impl[L[_], FH[_[_]], FT[_[_]], R](c: Context)
    (implicit
      lTag: c.WeakTypeTag[L[_]],
      fhTag: c.WeakTypeTag[FH[Any]],
      ftTag: c.WeakTypeTag[FT[Any]]
    ): c.Expr[R] =
      c.Expr[R](inst(c).mkIsHCons1Impl[L, FH, FT])
}

class IsCCons1Macros[C <: Context](val c: C) extends IsCons1Macros {
  import c.universe._

  def mkIsCCons1Impl[L[_], FH[_[_]], FT[_[_]]]
    (implicit lTag: WeakTypeTag[L[_]], fhTag: WeakTypeTag[FH[Any]], ftTag: WeakTypeTag[FT[Any]]): Tree =
      mkIsCons1(lTag.tpe, fhTag.tpe.typeConstructor, ftTag.tpe.typeConstructor)

  val isCons1TC: Tree = tq"_root_.shapeless.IsCCons1"
  val consTpe: Type = cconsTpe

  def mkPackUnpack(nme: TypeName, lTpt: Tree, hdTpt: Tree, tlTpt: Tree): (Tree, Tree) =
    (
      q"""
        def pack[$nme](u: _root_.scala.Either[$hdTpt, $tlTpt]): $lTpt = u match {
          case _root_.scala.Left(hd) => _root_.shapeless.Inl[$hdTpt, $tlTpt](hd)
          case _root_.scala.Right(tl) => _root_.shapeless.Inr[$hdTpt, $tlTpt](tl)
        }
      """,
      q"""
        def unpack[$nme](p: $lTpt): _root_.scala.Either[$hdTpt, $tlTpt] = p match {
          case _root_.shapeless.Inl(hd) => _root_.scala.Left[$hdTpt, $tlTpt](hd)
          case _root_.shapeless.Inr(tl) => _root_.scala.Right[$hdTpt, $tlTpt](tl)
        }
      """
    )
}

object IsCCons1Macros {
  def inst(c: Context) = new IsCCons1Macros[c.type](c)

  def mkIsCCons1Impl[L[_], FH[_[_]], FT[_[_]], R](c: Context)
    (implicit
      lTag: c.WeakTypeTag[L[_]],
      fhTag: c.WeakTypeTag[FH[Any]],
      ftTag: c.WeakTypeTag[FT[Any]]
    ): c.Expr[R] =
      c.Expr[R](inst(c).mkIsCCons1Impl[L, FH, FT])
}

trait IsCons1Macros extends CaseClassMacros {
  import c.universe._

  val isCons1TC: Tree
  val consTpe: Type

  def mkPackUnpack(nme: TypeName, lTpt: Tree, hdTpt: Tree, tlTpt: Tree): (Tree, Tree)

  def mkIsCons1(lTpe: Type, fhTpe0: Type, ftTpe0: Type): Tree = {
    val TypeRef(_, lSym, _) = lTpe
    val lParam = lSym.asType.typeParams.head
    val lParamTpe = lParam.asType.toType
    val lDealiasedTpe = appliedType(lTpe, List(lParamTpe)).normalize

    val (fhTpe, ftTpe) =
      c.openImplicits.headOption match {
        case Some((TypeRef(_, _, List(_, fh, ft)), _)) => (fh, ft)
        case other => (fhTpe0, ftTpe0)
      }

    if(!(lDealiasedTpe.typeConstructor =:= consTpe))
      abort("Not H/CCons")

    val TypeRef(_, _, List(hd, tl)) = lDealiasedTpe

    val lPoly = polyType(List(lParam), lDealiasedTpe)
    val hdPoly = polyType(List(lParam), hd)
    val tlPoly = polyType(List(lParam), tl)

    val nme = newTypeName(c.fresh)
    val lTpt = appliedTypTree1(lPoly, lParamTpe, nme)
    val hdTpt = appliedTypTree1(hdPoly, lParamTpe, nme)
    val tlTpt = appliedTypTree1(tlPoly, lParamTpe, nme)

    val (pack, unpack) = mkPackUnpack(nme, lTpt, hdTpt, tlTpt)

    q"""
      type Apply0[F[_], T] = F[T]
      type Apply1[F[_[_]], T[_]] = F[T]

      new $isCons1TC[$lTpe, $fhTpe, $ftTpe] {
        type H[$nme] = $hdTpt
        type T[$nme] = $tlTpt

        def mkFhh: Apply1[$fhTpe, H] = _root_.shapeless.lazily[Apply1[$fhTpe, H]]
        def mkFtt: Apply1[$ftTpe, T] = _root_.shapeless.lazily[Apply1[$ftTpe, T]]

        $pack
        $unpack
      }
    """
  }
}

class Split1Macros[C <: Context](val c: C) extends CaseClassMacros {
  import c.universe._

  def mkSplit1Impl[L[_], FO[_[_]], FI[_[_]]]
    (implicit lTag: WeakTypeTag[L[_]], foTag: WeakTypeTag[FO[Any]], fiTag: WeakTypeTag[FI[Any]]): Tree = {
    val lTpe = lTag.tpe

    val (foTpe, fiTpe) =
      c.openImplicits.headOption match {
        case Some((TypeRef(_, _, List(_, fo, fi)), _)) => (fo, fi)
        case other => (foTag.tpe.typeConstructor, fiTag.tpe.typeConstructor)
      }

    if(isReprType1(lTpe))
      abort("No Split1 instance available for HList or Coproduct")

    val lParam = lTpe match {
      case TypeRef(_, sym, _) => sym.asType.typeParams.head
      case PolyType(List(sym), _) => sym
    }
    val lParamTpe = lParam.asType.toType
    val lDealiasedTpe = appliedType(lTpe, List(lParamTpe)).normalize

    val nme = newTypeName(c.fresh)

    def balanced(args: List[Type]): Boolean =
      args.find(_.contains(lParam)).map { pivot =>
        !(pivot =:= lParamTpe) &&
        args.forall { arg =>
          arg =:= pivot || !arg.contains(lParam)
        }
      }.getOrElse(false)

    val (oTpt, iTpt) =
      lDealiasedTpe match {
        case tpe @ TypeRef(pre, sym, args) if balanced(args) =>
          val Some(pivot) = args.find(_.contains(lParam))
          val oPoly = polyType(List(lParam), appliedType(tpe.typeConstructor, args.map { arg => if(arg =:= pivot) lParamTpe else arg }))
          val oTpt = appliedTypTree1(oPoly, lParamTpe, nme)
          val iPoly = polyType(List(lParam), pivot)
          val iTpt = appliedTypTree1(iPoly, lParamTpe, nme)
          (oTpt, iTpt)
        case other =>
          c.abort(c.enclosingPosition, s"Can't split $other into a non-trivial outer and inner type constructor")
      }

    val lPoly = polyType(List(lParam), lDealiasedTpe)
    val lTpt = appliedTypTree1(lPoly, lParamTpe, nme)

    q"""
      type Apply0[F[_], T] = F[T]
      type Apply1[F[_[_]], T[_]] = F[T]

      new _root_.shapeless.Split1[$lTpe, $foTpe, $fiTpe] {
        type O[$nme] = $oTpt
        type I[$nme] = $iTpt

        def mkFoo: Apply1[$foTpe, O] = _root_.shapeless.lazily[Apply1[$foTpe, O]]
        def mkFii: Apply1[$fiTpe, I] = _root_.shapeless.lazily[Apply1[$fiTpe, I]]

        def pack[$nme](u: O[I[$nme]]): $lTpt = u
        def unpack[$nme](p: $lTpt): O[I[$nme]] = p
      }
    """
  }
}

object Split1Macros {
  def inst(c: Context) = new Split1Macros[c.type](c)

  def mkSplit1Impl[L[_], FO[_[_]], FI[_[_]], R](c: Context)
    (implicit
      lTag: c.WeakTypeTag[L[_]],
      foTag: c.WeakTypeTag[FO[Any]],
      fiTag: c.WeakTypeTag[FI[Any]]
    ): c.Expr[R] =
      c.Expr[R](inst(c).mkSplit1Impl[L, FO, FI])
}
