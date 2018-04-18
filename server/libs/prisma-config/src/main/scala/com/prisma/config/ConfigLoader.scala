package com.prisma.config

import java.io.File

import org.yaml.snakeyaml.Yaml

import scala.collection.mutable
import scala.util.Try

object ConfigLoader {
  import scala.collection.JavaConverters.mapAsScalaMap

  private val yaml                = new Yaml()
  private val configFile          = "prisma"
  private val commonYmlExtensions = Seq("yml", "yaml")
  def emptyJavaMap                = new java.util.LinkedHashMap[String, Any]()

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

  // Todo error propagation concept with meaningful user messages
  def load(): Try[PrismaConfig] =
    Try {
      sys.env
        .get("PRISMA_CONFIG")
        .orElse(findPrismaConfigFilePath().map(scala.io.Source.fromFile(_).mkString))
        .orElse(legacyConfig())
        .getOrElse(sys.error("No valid Prisma config could be loaded."))
    }.flatMap(loadString)

  def loadString(config: String): Try[PrismaConfig] = Try {
    convertToConfig(extractScalaMap(yaml.load(config).asInstanceOf[java.util.Map[String, Any]], path = "root"))
  }

  private def legacyConfig(): Option[String] =
    Try {
      val port         = sys.env.getOrElse("PORT", "4466").toInt
      val secret       = sys.env.getOrElse("PRISMA_MANAGEMENT_API_JWT_SECRET", "")
      val legacySecret = sys.env.getOrElse("CLUSTER_PUBLIC_KEY", "")
      val dbHost       = sys.env.getOrElse("SQL_CLIENT_HOST", sys.error("Env var SQL_CLIENT_HOST required but not found"))
      val dbPort       = sys.env.getOrElse("SQL_CLIENT_PORT", "3306").toInt
      val dbUser       = sys.env.getOrElse("SQL_CLIENT_USER", sys.error("Env var SQL_CLIENT_USER required but not found"))
      val dbPass       = sys.env.getOrElse("SQL_CLIENT_PASSWORD", sys.error("Env var SQL_CLIENT_PASSWORD required but not found"))
      val dbConn       = sys.env.getOrElse("SQL_INTERNAL_CONNECTION_LIMIT", "10")

      s"""
        |port: $port
        |managementApiSecret: $secret
        |legacySecret: $legacySecret
        |databases:
        |  default:
        |    connector: mysql
        |    active: true
        |    host: $dbHost
        |    port: $dbPort
        |    user: $dbUser
        |    password: $dbPass
        |    connectionLimit: $dbConn
      """.stripMargin
    }.toOption

  def convertToConfig(map: mutable.Map[String, Any]): PrismaConfig = {
    val port         = extractIntOpt("port", map)
    val secret       = extractStringOpt("managementApiSecret", map)
    val legacySecret = extractStringOpt("clusterPublicKey", map)
    val databases = extractScalaMap(map.getOrElse("databases", emptyJavaMap), path = "databases").map {
      case (dbName, dbMap) =>
        val db          = extractScalaMap(dbMap, path = dbName)
        val dbConnector = extractString("connector", db)
        val dbActive    = extractBoolean("active", db)
        val dbHost      = extractString("host", db)
        val dbPort      = extractInt("port", db)
        val dbUser      = extractString("user", db)
        val dbPass      = extractString("password", db)
        val connLimit   = extractIntOpt("connectionLimit", db)

        DatabaseConfig(dbName, dbConnector, dbActive, dbHost, dbPort, dbUser, dbPass, connLimit)
    }.toSeq

    if (databases.isEmpty) {
      throw InvalidConfiguration("No databases defined")
    }

    PrismaConfig(port, secret, legacySecret, databases)
  }

  private def extractScalaMap(in: Any, required: Boolean = true, path: String = ""): mutable.Map[String, Any] = {
    val out = mapAsScalaMap(in.asInstanceOf[java.util.Map[String, Any]])
    if (required && out.isEmpty) {
      throw InvalidConfiguration(s"Expected hash under '$path' to be non-empty")
    }

    out
  }

  private def extractString(key: String, map: mutable.Map[String, Any]): String = {
    extractStringOpt(key, map) match {
      case Some(x) => x
      case None    => throw InvalidConfiguration(s"Expected $key to be non-empty")
    }
  }

  private def extractStringOpt(key: String, map: mutable.Map[String, Any]): Option[String] = {
    map.get(key) match {
      case x @ Some(v) if v.toString.nonEmpty => x.map(_.toString)
      case _                                  => None
    }
  }

  private def extractBoolean(key: String, map: mutable.Map[String, Any]): Boolean = {
    extractBooleanOpt(key, map) match {
      case Some(x) => x
      case None    => throw InvalidConfiguration(s"Expected Boolean for field $key, got ${map.getOrElse(key, "<unset>").toString}")
    }
  }

  private def extractBooleanOpt(key: String, map: mutable.Map[String, Any]): Option[Boolean] = {
    map.get(key).map(_.toString.toLowerCase()) match {
      case Some("true")  => Some(true)
      case Some("false") => Some(false)
      case _             => None
    }
  }

  private def extractInt(key: String, map: mutable.Map[String, Any]): Int = {
    extractIntOpt(key, map) match {
      case Some(x) => x
      case None    => throw InvalidConfiguration(s"Expected Int for field $key, got ${map.getOrElse(key, "<unset>").toString}")
    }
  }

  private def extractIntOpt(key: String, map: mutable.Map[String, Any]): Option[Int] = {
    try { map.get(key).map(_.toString.toInt) } catch {
      case _: Throwable => None
    }
  }
}

case class PrismaConfig(port: Option[Int], managementApiSecret: Option[String], legacySecret: Option[String], databases: Seq[DatabaseConfig])
case class DatabaseConfig(name: String,
                          connector: String,
                          active: Boolean,
                          host: String,
                          port: Int,
                          user: String,
                          password: String,
                          connectionLimit: Option[Int])

abstract class ConfigError(reason: String)       extends Exception(reason)
case class InvalidConfiguration(message: String) extends ConfigError(message)
