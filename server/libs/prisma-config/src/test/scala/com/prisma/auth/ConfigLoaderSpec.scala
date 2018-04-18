package com.prisma.auth

import com.prisma.config.{ConfigLoader, InvalidConfiguration}
import org.scalatest.{Matchers, WordSpec}

class ConfigLoaderSpec extends WordSpec with Matchers {
  val validConfig = """
    |port: 4466
    |managementApiSecret: somesecret
    |databases:
    |  default:
    |    connector: mysql
    |    active: true
    |    host: localhost
    |    port: 3306
    |    user: root
    |    password: prisma
  """.stripMargin

  "a valid config" should {
    "be parsed without errors" in {
      val config = ConfigLoader.load(validConfig)

      config.isSuccess shouldBe true
      config.get.port shouldBe 4466
      config.get.managementApiSecret shouldBe "somesecret"
      config.get.databases.length shouldBe 1
      config.get.databases.head.connector shouldBe "mysql"
      config.get.databases.head.active shouldBe true
      config.get.databases.head.port shouldBe 3306
      config.get.databases.head.user shouldBe "root"
      config.get.databases.head.password shouldBe "prisma"
    }
  }

  "an invalid config" should {
    "fail with an invalid config format error" in {
      val invalidConfig = """
                            |port: Invalid
                            |managementApiSecret: somesecret
                            |databases:
                            |  default:
                            |    connector: mysql
                            |    active: true
                            |    host: localhost
                            |    port: 3306
                            |    user: root
                            |    password: prisma
                          """.stripMargin

      val config = ConfigLoader.load(invalidConfig)

      config.isSuccess shouldBe false
      config.failed.get shouldBe a[InvalidConfiguration]
    }
  }
}
