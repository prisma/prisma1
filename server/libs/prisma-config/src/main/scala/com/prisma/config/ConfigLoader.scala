package com.prisma.config

import java.io.File
import org.yaml.snakeyaml.Yaml
import scala.collection.mutable
import scala.util.Try

object ConfigLoader {
  import scala.collection.JavaConverters.mapAsScalaMap

  val yaml                = new Yaml()
  val configFile          = "prisma"
  val commonYmlExtensions = Seq("yml", "yaml")

  private def findPrismaConfigFilePath(): Option[String] = {
    val searchPath = System.getProperty("user.dir") + File.pathSeparator
    val sep        = File.pathSeparator

    commonYmlExtensions
      .map { ext =>
        val candidate = new File(s"$searchPath$sep$configFile.$ext")
        if (candidate.isFile) {
          candidate.getAbsolutePath
        } else {
          ""
        }
      }
      .find(_.nonEmpty)
  }

  // should this be a thing?
  val defaultConfig =
    """
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

  def load(): Try[PrismaConfig] = Try {
    val configString = sys.env.get("PRISMA_CONFIG") match {
      case Some(config) => config
      case None =>
        findPrismaConfigFilePath() match {
          case Some(path) => scala.io.Source.fromFile(path).mkString
          case None       => defaultConfig //sys.error("No prisma config found.")
        }
    }

    convertToConfig(extractScalaMap(yaml.load(configString)))
  }

  def convertToConfig(map: mutable.Map[String, Any]): PrismaConfig = {
    val port   = extractInt("port", map, "4466")
    val secret = extractString("managementApiSecret", map)
    val databases = extractScalaMap(map.getOrElse("databases", Map.empty)).map {
      case (dbName, dbMap) =>
        val db          = extractScalaMap(dbMap)
        val dbConnector = extractString("connector", db)
        val dbActive    = extractBoolean("active", db)
        val dbHost      = extractString("host", db)
        val dbPort      = extractInt("port", db)
        val dbUser      = extractString("user", db)
        val dbPass      = extractString("password", db)
        DatabaseConfig(dbName, dbConnector, dbActive, dbHost, dbPort, dbUser, dbPass)
    }.toSeq

    PrismaConfig(port, secret, databases)
  }

  private def extractScalaMap[T <: java.util.Map[String, Any]](in: T): mutable.Map[String, Any] = {
    mapAsScalaMap(in.asInstanceOf[java.util.Map[String, Any]])
  }

  private def extractString(key: String, map: mutable.Map[String, Any], mapDefaultValue: String = "", required: Boolean = true): String = {
    val value = map.getOrElse(key, mapDefaultValue).toString
    if (required && value.isEmpty) {
      throw InvalidConfiguration(s"Expected $key to be non-empty")
    }

    value
  }

  private def extractBoolean(key: String, map: mutable.Map[String, Any], mapDefaultValue: String = ""): Boolean = {
    map.getOrElse(key, mapDefaultValue).toString.toLowerCase() match {
      case "true"  => true
      case "false" => false
      case x       => throw InvalidConfiguration(s"Expected Boolean for field $key, got: $x")
    }
  }

  private def extractInt(key: String, map: mutable.Map[String, Any], mapDefaultValue: String = ""): Int = {
    val field = map.getOrElse("port", mapDefaultValue).toString
    try { field.toInt } catch {
      case _: Throwable => throw InvalidConfiguration(s"Expected Int for field $key, got $field")
    }
  }
}

case class PrismaConfig(port: Int, managementApiSecret: String, databases: Seq[DatabaseConfig])
case class DatabaseConfig(name: String, connector: String, active: Boolean, host: String, port: Int, user: String, password: String)

abstract class ConfigError(reason: String)       extends Exception(reason)
case class InvalidConfiguration(message: String) extends ConfigError(message)
