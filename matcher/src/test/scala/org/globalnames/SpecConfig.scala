package org.globalnames

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

trait SpecConfig extends WordSpec with Matchers with OptionValues
                    with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures
