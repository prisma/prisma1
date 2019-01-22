package com.prisma.deploy.migration
import com.prisma.deploy.specutils.PassiveDeploySpecBase
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability}
import com.prisma.shared.models.ConnectorCapability.{IntIdCapability, MigrationsCapability}
import org.scalatest.{Matchers, WordSpecLike}

class DatabaseSchemaValidatorSpec extends WordSpecLike with Matchers with PassiveDeploySpecBase {
  override def doNotRunForCapabilities: Set[ConnectorCapability] = Set.empty
  override def runOnlyForCapabilities                            = Set(MigrationsCapability)

  "woot" in {
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

    deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability), noMigration = true)
  }
}
