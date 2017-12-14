package cool.graph.deploy.migration

import cool.graph.deploy.specutils.DeploySpecBase
import cool.graph.shared.models._
import cool.graph.shared.project_dsl.SchemaDsl.SchemaBuilder
import org.scalatest.{FlatSpec, Matchers}

class MigrationStepsProposerSpec extends FlatSpec with Matchers with DeploySpecBase {

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

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val steps    = proposer.evaluate()

    steps shouldBe empty
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

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 4
    steps should contain allOf (
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

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 1
    steps.last shouldBe DeleteModel("Test2")
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

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 1
    steps.last shouldBe UpdateModel("Test", "Test2")
  }

  "Creating fields" should "create CreateField migration steps" in {
    val renames = Renames.empty

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 1
    steps.last shouldBe CreateField("Test", "b", "Int", isRequired = false, isList = false, isUnique = false, None, None, None)
  }

  "Deleting fields" should "create DeleteField migration steps" in {
    val renames = Renames.empty

    val previousProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String).field("b", _.Int)
    }
    val nextProject = SchemaBuilder() { schema =>
      schema.model("Test").field("a", _.String)
    }

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 1
    steps.last shouldBe DeleteField("Test", "b")
  }

  "Updating fields" should "create UpdateField migration steps" in {
    val renames = Renames(
      fields = Vector(
        FieldRename("Test", "a", "Test", "a2")
      )
    )

    val previousProject = SchemaBuilder() { schema =>
      schema
        .model("Test")
        .field_!("id", _.GraphQLID, isUnique = true)
        .field("a", _.String)
        .field("b", _.String)
        .field("c", _.String)
        .field("d", _.String)
        .field("e", _.String)
    }

    val nextProject = SchemaBuilder() { schema =>
      schema
        .model("Test")
        .field_!("id", _.GraphQLID, isUnique = true, isHidden = true) // Id field hidden
        .field("a2", _.String) // Rename
        .field("b", _.Int) // Type change
        .field_!("c", _.String) // Now required
        .field("d", _.String, isList = true) // Now a list
        .field("e", _.String, isUnique = true) // Now unique
    }

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val steps    = proposer.evaluate()

    steps.length shouldBe 6
    steps should contain allOf (
      UpdateField("Test", "a", Some("a2"), None, None, None, None, None, None, None, None),
      UpdateField("Test", "b", None, Some("Int"), None, None, None, None, None, None, None),
      UpdateField("Test", "c", None, None, Some(true), None, None, None, None, None, None),
      UpdateField("Test", "d", None, None, None, Some(true), None, None, None, None, None),
      UpdateField("Test", "e", None, None, None, None, Some(true), None, None, None, None),
      UpdateField("Test", "id", None, None, None, None, None, Some(true), None, None, None)
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

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, Renames.empty)
    val steps    = proposer.evaluate()

    steps.length shouldBe 3
    val relationName = nextProject.relations.head.name
    steps should contain allOf (
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

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, Renames.empty)
    val steps    = proposer.evaluate()

    steps should have(size(3))
    steps should contain allOf (
      DeleteField("Todo", "comments"),
      DeleteField("Comment", "todo"),
      DeleteRelation(previousProject.relations.head.name)
    )
  }

  "Switching modelA and modelB in a Relation" should "not generate any migration step" in {
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

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, Renames.empty)
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

    val proposer = MigrationStepsProposerImpl(previousProject, nextProject, Renames.empty)
    val steps    = proposer.evaluate()

    steps should have(size(2))
    steps should contain allOf (
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

    val steps = MigrationStepsProposerImpl(previousProject, nextProject, renames).evaluate()

    steps should have(size(2))
    steps should contain allOf (
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
        isHidden = None,
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

    val steps = MigrationStepsProposerImpl(previousProject, nextProject, renames).evaluate()

    steps should have(size(1))
    steps should contain(
      UpdateEnum(
        name = "TodoStatus",
        newName = None,
        values = Some(Vector("Active", "AbsolutelyDone"))
      )
    )
  }

  // Regression
  "Enums" should "not be displayed as updated if they haven't been touched in a deploy" in {
    val renames = Renames(
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

    val steps = MigrationStepsProposerImpl(previousProject, nextProject, renames).evaluate()
    steps should have(size(1))
    steps should contain(
      CreateField(
        model = "Todo",
        name = "someField",
        typeName = "String",
        isRequired = false,
        isList = false,
        isUnique = false,
        relation = None,
        defaultValue = None,
        enum = None
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
      schema.model("Todo")
    }

    val steps = MigrationStepsProposerImpl(previousProject, nextProject, renames).evaluate()

    steps should have(size(1))
    steps should contain(
      DeleteEnum(
        name = "TodoStatus"
      )
    )
  }
}
