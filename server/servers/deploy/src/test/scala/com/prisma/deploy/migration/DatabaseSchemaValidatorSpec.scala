package com.prisma.deploy.migration
import com.prisma.deploy.specutils.PassiveDeploySpecBase
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability}
import com.prisma.shared.models.ConnectorCapability.{IntIdCapability, IntrospectionCapability, MigrationsCapability}
import org.scalatest.{Matchers, WordSpecLike}

class DatabaseSchemaValidatorSpec extends WordSpecLike with Matchers with PassiveDeploySpecBase {
  override def doNotRunForCapabilities: Set[ConnectorCapability] = Set.empty
  override def runOnlyForCapabilities                            = Set(MigrationsCapability)

  "it should error if a table is missing" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "a_wrong_table_name"){
         |  id: Int! @id
         |  title: Int!
         |}
       """.stripMargin

    val errors = deploy(dataModel, ConnectorCapabilities(IntIdCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(empty)
    error.description should be("Could not find the table for the model Blog in the database.")
  }

  "it should error if a column is missing" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: Int!
         |}
       """.stripMargin

    val errors = deploy(dataModel, ConnectorCapabilities(IntIdCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(Some("title"))
    error.description should be("Could not find the column for the field title in the database.")
  }

  "a simple column case" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   title text
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   title mediumtext,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: Int!
         |}
       """.stripMargin

    deploy(dataModel, ConnectorCapabilities(IntIdCapability))
  }

  private def deploy(dataModel: String, capabilities: ConnectorCapabilities) = {
    val actualCapabilities = ConnectorCapabilities(capabilities.capabilities ++ Set(IntrospectionCapability))
    val result             = deployThatMustError(dataModel, actualCapabilities, noMigration = true)
    println(result)
    result
  }
}
