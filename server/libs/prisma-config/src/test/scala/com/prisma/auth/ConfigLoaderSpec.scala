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
                          |    migrations: true
                          |    host: localhost
                          |    port: 3306
                          |    user: root
                          |    password: prisma
                          |    database: my_database
                          |    schema: my_schema
                          |    ssl: true
                          |    connectionLimit: 2
                          |    rawAccess: true
                        """.stripMargin

      val config = ConfigLoader.tryLoadString(validConfig)

      config.get.port shouldBe Some(4466)
      config.get.managementApiSecret should contain("somesecret")
      config.get.databases.length shouldBe 1
      val database = config.get.databases.head
      database.connector shouldBe "mysql"
      database.active shouldBe true
      database.port shouldBe 3306
      database.user shouldBe "root"
      database.password shouldBe Some("prisma")
      database.database shouldBe Some("my_database")
      database.schema shouldBe Some("my_schema")
      database.ssl shouldBe true
      database.connectionLimit shouldBe Some(2)
      database.rawAccess shouldBe true
    }

    "be parsed without errors if an optional field is missing" in {
      val validConfig = """
                          |port: 4466
                          |databases:
                          |  default:
                          |    connector: mysql
                          |    host: localhost
                          |    port: 3306
                          |    user: root
                          |    password: prisma
                        """.stripMargin

      val config = ConfigLoader.tryLoadString(validConfig)

      config.isSuccess shouldBe true
      config.get.port should contain(4466)
      config.get.managementApiSecret shouldBe None
      config.get.databases.length shouldBe 1
      val database = config.get.databases.head
      database.connector shouldBe "mysql"
      database.active shouldBe true
      database.port shouldBe 3306
      database.user shouldBe "root"
      database.password shouldBe Some("prisma")
      database.database shouldBe None
      database.schema shouldBe None
      database.ssl shouldBe false
      database.rawAccess shouldBe false
    }

    "be parsed without errors if an optional field is there but set to nothing" in {
      val validConfig = """
                          |port: 4466
                          |managementApiSecret:
                          |databases:
                          |  default:
                          |    connector: mysql
                          |    migrations: true
                          |    host: localhost
                          |    port: 3306
                          |    user: root
                          |    password: prisma
                          |    database:
                          |    schema:
                          |    ssl:
                        """.stripMargin

      val config = ConfigLoader.tryLoadString(validConfig)

      config.isSuccess shouldBe true
      config.get.port should contain(4466)
      config.get.managementApiSecret shouldBe None
      config.get.databases.length shouldBe 1
      val database = config.get.databases.head
      database.connector shouldBe "mysql"
      database.active shouldBe true
      database.port shouldBe 3306
      database.user shouldBe "root"
      database.password shouldBe Some("prisma")
      database.database shouldBe None
      database.schema shouldBe None
      database.ssl shouldBe false
    }

    "be parsed without errors if the URI is used" in {
      val validConfig = """
          |port: 4466
          |managementApiSecret:
          |databases:
          |  default:
          |    connector: postgres
          |    migrations: true
          |    uri: postgres://user:password@host:5432/database?ssl=1
        """.stripMargin

      val config = ConfigLoader.tryLoadString(validConfig)

      config.isSuccess shouldBe true
      config.get.port should contain(4466)
      config.get.managementApiSecret shouldBe None
      config.get.databases.length shouldBe 1
      val database = config.get.databases.head
      database.connector shouldBe "postgres"
      database.active shouldBe true
      database.port shouldBe 5432
      database.user shouldBe "user"
      database.password shouldBe Some("password")
      database.database shouldBe Some("database")
      database.schema shouldBe None
      database.ssl shouldBe true
    }
  }

  "an invalid config" should {
    "fail with an invalid config format error for an invalid int conversion" ignore {
      val invalidConfig = """
                            |port: Invalid
                            |managementApiSecret: somesecret
                            |databases:
                            |  default:
                            |    connector: mysql
                            |    migrations: true
                            |    host: localhost
                            |    port: 3306
                            |    user: root
                            |    password: prisma
                          """.stripMargin

      val config = ConfigLoader.tryLoadString(invalidConfig)

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
                          |    host: localhost
                          |    port: notanumber
                          |    user: root
                          |    password: prisma
                        """.stripMargin

    val config = ConfigLoader.tryLoadString(invalidConfig)

    config.isSuccess shouldBe false
    config.failed.get shouldBe a[InvalidConfiguration]
  }

  "fail with an invalid config format error for a missing top level field" in {
    val invalidConfig = """
                          |port: 4466
                          |managementApiSecret: somesecret
                        """.stripMargin

    val config = ConfigLoader.tryLoadString(invalidConfig)

    config.isSuccess shouldBe false
    config.failed.get shouldBe a[InvalidConfiguration]
  }

  "fail if connectionLimit is set to less than 2 " in {
    val configString = """
                        |port: 4466
                        |databases:
                        |  default:
                        |    connector: mysql
                        |    host: localhost
                        |    port: 3306
                        |    user: root
                        |    password: prisma
                        |    connectionLimit: 1
                      """.stripMargin

    val config = ConfigLoader.tryLoadString(configString)
    config.isSuccess shouldBe false
    val exception = config.failed.get
    exception shouldBe a[InvalidConfiguration]
    exception.getMessage should equal("The parameter connectionLimit must be set to at least 2.")
  }

  "succeed for a valid mongo connector config" in {
    val uri           = "mongodb://myAdminUser:abc123@host.docker.internal:27017/admin"
    val invalidConfig = s"""
                        |port: 4466
                        |databases:
                        |  default:
                        |    connector: mongo
                        |    uri: $uri
                      """.stripMargin
    val config        = ConfigLoader.tryLoadString(invalidConfig)
    config.isSuccess shouldBe true
    val db = config.get.databases.head
    db.connector should be("mongo")
    db.uri should be(uri)
  }
}
