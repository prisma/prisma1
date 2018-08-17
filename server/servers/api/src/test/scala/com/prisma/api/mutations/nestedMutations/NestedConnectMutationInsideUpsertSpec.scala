package com.prisma.api.mutations.nestedMutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedConnectMutationInsideUpsertSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def doNotRunSuiteForMongo: Boolean = true

  "a one to many relation" should "be connectable by id within an upsert in the create case" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val customer = schema.model("Customer").field_!("name", _.String)
      schema.model("Tenant").field_!("name", _.String).oneToManyRelation("customers", "tenant", customer)
    }
    database.setup(project)

    val tenantId = server.query("""mutation { createTenant(data: {name:"Gustav G"}){ id } }""", project).pathAsString("data.createTenant.id")

    val result = server.query(
      s"""mutation{upsertCustomer(where: {id: "DOESNOTEXIST"}, create: {name: "Paul P", tenant:{connect:{id:"$tenantId"}}}, update: {name: "Paul P"}) {
         |    tenant{name}
         |  }
         |}
      """,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.upsertCustomer.tenant").toString, """{"name":"Gustav G"}""")
  }

  "a one to many relation" should "be connectable by id within an upsert in the update case" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val customer = schema.model("Customer").field_!("name", _.String)
      schema.model("Tenant").field_!("name", _.String).oneToManyRelation("customers", "tenant", customer)
    }
    database.setup(project)

    val tenantId   = server.query("""mutation { createTenant(data: {name:"Gustav G"}){ id } }""", project).pathAsString("data.createTenant.id")
    val customerId = server.query("""mutation { createCustomer(data: {name:"Paul P"}){ id } }""", project).pathAsString("data.createCustomer.id")

    val result = server.query(
      s"""mutation{upsertCustomer(where: {id: "$customerId"}, create: {name: "Bernd B"}, update: {name: "Bernd B",tenant:{connect:{id:"$tenantId"}}}) {
         |    tenant{name}
         |  }
         |}
      """,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.upsertCustomer.tenant").toString, """{"name":"Gustav G"}""")
  }

  "a one to many relation" should "be connectable by unique field within an upsert in the update case" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val customer = schema.model("Customer").field_!("name", _.String, isUnique = true)
      schema.model("Tenant").field_!("name", _.String, isUnique = true).oneToManyRelation("customers", "tenant", customer)
    }
    database.setup(project)

    server.query("""mutation { createTenant(data: {name:"Gustav G"}){ id } }""", project)
    server.query("""mutation { createCustomer(data: {name:"Paul P"}){ id } }""", project)

    val result = server.query(
      s"""mutation{upsertCustomer(where: {name: "Paul P"}, create: {name: "Bernd B"}, update: {name: "Bernd B",tenant:{connect:{name:"Gustav G"}}}) {
         |    tenant{name}
         |  }
         |}
      """,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.upsertCustomer.tenant").toString, """{"name":"Gustav G"}""")
  }

  "a one to many relation" should "throw the correct error for a connect by unique field within an upsert in the update case" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val customer = schema.model("Customer").field_!("name", _.String, isUnique = true)
      schema.model("Tenant").field_!("name", _.String, isUnique = true).oneToManyRelation("customers", "tenant", customer)
    }
    database.setup(project)

    server.query("""mutation { createTenant(data: {name:"Gustav G"}){ id } }""", project)
    server.query("""mutation { createCustomer(data: {name:"Paul P"}){ id } }""", project)

    server.queryThatMustFail(
      s"""mutation{upsertCustomer(where: {name: "Paul P"}, create: {name: "Bernd B"}, update: {name: "Bernd B",tenant:{connect:{name:"DOES NOT EXIST"}}}) {
         |    tenant{name}
         |  }
         |}
      """,
      project,
      3039
    )
  }

}
