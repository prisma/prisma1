package com.prisma.shared.models

import org.scalatest.{Matchers, WordSpec}

class ProjectIdSpec extends WordSpec with Matchers {
  val encoder = ProjectIdEncoder('@')

  ".toEncodedString" when {
    "given neither service nor stage" should {
      "assume the default name and stage" in {
        encoder.toEncodedString(List.empty) shouldEqual "default@default"
      }
    }

    "given no stage" should {
      "add the default state of the service" in {
        encoder.toEncodedString("test", "") shouldEqual "test@default"
      }
    }

    "Fail if given invalid input" in {
      assertThrows[RuntimeException] {
        encoder.toEncodedString(List("too", "many", "segments", "given"))
      }
    }
  }
}
