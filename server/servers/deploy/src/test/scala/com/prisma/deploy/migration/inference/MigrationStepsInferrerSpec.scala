package com.prisma.deploy.migration.inference

import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedScalarListsCapability
import com.prisma.shared.models._
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MigrationStepsInferrerSpec extends FlatSpec with Matchers with DeploySpecBase {

  val capas: Set[ConnectorCapability] = Set(EmbeddedScalarListsCapability)

  /**
    * Basic tests
    */
  // due to the way we set up our schemas, the schemainferrer will always create an UpdateModel at the moment.

  "No changes" should "create no migration steps" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaDsl.fromStringV11() {
      """type Test {
        |  id: ID! @id
        |  a: String
        |  b: Int
        |}
      """.stripMargin
    }
    val nextProject = previousProject

    val inferrer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames)
    val steps    = inferrer.evaluate()

    steps should be(empty)
  }

  "Creating models" should "create CreateModel and CreateField migration steps" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaDsl.fromStringV11() {
      """
        |type Test {
        |  id: ID! @id
        |  a: String
        |  b: Int
        |}
      """.stripMargin
    }

    val nextProject = SchemaDsl.fromStringV11() {
      """
        |type Test {
        |  id: ID! @id
        |  a: String
        |  b: Int
        |}
        |
        |type Test2 {
        |  id: ID! @id
        |  c: String
        |  d: Int
        |}
      """.stripMargin
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 5
    steps should contain allOf (
      UpdateModel("Test", "Test"),
      CreateModel("Test2"),
      CreateField("Test2", "id"),
      CreateField("Test2", "c"),
      CreateField("Test2", "d")
    )
  }

  "Deleting models" should "create DeleteModel migration steps" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaDsl.fromStringV11() {
      """type Test {
        |  id: ID! @id
        |}
        |
        |type Test2 {
        |  id: ID! @id
        |}
      """.stripMargin
    }

    val nextProject = SchemaDsl.fromStringV11() {
      """
        |type Test {
        |  id: ID! @id
        |}
      """.stripMargin
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 2
    steps should contain allOf (UpdateModel("Test", "Test"), DeleteModel("Test2"))
  }

  "Updating models" should "create UpdateModel migration steps" in {
    val renames = SchemaMapping(
      models = Vector(Mapping(previous = "Test", next = "Test2"))
    )
    val previousProject = SchemaDsl.fromStringV11() {
      """
        |type Test {
        |  id: ID! @id
        |  a: String
        |  b: Int
        |}
      """.stripMargin
    }
    val nextProject = SchemaDsl.fromStringV11() {
      """
        |type Test2 {
        |  id: ID! @id
        |  a: String
        |  b: Int
        |}
      """.stripMargin
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 1
    steps.last shouldBe UpdateModel("Test", "Test2")
  }

  "Creating fields" should "create CreateField migration steps" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaDsl.fromStringV11() {
      """
        |type Test {
        |  id: ID! @id
        |}
      """.stripMargin
    }
    val nextProject = SchemaDsl.fromStringV11() {
      """
        |type Test {
        |  id: ID! @id
        |  b: Int
        |}
      """.stripMargin
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 2
    steps should contain allOf (UpdateModel("Test", "Test"), CreateField("Test", "b"))
  }

  "Deleting fields" should "create DeleteField migration steps" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaDsl.fromStringV11() {
      """
        |type Test {
        |  id: ID! @id
        |  b: Int
        |}
      """.stripMargin
    }
    val nextProject = SchemaDsl.fromStringV11() {
      """
        |type Test {
        |  id: ID! @id
        |}
      """.stripMargin
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 2
    steps should contain allOf (UpdateModel("Test", "Test"), DeleteField("Test", "b"))
  }

  "Updating fields" should "create UpdateField migration steps" in {
    val renames = SchemaMapping(
      fields = Vector(
        FieldMapping("Test", "a", "Test", "a2")
      )
    )

    val previousProject = SchemaDsl.fromStringV11() {
      """
        |type Test {
        |  id: ID! @id
        |  a: String
        |  b: String
        |  c: String
        |  d: String
        |  e: String
        |  f: String
        |}
      """.stripMargin
    }
    val nextProject = SchemaDsl.fromStringV11Capabilities(capas) {
      """
        |type Test {
        |  id: ID! @id
        |  a2: String         # rename
        |  b: Int             # type change
        |  c: String!         # now required
        |  d: [String]        # now a list
        |  e: String @unique  # now unique
        |  f: String          # no change
        |}
      """.stripMargin
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 6
    steps should contain allOf (
      UpdateModel("Test", "Test"),
      UpdateField("Test", "Test", "a", Some("a2")),
      UpdateField("Test", "Test", "b", None),
      UpdateField("Test", "Test", "c", None),
      UpdateField("Test", "Test", "d", None),
      UpdateField("Test", "Test", "e", None)
    )
  }

  "Creating Relations" should "create CreateRelation and CreateField migration steps" in {

    val previousProject = SchemaDsl.fromStringV11Capabilities(capas) {
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  text: String
        |}
      """.stripMargin
    }

    val nextProject = SchemaDsl.fromStringV11Capabilities(capas) {
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment]
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  text: String
        |  todo: Todo
        |}
      """.stripMargin
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, SchemaMapping.empty)
    val steps    = proposer.evaluate()

    steps.length shouldBe 5
    val relationName = nextProject.relations.head.name
    steps should contain allOf (
      UpdateModel("Todo", "Todo"),
      UpdateModel("Comment", "Comment"),
      CreateField(
        model = "Todo",
        name = "comments"
      ),
      CreateField(
        model = "Comment",
        name = "todo"
      ),
      CreateRelation(
        name = relationName
      )
    )
  }

  "Deleting Relations" should "create DeleteRelation and DeleteField migration steps" in {
    val previousProject = SchemaDsl.fromStringV11Capabilities(capas) {
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment]
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  text: String
        |  todo: Todo
        |}
      """.stripMargin
    }

    val nextProject = SchemaDsl.fromStringV11Capabilities(capas) {
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  text: String
        |}
      """.stripMargin
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, SchemaMapping.empty)
    val steps    = proposer.evaluate()

    steps.length should be(5)
    steps should contain allOf (
      UpdateModel("Todo", "Todo"),
      UpdateModel("Comment", "Comment"),
      DeleteField("Todo", "comments"),
      DeleteField("Comment", "todo"),
      DeleteRelation(previousProject.relations.head.name)
    )
  }

  "Updating Relations" should "create UpdateRelation steps (even when there are lots of renames)" in {
    val previousProject = SchemaDsl.fromStringV11Capabilities() {
      """type Todo {
        |  id: ID! @id
        |  comments: [Comment] @relation(name: "CommentToTodo")
        |}
        |
        |type Comment {
        |  id: ID! @id
        |  todo: Todo @relation(name: "CommentToTodo")
        |}
      """.stripMargin
    }

    val nextProject = SchemaDsl.fromStringV11Capabilities() {
      """type TodoNew {
        |  id: ID! @id
        |  commentsNew: [CommentNew] @relation(name: "CommentNewToTodoNew")
        |}
        |
        |type CommentNew {
        |  id: ID! @id
        |  todoNew: TodoNew @relation(name: "CommentNewToTodoNew")
        |}
      """.stripMargin
    }

    val mappings = SchemaMapping(
      models = Vector(
        Mapping(previous = "Todo", next = "TodoNew"),
        Mapping(previous = "Comment", next = "CommentNew")
      ),
      fields = Vector(
        FieldMapping(previousModel = "Todo", previousField = "comments", nextModel = "TodoNew", nextField = "commentsNew"),
        FieldMapping(previousModel = "Comment", previousField = "todo", nextModel = "CommentNew", nextField = "todoNew")
      )
    )

    val inferrer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, mappings)
    val steps    = inferrer.evaluate()

    steps should have(size(5))
    steps should contain(UpdateRelation("CommentToTodo", newName = Some("CommentNewToTodoNew")))
    steps should contain(UpdateModel("Comment", newName = "CommentNew"))
    steps should contain(UpdateModel("Todo", newName = "TodoNew"))
    steps should contain(UpdateField("Comment", "CommentNew", "todo", Some("todoNew")))
    steps should contain(UpdateField("Todo", "TodoNew", "comments", Some("commentsNew")))
  }

  // TODO: this spec probably cannot be fulfilled. And it probably does need to because the NextProjectInferer guarantees that those swaps cannot occur. Though this must be verified by extensive testing.
  "Switching modelA and modelB in a Relation" should "not generate any migration step" ignore {
//    val relationName = "TodoToComments"
//    val previousProject = SchemaBuilder() { schema =>
//      val comment = schema.model("Comment")
//      val todo    = schema.model("Todo")
//      todo.oneToManyRelation("comments", "todo", comment, relationName = Some(relationName))
//    }
//
//    val nextProject = SchemaBuilder() { schema =>
//      val comment = schema.model("Comment")
//      val todo    = schema.model("Todo")
//      comment.manyToOneRelation("todo", "comments", todo, relationName = Some(relationName))
//    }
//
//    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, SchemaMapping.empty)
//    val steps    = proposer.evaluate()
//
//    steps should have(size(0))
  }

  "Creating and using Enums" should "create CreateEnum and CreateField migration steps" in {
    val previousProject = SchemaDsl.fromStringV11Capabilities() {
      """
        |type Todo {
        |  id: ID! @id
        |}
      """.stripMargin
    }

    val nextProject = SchemaDsl.fromStringV11Capabilities() {
      """
        |type Todo {
        |  id: ID! @id
        |  status: TodoStatus
        |}
        |
        |enum TodoStatus {
        |  Active
        |  Done
        |}
      """.stripMargin
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, SchemaMapping.empty)
    val steps    = proposer.evaluate()

    steps should have(size(3))
    steps should contain allOf (
      UpdateModel("Todo", "Todo"),
      CreateEnum("TodoStatus"),
      CreateField(
        model = "Todo",
        name = "status"
      )
    )
  }

  "Updating an Enum Name" should "create one UpdateEnum and one UpdateField for each field using that Enum" in {
    val renames = SchemaMapping(enums = Vector(Mapping(previous = "TodoStatus", next = "TodoStatusNew")))

    val previousProject = SchemaDsl.fromStringV11Capabilities() {
      """
        |type Todo {
        |  id: ID! @id
        |  status: TodoStatus
        |}
        |
        |enum TodoStatus {
        |  Active
        |  Done
        |}
      """.stripMargin
    }

    val nextProject = SchemaDsl.fromStringV11Capabilities() {
      """
        |type Todo {
        |  id: ID! @id
        |  status: TodoStatusNew
        |}
        |
        |enum TodoStatusNew {
        |  Active
        |  Done
        |}
      """.stripMargin
    }

    val steps = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames).evaluate()

    steps.length should be(2)
    steps should contain allOf (UpdateModel("Todo", "Todo"),
    UpdateEnum(
      name = "TodoStatus",
      newName = Some("TodoStatusNew")
    ))
  }

  "Updating the values of an Enum" should "create one UpdateEnum step" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaDsl.fromStringV11Capabilities() {
      """
        |type Todo {
        |  id: ID! @id
        |  status: TodoStatus
        |}
        |
        |enum TodoStatus {
        |  Active
        |  Done
        |}
      """.stripMargin
    }

    val nextProject = SchemaDsl.fromStringV11Capabilities() {
      """
        |type Todo {
        |  id: ID! @id
        |  status: TodoStatus
        |}
        |
        |enum TodoStatus {
        |  Active
        |  AbsolutelyDone
        |}
      """.stripMargin
    }

    val steps = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames).evaluate()

    println(steps)
    steps.length should be(2)
    steps should contain allOf (
      UpdateModel("Todo", "Todo"),
      UpdateEnum(
        name = "TodoStatus",
        newName = None
      )
    )
  }

  // Regression
  "Enums" should "not be displayed as updated if they haven't been touched in a deploy" in {
    val renames = SchemaMapping(
      enums = Vector()
    )

    val previousProject = SchemaDsl.fromStringV11Capabilities() {
      """
        |type Todo {
        |  id: ID! @id
        |  status: TodoStatus
        |}
        |
        |enum TodoStatus {
        |  Active
        |  Done
        |}
      """.stripMargin
    }

    val nextProject = SchemaDsl.fromStringV11Capabilities() {
      """
        |type Todo {
        |  id: ID! @id
        |  status: TodoStatus
        |  someField: String
        |}
        |
        |enum TodoStatus {
        |  Active
        |  Done
        |}
      """.stripMargin
    }

    val steps = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames).evaluate()
    steps.length should be(2)
    steps should contain allOf (
      UpdateModel("Todo", "Todo"),
      CreateField(
        model = "Todo",
        name = "someField"
      )
    )
  }

  "Removing Enums" should "create an DeleteEnum step" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaDsl.fromStringV11Capabilities() {
      """
        |type Todo {
        |  id: ID! @id
        |}
        |
        |enum TodoStatus {
        |  Active
        |  Done
        |}
      """.stripMargin
    }

    val nextProject = SchemaDsl.fromStringV11Capabilities() {
      """
        |type Todo {
        |  id: ID! @id
        |}
      """.stripMargin
    }

    val steps = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames).evaluate()

    steps.length should be(2)
    steps should contain allOf (
      UpdateModel("Todo", "Todo"),
      DeleteEnum(
        name = "TodoStatus"
      )
    )
  }
}
