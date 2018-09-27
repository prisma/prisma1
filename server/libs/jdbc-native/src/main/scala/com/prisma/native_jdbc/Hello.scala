package com.prisma.native_jdbc

import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.postgresql.core.Parser

object Hello {
  val binding = RustJnaImpl
  val driver  = CustomJdbcDriver(binding.asInstanceOf[RustBinding[RustConnection]])

  def main(args: Array[String]): Unit = {
    testSqlViaGraal()
    testSqlMultiple()
    testTransaction()
    testTransactionRollback()
  }

  def testSqlViaGraal(): Unit = {
    import org.jooq.impl.DSL.{field, table}
    val sql = DSL.using(SQLDialect.POSTGRES, new Settings().withRenderFormatted(true))
    val query = sql
      .select()
      .from(table("posts"))
      .where(field("id").in("?", "?"))

    val standardConformingStrings  = true
    val withParameters             = true
    val splitStatements            = true
    val isBatchedReWriteConfigured = false
    val rawSqlString =
      Parser.parseJdbcSql(query.getSQL(), standardConformingStrings, withParameters, splitStatements, isBatchedReWriteConfigured).get(0).nativeSql

    println(s"raw sql string: $rawSqlString")
    val connection = driver.connect("postgres://postgres:prisma@localhost/", null)
    val ps         = connection.prepareStatement(rawSqlString)
    ps.setInt(0, 1)
    ps.setInt(1, 2)

    val rs = ps.executeQuery()
    while (rs.next()) {
      println(s"body: ${rs.getString("body")}")
      println(s"id: ${rs.getInt("id")}")
      println(s"published: ${rs.getBoolean("published")}")
      println(s"title: ${rs.getString("title")}")
    }
    connection.close()
  }

  def testSqlMultiple(): Unit = {
    import org.jooq.impl.DSL.{field, table}

    println("Testing multiple")

    val sql = DSL.using(SQLDialect.POSTGRES, new Settings().withRenderFormatted(true))
    val query = sql
      .insertInto(table("posts"))
      .columns(field("title"), field("body"), field("published"))
      .values("?", "?", "?")

    val standardConformingStrings  = true
    val withParameters             = true
    val splitStatements            = true
    val isBatchedReWriteConfigured = false
    val rawSqlString =
      Parser.parseJdbcSql(query.getSQL(), standardConformingStrings, withParameters, splitStatements, isBatchedReWriteConfigured).get(0).nativeSql

    println(s"raw sql string: $rawSqlString")

    val connection = driver.connect("postgres://postgres:prisma@localhost/", null)
    val ps         = connection.prepareStatement(rawSqlString)
    ps.setString(0, "Test1")
    ps.setString(1, "TestBody1")
    ps.setBoolean(2, true)

    val ps2 = connection.prepareStatement(rawSqlString)
    ps2.setString(0, "Test2")
    ps2.setString(1, "TestBody2")
    ps2.setBoolean(2, false)

    println("Executing 1")
    ps.execute()
    println("Executing 2")
    ps2.execute()

    println("Done, closing connection.")
    connection.close()
  }

  def testTransaction(): Unit = {
    println("Test transaction")
    import org.jooq.impl.DSL.{field, table}
    val sql = DSL.using(SQLDialect.POSTGRES, new Settings().withRenderFormatted(true))
    val query = sql
      .insertInto(table("posts"))
      .columns(field("title"), field("body"), field("published"))
      .values("?", "?", "?")

    val standardConformingStrings  = true
    val withParameters             = true
    val splitStatements            = true
    val isBatchedReWriteConfigured = false
    val rawSqlString =
      Parser.parseJdbcSql(query.getSQL(), standardConformingStrings, withParameters, splitStatements, isBatchedReWriteConfigured).get(0).nativeSql

    val connection = driver.connect("postgres://postgres:prisma@localhost/", null)
    val ps         = connection.prepareStatement(rawSqlString)
    ps.setString(0, "Test")
    ps.setString(1, "TestBody")
    ps.setBoolean(2, true)

    connection.setAutoCommit(false)
    ps.execute()
    connection.commit()
    connection.close()
  }

  def testTransactionRollback(): Unit = {
    println("Test transaction rollback")
    import org.jooq.impl.DSL.{field, table}
    val sql = DSL.using(SQLDialect.POSTGRES, new Settings().withRenderFormatted(true))
    val query = sql
      .insertInto(table("posts"))
      .columns(field("title"), field("body"), field("published"))
      .values("?", "?", "?")

    val standardConformingStrings  = true
    val withParameters             = true
    val splitStatements            = true
    val isBatchedReWriteConfigured = false
    val rawSqlString =
      Parser.parseJdbcSql(query.getSQL(), standardConformingStrings, withParameters, splitStatements, isBatchedReWriteConfigured).get(0).nativeSql

    val connection = driver.connect("postgres://postgres:prisma@localhost/", null)
    val ps         = connection.prepareStatement(rawSqlString)
    ps.setString(0, "Test")
    ps.setString(1, "TestBody")
    ps.setBoolean(2, true)

    connection.setAutoCommit(false)
    ps.execute()
    connection.rollback()
    connection.close()
  }
}
