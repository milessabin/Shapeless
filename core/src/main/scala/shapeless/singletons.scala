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

import scala.language.existentials
import scala.language.experimental.macros

import scala.reflect.macros.whitebox

import tag.@@

trait Witness {
  type T
  val value: T {}
}

object Witness {
  type Aux[T0] = Witness { type T = T0 }
  type Lt[Lub] = Witness { type T <: Lub }

  implicit def apply[T]: Witness.Aux[T] = macro SingletonTypeMacros.materializeImpl[T]

  implicit def apply[T](t: T): Witness.Lt[T] = macro SingletonTypeMacros.convertImpl[T]

  implicit val witness0: Witness.Aux[_0] =
    new Witness {
      type T = _0
      val value = Nat._0
    }

  implicit def witnessN[P <: Nat]: Witness.Aux[Succ[P]] =
    new Witness {
      type T = Succ[P]
      val value = new Succ[P]()
    }
}

trait WitnessWith[TC[_]] extends Witness {
  val instance: TC[T]
  type Out
}

trait LowPriorityWitnessWith {
  implicit def apply2[H, TC2[_ <: H, _], S <: H, T](t: T): WitnessWith.Lt[({ type λ[X] = TC2[S, X] })#λ, T] =
    macro SingletonTypeMacros.convertInstanceImpl2[H, TC2, S, T]
}

object WitnessWith extends LowPriorityWitnessWith {
  type Aux[TC[_], T0] = WitnessWith[TC] { type T = T0  }
  type Lt[TC[_], Lub] = WitnessWith[TC] { type T <: Lub }

  implicit def apply1[TC[_], T](t: T): WitnessWith.Lt[TC, T] = macro SingletonTypeMacros.convertInstanceImpl1[TC, T]
}

trait SingletonTypeMacros[C <: whitebox.Context] {
  import syntax.SingletonOps
  type SingletonOpsLt[Lub] = SingletonOps { type T <: Lub }

  val c: C

  import c.universe._
  import internal._
  import decorators._
  import Flag._

  def mkWitnessT(sTpe: Type, s: Any): Tree =
    mkWitness(TypeTree(sTpe), Literal(Constant(s)))

  def mkWitness(sTpt: TypTree, s: Tree): Tree = {
    val witnessTpt = Ident(typeOf[Witness].typeSymbol)
    val T = TypeDef(Modifiers(), TypeName("T"), List(), sTpt)
    val value = ValDef(Modifiers(), TermName("value"), sTpt, s)
    mkImplClass(witnessTpt, List(T, value), List())
  }

  def materializeImpl(tpe: Type): Tree = {
    val SymTpe = typeOf[scala.Symbol]
    val TaggedSym = typeOf[tag.Tagged[_]].typeConstructor.typeSymbol

    val ScalaName = TermName("scala")
    val SymName = TermName("Symbol")
    val ApplyName = TermName("apply")

    tpe.dealias match {
      case t @ ConstantType(Constant(s)) => mkWitnessT(t, s)

      case t @ SingleType(p, v) if !v.isParameter =>
        mkWitness(TypeTree(t), TypeApply(Select(Ident(v), TermName("asInstanceOf")), List(TypeTree(t))))

      case t @ RefinedType(List(SymTpe, TypeRef(_, TaggedSym, List(ConstantType(const)))), _) =>
        val tTpt = TypeTree(t)
        val symTree = Apply(Select(Select(Ident(ScalaName), SymName), ApplyName), List(Literal(const)))
        mkWitness(tTpt, mkTagged(tTpt, symTree))

      case t =>
        c.abort(c.enclosingPosition, s"Type argument $t is not a singleton type")
    }
  }

  def convertImpl(t: c.Expr[Any]): Tree = {
    val SymTpe = typeOf[scala.Symbol]

    (t.actualType, t.tree) match {
      case (tpe @ ConstantType(const: Constant), _) =>
        mkWitness(TypeTree(tpe), Literal(const))

      case (tpe @ SingleType(p, v), tree) if !v.isParameter =>
        mkWitness(TypeTree(tpe), tree)

      case (tpe: TypeRef, Literal(const: Constant)) =>
        mkWitness(TypeTree(constantType(const)), Literal(const))

      case (SymTpe, Apply(Select(Select(Ident(scala_), symbol), apply), List(Literal(const: Constant))))
      if scala_ == TermName("scala") && symbol == TermName("Symbol") && apply == TermName("apply") =>
        val atatTpe = typeOf[@@[_,_]].typeConstructor
        val sTpt = TypeTree(appliedType(atatTpe, List(SymTpe, constantType(const))))
        val sVal = mkTagged(sTpt, t.tree)
        mkWitness(sTpt, sVal)

      case _ =>
        c.abort(c.enclosingPosition, s"Expression ${t.tree} does not evaluate to a constant or a stable value")
    }
  }

  def mkWitnessWith(singletonInstanceTpt: TypTree, sTpt: TypTree, s: Tree, i: Tree): Tree = {
    val iTpe =
      (i.tpe match {
        case NullaryMethodType(resTpe) => resTpe
        case other => other
      }).dealias

    val iOut = iTpe.member(TypeName("Out")) match {
      case NoSymbol => definitions.NothingClass
      case other => other
    }

    val niTpt = TypeTree(iTpe)

    val T = TypeDef(Modifiers(), TypeName("T"), List(), sTpt)
    val value = ValDef(Modifiers(), TermName("value"), sTpt, s)
    val instance = ValDef(Modifiers(), TermName("instance"), niTpt, i)
    val Out = TypeDef(Modifiers(), TypeName("Out"), List(), Ident(iOut))
    mkImplClass(singletonInstanceTpt, List(T, value, instance, Out), List())
  }

  def convertInstanceImpl[TC[_]](t: c.Expr[Any])
    (implicit tcTag: c.WeakTypeTag[TC[_]]): Tree = {
      val SymTpe = typeOf[scala.Symbol]

      val tc = tcTag.tpe.typeConstructor
      val siTpt =
        AppliedTypeTree(
          Select(Ident(TermName("shapeless")), TypeName("WitnessWith")),
          List(TypeTree(tc))
        )

      (t.actualType, t.tree) match {
        case (tpe @ ConstantType(const: Constant), _) =>
          val tci = appliedType(tc, List(tpe))
          val i = c.inferImplicitValue(tci, silent = false)
          mkWitnessWith(siTpt, TypeTree(tpe), Literal(const), i)

        case (tpe @ SingleType(p, v), tree) if !v.isParameter =>
          val tci = appliedType(tc, List(tpe))
          val i = c.inferImplicitValue(tci, silent = false)
          mkWitnessWith(siTpt, TypeTree(tpe), tree, i)

        case (SymTpe, Apply(Select(Select(Ident(scala_), symbol), apply), List(Literal(const: Constant))))
        if scala_ == TermName("scala") && symbol == TermName("Symbol") && apply == TermName("apply") =>
          val atatTpe = typeOf[@@[_,_]].typeConstructor
          val tci = appliedType(tc, List(appliedType(atatTpe, List(SymTpe, constantType(const)))))
          val sTpt = TypeTree(appliedType(atatTpe, List(SymTpe, constantType(const))))
          val sVal = mkTagged(sTpt, t.tree)
          val i = c.inferImplicitValue(tci, silent = false)
          mkWitnessWith(siTpt, sTpt, sVal, i)

        case (tpe: TypeRef, Literal(const: Constant)) =>
          val tci = appliedType(tc, List(constantType(const)))
          val i = c.inferImplicitValue(tci, silent = false)
          mkWitnessWith(siTpt, TypeTree(constantType(const)), Literal(const), i)

        case _ =>
          c.abort(c.enclosingPosition, s"Expression ${t.tree} does not evaluate to a constant or a stable value")
      }
  }

  def convertInstanceImpl2[H, TC2[_ <: H, _], S <: H](t: c.Expr[Any])
    (implicit tc2Tag: c.WeakTypeTag[TC2[_, _]], sTag: c.WeakTypeTag[S]): Tree = {
      val SymTpe = typeOf[scala.Symbol]

      val tc2 = tc2Tag.tpe.typeConstructor
      val s = sTag.tpe

      val pre = weakTypeOf[WitnessWith[({ type λ[X] = TC2[S, X] })#λ]]
      val pre2 = pre.map { _ match {
        case TypeRef(prefix, sym, args) if sym.isFreeType =>
          typeRef(NoPrefix, tc2.typeSymbol, args)
        case tpe => tpe
      }}
      val tc = pre2.dealias

      (t.actualType, t.tree) match {
        case (tpe @ ConstantType(const: Constant), _) =>
          val tci = appliedType(tc2, List(s, tpe))
          val i = c.inferImplicitValue(tci, silent = false)
          mkWitnessWith(TypeTree(tc), TypeTree(tpe), Literal(const), i)

        case (tpe @ SingleType(p, v), tree) if !v.isParameter =>
          val tci = appliedType(tc2, List(s, tpe))
          val i = c.inferImplicitValue(tci, silent = false)
          mkWitnessWith(TypeTree(tc), TypeTree(tpe), tree, i)

        case (SymTpe, Apply(Select(Select(Ident(scala_), symbol), apply), List(Literal(const: Constant))))
        if scala_ == TermName("scala") && symbol == TermName("Symbol") && apply == TermName("apply") =>
          val atatTpe = typeOf[@@[_,_]].typeConstructor
          val tci = appliedType(tc2, List(s, appliedType(atatTpe, List(SymTpe, constantType(const)))))
          val sTpt = TypeTree(appliedType(atatTpe, List(SymTpe, constantType(const))))
          val sVal = mkTagged(sTpt, t.tree)
          val i = c.inferImplicitValue(tci, silent = false)
          mkWitnessWith(TypeTree(tc), sTpt, sVal, i)

        case (tpe: TypeRef, Literal(const: Constant)) =>
          val tci = appliedType(tc2, List(s, constantType(const)))
          val i = c.inferImplicitValue(tci, silent = false)
          mkWitnessWith(TypeTree(tc), TypeTree(constantType(const)), Literal(const), i)

        case _ =>
          c.abort(c.enclosingPosition, s"Expression ${t.tree} does not evaluate to a constant or a stable value")
      }
  }

  def mkOps(sTpt: TypTree, w: Tree): c.Expr[SingletonOps] = {
    val opsTpt = Ident(typeOf[SingletonOps].typeSymbol)
    val T = TypeDef(Modifiers(), TypeName("T"), List(), sTpt)
    val value = ValDef(Modifiers(), TermName("witness"), TypeTree(), w)
    c.Expr[SingletonOps] {
      mkImplClass(opsTpt, List(T, value), List())
    }
  }

  def mkTagged(tpt: Tree, t: Tree): Tree =
    TypeApply(Select(t, TermName("asInstanceOf")), List(tpt))

  def mkSingletonOps(t: c.Expr[Any]): c.Expr[SingletonOps] = {
    val SymTpe = typeOf[scala.Symbol]

    (t.actualType, t.tree) match {
      case (tpe @ ConstantType(const: Constant), _) =>
        val sTpt = TypeTree(tpe)
        mkOps(sTpt, mkWitness(sTpt, Literal(const)))

      case (tpe @ SingleType(p, v), tree) if !v.isParameter =>
        val sTpt = TypeTree(tpe)
        mkOps(sTpt, mkWitness(sTpt, tree))

      case (tpe: TypeRef, Literal(const: Constant)) =>
        val sTpt = TypeTree(constantType(const))
        mkOps(sTpt, mkWitness(sTpt, Literal(const)))

      case (SymTpe, Apply(Select(Select(Ident(scala_), symbol), apply), List(Literal(const: Constant))))
      if scala_ == TermName("scala") && symbol == TermName("Symbol") && apply == TermName("apply") =>
        val atatTpe = typeOf[@@[_,_]].typeConstructor
        val sTpt = TypeTree(appliedType(atatTpe, List(SymTpe, constantType(const))))
        val sVal = mkTagged(sTpt, t.tree)
        mkOps(sTpt, mkWitness(sTpt, sVal))

      case (tpe @ TypeRef(pre, sym, args), tree) =>
        val sTpt = SingletonTypeTree(tree)
        mkOps(sTpt, mkWitness(sTpt, tree))

      case (tpe, tree) =>
        c.abort(c.enclosingPosition, s"Expression ${t.tree} does not evaluate to a constant or a stable value")
    }
  }

  def narrowSymbol[S <: String](t: c.Expr[scala.Symbol])(implicit sTag: c.WeakTypeTag[S]): c.Expr[scala.Symbol @@ S] = {
    (sTag.tpe, t.tree) match {
      case (ConstantType(Constant(s1)),
            Apply(Select(Select(Ident(scala_), symbol), apply), List(Literal(Constant(s2)))))
      if s1 == s2 && scala_ == TermName("scala") && symbol == TermName("Symbol") && apply == TermName("apply") =>
        reify { t.splice.asInstanceOf[scala.Symbol @@ S] }
      case _ =>
        c.abort(c.enclosingPosition, s"Expression ${t.tree} is not an appropriate Symbol literal")
    }
  }

  def constructor(prop: Boolean) =
    DefDef(
      Modifiers(),
      termNames.CONSTRUCTOR,
      List(),
      List(
        if(prop)
          List(
            ValDef(Modifiers(PARAM), TermName("i"), Ident(typeOf[Int].typeSymbol), EmptyTree)
          )
        else
          Nil
      ),
      TypeTree(),
      Block(
        List(
          Apply(
            Select(
              Super(This(typeNames.EMPTY), typeNames.EMPTY), termNames.CONSTRUCTOR
            ),
            if(prop)
              List(Ident(TermName("i")))
            else
              Nil
          )
        ),
        Literal(Constant(()))
      )
    )

  def mkImplClass(parent: Tree, defns: List[Tree], args: List[Tree]): Tree = {
    val name = TypeName(c.freshName())

    val classDef =
      ClassDef(
        Modifiers(FINAL),
        name,
        List(),
        Template(
          List(parent),
          noSelfType,
          constructor(args.size > 0) :: defns
        )
      )

    Block(
      List(classDef),
      Apply(Select(New(Ident(name)), termNames.CONSTRUCTOR), args)
    )
  }
}

object SingletonTypeMacros {
  import syntax.SingletonOps
  type SingletonOpsLt[Lub] = SingletonOps { type T <: Lub }

  def inst(c0: whitebox.Context) = new SingletonTypeMacros[c0.type] { val c: c0.type = c0 }

  def materializeImpl[T: c.WeakTypeTag](c: whitebox.Context): c.Expr[Witness.Aux[T]] =
    c.Expr[Witness.Aux[T]](inst(c).materializeImpl(c.weakTypeOf[T]))

  def convertImpl[T](c: whitebox.Context)(t: c.Expr[Any]): c.Expr[Witness.Lt[T]] = c.Expr(inst(c).convertImpl(t))

  def convertInstanceImpl1[TC[_], T](c: whitebox.Context)(t: c.Expr[Any])
    (implicit tcTag: c.WeakTypeTag[TC[_]]):
      c.Expr[WitnessWith.Lt[TC, T]] = c.Expr[WitnessWith.Lt[TC, T]](inst(c).convertInstanceImpl[TC](t))

  def convertInstanceImpl2[H, TC2[_ <: H, _], S <: H, T](c: whitebox.Context)(t: c.Expr[Any])
    (implicit tcTag: c.WeakTypeTag[TC2[_, _]], sTag: c.WeakTypeTag[S]):
      c.Expr[WitnessWith.Lt[({ type λ[X] = TC2[S, X] })#λ, T]] =
        c.Expr[WitnessWith.Lt[({ type λ[X] = TC2[S, X] })#λ, T]](inst(c).convertInstanceImpl2[H, TC2, S](t))

  def mkSingletonOps(c: whitebox.Context)(t: c.Expr[Any]): c.Expr[SingletonOps] = inst(c).mkSingletonOps(t)

  def narrowSymbol[S <: String](c: whitebox.Context)(t: c.Expr[scala.Symbol])
    (implicit sTag: c.WeakTypeTag[S]): c.Expr[scala.Symbol @@ S] = inst(c).narrowSymbol[S](t)
}
