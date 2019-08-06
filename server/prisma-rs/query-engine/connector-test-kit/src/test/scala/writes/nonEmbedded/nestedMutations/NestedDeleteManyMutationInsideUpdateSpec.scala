package writes.nonEmbedded.nestedMutations

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class NestedDeleteManyMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "A 1-n relation" should "error if trying to use nestedDeleteMany" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val parent1Id = server
        .query(
          """mutation {
          |  createParent(data: {p: "p1"})
          |  {
          |    id
          |  }
          |}""".stripMargin,
          project
        )
        .pathAsString("data.createParent.id")

      val res = server.queryThatMustFail(
        s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parent1Id"}
         |  data:{
         |    p: "p2"
         |    childOpt: {deleteMany: {
         |        where:{c: "c"}
         |    }}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project,
        errorCode = 0,
        errorContains = """ Reason: 'childOpt.deleteMany' Field 'deleteMany' is not defined in the input type 'ChildUpdateOneWithoutParentOptInput'."""
      )
    }
  }

  "a PM to C1!  relation " should "work" in {
    schemaPMToC1req.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      setupData(project)

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      server.query(
        s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: {c_contains:"c"}
         |    }
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(2)

      server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
        """{"data":{"parents":[{"p":"p1","childrenOpt":[]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
    }
  }

  "a PM to C1  relation " should "work" in {
    schemaPMToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      setupData(project)

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      server.query(
        s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: {c_contains:"c"}
         |   }
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(2)

      server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
        """{"data":{"parents":[{"p":"p1","childrenOpt":[]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
    }
  }

  "a PM to CM  relation " should "work" in {
    schemaPMToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      setupData(project)

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      server.query(
        s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: {
         |          c_contains:"c"
         |      }
         |    }
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(2)

      server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
        """{"data":{"parents":[{"p":"p1","childrenOpt":[]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
    }
  }

  "a PM to C1!  relation " should "work with several deleteManys" in {
    schemaPMToC1req.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      setupData(project)

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      server.query(
        s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: [
         |    {
         |        c_contains:"1"
         |    },
         |    {
         |        c_contains:"2"
         |    }
         |    ]}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(2)

      server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
        """{"data":{"parents":[{"p":"p1","childrenOpt":[]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")

    }
  }

  "a PM to C1!  relation " should "work with empty Filter" in {
    schemaPMToC1req.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      setupData(project)

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      server.query(
        s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: [
         |    {}
         |    ]}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(2)

      server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
        """{"data":{"parents":[{"p":"p1","childrenOpt":[]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
    }
  }

  "a PM to C1!  relation " should "not change anything when there is no hit" in {
    schemaPMToC1req.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      setupData(project)

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      server.query(
        s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: [
         |    {
         |        c_contains:"3"
         |    },
         |    {
         |        c_contains:"4"
         |    }
         |    ]}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)
      //dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(4)

      server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
        """{"data":{"parents":[{"p":"p1","childrenOpt":[{"c":"c1","test":null},{"c":"c2","test":null}]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
    }
  }

  private def setupData(project: Project) = {
    server.query(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      create: [{c: "c1"},{c: "c2"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    server.query(
      """mutation {
        |  createParent(data: {
        |    p: "p2"
        |    childrenOpt: {
        |      create: [{c: "c3"},{c: "c4"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )
  }

}
