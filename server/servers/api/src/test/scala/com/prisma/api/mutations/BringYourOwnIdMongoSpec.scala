package com.prisma.api.mutations

import com.prisma.ConnectorTag
import com.prisma.ConnectorTag.MongoConnectorTag
import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.{SchemaBase, SchemaBaseV11}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class BringYourOwnIdMongoSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForConnectors: Set[ConnectorTag] = Set(MongoConnectorTag)

  "A Create Mutation" should "create and return item with own Id" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val res = server.query(
        s"""mutation {
         |  createParent(data: {p: "Parent", id: "5c88f558dee5fb6fe357c7a9"}){p, id}
         |}""",
        project = project
      )

      res.toString should be(s"""{"data":{"createParent":{"p":"Parent","id":"5c88f558dee5fb6fe357c7a9"}}}""")

      server.queryThatMustFail(
        s"""mutation {
         |  createParent(data: {p: "Parent", id: "5c88f558dee5fb6fe357c7a9"}){p, id}
         |}""",
        project = project,
        errorCode = 3010,
        errorContains = "A unique constraint would be violated on Parent. Details: Field name: id"
      )
    }
  }

  "A Create Mutation" should "error for id that is invalid" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.queryThatMustFail(
        s"""mutation {
         |  createParent(data: {p: "Parent", id: 12}){p, id}
         |}""",
        project = project,
        errorCode = 3044,
        errorContains = "You provided an ID that was not a valid MongoObjectId: 12"
      )
    }
  }

  "A Create Mutation" should "error for id that is invalid 2" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.queryThatMustFail(
        s"""mutation {
         |  createParent(data: {p: "Parent", id: true}){p, id}
         |}""",
        project = project,
        errorCode = 0,
        errorContains = "Reason: 'id' String or Int value expected"
      )
    }
  }

  "A Create Mutation" should "error for id that is invalid 3" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.queryThatMustFail(
        s"""mutation {
         |  createParent(data: {p: "Parent", id: "this is probably way to long, lets see what error it throws"}){p, id}
         |}""",
        project = project,
        errorCode = 3044,
        errorContains = "You provided an ID that was not a valid MongoObjectId: this is probably way to long, lets see what error it throws"
      )
    }
  }

  "A Nested Create Mutation" should "create and return item with own Id" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val res = server.query(
        s"""mutation {
         |  createParent(data: {p: "Parent", id: "5c88f558dee5fb6fe357c7a9", childOpt:{create:{c:"Child", id: "5c88f558dee5fb6fe357c7a5"}}}){p, id, childOpt { c, id} }
         |}""",
        project = project
      )

      res.toString should be(
        s"""{"data":{"createParent":{"p":"Parent","id":"5c88f558dee5fb6fe357c7a9","childOpt":{"c":"Child","id":"5c88f558dee5fb6fe357c7a5"}}}}""")

      server.queryThatMustFail(
        s"""mutation {
         |createParent(data: {p: "Parent 2", id: "5c88f558dee5fb6fe357c7a3", childOpt:{create:{c:"Child 2", id: "5c88f558dee5fb6fe357c7a5"}}}){p, id, childOpt { c, id} }
         |}""",
        project = project,
        errorCode = 3010,
        errorContains = "A unique constraint would be violated on Child. Details: Field name: id"
      )
    }
  }

  "A Nested Create Mutation" should "error with invalid id" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.queryThatMustFail(
        s"""mutation {
         |createParent(data: {p: "Parent 2", id: "5c88f558dee5fb6fe357c7a9", childOpt:{create:{c:"Child 2", id: "5c88f558dee5fb6fe357c7a9afafasfsadfasdf"}}}){p, id, childOpt { c, id} }
         |}""",
        project = project,
        errorCode = 3044,
        errorContains = "You provided an ID that was not a valid MongoObjectId: 5c88f558dee5fb6fe357c7a9afafasfsadfasdf"
      )
    }
  }

  "An Upsert Mutation" should "work" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val res = server.query(
        s"""mutation {
         |upsertParent(
         |    where: {id: "5c88f558dee5fb6fe357c7a9"}
         |    create: {p: "Parent 2", id: "5c88f558dee5fb6fe357c7a9"}
         |    update: {p: "Parent 2"}
         |    )
         |  {p, id}
         |}""",
        project = project
      )

      res.toString should be("""{"data":{"upsertParent":{"p":"Parent 2","id":"5c88f558dee5fb6fe357c7a9"}}}""")
    }
  }

  "An Upsert Mutation" should "error with id that is too long" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.queryThatMustFail(
        s"""mutation {
         |upsertParent(
         |    where: {id: "5c88f558dee5fb6fe357c7a9"}
         |    create: {p: "Parent 2", id: "5c88f558dee5fb6fe357c7a9aggfasffgasdgasg"}
         |    update: {p: "Parent 2"}
         |    )
         |  {p, id}
         |}""",
        project = project,
        errorCode = 3044,
        errorContains = "You provided an ID that was not a valid MongoObjectId: 5c88f558dee5fb6fe357c7a9aggfasffgasdgasg"
      )
    }
  }

  "An nested Upsert Mutation" should "work" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val res = server.query(
        s"""mutation {
         |  createParent(data: {p: "Parent", id: "5c88f558dee5fb6fe357c7a9"}){p, id}
         |}""",
        project = project
      )

      res.toString should be(s"""{"data":{"createParent":{"p":"Parent","id":"5c88f558dee5fb6fe357c7a9"}}}""")

      val res2 = server.query(
        s"""mutation {
         |updateParent(
         |    where: {id: "5c88f558dee5fb6fe357c7a9"}
         |    data: {
         |        childOpt: {upsert:{
         |              create:{ id: "5c88f558dee5fb6fe357c7a4", c: "test 3"}
         |              update:{ c: "Does not matter"}
         |        }}
         |      }
         |    )
         |  {p, id, childOpt{c, id}}
         |}""",
        project = project
      )

      res2.toString should be(
        """{"data":{"updateParent":{"p":"Parent","id":"5c88f558dee5fb6fe357c7a9","childOpt":{"c":"test 3","id":"5c88f558dee5fb6fe357c7a4"}}}}""")
    }
  }
}
