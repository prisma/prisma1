package com.prisma.api.mutations.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.{JoinRelationLinksCapability, NonEmbeddedScalarListCapability, ScalarListsCapability}
import com.prisma.shared.models.{ConnectorCapability, Project}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NonEmbeddedUpsertListDesignSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability, ScalarListsCapability)
  //region top level upserts

  "An upsert on the top level" should "only execute the scalar lists of the correct create branch" in {

    val project = SchemaDsl.fromString() {
      """type List{
        |   id: ID! @unique
        |   uList: String @unique
        |   listInts: [Int]
        |   todo: Todo
        |}
        |
        |type Todo{
        |   id: ID! @unique
        |   uTodo: String @unique
        |   todoInts: [Int]
        |   list: List
        |}"""
    }

    database.setup(project)

    server
      .query(
        s"""mutation upsertListValues {upsertList(
           |                             where:{uList: "Does not Exist"}
           |                             create:{uList:"A" listInts:{set: [70, 80]}}
           |                             update:{listInts:{set: [75, 85]}}
           |){id}}""".stripMargin,
        project
      )

    val result = server.query(s"""query{lists {uList, listInts}}""", project)
    result.toString should equal("""{"data":{"lists":[{"uList":"A","listInts":[70,80]}]}}""")
    countItems(project, "lists") should be(1)
    countItems(project, "todoes") should be(0)
  }

  "An upsert on the top level" should "be able to reset lists to empty" in {

    val project = SchemaDsl.fromString() {
      """type List{
        |   id: ID! @unique
        |   uList: String @unique
        |   listInts: [Int]
        |   todo: Todo
        |}
        |
        |type Todo{
        |   id: ID! @unique
        |   uTodo: String @unique
        |   todoInts: [Int]
        |   list: List
        |}"""
    }

    database.setup(project)

    server
      .query(
        s"""mutation upsertListValues {upsertList(
           |                             where:{uList: "Does not Exist"}
           |                             create:{uList:"A" listInts:{set: [70, 80]}}
           |                             update:{listInts:{set: [75, 85]}}
           |){id}}""".stripMargin,
        project
      )

    val result = server.query(s"""query{lists {uList, listInts}}""", project)
    result.toString should equal("""{"data":{"lists":[{"uList":"A","listInts":[70,80]}]}}""")
    countItems(project, "lists") should be(1)
    countItems(project, "todoes") should be(0)

    server
      .query(
        s"""mutation upsertListValues {upsertList(
           |                             where:{uList: "A"}
           |                             create:{uList:"A" listInts:{set: [70, 80]}}
           |                             update:{listInts:{set: []}}
           |){id}}""".stripMargin,
        project
      )

    val result2 = server.query(s"""query{lists {uList, listInts}}""", project)
    result2.toString should equal("""{"data":{"lists":[{"uList":"A","listInts":[]}]}}""")
    countItems(project, "lists") should be(1)
    countItems(project, "todoes") should be(0)
  }

  "An upsert on the top level" should "only execute the scalar lists of the correct update branch" in {

    val project = SchemaDsl.fromString() {
      """type List{
        |   id: ID! @unique
        |   uList: String @unique
        |   listInts: [Int]
        |   todo: Todo
        |}
        |
        |type Todo{
        |   id: ID! @unique
        |   uTodo: String @unique
        |   todoInts: [Int]
        |   list: List
        |}"""
    }

    database.setup(project)

    server.query("""mutation {createList(data: {uList: "A" listInts: {set: [1, 2]}}){id}}""", project)

    server
      .query(
        s"""mutation upsertListValues {upsertList(
           |                             where:{uList: "A"}
           |                             create:{uList:"A" listInts:{set: [70, 80]}}
           |                             update:{listInts:{set: [75, 85]}}
           |){id}}""".stripMargin,
        project
      )

    val result = server.query(s"""query{lists {uList, listInts}}""", project)

    result.toString should equal("""{"data":{"lists":[{"uList":"A","listInts":[75,85]}]}}""")

    countItems(project, "lists") should be(1)
    countItems(project, "todoes") should be(0)
  }

  //endregion

  //region nested upserts

  "A nested upsert" should "only execute the nested scalarlists of the correct update branch" in {

    val project = SchemaDsl.fromString() {
      """type List{
        |   id: ID! @unique
        |   uList: String @unique
        |   listInts: [Int]
        |   todoes: [Todo]
        |}
        |
        |type Todo{
        |   id: ID! @unique
        |   uTodo: String @unique
        |   todoInts: [Int]
        |   lists: [List]
        |}"""
    }

    database.setup(project)

    server.query("""mutation {createList(data: {uList: "A" todoes: {create: {uTodo: "B", todoInts: {set: [3, 4]}}}}){id}}""", project)

    server
      .query(
        s"""mutation{updateList(where:{uList: "A"}
        |                       data:{todoes: { upsert:{
        |                               where:{uTodo: "B"}
        |		                            create:{uTodo:"Should Not Matter", todoInts:{set: [300, 400]}}
        |		                            update:{uTodo: "C", todoInts:{set: [700, 800]}}
        |}}
        |}){id}}""".stripMargin,
        project
      )

    val result = server.query(s"""query{lists {uList, todoes {uTodo, todoInts}}}""", project)
    result.toString should equal("""{"data":{"lists":[{"uList":"A","todoes":[{"uTodo":"C","todoInts":[700,800]}]}]}}""")

    countItems(project, "lists") should be(1)
    countItems(project, "todoes") should be(1)
  }

  "A nested upsert" should "only execute the nested scalarlists of the correct create branch" in {

    val project = SchemaDsl.fromString() {
      """type List{
        |   id: ID! @unique
        |   uList: String @unique
        |   listInts: [Int]
        |   todoes: [Todo]
        |}
        |
        |type Todo{
        |   id: ID! @unique
        |   uTodo: String @unique
        |   todoInts: [Int]
        |   lists: [List]
        |}"""
    }

    database.setup(project)

    server.query("""mutation {createList(data: {uList: "A" todoes: {create: {uTodo: "B", todoInts: {set: [3, 4]}}}}){id}}""", project)

    server
      .query(
        s"""mutation{updateList(where:{uList: "A"}
           |                    data:{todoes: { upsert:{
           |                               where:{uTodo: "Does not Matter"}
           |		                           create:{uTodo:"C", todoInts:{set: [100, 200]}}
           |		                           update:{uTodo:"D", todoInts:{set: [700, 800]}}
           |}}
           |}){id}}""".stripMargin,
        project
      )

    val result = server.query(s"""query{lists {uList, todoes {uTodo, todoInts}}}""", project)
    result.toString should equal("""{"data":{"lists":[{"uList":"A","todoes":[{"uTodo":"B","todoInts":[3,4]},{"uTodo":"C","todoInts":[100,200]}]}]}}""")

    countItems(project, "lists") should be(1)
    countItems(project, "todoes") should be(2)
  }

  "A nested upsert" should "only execute the nested scalarlists of the correct create branch for to many relations" in {

    val project = SchemaDsl.fromString() {
      """type List{
        |   id: ID! @unique
        |   uList: String @unique
        |   listInts: [Int]
        |   todoes: [Todo]
        |}
        |
        |type Todo{
        |   id: ID! @unique
        |   uTodo: String @unique
        |   todoInts: [Int]
        |   list: List
        |}"""
    }

    database.setup(project)

    server.query("""mutation {createList(data: {uList: "A" todoes: {create: {uTodo: "B", todoInts: {set: [3, 4]}}}}){id}}""", project)

    server
      .query(
        s"""mutation{updateList(where:{uList: "A"}
           |                    data:{todoes: { upsert:{
           |                               where:{uTodo: "Does not Matter"}
           |		                           create:{uTodo:"C", todoInts:{set: [100, 200]}}
           |		                           update:{uTodo:"D", todoInts:{set: [700, 800]}}
           |}}
           |}){id}}""".stripMargin,
        project
      )

    val result = server.query(s"""query{lists {uList, todoes {uTodo, todoInts}}}""", project)
    result.toString should equal("""{"data":{"lists":[{"uList":"A","todoes":[{"uTodo":"B","todoInts":[3,4]},{"uTodo":"C","todoInts":[100,200]}]}]}}""")

    countItems(project, "lists") should be(1)
    countItems(project, "todoes") should be(2)
  }

  "A nested upsert" should "be able to reset lists to empty" in {

    val project = SchemaDsl.fromString() {
      """type List{
        |   id: ID! @unique
        |   uList: String @unique
        |   listInts: [Int]
        |   todoes: [Todo]
        |}
        |
        |type Todo{
        |   id: ID! @unique
        |   uTodo: String @unique
        |   todoInts: [Int]
        |   list: List
        |}"""
    }

    database.setup(project)

    server.query("""mutation {createList(data: {uList: "A" todoes: {create: {uTodo: "B", todoInts: {set: [3, 4]}}}}){id}}""", project)

    server
      .query(
        s"""mutation{updateList(where:{uList: "A"}
           |                    data:{todoes: { upsert:{
           |                               where:{uTodo: "B"}
           |		                           create:{uTodo:"C", todoInts:{set: [100, 200]}}
           |		                           update:{ todoInts:{set: []}}
           |}}
           |}){id}}""".stripMargin,
        project
      )

    val result = server.query(s"""query{lists {uList, todoes {uTodo, todoInts}}}""", project)
    result.toString should equal("""{"data":{"lists":[{"uList":"A","todoes":[{"uTodo":"B","todoInts":[]}]}]}}""")

    countItems(project, "lists") should be(1)
    countItems(project, "todoes") should be(1)
  }

  //endregion

  def countItems(project: Project, name: String): Int = {
    server.query(s"""query{$name{id}}""", project).pathAsSeq(s"data.$name").length
  }
}
