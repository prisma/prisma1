package writes.nonEmbedded

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.{JoinRelationLinksCapability, NonEmbeddedScalarListCapability, ScalarListsCapability}
import util._

class NonEmbeddedDeleteScalarListsSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(ScalarListsCapability, JoinRelationLinksCapability)

  val scalarListStrategy = if (capabilities.has(NonEmbeddedScalarListCapability)) {
    "@scalarList(strategy: RELATION)"
  } else {
    ""
  }

  "A nested delete  mutation" should "also delete ListTable entries" in {

    val project: Project = SchemaDsl.fromStringV11() {
      s"""type Top {
        | id: ID! @id
        | name: String! @unique
        | bottom: Bottom @relation(link: INLINE)
        |}
        |
        |type Bottom {
        | id: ID! @id
        | name: String! @unique
        | list: [Int] $scalarListStrategy
        |}"""
    }

    database.setup(project)

    server.query(
      """mutation {
        |  createTop(
        |    data: { name: "test", bottom: {create: {name: "test2", list: {set: [1,2,3]}} }}
        |  ){
        |    name
        |    bottom{name, list} 
        |  }
        |}
      """,
      project
    )

    val res = server.query("""mutation{updateTop(where:{name:"test" }data: {bottom: {delete: true}}){name, bottom{name}}}""", project)

    res.toString should be("""{"data":{"updateTop":{"name":"test","bottom":null}}}""")
  }

  "A cascading delete  mutation" should "also delete ListTable entries" taggedAs (IgnoreMongo, IgnoreSQLite, IgnorePostgres, IgnoreMySql) in { // TODO: Remove ignore when cascading again

    val project: Project = SchemaDsl.fromStringV11() {
      s"""type Top {
        | id: ID! @id
        | name: String! @unique
        | bottom: Bottom @relation(name: "Test", onDelete: CASCADE, link: INLINE)
        |}
        |
        |type Bottom {
        | id: ID! @id
        | name: String! @unique
        | list: [Int] $scalarListStrategy
        | top: Top! @relation(name: "Test")
        |}"""
    }

    database.setup(project)

    server.query(
      """mutation {
        |  createTop(
        |    data: { name: "test", bottom: {create: {name: "test2", list: {set: [1,2,3]}} }}
        |  ){
        |    name
        |    bottom{name, list}
        |  }
        |}
      """,
      project
    )

    server.query("""mutation{deleteTop(where:{name:"test"}){name}}""", project)

    server.query("""query{tops{name}}""", project).toString() should be("""{"data":{"tops":[]}}""")
    server.query("""query{bottoms{name}}""", project).toString() should be("""{"data":{"bottoms":[]}}""")
  }
}
