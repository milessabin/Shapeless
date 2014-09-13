/*
 * Copyright (c) 2011-14 Miles Sabin
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

 object numbers {

  /**
   * Type class witnessing that `B` is the predecessor of `A`.
   * 
   * @author Miles Sabin
   */
   trait Predecessor[A <: RInt] { type Out <: RInt }

   object Predecessor {
    def apply[A <: RInt](implicit pred: Predecessor[A]): Aux[A, pred.Out] = pred

    type Aux[A <: RInt, B <: RInt] = Predecessor[A] { type Out = B }

    implicit val predZero = new Predecessor[_0] {type Out = Minus[Succ[_0]]} 
    implicit def predNat[B <: Nat]: Aux[Succ[B], B] = new Predecessor[Succ[B]] { type Out = B }
    implicit def predNeg[B <: SPos]: Aux[Minus[B], Minus[Succ[B]]] = new Predecessor[Minus[B]] { type Out = Minus[Succ[B]] }
  }


  /**
   * Type class witnessing that `B` is the successor of `A`.
   * 
   * @author Olivier Mélois
   */
   trait Successor[A <: RInt] { type Out <: RInt }

   object Successor {
    def apply[A <: RInt](implicit succ: Successor[A]): Aux[A, succ.Out] = succ

    type Aux[A <: RInt, B <: RInt] = Successor[A] { type Out = B }

    implicit val succMinusOne : Aux[Minus[Succ[_0]], _0] = new Successor[Minus[Succ[_0]]] { type Out = _0}
    implicit def succNat[B <: Nat]: Aux[B, Succ[B]] = new Successor[B] { type Out = Succ[B]}
    implicit def succNeg[B <: SPos]: Aux[Minus[Succ[B]], Minus[B]] = new Successor[Minus[Succ[B]]] { type Out = Minus[B]}
  }

  /**
   * Type class supporting conversion of type-level RInts to value level Ints.
   * 
   * @author Miles Sabin
   */
   trait ToInt[N <: RInt] {
    def apply() : Int
  }

  object ToInt {
    def apply[N <: RInt](implicit toInt: ToInt[N]): ToInt[N] = toInt

    implicit val toInt0 = new ToInt[_0] {
      def apply() = 0 
    }
    implicit def toIntSucc[N <: Nat](implicit toIntN : ToInt[N]) = new ToInt[Succ[N]] {
      def apply() = toIntN()+1
    }

    implicit def toIntMinus[N <: SPos](implicit toIntN : ToInt[N]) = new ToInt[Minus[N]] {
      def apply() = -toIntN()
    }
  }


	/**
   	 * Type class witnessing that B and A are opposite integers.
   	 * 
   	 * @author Olivier Mélois
   	 */
   	 trait Opposite[A <: Number] {type Out <: RInt}

   	 object Opposite{
   	 	def apply[A <: Number](implicit opposite: Opposite[A]): Aux[A, opposite.Out] = opposite

   	 	type Aux[A <: Number, B <: Number] = Opposite[A] {type Out = B}
   	 	implicit val oppositeZero = new Opposite[_0] {type Out = _0}
   	 	implicit def oppositePos[B <: SPos] : Aux[B, Minus[B]] = new Opposite[B]{type Out = Minus[B]}
   	 	implicit def oppositeNeg[B <: SPos] : Aux[Minus[B], B] = new Opposite[Minus[B]]{type Out = B}
   	 }

	/**
   	 * Type class witnessing that `C` is the sum of `A` and `B`.
   	 * 
   	 * @author Miles Sabin
   	 */
   	 trait Sum[A <: Number, B <: Number] { type Out <: Number}

   	 object Sum {
   	 	def apply[A <: Number, B <: Number](implicit sum: Sum[A, B]): Aux[A, B, sum.Out] = sum

   	 	type Aux[A <: Number, B <: Number, C <: Number] = Sum[A, B] { type Out = C }

   	 	implicit val sum00 = new Sum[_0,_0]{type Out = _0} 
   	 	implicit def sum0Num[B <: Number]: Aux[_0, B, B] = new Sum[_0, B] { type Out = B }
   	 	implicit def sumNum0[B <: Number]: Aux[B, _0, B] = new Sum[B, _0] { type Out = B }
   	 	implicit def sumNatInt[A <: Nat, B <: RInt, SA <: Nat, SB <: RInt]
   	 	(implicit succA : Successor.Aux[A, SA], succB : Successor.Aux[B, SB], sum : Sum[A, SB]) : Aux[SA, B, sum.Out] = new Sum[SA, B]{type Out = sum.Out}

   	 	implicit def sumNegInt[A <: Neg, B <: RInt, PA <: Neg, PB <: RInt]
   	 	(implicit predA : Predecessor.Aux[A, PA], predB : Predecessor.Aux[B, PB], sum : Sum[A, PB]) : Aux[PA, B, sum.Out] = new Sum[PA, B]{type Out = sum.Out}
   	 }

	/**
     * Type class witnessing that `C` is the sum of `A` and `B`.
     * 
     * @author Miles Sabin
     */
     trait Diff[A <: Number, B <: Number] { type Out <: Number}

     object Diff {
     	def apply[A <: Number, B <: Number](implicit sum: Diff[A, B]): Aux[A, B, sum.Out] = sum
     	type Aux[A <: Number, B <: Number, C <: Number] = Diff[A, B] { type Out = C }

     	implicit def diff[A <: Number, B <: Number, OB <: Number] (implicit opposite : Opposite.Aux[B, OB], sum : Sum[A, OB]) : Aux[A, B, sum.Out] = new Diff[A,B]{type Out = sum.Out}
     }

   /**
     * Type class witnessing that `C` is the product of `A` and `B`.
     * 
     * @author Miles Sabin
     */
     trait Prod[A <: Number, B <: Number] { type Out <: Number }

     object Prod {
     	def apply[A <: Number, B <: Number](implicit prod: Prod[A, B]): Aux[A, B, prod.Out] = prod

     	type Aux[A <: Number, B <: Number, C <: Number] = Prod[A, B] { type Out = C }

     	implicit def prod0[B <: Number]: Aux[_0, B, _0] = new Prod[_0, B] { type Out = _0 }
     	//If a * b = c then (a + 1) * b = b + c 
     	implicit def prodNatNum[A <: Nat, B <: Number, C <: Number] 
     	(implicit prod: Prod.Aux[A, B, C], sum: Sum[B, C], succA : Successor[A]): Aux[succA.Out, B, sum.Out] = new Prod[succA.Out, B] { type Out = sum.Out }
      implicit def prodNegNum[A <: Neg, OA <: Number, B <: Number, C <: Number] 
      (implicit oppA : Opposite.Aux[A, OA], prod: Prod.Aux[OA, B, C], oppC : Opposite[C]): Aux[A, B, oppC.Out] = new Prod[A, B] { type Out = oppC.Out }
    }

  }

