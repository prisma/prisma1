package com.prisma.native_jdbc

import java.net.URLDecoder
import java.sql.{Driver, DriverManager}
import java.util.Properties

import com.prisma.native_jdbc.graalvm.RustBindingGraal
import com.prisma.native_jdbc.jna.RustBindingJna

object CustomJdbcDriver {
  lazy val jna   = driverWithBinding(RustBindingJna)
  lazy val graal = driverWithBinding(RustBindingGraal.initialize())

  def driverWithBinding(binding: RustBinding): CustomJdbcDriver = {
    val driver = CustomJdbcDriver(binding)
    driver.register()
    driver
  }
}

case class PgUrl(var dbName: String, var port: String, var host: String)

case class CustomJdbcDriver(binding: RustBinding) extends Driver {
  override def getParentLogger                                = ???
  override def getMajorVersion                                = 1
  override def getMinorVersion                                = 0
  override def jdbcCompliant()                                = false
  override def acceptsURL(url: String)                        = true
  override def getPropertyInfo(url: String, info: Properties) = Array.empty

  override def connect(url: String, info: Properties) = {
    val props  = parseURL(url, new Properties())
    val dbName = props.getProperty("PGDBNAME")
    val port   = props.getProperty("PGPORT").toInt
    val host   = props.getProperty("PGHOST")
    val schema = props.getProperty("currentSchema")
    val user   = info.getProperty("user")
    val pass   = info.getProperty("password")

    new CustomJdbcConnection(s"postgres://$user:$pass@$host:$port/$dbName?search_path=$schema", binding)
  }

  def register(): Unit = {
    DriverManager.registerDriver(this)
  }

  // todo find better solution, this is copied from the postgres driver
  def parseURL(url: String, defaults: Properties): Properties = {
    val urlProps    = new Properties(defaults)
    var l_urlServer = url
    var l_urlArgs   = ""
    val l_qPos      = url.indexOf('?')
    if (l_qPos != -1) {
      l_urlServer = url.substring(0, l_qPos)
      l_urlArgs = url.substring(l_qPos + 1)
    }
    if (!l_urlServer.startsWith("jdbc:postgresql:")) return null
    l_urlServer = l_urlServer.substring("jdbc:postgresql:".length)
    if (l_urlServer.startsWith("//")) {
      l_urlServer = l_urlServer.substring(2)
      val slash = l_urlServer.indexOf('/')
      if (slash == -1) return null
      urlProps.setProperty("PGDBNAME", URLDecoder.decode(l_urlServer.substring(slash + 1), "UTF-8"))
      val addresses = l_urlServer.substring(0, slash).split(",")
      val hosts     = new StringBuilder
      val ports     = new StringBuilder
      for (address <- addresses) {
        val portIdx = address.lastIndexOf(':')
        if (portIdx != -1 && address.lastIndexOf(']') < portIdx) {
          val portStr = address.substring(portIdx + 1)
          try // squid:S2201 The return value of "parseInt" must be used.
          // The side effect is NumberFormatException, thus ignore sonar error here
          portStr.toInt //NOSONAR
          catch {
            case ex: NumberFormatException =>
              return null
          }
          ports.append(portStr)
          hosts.append(address.subSequence(0, portIdx))
        } else {
          ports.append("5432")
          hosts.append(address)
        }
        ports.append(',')
        hosts.append(',')
      }
      ports.setLength(ports.length - 1)
      hosts.setLength(hosts.length - 1)
      urlProps.setProperty("PGPORT", ports.toString)
      urlProps.setProperty("PGHOST", hosts.toString)
    } else {
      /*
            if there are no defaults set or any one of PORT, HOST, DBNAME not set
            then set it to default
       */
      if (defaults == null || !defaults.containsKey("PGPORT")) urlProps.setProperty("PGPORT", "5432")
      if (defaults == null || !defaults.containsKey("PGHOST")) urlProps.setProperty("PGHOST", "localhost")
      if (defaults == null || !defaults.containsKey("PGDBNAME")) urlProps.setProperty("PGDBNAME", URLDecoder.decode(l_urlServer, "UTF-8"))
    }
    // parse the args part of the url
    val args = l_urlArgs.split("&")
    for (token <- args) {
      if (token.nonEmpty) {
        val l_pos = token.indexOf('=')
        if (l_pos == -1) urlProps.setProperty(token, "")
        else urlProps.setProperty(token.substring(0, l_pos), URLDecoder.decode(token.substring(l_pos + 1), "UTF-8"))
      }
    }
    urlProps
  }
}
