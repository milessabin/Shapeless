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

import org.junit.Test
import org.junit.Assert._

import shapeless.test._

class SizedTests {
  import nat._
  import syntax.sized._
  
  @Test
  def testBasics {
    val l0 = List.empty[Int]
    val l1 = List(1)
    val l2 = List(1, 2)
    val l3 = List(1, 2, 3)
    
    val nl0 = l0.sized(0)
    assertTrue(nl0.isDefined)
    val nl0b = l0.sized(1)
    assertTrue(nl0b.isEmpty)
    val nl0c = l0.sized(2)
    assertTrue(nl0c.isEmpty)
    val nl0d = l0.sized(3)
    assertTrue(nl0d.isEmpty)
    
    illTyped("""
    val h0 = nl0.get.head
    """)
    illTyped("""
    val t0 = nl0.get.tail
    """)
    
    val nl1 = l1.sized(0)
    assertTrue(nl1.isEmpty)
    val nl1b = l1.sized(1)
    assertTrue(nl1b.isDefined)
    val nl1c = l1.sized(2)
    assertTrue(nl1c.isEmpty)
    val nl1d = l1.sized(3)
    assertTrue(nl1d.isEmpty)

    val h1 = nl1b.get.head
    val t1 = nl1b.get.tail

    illTyped("""
    val t1b = nl1b.get.tail.tail
    """)

    val nl2 = l2.sized(0)
    assertTrue(nl2.isEmpty)
    val nl2b = l2.sized(1)
    assertTrue(nl2b.isEmpty)
    val nl2c = l2.sized(2)
    assertTrue(nl2c.isDefined)
    val nl2d = l2.sized(3)
    assertTrue(nl2d.isEmpty)
    
    val h2 = nl2c.get.head
    val t2 = nl2c.get.tail
    val t2b = nl2c.get.tail.tail

    illTyped("""
    val t2c = nl1c.get.tail.tail.tail
    """)

    val nl3 = l3.sized(0)
    assertTrue(nl3.isEmpty)
    val nl3b = l3.sized(1)
    assertTrue(nl3b.isEmpty)
    val nl3c = l3.sized(2)
    assertTrue(nl3c.isEmpty)
    val nl3d = l3.sized(3)
    assertTrue(nl3d.isDefined)

    val h3 = nl3d.get.head
    val t3 = nl3d.get.tail
    val t3b = nl3d.get.tail.tail
    val t3c = nl3d.get.tail.tail.tail

    illTyped("""
    val t3d = nl1d.get.tail.tail.tail.tail
    """)
    
    val rs = "foo".sized(3).get.unsized
    
    val rl = List(1, 2, 3).sized(3).get.unsized
    
    val s1 = "foo".sized(3)
    assertTrue(s1.isDefined)
    val s2 = "bar".sized(3)
    assertTrue(s2.isDefined)

    val s3 = s1.get ++ s2.get
    typed[Sized[String, _6]](s3)
    assertEquals("foobar", s3.unsized)
    
    val cs = for(x <- s1 ; y <- s2) yield x ++ y
    assertTrue(cs.isDefined)
    typed[Sized[String, _6]](cs.get)
    assertEquals("foobar", cs.get.unsized)
    
    val s4 = 'c' +: s1.get
    
    val ll1 = List(1, 2, 3).sized(3)
    assertTrue(ll1.isDefined)
    val ll2 = List(4, 5, 6).sized(3)
    assertTrue(ll2.isDefined)

    val ll3 = ll1.get ++ ll2.get
    typed[Sized[List[Int], _6]](ll3)
    assertEquals(List(1, 2, 3, 4, 5, 6), ll3.unsized)
    
    val cl = for(x <- ll1 ; y <- ll2) yield x ++ y
    assertTrue(cl.isDefined)
    typed[Sized[List[Int], _6]](cl.get)
    assertEquals(List(1, 2, 3, 4, 5, 6), cl.get.unsized)

    val s = cl.get.size
    val evens = cl.get.filter(_ % 2 == 0)
    
    illTyped("""
    val Sized(cl1, cl2, cl3, cl4, cl5) = cl.get
    """)

    val p = cl.get match {
      case Sized(a, b, _, _, _, _) => {
        typed[Int](a)
        typed[Int](b)
        (a-b, a+b)
      }
      case _ => (9, 10)
    }
    typed[(Int, Int)](p)
    assertEquals((-1, 3), p)
    
    val j1 = ll1.get.take(1)
    
    val tk1 = cl.get.take(1)
    val tk4 = cl.get.take(4)

    illTyped("""
    val tk7 = cl.get.take(7)
    """)
    
    val dr1 = cl.get.drop(1)
    val dr4 = cl.get.drop(4)

    illTyped("""
    val dr7 = cl.get.drop(7)
    """)
    
    val (pr1, sf1) = cl.get.splitAt(1)
    val (pr4, sf4) = cl.get.splitAt(4)

    illTyped("""
    val (pr7, sf7) = cl.get.splitAt(7)
    """)
    
    val ml = cl.get map (_.toString)
    typed[Sized[List[String], _6]](ml)
    assertEquals(List("1", "2", "3", "4", "5", "6"), ml.unsized)

    import scala.collection.immutable.IndexedSeq

    val is0 = Sized()
    typed[Sized[IndexedSeq[Nothing], _0]](is0)
    val is1 = Sized("foo")
    typed[Sized[IndexedSeq[String], _1]](is1)
    val is2 = Sized("foo", "bar")
    typed[Sized[IndexedSeq[String], _2]](is2)
    
    val isl0 = Sized[List]()
    typed[Sized[List[Nothing], _0]](isl0)
    val isl1 = Sized[List]("foo")
    typed[Sized[List[String], _1]](isl1)
    val isl2 = Sized[List]("foo", "bar")
    typed[Sized[List[String], _2]](isl2)

    val isa0 = Sized[Array]()
    typed[Sized[Array[Nothing], _0]](isa0)
    val isa1 = Sized[Array]("foo")
    typed[Sized[Array[String], _1]](isa1)
    val isa2 = Sized[Array]("foo", "bar")
    typed[Sized[Array[String], _2]](isa2)
  }
}
