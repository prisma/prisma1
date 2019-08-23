package writes

import org.scalatest.{FlatSpec, Matchers}
import util._

class DefaultValueSpec extends FlatSpec with Matchers with ApiSpecBase {

  "A Create Mutation on a non-list field" should "utilize the defaultValue" in {
    val project = ProjectDsl.fromString {
      """
        |model ScalarModel {
        |  id        String  @id @default(cuid())
        |  reqString String? @default(value: "default")
        |}
      """.stripMargin
    }
    database.setup(project)

    val res = server.query(
      s"""mutation {
         |  createScalarModel(data: {
         |    }
         |  ){
         |  reqString
         |  }
         |}""".stripMargin,
      project = project
    )

    res.toString should be(s"""{"data":{"createScalarModel":{"reqString":"default"}}}""")

    val queryRes = server.query("""{ scalarModels{reqString}}""", project = project)

    queryRes.toString should be(s"""{"data":{"scalarModels":[{"reqString":"default"}]}}""")
  }

  "The default value" should "work for int" in {
    val project = ProjectDsl.fromString {
      """
        |model Service {
        |  id   String @id @default(cuid())
        |  name String
        |  int  Int?   @default(value: 1)
        |}
      """.stripMargin
    }
    database.setup(project)

    val res = server.query(
      s"""mutation createService{
         |  createService(
         |    data:{
         |      name: "issue1820"
         |    }
         |  ){
         |    name
         |    int
         |  }
         |}""".stripMargin,
      project = project
    )

    res.toString should be(s"""{"data":{"createService":{"name":"issue1820","int":1}}}""")
  }

  "The default value" should "work for enums" in {
    val project = ProjectDsl.fromString {
      """
        |enum IsActive{
        |  Yes
        |  No
        |}
        |
        |model Service {
        |  id           String    @id @default(cuid())
        |  name         String
        |  description  String?
        |  unit         String?
        |  active       IsActive? @default(value: Yes)
        |}
      """.stripMargin
    }
    database.setup(project)

    val res = server.query(
      s"""mutation createService{
         |  createService(
         |    data:{
         |      name: "issue1820"
         |    }
         |  ){
         |    name
         |    active
         |  }
         |}""".stripMargin,
      project = project
    )

    res.toString should be(s"""{"data":{"createService":{"name":"issue1820","active":"Yes"}}}""")
  }

}
