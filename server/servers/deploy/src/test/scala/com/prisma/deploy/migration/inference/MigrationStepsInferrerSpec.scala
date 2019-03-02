package com.prisma.deploy.migration.inference

import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models._
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.shared.schema_dsl.SchemaDsl.SchemaBuilder
import org.scalatest.{FlatSpec, Matchers}

class MigrationStepsInferrerSpec extends FlatSpec with Matchers with DeploySpecBase {

  /**
    * Basic tests
    */
  // due to the way we set up our schemas, the schemainferrer will always create an UpdateModel at the moment.

  "No changes" should "create no migration steps" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }

    val inferrer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames)
    val steps    = inferrer.evaluate()

    steps shouldBe Vector(UpdateModel("Test", "Test"))
  }

  "Creating models" should "create CreateModel and CreateField migration steps" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaDsl.fromBuilder { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }
    val nextProject = SchemaDsl.fromBuilder { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
      schema.model("Test2").field("c", _.String).field("d", _.Int)
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

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
      schema.model("Test2").field("c", _.String).field("d", _.Int)
    }

    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
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

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test2").field("a", _.String).field("b", _.Int)
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 1
    steps.last shouldBe UpdateModel("Test", "Test2")
  }

  "Creating fields" should "create CreateField migration steps" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 2
    steps should contain allOf (UpdateModel("Test", "Test"), CreateField("Test", "b"))
  }

  "Deleting fields" should "create DeleteField migration steps" in {
    val renames = SchemaMapping.empty

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String)
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

    val previousProject = SchemaBuilder() { schema =>
      schema
        .model("Test")
        .field_!("id", _.Cuid, isUnique = true)
        .field("a", _.String)
        .field("b", _.String)
        .field("c", _.String)
        .field("d", _.String)
        .field("e", _.String)
        .field("f", _.String)
    }

    val nextProject = SchemaBuilder() { schema =>
      schema
        .model("Test")
        .field_!("id", _.Cuid, isUnique = true)
        .field("a2", _.String) // Rename
        .field("b", _.Int) // Type change
        .field_!("c", _.String) // Now required
        .field("d", _.String, isList = true) // Now a list
        .field("e", _.String, isUnique = true) // Now unique
        .field("f", _.String) // no change
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
    val previousProject = SchemaBuilder() { schema =>
      schema.model("Comment").field("text", _.String)
      schema
        .model("Todo")
        .field("title", _.String)
    }

    val nextProject = SchemaBuilder() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema
        .model("Todo")
        .field("title", _.String)
        .oneToManyRelation_!("comments", "todo", comment)
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
    val previousProject = SchemaBuilder() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema
        .model("Todo")
        .field("title", _.String)
        .oneToManyRelation_!("comments", "todo", comment)
    }

    val nextProject = SchemaBuilder() { schema =>
      schema.model("Comment").field("text", _.String)
      schema
        .model("Todo")
        .field("title", _.String)
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
    val previousProject = SchemaBuilder() { schema =>
      val comment = schema.model("Comment")
      schema.model("Todo").oneToManyRelation("comments", "todo", comment, relationName = Some("CommentToTodo"))
    }

    val nextProject = SchemaBuilder() { schema =>
      val comment = schema.model("CommentNew")
      schema.model("TodoNew").oneToManyRelation("commentsNew", "todoNew", comment, relationName = Some("CommentNewToTodoNew"))
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
    val relationName = "TodoToComments"
    val previousProject = SchemaBuilder() { schema =>
      val comment = schema.model("Comment")
      val todo    = schema.model("Todo")
      todo.oneToManyRelation("comments", "todo", comment, relationName = Some(relationName))
    }

    val nextProject = SchemaBuilder() { schema =>
      val comment = schema.model("Comment")
      val todo    = schema.model("Todo")
      comment.manyToOneRelation("todo", "comments", todo, relationName = Some(relationName))
    }

    val proposer = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, SchemaMapping.empty)
    val steps    = proposer.evaluate()

    steps should have(size(0))
  }

  "Creating and using Enums" should "create CreateEnum and CreateField migration steps" in {
    val previousProject = SchemaBuilder() { schema =>
      schema.model("Todo")
    }

    val nextProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatus", Vector("Active", "Done"))
      schema
        .model("Todo")
        .field("status", _.Enum, enum = Some(enum))
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

    val previousProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatus", Vector("Active", "Done"))
      schema
        .model("Todo")
        .field("status", _.Enum, enum = Some(enum))
    }

    val nextProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatusNew", Vector("Active", "Done"))
      schema
        .model("Todo")
        .field("status", _.Enum, enum = Some(enum))
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
    val previousProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatus", Vector("Active", "Done"))
      schema
        .model("Todo")
        .field("status", _.Enum, enum = Some(enum))
        .field("status2", _.Enum, enum = Some(enum))
    }

    val nextProject = SchemaBuilder() { schema =>
      schema
        .model("Todo")
        .field("status", _.Enum, enum = Some(schema.enum("TodoStatus", Vector("Active", "AbsolutelyDone")))) // one value changed
        .field("status2", _.Enum, enum = Some(schema.enum("TodoStatus", Vector("Active", "Done")))) // no change
    }

    val steps = MigrationStepsInferrerImpl(previousProject.schema, nextProject.schema, renames).evaluate()

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

    val previousProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatus", Vector("Active", "Done"))
      schema
        .model("Todo")
        .field("status", _.Enum, enum = Some(enum))
    }

    val nextProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatus", Vector("Active", "Done"))
      schema
        .model("Todo")
        .field("someField", _.String)
        .field("status", _.Enum, enum = Some(enum))
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
    val previousProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatus", Vector("Active", "Done"))
      schema
        .model("Todo")
    }

    val nextProject = SchemaBuilder() { schema =>
      schema.model("Todo")
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
