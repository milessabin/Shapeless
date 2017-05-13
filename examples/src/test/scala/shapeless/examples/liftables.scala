/*
 * Copyright (c) 2016 Jan Bessai, Miles Sabin
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

import org.junit.Test
import org.junit.Assert._
import shapeless._
import test._


class LiftablesTest {
  @Test
  def LiftableExampleTest: Unit = {
    import LiftableExample._
    assertEquals(test1, materialize)
    assertEquals(test1, materializeCustom)
    illTyped { """materializeIllTyped"""}
  }
}
