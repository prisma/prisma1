package cool.graph.deploy.migration

import cool.graph.deploy.InternalTestDatabase
import cool.graph.shared.models._
import cool.graph.shared.project_dsl.SchemaDsl.SchemaBuilder
import cool.graph.shared.project_dsl.TestProject
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

class MigrationStepsProposerSpec extends FlatSpec with Matchers with AwaitUtils with InternalTestDatabase with BeforeAndAfterEach {

  /**
    * Basic tests
    */
  "No changes" should "create no migration steps" in {
    val renames = Renames(
      models = Map(
        "Test" -> "Test"
      ),
      enums = Map.empty,
      fields = Map(
        ("Test", "a") -> "a",
        ("Test", "b") -> "b"
      )
    )

    val schemaA = SchemaBuilder()
    schemaA.model("Test").field("a", _.String).field("b", _.Int)

    val schemaB = SchemaBuilder()
    schemaB.model("Test").field("a", _.String).field("b", _.Int)

    val (modelsA, _) = schemaA.build()
    val (modelsB, _) = schemaB.build()

    val previousProject: Project = TestProject().copy(models = modelsA.toList)
    val nextProject              = TestProject().copy(models = modelsB.toList)

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    result.steps shouldBe empty
  }

  "Creating models" should "create CreateModel and CreateField migration steps" in {
    val renames = Renames(
      models = Map(
        "Test"  -> "Test",
        "Test2" -> "Test2"
      ),
      enums = Map.empty,
      fields = Map(
        ("Test", "a")  -> "a",
        ("Test", "b")  -> "b",
        ("Test2", "c") -> "c",
        ("Test2", "d") -> "d"
      )
    )

    val schemaA = SchemaBuilder()
    schemaA.model("Test").field("a", _.String).field("b", _.Int)

    val schemaB = SchemaBuilder()
    schemaB.model("Test").field("a", _.String).field("b", _.Int)
    schemaB.model("Test2").field("c", _.String).field("d", _.Int)

    val (modelsA, _) = schemaA.build()
    val (modelsB, _) = schemaB.build()

    val previousProject: Project = TestProject().copy(models = modelsA.toList)
    val nextProject              = TestProject().copy(models = modelsB.toList)

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    println(result.steps)
    result.steps.length shouldBe 4
    result.steps should contain allOf (
      CreateModel("Test2"),
      CreateField("Test2", "id", "GraphQLID", isRequired = true, isList = false, isUnique = true, None, None, None),
      CreateField("Test2", "c", "String", isRequired = false, isList = false, isUnique = false, None, None, None),
      CreateField("Test2", "d", "Int", isRequired = false, isList = false, isUnique = false, None, None, None)
    )
  }

  "Deleting models" should "create DeleteModel migration steps" in {
    val renames = Renames(
      models = Map(
        "Test" -> "Test"
      ),
      enums = Map.empty,
      fields = Map(
        ("Test", "a")  -> "a",
        ("Test", "b")  -> "b",
        ("Test2", "c") -> "c",
        ("Test2", "d") -> "d"
      )
    )

    val schemaA = SchemaBuilder()
    schemaA.model("Test").field("a", _.String).field("b", _.Int)
    schemaA.model("Test2").field("c", _.String).field("d", _.Int)

    val schemaB = SchemaBuilder()
    schemaB.model("Test").field("a", _.String).field("b", _.Int)

    val (modelsA, _) = schemaA.build()
    val (modelsB, _) = schemaB.build()

    val previousProject: Project = TestProject().copy(models = modelsA.toList)
    val nextProject              = TestProject().copy(models = modelsB.toList)

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    println(result.steps)
    result.steps.length shouldBe 1
    result.steps.last shouldBe DeleteModel("Test2")
  }

  "Updating models" should "create UpdateModel migration steps" in {
    val renames = Renames(
      models = Map("Test2" -> "Test"),
      enums = Map.empty,
      fields = Map(
        ("Test2", "a") -> "a",
        ("Test2", "b") -> "b"
      )
    )

    val schemaA = SchemaBuilder()
    schemaA.model("Test").field("a", _.String).field("b", _.Int)

    val schemaB = SchemaBuilder()
    schemaB.model("Test2").field("a", _.String).field("b", _.Int)

    val (modelsA, _) = schemaA.build()
    val (modelsB, _) = schemaB.build()

    val previousProject: Project = TestProject().copy(models = modelsA.toList)
    val nextProject              = TestProject().copy(models = modelsB.toList)

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    println(result.steps)
    result.steps.length shouldBe 1
    result.steps.last shouldBe UpdateModel("Test", "Test2")
  }

  "Creating fields" should "create CreateField migration steps" in {
    val renames = Renames(
      models = Map("Test" -> "Test"),
      enums = Map.empty,
      fields = Map(
        ("Test", "a") -> "a",
        ("Test", "b") -> "b"
      )
    )

    val schemaA = SchemaBuilder()
    schemaA.model("Test").field("a", _.String)

    val schemaB = SchemaBuilder()
    schemaB.model("Test").field("a", _.String).field("b", _.Int)

    val (modelsA, _) = schemaA.build()
    val (modelsB, _) = schemaB.build()

    val previousProject: Project = TestProject().copy(models = modelsA.toList)
    val nextProject              = TestProject().copy(models = modelsB.toList)

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    println(result.steps)
    result.steps.length shouldBe 1
    result.steps.last shouldBe CreateField("Test", "b", "Int", isRequired = false, isList = false, isUnique = false, None, None, None)
  }

  "Deleting fields" should "create DeleteField migration steps" in {
    val renames = Renames(
      models = Map("Test" -> "Test"),
      enums = Map.empty,
      fields = Map(
        ("Test", "a") -> "a"
      )
    )

    val schemaA = SchemaBuilder()
    schemaA.model("Test").field("a", _.String).field("b", _.Int)

    val schemaB = SchemaBuilder()
    schemaB.model("Test").field("a", _.String)

    val (modelsA, _) = schemaA.build()
    val (modelsB, _) = schemaB.build()

    val previousProject: Project = TestProject().copy(models = modelsA.toList)
    val nextProject              = TestProject().copy(models = modelsB.toList)

    val proposer               = MigrationStepsProposerImpl(previousProject, nextProject, renames)
    val result: MigrationSteps = proposer.evaluate()

    println(result.steps)
    result.steps.length shouldBe 1
    result.steps.last shouldBe DeleteField("Test", "b")
  }

  // Todo: enums, relations
  "Updating fields" should "create UpdateField migration steps" in {
    val renames = Renames(
      models = Map("Test" -> "Test"),
      enums = Map.empty,
      fields = Map(
        ("Test", "a2") -> "a",
        ("Test", "b")  -> "b",
        ("Test", "c")  -> "c",
        ("Test", "d")  -> "d",
        ("Test", "e")  -> "e"
      )
    )

    val schemaA = SchemaBuilder()
    schemaA
      .model("Test")
      .field("a", _.String)
      .field("b", _.Int)
      .field("c", _.String)
      .field("d", _.String)
      .field("e", _.String)

    val schemaB = SchemaBuilder()
    schemaB
      .model("Test")
      .field("a2", _.String) // Rename
      .field("b", _.Int) // Type change
      .field_!("c", _.String) // Now required
      .field("d", _.String, isList = true) // Now a list
      .field("e", _.String, isUnique = true) // Now unique

    val (modelsA, _) = schemaA.build()
    val (modelsB, _) = schemaB.build()

    val previousProject: Project = TestProject().copy(models = modelsA.toList)
    val nextProject              = TestProject().copy(models = modelsB.toList)

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
}
