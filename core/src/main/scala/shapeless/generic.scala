/*
 * Copyright (c) 2012-15 Lars Hupel, Miles Sabin
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

import scala.language.experimental.macros

import scala.annotation.{ StaticAnnotation, tailrec }
import scala.reflect.macros.Context

import ops.{ hlist, coproduct }

trait Generic[T] extends Serializable {
  type Repr
  def to(t : T) : Repr
  def from(r : Repr) : T
}

object Generic {
  type Aux[T, Repr0] = Generic[T] { type Repr = Repr0 }

  def apply[T](implicit gen: Generic[T]): Aux[T, gen.Repr] = gen

  implicit def materialize[T, R]: Aux[T, R] = macro GenericMacros.materialize[T, R]
}

trait LabelledGeneric[T] extends Generic[T]

object LabelledGeneric {
  type Aux[T, Repr0] = LabelledGeneric[T]{ type Repr = Repr0 }

  def apply[T](implicit lgen: LabelledGeneric[T]): Aux[T, lgen.Repr] = lgen

  implicit def materializeProduct[T, K <: HList, V <: HList, R <: HList]
    (implicit
      lab: DefaultSymbolicLabelling.Aux[T, K],
      gen: Generic.Aux[T, V],
      zip: hlist.ZipWithKeys.Aux[K, V, R],
      ev: R <:< V
    ): Aux[T, R] =
    new LabelledGeneric[T] {
      type Repr = R
      def to(t: T): Repr = zip(gen.to(t))
      def from(r: Repr): T = gen.from(r)
    }

  implicit def materializeCoproduct[T, K <: HList, V <: Coproduct, R <: Coproduct]
    (implicit
      lab: DefaultSymbolicLabelling.Aux[T, K],
      gen: Generic.Aux[T, V],
      zip: coproduct.ZipWithKeys.Aux[K, V, R],
      ev: R <:< V
    ): Aux[T, R] =
    new LabelledGeneric[T] {
      type Repr = R
      def to(t: T): Repr = zip(gen.to(t))
      def from(r: Repr): T = gen.from(r)
    }
}

class nonGeneric extends StaticAnnotation

trait IsTuple[T] extends Serializable

object IsTuple {
  implicit def apply[T]: IsTuple[T] = macro GenericMacros.mkIsTuple[T]
}

trait HasProductGeneric[T] extends Serializable

object HasProductGeneric {
  implicit def apply[T]: HasProductGeneric[T] = macro GenericMacros.mkHasProductGeneric[T]
}

trait HasCoproductGeneric[T] extends Serializable

object HasCoproductGeneric {
  implicit def apply[T]: HasCoproductGeneric[T] = macro GenericMacros.mkHasCoproductGeneric[T]
}

trait ReprTypes {
  val c: Context
  import c.universe._

  def hlistTpe = typeOf[HList]
  def hnilTpe = typeOf[HNil]
  def hconsTpe = typeOf[::[_, _]].typeConstructor
  def coproductTpe = typeOf[Coproduct]
  def cnilTpe = typeOf[CNil]
  def cconsTpe = typeOf[:+:[_, _]].typeConstructor

  def atatTpe = typeOf[tag.@@[_,_]].typeConstructor
  def fieldTypeTpe = typeOf[shapeless.labelled.FieldType[_, _]].typeConstructor
}

trait CaseClassMacros extends ReprTypes {
  import c.universe._
  import Flag._

  def abort(msg: String) =
    c.abort(c.enclosingPosition, msg)

  def isReprType(tpe: Type): Boolean =
    tpe <:< hlistTpe || tpe <:< coproductTpe

  def isReprType1(tpe: Type): Boolean = {
    val normalized = appliedType(tpe, List(WildcardType)).normalize
    normalized <:< hlistTpe || normalized <:< coproductTpe
  }

  def isProduct(tpe: Type): Boolean =
    tpe =:= typeOf[Unit] || (tpe.typeSymbol.isClass && isCaseClassLike(classSym(tpe)))

  def isCoproduct(tpe: Type): Boolean = {
    val sym = tpe.typeSymbol
    if(!sym.isClass) false
    else {
      val sym = classSym(tpe)
      (sym.isTrait || sym.isAbstractClass) && sym.isSealed
    }
  }

  def ownerChain(sym: Symbol): List[Symbol] = {
    @tailrec
    def loop(sym: Symbol, acc: List[Symbol]): List[Symbol] =
      if(sym.owner == NoSymbol) acc
      else loop(sym.owner, sym :: acc)

    loop(sym, Nil)
  }

  def mkDependentRef(prefix: Type, path: List[Name]): (Type, Symbol) = {
    val (_, pre, sym) =
      path.foldLeft((prefix, NoType, NoSymbol)) {
        case ((pre, _, sym), nme) =>
          val sym0 = pre.member(nme)
          val pre0 = sym0.typeSignature
          (pre0, pre, sym0)
      }
    (pre, sym)
  }

  def fieldsOf(tpe: Type): List[(TermName, Type)] =
    tpe.declarations.toList collect {
      case sym: TermSymbol if isCaseAccessorLike(sym) =>
        val NullaryMethodType(restpe) = sym.typeSignatureIn(tpe)
        (sym.name.toTermName, restpe)
    }

  def productCtorsOf(tpe: Type): List[Symbol] =
    tpe.declarations.toList.filter { x => x.isMethod && x.asMethod.isConstructor }

  def ctorsOf(tpe: Type): List[Type] = distinctCtorsOfAux(tpe, false)
  def ctorsOf1(tpe: Type): List[Type] = distinctCtorsOfAux(tpe, true)

  def distinctCtorsOfAux(tpe: Type, hk: Boolean): List[Type] = {
    def distinct[A](list: List[A])(eq: (A, A) => Boolean): List[A] = list.foldLeft(List.empty[A]) { (acc, x) =>
      if (!acc.exists(eq(x, _))) x :: acc
      else acc
    }.reverse
    distinct(ctorsOfAux(tpe, hk))(_ =:= _)
  }

  def ctorsOfAux(tpe: Type, hk: Boolean): List[Type] = {
    def collectCtors(classSym: ClassSymbol): List[ClassSymbol] = {
      classSym.knownDirectSubclasses.toList flatMap { child0 =>
        val child = child0.asClass
        child.typeSignature // Workaround for <https://issues.scala-lang.org/browse/SI-7755>
        if (isCaseClassLike(child))
          List(child)
        else if (child.isSealed)
          collectCtors(child)
        else
          abort(s"$child is not case class like or a sealed trait")
      }
    }

    if(isProduct(tpe))
      List(tpe)
    else if(isCoproduct(tpe)) {
      def typeArgs(tpe: Type) = tpe match {
        case TypeRef(_, _, args) => args
        case _ => Nil
      }

      val basePre = prefix(tpe)
      val baseSym = classSym(tpe)
      val baseArgs: List[Type] =
        if(hk) {
          val tc = tpe.typeConstructor
          val TypeRef(_, sym, _) = tc
          val paramSym = sym.asType.typeParams.head
          val paramTpe = paramSym.asType.toType
          val appTpe = appliedType(tc, List(paramTpe))
          typeArgs(appTpe.normalize)
        }
        else typeArgs(tpe.normalize)

      val ctors = collectCtors(baseSym).sortBy(_.fullName)
      if (ctors.isEmpty)
        abort(s"Sealed trait $tpe has no case class subtypes")

      ctors map { sym =>
        def substituteArgs: List[Type] = {
          val subst = typeArgs(ThisType(sym).baseType(baseSym))
          sym.typeParams.map { param =>
            val paramTpe = param.asType.toType
            baseArgs(subst.indexWhere(_ =:= paramTpe))
          }
        }

        val suffix = ownerChain(sym).dropWhile(_ != basePre.typeSymbol)
        if(suffix.isEmpty) {
          if(sym.isModuleClass) {
            val moduleSym = sym.asClass.module
            val modulePre = prefix(moduleSym.typeSignature)
            singleType(modulePre, moduleSym)
          } else appliedType(sym.toTypeIn(c.prefix.tree.tpe), substituteArgs)
        } else {
          if(sym.isModuleClass) {
            val path = suffix.tail.map(_.name.toTermName)
            val (modulePre, moduleSym) = mkDependentRef(basePre, path)
            singleType(modulePre, moduleSym)
          } else {
            val path = suffix.tail.init.map(_.name.toTermName) :+ suffix.last.name.toTypeName
            val (subTpePre, subTpeSym) = mkDependentRef(basePre, path)
            typeRef(subTpePre, subTpeSym, substituteArgs)
          }
        }
      }
    }
    else
      abort(s"$tpe is not a case class, case class-like, a sealed trait or Unit")
  }

  def nameAsString(name: Name): String = name.decodedName.toString.trim

  def nameAsValue(name: Name): Constant = Constant(nameAsString(name))

  def nameOf(tpe: Type) = tpe.typeSymbol.name

  def mkCompoundTpe(nil: Type, cons: Type, items: List[Type]): Type =
    items.foldRight(nil) {
      case (tpe, acc) => appliedType(cons, List(devarargify(tpe), acc))
    }

  def mkLabelTpe(name: Name): Type =
    appliedType(atatTpe, List(typeOf[scala.Symbol], ConstantType(nameAsValue(name))))

  def mkFieldTpe(name: Name, valueTpe: Type): Type = {
    appliedType(fieldTypeTpe, List(mkLabelTpe(name), valueTpe))
  }

  def mkHListTpe(items: List[Type]): Type =
    mkCompoundTpe(hnilTpe, hconsTpe, items)

  def mkCoproductTpe(items: List[Type]): Type =
    mkCompoundTpe(cnilTpe, cconsTpe, items)

  def appliedTypTree1(tpe: Type, param: Type, arg: TypeName): Tree = {
    tpe match {
      case t if t =:= param =>
        Ident(arg)
      case PolyType(params, body) if params.head.asType.toType =:= param =>
        appliedTypTree1(body, param, arg)
      case t @ TypeRef(pre, sym, List()) if t.takesTypeArgs =>
        val argTrees = sym.asType.typeParams.map(sym => appliedTypTree1(sym.asType.toType, param, arg))
        AppliedTypeTree(mkAttributedRef(pre, sym), argTrees)
      case TypeRef(pre, sym, List()) =>
        mkAttributedRef(pre, sym)
      case TypeRef(pre, sym, args) =>
        val argTrees = args.map(appliedTypTree1(_, param, arg))
        AppliedTypeTree(mkAttributedRef(pre, sym), argTrees)
      case t if t.takesTypeArgs =>
        val argTrees = t.typeSymbol.asType.typeParams.map(sym => appliedTypTree1(sym.asType.toType, param, arg))
        AppliedTypeTree(mkAttributedRef(tpe.typeConstructor), argTrees)
      case t =>
        mkAttributedRef(t)
    }
  }

  def mkCompoundTypTree1(nil: Type, cons: Type, items: List[Type], param: Type, arg: TypeName): Tree =
    items.foldRight(mkAttributedRef(nil): Tree) { case (tpe, acc) =>
      AppliedTypeTree(mkAttributedRef(cons), List(appliedTypTree1(tpe, param, arg), acc))
    }

  def mkHListTypTree1(items: List[Type], param: Type, arg: TypeName): Tree =
    mkCompoundTypTree1(hnilTpe, hconsTpe, items, param, arg)

  def mkCoproductTypTree1(items: List[Type], param: Type, arg: TypeName): Tree =
    mkCompoundTypTree1(cnilTpe, cconsTpe, items, param, arg)

  def unfoldCompoundTpe(compoundTpe: Type, nil: Type, cons: Type): List[Type] = {
    @tailrec
    def loop(tpe: Type, acc: List[Type]): List[Type] =
      tpe.normalize match {
        case TypeRef(_, consSym, List(hd, tl))
          if consSym.asType.toType.typeConstructor =:= cons => loop(tl, hd :: acc)
        case `nil` => acc
        case other => abort(s"Bad compound type $compoundTpe")
      }
    loop(compoundTpe, Nil).reverse
  }

  def hlistElements(tpe: Type): List[Type] =
    unfoldCompoundTpe(tpe, hnilTpe, hconsTpe)

  def coproductElements(tpe: Type): List[Type] =
    unfoldCompoundTpe(tpe, cnilTpe, cconsTpe)

  def reprTpe(tpe: Type): Type = {
    if(isProduct(tpe)) mkHListTpe(fieldsOf(tpe).map(_._2))
    else mkCoproductTpe(ctorsOf(tpe))
  }

  def param1(tpe: Type): Type =
    tpe match {
      case t @ TypeRef(_, sym, args) if(t.takesTypeArgs) => sym.asType.typeParams.head.asType.toType
      case TypeRef(_, _, List(arg)) => arg
      case _ => NoType
    }

  def reprTypTree1(tpe: Type, arg: TypeName): Tree = {
    val param = param1(tpe)
    if(isProduct(tpe)) mkHListTypTree1(fieldsOf(tpe).map(_._2), param, arg)
    else mkCoproductTypTree1(ctorsOf1(tpe), param, arg)
  }

  def isCaseClassLike(sym: ClassSymbol): Boolean = {
    def checkCtor: Boolean = {
      def unique[T](s: Seq[T]): Option[T] =
        s.headOption.find(_ => s.tail.isEmpty)

      val tpe = sym.typeSignature
      (for {
        ctor <- unique(productCtorsOf(tpe))
        params <- unique(ctor.asMethod.paramss)
      } yield params.size == fieldsOf(tpe).size).getOrElse(false)
    }

    sym.isCaseClass ||
    (!sym.isAbstractClass && !sym.isTrait &&
     sym.knownDirectSubclasses.isEmpty && checkCtor)
  }

  def isCaseObjectLike(sym: ClassSymbol): Boolean = sym.isModuleClass && isCaseClassLike(sym)

  def isCaseAccessorLike(sym: TermSymbol): Boolean =
    !isNonGeneric(sym) && sym.isPublic && (if(sym.owner.asClass.isCaseClass) sym.isCaseAccessor else sym.isAccessor)

  def isSealedHierarchyClassSymbol(symbol: ClassSymbol): Boolean = {
    def helper(classSym: ClassSymbol): Boolean = {
      classSym.knownDirectSubclasses.toList forall { child0 =>
        val child = child0.asClass
        child.typeSignature // Workaround for <https://issues.scala-lang.org/browse/SI-7755>

        isCaseClassLike(child) || (child.isSealed && helper(child))
      }
    }

    symbol.isSealed && helper(symbol)
  }

  def classSym(tpe: Type): ClassSymbol = {
    val sym = tpe.typeSymbol
    if (!sym.isClass)
      abort(s"$sym is not a class or trait")

    val classSym = sym.asClass
    classSym.typeSignature // Workaround for <https://issues.scala-lang.org/browse/SI-7755>

    classSym
  }

  def mkAttributedRef(pre: Type, sym: Symbol): Tree = {
    val global = c.universe.asInstanceOf[scala.tools.nsc.Global]
    val gPre = pre.asInstanceOf[global.Type]
    val gSym = sym.asInstanceOf[global.Symbol]
    global.gen.mkAttributedRef(gPre, gSym).asInstanceOf[Tree]
  }

  // See https://github.com/milessabin/shapeless/issues/212
  def companionRef(tpe: Type): Tree = {
    val global = c.universe.asInstanceOf[scala.tools.nsc.Global]
    val gTpe = tpe.asInstanceOf[global.Type]
    val pre = gTpe.prefix
    val sym = gTpe.typeSymbol
    val cSym = sym.companionSymbol
    if(cSym != NoSymbol)
      global.gen.mkAttributedRef(pre, cSym).asInstanceOf[Tree]
    else
      Ident(tpe.typeSymbol.name.toTermName) // Attempt to refer to local companion
  }

  def prefix(tpe: Type): Type = {
    val global = c.universe.asInstanceOf[scala.tools.nsc.Global]
    val gTpe = tpe.asInstanceOf[global.Type]
    gTpe.prefix.asInstanceOf[Type]
  }

  def mkAttributedRef(tpe: Type): Tree = {
    val global = c.universe.asInstanceOf[scala.tools.nsc.Global]
    val gTpe = tpe.asInstanceOf[global.Type]
    val pre = gTpe.prefix
    val sym = gTpe.typeSymbol
    global.gen.mkAttributedRef(pre, sym).asInstanceOf[Tree]
  }

  def isNonGeneric(sym: Symbol): Boolean = {
    def check(sym: Symbol): Boolean = {
      // See https://issues.scala-lang.org/browse/SI-7424
      sym.typeSignature                   // force loading method's signature
      sym.annotations.foreach(_.tpe) // force loading all the annotations

      sym.annotations.exists(_.tpe =:= typeOf[nonGeneric])
    }

    // See https://issues.scala-lang.org/browse/SI-7561
    check(sym) ||
    (sym.isTerm && sym.asTerm.isAccessor && check(sym.asTerm.accessed)) ||
    sym.allOverriddenSymbols.exists(isNonGeneric)
  }

  def isTuple(tpe: Type): Boolean =
    tpe <:< typeOf[Unit] || definitions.TupleClass.seq.contains(tpe.typeSymbol)

  def isVararg(tpe: Type): Boolean =
    tpe.typeSymbol == c.universe.definitions.RepeatedParamClass

  def devarargify(tpe: Type): Type =
    tpe match {
      case TypeRef(pre, _, args) if isVararg(tpe) =>
        appliedType(typeOf[scala.collection.Seq[_]].typeConstructor, args)
      case _ => tpe
    }
}

class GenericMacros[C <: Context](val c: C) extends CaseClassMacros {
  import c.universe._
  import Flag._

  def materialize[T: WeakTypeTag, R: WeakTypeTag]: Tree = {
    val tpe = weakTypeOf[T]
    if(isReprType(tpe))
      abort("No Generic instance available for HList or Coproduct")

    def mkCoproductCases(tpe: Type, index: Int): (CaseDef, CaseDef) = {
      val name = newTermName(c.fresh("pat"))

      def mkCoproductValue(tree: Tree): Tree =
        (0 until index).foldLeft(q"_root_.shapeless.Inl($tree)": Tree) {
          case (acc, _) => q"_root_.shapeless.Inr($acc)"
        }

      if(isCaseObjectLike(tpe.typeSymbol.asClass)) {
        val singleton =
          tpe match {
            case SingleType(pre, sym) =>
              mkAttributedRef(pre, sym)
            case TypeRef(pre, sym, List()) if sym.isModule =>
              mkAttributedRef(pre, sym.asModule)
            case TypeRef(pre, sym, List()) if sym.isModuleClass =>
              mkAttributedRef(pre, sym.asClass.module)
            case other =>
              abort(s"Bad case object-like type $tpe")
          }

        val body = mkCoproductValue(q"$name.asInstanceOf[$tpe]")
        val pat = mkCoproductValue(pq"$name")
        (
          cq"$name if $name eq $singleton => $body",
          cq"$pat => $singleton: $tpe"
        )
      } else {
        val body = mkCoproductValue(q"$name: $tpe")
        val pat = mkCoproductValue(pq"$name")
        (
          cq"$name: $tpe => $body",
          cq"$pat => $name"
        )
      }
    }

    def mkProductCases(tpe: Type): (CaseDef, CaseDef) = {
      if(tpe =:= typeOf[Unit])
        (
          cq"() => _root_.shapeless.HNil",
          cq"_root_.shapeless.HNil => ()"
        )
      else if(isCaseObjectLike(tpe.typeSymbol.asClass)) {
        val singleton =
          tpe match {
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
          cq"_: $tpe => _root_.shapeless.HNil",
          cq"_root_.shapeless.HNil => $singleton: $tpe"
        )
      } else {
        val sym = tpe.typeSymbol
        val isCaseClass = sym.asClass.isCaseClass
        def hasNonGenericCompanionMember(name: String): Boolean = {
          val mSym = sym.companionSymbol.typeSignature.member(newTermName(name))
          mSym != NoSymbol && !isNonGeneric(mSym)
        }

        val binders = fieldsOf(tpe).map { case (name, tpe) => (newTermName(c.fresh("pat")), name, tpe, isVararg(tpe)) }

        val to =
          if(isCaseClass || hasNonGenericCompanionMember("unapply")) {
            val wcard = Star(Ident(nme.WILDCARD))  // like pq"_*" except that it does work
            val lhs = pq"${companionRef(tpe)}(..${binders.map(x => if (x._4) pq"${x._1} @ $wcard" else pq"${x._1}")})"
            val rhs =
              binders.foldRight(q"_root_.shapeless.HNil": Tree) {
                case ((bound, name, tpe, _), acc) => q"_root_.shapeless.::($bound, $acc)"
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
              case ((bound, _, _, _), acc) => pq"_root_.shapeless.::($bound, $acc)"
            }

          val rhs = {
            val ctorArgs = binders.map { case (bound, name, tpe, vararg) => if (vararg) q"$bound: _*" else Ident(bound) }
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

    val (toCases, fromCases) =
      if(isProduct(tpe)) {
        val (to, from) = mkProductCases(tpe)
        (List(to), List(from))
      } else {
        val (to, from) = (ctorsOf(tpe) zip (Stream from 0) map (mkCoproductCases _).tupled).unzip
        (to, from :+ cq"_ => _root_.scala.Predef.???")
      }

    val clsName = newTypeName(c.fresh())
    q"""
      final class $clsName extends _root_.shapeless.Generic[$tpe] {
        type Repr = ${reprTpe(tpe)}
        def to(p: $tpe): Repr = p match { case ..$toCases }
        def from(p: Repr): $tpe = p match { case ..$fromCases }
      }
      new $clsName()
    """
  }

  def mkIsTuple[T: WeakTypeTag]: Tree = {
    val tTpe = weakTypeOf[T]
    if(!isTuple(tTpe))
      abort(s"Unable to materialize IsTuple for non-tuple type $tTpe")

    q"""new IsTuple[$tTpe] {}"""
  }

  def mkHasProductGeneric[T: WeakTypeTag]: Tree = {
    val tTpe = weakTypeOf[T]
    if(isReprType(tTpe) || !isProduct(tTpe))
      abort(s"Unable to materialize HasProductGeneric for $tTpe")

    q"""new HasProductGeneric[$tTpe] {}"""
  }

  def mkHasCoproductGeneric[T: WeakTypeTag]: Tree = {
    val tTpe = weakTypeOf[T]
    if(isReprType(tTpe) || !isCoproduct(tTpe))
      abort(s"Unable to materialize HasCoproductGeneric for $tTpe")

    q"""new HasCoproductGeneric[$tTpe] {}"""
  }
}

object GenericMacros {
  def inst(c: Context) = new GenericMacros[c.type](c)

  def materialize[T: c.WeakTypeTag, R: c.WeakTypeTag](c: Context): c.Expr[Generic.Aux[T, R]] =
    c.Expr[Generic.Aux[T, R]](inst(c).materialize[T, R])

  def mkIsTuple[T: c.WeakTypeTag](c: Context): c.Expr[IsTuple[T]] =
    c.Expr[IsTuple[T]](inst(c).mkIsTuple[T])

  def mkHasProductGeneric[T: c.WeakTypeTag](c: Context): c.Expr[HasProductGeneric[T]] = 
    c.Expr[HasProductGeneric[T]](inst(c).mkHasProductGeneric[T])

  def mkHasCoproductGeneric[T: c.WeakTypeTag](c: Context): c.Expr[HasCoproductGeneric[T]] =
    c.Expr[HasCoproductGeneric[T]](inst(c).mkHasCoproductGeneric[T])
}
