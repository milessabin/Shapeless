/*
 * Copyright (c) 2011-16 Miles Sabin
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

import scala.language.dynamics
import scala.language.experimental.macros

import scala.reflect.macros.whitebox

/**
 * Record operations on `HList`'s with field-like elements.
 *
 * @author Miles Sabin
 */
object record {
  import syntax.RecordOps

  implicit def recordOps[L <: HList](l : L): RecordOps[L] = new RecordOps(l)

  /**
   * Records encoded as `HLists` of their value types intersected with the
   * singleton types of their keys.
   *
   * Record types may be written using a relatively concise syntax thanks to a trick
   * due to Denys Shabalin (@den_sh) and Eugene Burmako (@xeno_by). We use a
   * combination of `selectDynamic` and backticks to embed a type in a path which
   * appears to the compiler as stable,
   *
   * {{{
   * type Xyz = Record.`"x" -> Int, "y" -> String, "z" -> Boolean`.T
   * }}}
   *
   * The use of singleton-typed `Symbols` as keys would make this type extremely
   * laborious to write out by hand.
   *
   * There is also a mechanism for creating values of record types using Scala's
   * named argument syntax. Values of the type just defined can be created as follows,
   *
   * {{{
   * val xyz = Record(x = 23, y = "foo", z = true)
   * xyz("y") // == "foo"
   * }}}
   */
  object Record extends Dynamic {
    def applyDynamic(method: String)(rec: Any*): HList = macro RecordMacros.mkRecordEmptyImpl
    def applyDynamicNamed(method: String)(rec: Any*): HList = macro RecordMacros.mkRecordNamedImpl
    def selectDynamic(tpeSelector: String): Any = macro LabelledMacros.recordType
  }
}

/**
 * Trait supporting mapping named argument lists to record arguments.
 *
 * Mixing in this trait enables method applications of the form,
 *
 * {{{
 * lhs.method(x = 23, y = "foo", z = true)
 * }}}
 *
 * to be rewritten as,
 *
 * {{{
 * lhs.methodRecord("x" ->> 23 :: "y" ->> "foo", "z" ->> true)
 * }}}
 *
 * ie. the named arguments are rewritten as record fields with the argument name
 * encoded as a singleton-typed `Symbol` and the application is rewritten to an
 * application of an implementing method (identified by the "Record" suffix) which
 * accepts a single record argument.
 */
trait RecordArgs extends Dynamic {
  def applyDynamic(method: String)(): Any = macro RecordMacros.forwardImpl
  def applyDynamicNamed(method: String)(rec: Any*): Any = macro RecordMacros.forwardNamedImpl
}

/**
 * Trait supporting mapping record arguments to named argument lists, inverse of RecordArgs.
 *
 * Mixing in this trait enables method applications of the form,
 *
 * {{{
 * lhs.methodRecord("x" ->> 23 :: "y" ->> "foo" :: "z" ->> true :: HNil)
 * }}}
 *
 * to be rewritten as,
 *
 * {{{
 * lhs.method(x = 23, y = "foo", z = true)
 * }}}
 *
 * ie. the record argument is used to look up arguments for a target method
 * (the called method named minus the "Record" suffix) by name and type and the application
 * is rewritten to an application of the target method
 */
trait FromRecordArgs extends Dynamic {
  def applyDynamic[L <: HList](method: String)(rec: L): Any = macro RecordMacros.forwardFromRecordImpl[L]
}

class RecordMacros(val c: whitebox.Context) extends CaseClassMacros {
  import c.universe._
  import internal.constantType

  val hconsValueTree: Tree = reify(::).tree
  val hnilValueTree: Tree = reify(HNil: HNil).tree

  def mkRecordEmptyImpl(method: Tree)(rec: Tree*): Tree = {
    if (rec.nonEmpty) abort("this method must be called with named arguments")
    hnilValueTree
  }

  def mkRecordNamedImpl(method: Tree)(rec: Tree*): Tree = {
    val q"${methodString: String}" = (method: @unchecked)
    if (methodString != "apply") abort(s"this method must be called as 'apply' not '$methodString'")
    mkRecordImpl(rec: _*)
  }

  def forwardImpl(method: Tree)(): Tree =
    forwardNamedImpl(method)()

  def forwardNamedImpl(method: Tree)(rec: Tree*): Tree = {
    val lhs = c.prefix.tree 
    val lhsTpe = lhs.tpe
    val q"${methodString: String}" = (method: @unchecked)
    val methodName = TermName(methodString + "Record")
    if (lhsTpe.member(methodName) == NoSymbol)
      abort(s"missing method '$methodName'")

    val recTree = mkRecordImpl(rec: _*)
    q"$lhs.$methodName($recTree)"
  }

  def forwardFromRecordImpl[L <: HList](method: Tree)(rec: Expr[L]): Tree = {
    val lhs = c.prefix.tree
    val lhsTpe = lhs.tpe
    val q"${methodString: String}" = (method: @unchecked)
    if (!methodString.matches(".*Record$"))
      abort(s"missing method '$methodString'")

    val methodName = TermName(methodString.replaceAll("Record$", ""))
    if (!lhsTpe.member(methodName).isMethod)
      abort(s"missing method '$methodName'")

    val params = mkParamsImpl(lhsTpe.member(methodName).asMethod, rec)
    q"$lhs.$methodName(...$params)"
  }

  def mkRecordImpl(rec: Tree*): Tree = {
    def mkElem(key: Type, value: Tree): Tree =
      q"$value.asInstanceOf[${FieldType(key, value.tpe.widen)}]"

    def promoteElem(elem: Tree): Tree = elem match {
      case q"$_(${Literal(k)}, $v)" => mkElem(constantType(k), v)
      case _ => abort(s"$elem has the wrong shape for a record field")
    }

    rec.foldRight(hnilValueTree) { (elem, acc) =>
      q"$hconsValueTree(${promoteElem(elem)}, $acc)"
    }
  }

  def mkParamsImpl[L <: HList](method: MethodSymbol, rec: Expr[L]): List[List[Tree]] = {
    val selector = reify(ops.hlist.Selector)
    def mkElem(key: Type, value: Tree): Tree =
      q"$selector[${rec.actualType}, ${FieldType(key, value.tpe.widen)}].apply($rec)"

    method.paramLists.filterNot(_.forall(_.isImplicit)).map(_.map { p =>
      mkElem(constantType(nameAsValue(p.name)), q"${p.typeSignature}")
    })
  }
}
