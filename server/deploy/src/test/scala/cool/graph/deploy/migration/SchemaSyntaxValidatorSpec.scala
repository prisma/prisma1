package cool.graph.deploy.migration

import validation.{DirectiveRequirement, RequiredArg, SchemaSyntaxValidator}
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.immutable.Seq

class SchemaSyntaxValidatorSpec extends WordSpecLike with Matchers {

  "succeed if the schema is fine" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |}
      """.stripMargin
    SchemaSyntaxValidator(schema).validate should be(empty)
  }

  "fail if the schema is syntactically incorrect" in {
    val schema =
      """
        |type Todo @model {
        |  title: String
        |  isDone
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    result.head.`type` should equal("Global")
  }

  "succeed if an unambiguous relation field does not specify the relation directive" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!]!
        |}
        |
        |type Comment @model{
        |  text: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(0))
  }

  "fail if ambiguous relation fields do not specify the relation directive" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!]!
        |  comments2: [Comment!]!
        |}
        |
        |type Comment @model{
        |  text: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(2))

    result.head.`type` should equal("Todo")
    result.head.field should equal(Some("comments"))
    result.head.description should include("The relation field `comments` must specify a `@relation` directive")

    result(1).`type` should equal("Todo")
    result(1).field should equal(Some("comments2"))
    result(1).description should include("The relation field `comments2` must specify a `@relation` directive")
  }

  // TODO: adapt when back relations are optional
  "fail if ambiguous relation fields specify the same relation name" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |  comments2: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment @model{
        |  todo: Todo! @relation(name: "TodoToComments")
        |  todo2: Todo! @relation(name: "TodoToComments")
        |  text: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(4))
    result.forall(_.description.contains("A relation directive with a name must appear exactly 2 times.")) should be(true)
  }

  // TODO: adapt when back relations are optional
  "succeed if ambiguous relation fields specify the relation directive" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments1")
        |  comments2: [Comment!]! @relation(name: "TodoToComments2")
        |}
        |
        |type Comment @model{
        |  todo: Todo! @relation(name: "TodoToComments1")
        |  todo2: Todo! @relation(name: "TodoToComments2")
        |  text: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(0))
  }

  "fail if a relation directive appears on a scalar field" in {
    val schema =
      """
        |type Todo @model {
        |  title: String @relation(name: "TodoToComments")
        |}
        |
        |type Comment @model{
        |  bla: String
        |}
        """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    result.head.`type` should equal("Todo")
    result.head.field should equal(Some("title"))
    result.head.description should include("cannot specify the `@relation` directive.")
  }

  // TODO: adapt when back relations are optional
  "fail if a normal relation name does not appear exactly two times" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |  comments2: [Comment!]! @relation(name: "TodoToComments2")
        |}
        |
        |type Comment @model{
        |  bla: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(2))
    result.head.`type` should equal("Todo")
    result.head.field should equal(Some("comments"))
    result.head.description should include("exactly 2 times")
  }

  // TODO: adapt
  "succeed if a relation gets renamed" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToCommentsNew", oldName: "TodoToComments")
        |}
        |
        |type Comment @model{
        |  bla: String
        |  todo: Todo @relation(name: "TodoToComments")
        |}
      """.stripMargin

    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(0))
  }

  // TODO: adapt
  "succeed if a one field self relation does appear only once" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  todo: Todo @relation(name: "OneFieldSelfRelation")
        |  todos: [Todo!]! @relation(name: "OneFieldManySelfRelation")
        |}
      """.stripMargin

    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(0))
  }

  "fail if the relation directive does not appear on the right fields case 1" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment @model{
        |  bla: String
        |}
        |
        |type Author @model{
        |  name: String
        |  todo: Todo @relation(name: "TodoToComments")
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    val first = result.head
    first.`type` should equal("Todo")
    first.field should equal(Some("comments"))
    first.description should include("But the other directive for this relation appeared on the type")
  }

  "fail if the relation directive does not appear on the right fields case 2" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment @model{
        |  bla: String
        |}
        |
        |type Author @model{
        |  name: String
        |  whatever: Comment @relation(name: "TodoToComments")
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(2))
    val first = result.head
    first.`type` should equal("Todo")
    first.field should equal(Some("comments"))
    first.description should include("But the other directive for this relation appeared on the type")

    val second = result(1)
    second.`type` should equal("Author")
    second.field should equal(Some("whatever"))
    second.description should include("But the other directive for this relation appeared on the type")
  }

  "not accept that a many relation field is not marked as required" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!] @relation(name: "TodoToComments")
        |}
        |
        |type Comment @model{
        |  text: String
        |  todo: Todo @relation(name: "TodoToComments")
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
  }

  "succeed if a one relation field is marked as required" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment @model{
        |  text: String
        |  todo: Todo! @relation(name: "TodoToComments")
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(0))
  }

  "fail if schema refers to a type that is not there" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |  comments: [Comment!]!
        |}
      """.stripMargin

    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    val error = result.head
    error.`type` should equal("Todo")
    error.field should equal(Some("comments"))
    error.description should include("no type or enum declaration with that name")
  }

  "NOT fail if the directives contain all required attributes" in {
    val directiveRequirements = Seq(
      DirectiveRequirement("zero", Seq.empty),
      DirectiveRequirement("one", Seq(RequiredArg("a", mustBeAString = true))),
      DirectiveRequirement("two", Seq(RequiredArg("a", mustBeAString = false), RequiredArg("b", mustBeAString = true)))
    )
    val schema =
      """
        |type Todo @model{
        |  title: String @zero @one(a: "") @two(a:1, b: "")
        |}
      """.stripMargin

    val result = SchemaSyntaxValidator(schema, directiveRequirements, reservedFieldsRequirements = Vector.empty).validate
    result should have(size(0))
  }

  "fail if a directive misses a required attribute" in {
    val directiveRequirements = Seq(
      DirectiveRequirement("one", Seq(RequiredArg("a", mustBeAString = true))),
      DirectiveRequirement("two", Seq(RequiredArg("a", mustBeAString = false), RequiredArg("b", mustBeAString = true)))
    )
    val schema =
      """
        |type Todo @model{
        |  title: String @one(a:1) @two(a:1)
        |}
      """.stripMargin

    val result = SchemaSyntaxValidator(schema, directiveRequirements, reservedFieldsRequirements = Vector.empty).validate
    result should have(size(2))
    val error1 = result.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("title"))
    error1.description should include(missingDirectiveArgument("one", "a"))

    val error2 = result(1)
    error2.`type` should equal("Todo")
    error2.field should equal(Some("title"))
    error2.description should include(missingDirectiveArgument("two", "b"))
  }

  "fail if the values in an enum declaration don't begin uppercase" in {
    val schema =
      """
        |type Todo @model{
        |  title: String @one @two(a:"")
        |  status: TodoStatus
        |}
        |enum TodoStatus {
        |  active
        |  done
        |}
      """.stripMargin

    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    val error1 = result.head
    error1.`type` should equal("TodoStatus")
    error1.field should equal(None)
    error1.description should include("uppercase")
  }

  "fail if the values in an enum declaration don't pass the validation" in {
    val longEnumValue = "A" * 192
    val schema =
      s"""
         |type Todo @model{
         |  title: String @one @two(a:"")
         |  status: TodoStatus
         |}
         |enum TodoStatus {
         |  $longEnumValue
         |}
      """.stripMargin

    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    val error1 = result.head
    error1.`type` should equal("TodoStatus")
    error1.field should equal(None)
    error1.description should include(s"$longEnumValue")
  }

  "fail if a directive appears more than once on a field" in {
    val schema =
      """
        |type Todo @model{
        |  title: String @defaultValue(value: "foo") @defaultValue(value: "bar")
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    val error1 = result.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("title"))
    error1.description should include(s"Directives must appear exactly once on a field.")
  }

  "fail if an id field does not specify @unique directive" in {
    val schema =
      """
        |type Todo @model{
        |  id: ID!
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    val error1 = result.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("id"))
    error1.description should include(s"The field `id` is reserved and has to have the format: id: ID! @unique.")
  }

  "not fail if a model does not specify an id field at all" in {
    val schema =
      """
        |type Todo @model{
        |  title: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(0))
  }

  def missingDirectiveArgument(directive: String, argument: String) = {
    s"the directive `@$directive` but it's missing the required argument `$argument`"
  }
}
