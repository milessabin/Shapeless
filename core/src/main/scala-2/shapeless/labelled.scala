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

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

trait LabellingScalaCompat {

  implicit def mkLabelling[T]: Labelling[T] =
    macro LabelledMacros.mkLabelling[T]
}

class LabelledMacros(override val c: whitebox.Context) extends GenericMacros(c) with SingletonTypeUtils {
  import c.universe._
  import internal.constantType

  private def commaSeparated(str: String): List[String] = {
    val builder = List.newBuilder[String]
    var i, j, k = 0
    while (j < str.length) {
      str.charAt(j) match {
        case ',' if k == 0 =>
          builder += str.substring(i, j).trim
          i = j + 1
        case '(' | '[' =>
          k += 1
        case ')' | ']' =>
          k = k - 1 max 0
        case _ =>
      }

      j += 1
    }

    val last = str.substring(i, j).trim
    if (last.nonEmpty) builder += last
    builder.result()
  }

  private def parseTypeOrFail(tpe: String): Type =
    parseType(tpe).getOrElse(abort(s"Malformed literal or standard type $tpe"))

  private def parseLiteralTypeOrFail(tpe: String): Type =
    parseLiteralType(tpe).getOrElse(abort(s"Malformed literal type $tpe"))

  private def labelsOf(tpe: Type): List[Constant] =
    if (isProduct(tpe)) fieldsOf(tpe).map { case (f, _) => nameAsValue(f) }
    else if (isCoproduct(tpe)) ctorsOf(tpe).map(c => nameAsValue(nameOf(c)))
    else abort(s"$tpe is not case class like or the root of a sealed family of types")

  def mkLabelledGeneric[T: WeakTypeTag, R]: Tree = {
    val tpe = weakTypeOf[T]
    val keys = labelsOf(tpe).map(constantType)
    val generic @ q"$_.instance[$_, ${repr: Tree}]($_, $_)" = (mkGeneric[T]: @unchecked)
    val isProduct = repr.tpe <:< hlistTpe
    val values = if (isProduct) unpackHList(repr.tpe) else unpackCoproduct(repr.tpe)
    val items = keys.zip(values).map((FieldType.apply _).tupled)
    val labelled = if (isProduct) mkHListTpe(items) else mkCoproductTpe(items)
    q"${reify(LabelledGeneric)}.unsafeInstance[$tpe, $labelled]($generic)"
  }

  def mkLabelling[T: WeakTypeTag]: Tree = {
    val tpe = weakTypeOf[T]
    val labels = labelsOf(tpe)
    val labelsType = mkHListTpe(labels.map(constantType))
    val labelsValue = mkHListValue(labels.map(Literal.apply))
    q"${reify(Labelling)}.instance[$tpe, $labelsType]($labelsValue.asInstanceOf[$labelsType])"
  }

  def recordType(tpeSelector: Tree): Tree =
    labelledType(tpeSelector, "record", hnilTpe, hconsTpe)

  def unionType(tpeSelector: Tree): Tree =
    labelledType(tpeSelector, "union", cnilTpe, cconsTpe)

  def labelledType(tpeSelector: Tree, variety: String, nil: Type, cons: Type): Tree = {
    val q"${tpeString: String}" = (tpeSelector: @unchecked)
    val labelledTpe = commaSeparated(tpeString).foldRight(nil) { (element, acc) =>
      element.split("->") match {
        case Array(keyString, valueString) =>
          val key = parseLiteralTypeOrFail(keyString.trim)
          val value = parseTypeOrFail(valueString.trim)
          appliedType(cons, FieldType(key, value), acc)
        case _ =>
          abort(s"Malformed $variety type $tpeString")
      }
    }

    typeCarrier(labelledTpe)
  }

  def hlistType(tpeSelector: Tree): Tree =
    nonLabelledType(tpeSelector, hnilTpe, hconsTpe)

  def coproductType(tpeSelector: Tree): Tree =
    nonLabelledType(tpeSelector, cnilTpe, cconsTpe)

  def nonLabelledType(tpeSelector: Tree, nil: Type, cons: Type): Tree = {
    val q"${tpeString: String}" = (tpeSelector: @unchecked)
    val tpe = commaSeparated(tpeString).foldRight(nil) { (element, acc) =>
      appliedType(cons, parseTypeOrFail(element), acc)
    }

    typeCarrier(tpe)
  }
}
