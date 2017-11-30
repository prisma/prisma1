package cool.graph.deploy.migration

import cool.graph.deploy.InternalTestDatabase
import cool.graph.shared.models._
import cool.graph.shared.project_dsl.SchemaDsl.SchemaBuilder
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

class MigrationStepsProposerSpec extends FlatSpec with Matchers with AwaitUtils with InternalTestDatabase with BeforeAndAfterEach {

  /**
    * Basic tests
    */
  "No changes" should "create no migration steps" in {
    val renames = Renames.empty

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    result.steps shouldBe empty
  }

  "Creating models" should "create CreateModel and CreateField migration steps" in {
    val renames = Renames.empty

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
      schema.model("Test2").field("c", _.String).field("d", _.Int)
    }

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    result.steps.length shouldBe 4
    result.steps should contain allOf (
      CreateModel("Test2"),
      CreateField("Test2", "id", "GraphQLID", isRequired = true, isList = false, isUnique = true, None, None, None),
      CreateField("Test2", "c", "String", isRequired = false, isList = false, isUnique = false, None, None, None),
      CreateField("Test2", "d", "Int", isRequired = false, isList = false, isUnique = false, None, None, None)
    )
  }

  "Deleting models" should "create DeleteModel migration steps" in {
    val renames = Renames.empty

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
      schema.model("Test2").field("c", _.String).field("d", _.Int)
    }

    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    result.steps.length shouldBe 1
    result.steps.last shouldBe DeleteModel("Test2")
  }

  "Updating models" should "create UpdateModel migration steps" in {
    val renames = Renames(
      models = Vector(Rename(previous = "Test", next = "Test2"))
    )

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test2").field("a", _.String).field("b", _.Int)
    }

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    result.steps.length shouldBe 1
    result.steps.last shouldBe UpdateModel("Test", "Test2")
  }

  "Creating fields" should "create CreateField migration steps" in {
    val renames = Renames.empty

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    println(result.steps)
    result.steps.length shouldBe 1
    result.steps.last shouldBe CreateField("Test", "b", "Int", isRequired = false, isList = false, isUnique = false, None, None, None)
  }

  "Deleting fields" should "create DeleteField migration steps" in {
    val renames = Renames.empty

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String)
    }

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    println(result.steps)
    result.steps.length shouldBe 1
    result.steps.last shouldBe DeleteField("Test", "b")
  }

  // Todo: enums, relations
  "Updating fields" should "create UpdateField migration steps" in {
    val renames = Renames(
      fields = Vector(
        FieldRename("Test", "a", "Test", "a2")
      )
    )

    val previousProject = SchemaBuilder() { schema =>
      schema
        .model("Test")
        .field("a", _.String)
        .field("b", _.String)
        .field("c", _.String)
        .field("d", _.String)
        .field("e", _.String)
    }

    val nextProject = SchemaBuilder() { schema =>
      schema
        .model("Test")
        .field("a2", _.String) // Rename
        .field("b", _.Int) // Type change
        .field_!("c", _.String) // Now required
        .field("d", _.String, isList = true) // Now a list
        .field("e", _.String, isUnique = true) // Now unique
    }

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    println(result.steps)
    result.steps.length shouldBe 5
    result.steps should contain allOf (
      UpdateField("Test", "a", Some("a2"), None, None, None, None, None, None, None),
      UpdateField("Test", "b", None, Some("Int"), None, None, None, None, None, None),
      UpdateField("Test", "c", None, None, Some(true), None, None, None, None, None),
      UpdateField("Test", "d", None, None, None, Some(true), None, None, None, None),
      UpdateField("Test", "e", None, None, None, None, Some(true), None, None, None)
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

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, Renames.empty)
    val result: MigrationSteps = proposer.evaluate()

    result.steps.length shouldBe 3
    val relationName = nextProject.relations.head.name
    result.steps should contain allOf (
      CreateField(
        model = "Todo",
        name = "comments",
        typeName = "Relation",
        isRequired = false,
        isList = true,
        isUnique = false,
        relation = Some(relationName),
        defaultValue = None,
        enum = None
      ),
      CreateField(
        model = "Comment",
        name = "todo",
        typeName = "Relation",
        isRequired = true,
        isList = false,
        isUnique = false,
        relation = Some(relationName),
        defaultValue = None,
        enum = None
      ),
      CreateRelation(
        name = relationName,
        leftModelName = "Todo",
        rightModelName = "Comment"
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

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, Renames.empty)
    val result: MigrationSteps = proposer.evaluate()

    result.steps should have(size(3))
    result.steps should contain allOf (
      DeleteField("Todo", "comments"),
      DeleteField("Comment", "todo"),
      DeleteRelation(previousProject.relations.head.name)
    )
  }

  "Creating and using Enums" should "create CreateEnum and CreateField migration steps" in {
    val previousProject = SchemaBuilder() { schema =>
      schema
        .model("Todo")
    }

    val nextProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatus", Vector("Active", "Done"))
      schema
        .model("Todo")
        .field("status", _.Enum, enum = Some(enum))
    }

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, Renames.empty)
    val result: MigrationSteps = proposer.evaluate()

    result.steps should have(size(2))
    result.steps should contain allOf (
      CreateEnum("TodoStatus", Seq("Active", "Done")),
      CreateField(
        model = "Todo",
        name = "status",
        typeName = "Enum",
        isRequired = false,
        isList = false,
        isUnique = false,
        relation = None,
        defaultValue = None,
        enum = Some(nextProject.enums.head.name)
      )
    )
  }

  "Updating an Enum Name" should "create one UpdateEnum and one UpdateField for each field using that Enum" in {
    val renames = Renames(
      enums = Vector(Rename(previous = "TodoStatus", next = "TodoStatusNew"))
    )
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

    val result = MigrationStepsProposerImpl(previousProject, nextProject, renames).evaluate()

    result.steps should have(size(2))
    result.steps should contain allOf (
      UpdateEnum(
        name = "TodoStatus",
        newName = Some("TodoStatusNew"),
        values = None
      ),
      UpdateField(
        model = "Todo",
        name = "status",
        newName = None,
        typeName = None,
        isRequired = None,
        isList = None,
        isUnique = None,
        relation = None,
        defaultValue = None,
        enum = Some(Some("TodoStatusNew"))
      )
    )
  }

  "Updating the values of an Enum" should "create one UpdateEnum step" in {
    val renames = Renames.empty
    val previousProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatus", Vector("Active", "Done"))
      schema
        .model("Todo")
        .field("status", _.Enum, enum = Some(enum))
    }
    val nextProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatus", Vector("Active", "AbsolutelyDone"))
      schema
        .model("Todo")
        .field("status", _.Enum, enum = Some(enum))
    }

    val result = MigrationStepsProposerImpl(previousProject, nextProject, renames).evaluate()

    result.steps should have(size(1))
    result.steps should contain(
      UpdateEnum(
        name = "TodoStatus",
        newName = None,
        values = Some(Vector("Active", "AbsolutelyDone"))
      )
    )
  }

  "Removing Enums" should "create an DeleteEnum step" in {
    val renames = Renames.empty
    val previousProject = SchemaBuilder() { schema =>
      val enum = schema.enum("TodoStatus", Vector("Active", "Done"))
      schema
        .model("Todo")
    }
    val nextProject = SchemaBuilder() { schema =>
      schema
        .model("Todo")
    }

    val result = MigrationStepsProposerImpl(previousProject, nextProject, renames).evaluate()

    result.steps should have(size(1))
    result.steps should contain(
      DeleteEnum(
        name = "TodoStatus"
      )
    )
  }
}
