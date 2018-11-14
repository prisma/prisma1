package com.prisma.deploy.connector.postgres

import java.sql.Driver

import com.prisma.config.DatabaseConfig
import com.prisma.connector.shared.jdbc.{Databases, SlickDatabase}
import com.zaxxer.hikari.HikariDataSource
import slick.jdbc.hikaricp.HikariCPJdbcDataSource
import slick.jdbc.{DataSourceJdbcDataSource, DriverDataSource, PostgresProfile}

case class PostgresInternalDatabaseDefs(dbConfig: DatabaseConfig, driver: Driver) {
  import slick.jdbc.PostgresProfile.api._

  // Only used during setup - this is the default PSQL db, which is only used for administrative commands
  lazy val setupDatabase = getDatabase("postgres", "public")

  // Used during runtime & setup
  lazy val dbName               = dbConfig.database.getOrElse("prisma")
  lazy val managementSchemaName = dbConfig.managementSchema.getOrElse("management")
  lazy val managementDatabase   = getDatabase(dbName, managementSchemaName)

  def getDatabase(dbToUse: String, schemaToUse: String): Databases = {
    val masterDb      = databaseForConfig(dbToUse, schemaToUse)
    val slickDatabase = SlickDatabase(PostgresProfile, masterDb)

    Databases(primary = slickDatabase, replica = slickDatabase)
  }

  def databaseForConfig(database: String, schema: String) = {
//    val source         = hikariDataSource(database, schema)
    val source         = simpleDataSource(database, schema)
    val poolName       = "database"
    val numThreads     = 1
    val maxConnections = 1
    val executor       = AsyncExecutor(poolName, numThreads, numThreads, 1000, maxConnections, registerMbeans = false)
    Database.forSource(source, executor)
  }

  def simpleDataSource(database: String, schema: String) = {
    new DataSourceJdbcDataSource(
      new DriverDataSource(
        url =
          s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/$database?currentSchema=$schema&ssl=${dbConfig.ssl}&sslfactory=org.postgresql.ssl.NonValidatingFactory",
        user = dbConfig.user,
        password = dbConfig.password.getOrElse(""),
        driverObject = driver
      ),
      keepAliveConnection = true,
      maxConnections = None
    )
  }

  // Todo: Connection attempts stall until timeouts kick in. Might be a deadlock somewhere, or something similar.
  def hikariDataSource(database: String, schema: String): HikariCPJdbcDataSource = {
    val ds = new HikariDataSource()

    ds.setJdbcUrl(
      s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/$database?currentSchema=$schema&ssl=${dbConfig.ssl}&sslfactory=org.postgresql.ssl.NonValidatingFactory"
    )

//    dataSourceProperties = new Properties();
//    healthCheckProperties = new Properties();
//
//    minIdle = -1;
//    maxPoolSize = -1;
//    maxLifetime = MAX_LIFETIME;
//    connectionTimeout = CONNECTION_TIMEOUT;
//    validationTimeout = VALIDATION_TIMEOUT;
//    idleTimeout = IDLE_TIMEOUT;
//
//    isAutoCommit = true;
//    isInitializationFailFast = true;
//
//    String systemProp = System.getProperty("hikaricp.configurationFile");
//    if (systemProp != null) {
//      loadProperties(systemProp);
//    }

    ds.setUsername(dbConfig.user)
    ds.setPassword(dbConfig.password.getOrElse(""))

    // Pool configuration
    ds.setConnectionTimeout(5000)
    ds.setValidationTimeout(1000)
    ds.setIdleTimeout(600000)
    ds.setMaxLifetime(1800000)
    ds.setLeakDetectionThreshold(0)
    ds.setInitializationFailFast(false)

    val numThreads = 1
    ds.setMaximumPoolSize(numThreads)
    ds.setMinimumIdle(numThreads)
    ds.setPoolName("database")
    ds.setRegisterMbeans(false)

    ds.setReadOnly(false)
    ds.setCatalog(null)

    new HikariCPJdbcDataSource(ds, ds)

//    hconf.setJdbcUrl(c.getStringOr("url", null))
//    c.getStringOpt("user").foreach(hconf.setUsername)
//    c.getStringOpt("password").foreach(hconf.setPassword)
//    c.getPropertiesOpt("properties").foreach(hconf.setDataSourceProperties)
//
//    // Pool configuration
//    hconf.setConnectionTimeout(c.getMillisecondsOr("connectionTimeout", 1000))
//    hconf.setValidationTimeout(c.getMillisecondsOr("validationTimeout", 1000))
//    hconf.setIdleTimeout(c.getMillisecondsOr("idleTimeout", 600000))
//    hconf.setMaxLifetime(c.getMillisecondsOr("maxLifetime", 1800000))
//    hconf.setLeakDetectionThreshold(c.getMillisecondsOr("leakDetectionThreshold", 0))
//    hconf.setInitializationFailFast(c.getBooleanOr("initializationFailFast", false))
//    c.getStringOpt("connectionTestQuery").foreach(hconf.setConnectionTestQuery)
//    c.getStringOpt("connectionInitSql").foreach(hconf.setConnectionInitSql)
//    val numThreads = c.getIntOr("numThreads", 20)
//    hconf.setMaximumPoolSize(c.getIntOr("maxConnections", numThreads))
//    hconf.setMinimumIdle(c.getIntOr("minConnections", numThreads))
//    hconf.setPoolName(c.getStringOr("poolName", name))
//    hconf.setRegisterMbeans(c.getBooleanOr("registerMbeans", false))
//
//    // Equivalent of ConnectionPreparer
//    hconf.setReadOnly(c.getBooleanOr("readOnly", false))
//    c.getStringOpt("isolation").map("TRANSACTION_" + _).foreach(hconf.setTransactionIsolation)
//    hconf.setCatalog(c.getStringOr("catalog", null))
//
//    val ds = new HikariDataSource(hconf)
//    new HikariCPJdbcDataSource(ds, hconf)
  }
}
