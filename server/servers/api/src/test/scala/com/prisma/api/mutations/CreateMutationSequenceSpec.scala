package com.prisma.api.mutations

import com.prisma.ConnectorTag
import com.prisma.ConnectorTag.{MongoConnectorTag, SQLiteConnectorTag}
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.ScalarListsCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class CreateMutationSequenceSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(ScalarListsCapability)

  override def doNotRunForConnectors: Set[ConnectorTag] = Set(SQLiteConnectorTag, MongoConnectorTag)

  val project = SchemaDsl.fromStringV11() {
    s"""
      type AtomicRefNumber {
       |  id: Int!
       |    @id(strategy: SEQUENCE)
       |    @sequence(name: "AtomicRefNumberSequence", initialValue: 10000, allocationSize: 1)
       |}
    """.stripMargin
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  //This was producing invalid SQL due to no field values being inserted into the table. I special cased this in NodeActions -> Create
  "A Create Mutation for a Type with only an autoincrementing Id" should "work" in {

    val res = server.query(
      s"""
         |mutation {
         |  createAtomicRefNumber {
         |    id
         |  }
         |}
       """.stripMargin,
      project = project
    )

    res.toString should be(s"""{"data":{"createAtomicRefNumber":{"id":10000}}}""")
  }
}
