package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.ScalarListsCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class CreateMutationSequenceSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(ScalarListsCapability)

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
