package com.prisma.auth

import com.prisma.config.{ConfigLoader, InvalidConfiguration}
import org.scalatest.{Matchers, WordSpec}

class ConfigLoaderSpec extends WordSpec with Matchers {
  "a valid config" should {
    "be parsed without errors" in {
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

      val config = ConfigLoader.loadString(validConfig)

      config.isSuccess shouldBe true
      config.get.port shouldBe 4466
      config.get.managementApiSecret should contain("somesecret")
      config.get.databases.length shouldBe 1
      config.get.databases.head.connector shouldBe "mysql"
      config.get.databases.head.active shouldBe true
      config.get.databases.head.port shouldBe 3306
      config.get.databases.head.user shouldBe "root"
      config.get.databases.head.password shouldBe "prisma"
    }

    "be parsed without errors if an optional field is missing" in {
      val validConfig = """
                          |port: 4466
                          |databases:
                          |  default:
                          |    connector: mysql
                          |    active: true
                          |    host: localhost
                          |    port: 3306
                          |    user: root
                          |    password: prisma
                        """.stripMargin

      val config = ConfigLoader.loadString(validConfig)

      config.isSuccess shouldBe true
      config.get.port should contain(4466)
      config.get.managementApiSecret shouldBe None
      config.get.databases.length shouldBe 1
      config.get.databases.head.connector shouldBe "mysql"
      config.get.databases.head.active shouldBe true
      config.get.databases.head.port shouldBe 3306
      config.get.databases.head.user shouldBe "root"
      config.get.databases.head.password shouldBe "prisma"
    }

    "be parsed without errors if an optional field is missing but set to nothing" in {
      val validConfig = """
                          |port: 4466
                          |managementApiSecret:
                          |databases:
                          |  default:
                          |    connector: mysql
                          |    active: true
                          |    host: localhost
                          |    port: 3306
                          |    user: root
                          |    password: prisma
                        """.stripMargin

      val config = ConfigLoader.loadString(validConfig)

      config.isSuccess shouldBe true
      config.get.port should contain(4466)
      config.get.managementApiSecret shouldBe None
      config.get.databases.length shouldBe 1
      config.get.databases.head.connector shouldBe "mysql"
      config.get.databases.head.active shouldBe true
      config.get.databases.head.port shouldBe 3306
      config.get.databases.head.user shouldBe "root"
      config.get.databases.head.password shouldBe "prisma"
    }
  }

  "an invalid config" should {
    "fail with an invalid config format error for an invalid int conversion" in {
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

      val config = ConfigLoader.loadString(invalidConfig)

      config.isSuccess shouldBe false
      config.failed.get shouldBe a[InvalidConfiguration]
    }
  }

  "fail with an invalid config format error for an invalid boolean conversion" in {
    val invalidConfig = """
                          |port: 4466
                          |managementApiSecret: somesecret
                          |databases:
                          |  default:
                          |    connector: mysql
                          |    active: notaboolean
                          |    host: localhost
                          |    port: 3306
                          |    user: root
                          |    password: prisma
                        """.stripMargin

    val config = ConfigLoader.loadString(invalidConfig)

    config.isSuccess shouldBe false
    config.failed.get shouldBe a[InvalidConfiguration]
  }

  "fail with an invalid config format error for a missing top level field" in {
    val invalidConfig = """
                          |port: 4466
                          |managementApiSecret: somesecret
                        """.stripMargin

    val config = ConfigLoader.loadString(invalidConfig)

    config.isSuccess shouldBe false
    config.failed.get shouldBe a[InvalidConfiguration]
  }
}
