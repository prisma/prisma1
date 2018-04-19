package com.prisma.shared.models

import org.scalatest.{Matchers, WordSpec}

class ProjectIdSpec extends WordSpec with Matchers {
  ".toEncodedString" when {
    "given neither service nor stage" should {
      "assume the default name and stage" in {
        ProjectId.toEncodedString(List.empty) shouldEqual "default@default"
      }
    }

    "given no stage" should {
      "add the default state of the service" in {
        ProjectId.toEncodedString("test", "") shouldEqual "test@default"
      }
    }

    "Fail if given invalid input" in {
      assertThrows[RuntimeException] {
        ProjectId.toEncodedString(List("too", "many", "segments", "given"))
      }
    }
  }
}
