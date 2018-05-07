package com.prisma.deploy.migration.validation

import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.immutable.Seq

class SchemaSyntaxValidatorSpec extends WordSpecLike with Matchers {

  "succeed if the schema is fine" in {
    val schema =
      """
        |type Todo {
        |  title: String
        |}
      """.stripMargin
    SchemaSyntaxValidator(schema).validate should be(empty)
  }

  "fail if the schema is syntactically incorrect" in {
    val schema =
      """
        |type Todo  {
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
        |type Todo {
        |  title: String
        |  comments: [Comment!]!
        |}
        |
        |type Comment {
        |  text: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(0))
  }

  "fail if ambiguous relation fields do not specify the relation directive" in {
    val schema =
      """
        |type Todo {
        |  title: String
        |  comments: [Comment!]!
        |  comments2: [Comment!]!
        |}
        |
        |type Comment {
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

  "fail if ambiguous relation fields specify the same relation name" in {
    val schema =
      """
        |type Todo {
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |  comments2: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  todo: Todo! @relation(name: "TodoToComments")
        |  todo2: Todo! @relation(name: "TodoToComments")
        |  text: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(4))
    result.forall(_.description.contains("A relation directive cannot appear more than twice.")) should be(true)
  }

  // TODO: the backwards field should not be required here.
  "succeed if ambiguous relation fields specify the relation directive" in {
    val schema =
      """
        |type Todo {
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments1")
        |  comments2: [Comment!]! @relation(name: "TodoToComments2")
        |}
        |
        |type Comment {
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
        |type Todo  {
        |  title: String @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  bla: String
        |}
        """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    result.head.`type` should equal("Todo")
    result.head.field should equal(Some("title"))
    result.head.description should include("cannot specify the `@relation` directive.")
  }

  "succeed if a relation name specifies the relation directive only once" in {
    val schema =
      """
        |type Todo {
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  bla: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(0))
  }

  "succeed if a relation directive specifies a valid onDelete attribute" in {
    val schema =
      """
        |type Todo {
        |  title: String
        |  comments1: [Comment1!]! @relation(name: "TodoToComments1", onDelete: CASCADE)
        |  comments2: [Comment2!]! @relation(name: "TodoToComments2", onDelete: SET_NULL)
        |  comments3: [Comment3!]! @relation(name: "TodoToComments3")
        |}
        |
        |type Comment1 {
        |  bla: String
        |}
        |type Comment2 {
        |  bla: String
        |}
        |type Comment3 {
        |  bla: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(0))
  }

  "fail if a relation directive specifies an invalid onDelete attribute" in {
    val schema =
      """
        |type Todo {
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments", onDelete: INVALID)
        |}
        |
        |type Comment {
        |  bla: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    result.head.description should include("not a valid value for onDelete")
  }

  // TODO: adapt
  "succeed if a relation gets renamed" in {
    val schema =
      """
        |type Todo {
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToCommentsNew", oldName: "TodoToComments")
        |}
        |
        |type Comment {
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
        |type Todo {
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
        |type Todo {
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  bla: String
        |}
        |
        |type Author {
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
        |type Todo {
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
        |  bla: String
        |}
        |
        |type Author {
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
        |type Todo {
        |  title: String
        |  comments: [Comment!] @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
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
        |type Todo {
        |  title: String
        |  comments: [Comment!]! @relation(name: "TodoToComments")
        |}
        |
        |type Comment {
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
        |type Todo {
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
      DirectiveRequirement("zero", Seq.empty, Seq.empty),
      DirectiveRequirement("one", Seq(RequiredArg("a", mustBeAString = true)), Seq.empty),
      DirectiveRequirement("two", Seq(RequiredArg("a", mustBeAString = false), RequiredArg("b", mustBeAString = true)), Seq.empty)
    )
    val schema =
      """
        |type Todo {
        |  title: String @zero @one(a: "") @two(a:1, b: "")
        |}
      """.stripMargin

    val result = SchemaSyntaxValidator(schema, directiveRequirements, reservedFieldsRequirements = Vector.empty).validate
    result should have(size(0))
  }

  "fail if a directive misses a required attribute" in {
    val directiveRequirements = Seq(
      DirectiveRequirement("one", Seq(RequiredArg("a", mustBeAString = true)), Seq.empty),
      DirectiveRequirement("two", Seq(RequiredArg("a", mustBeAString = false), RequiredArg("b", mustBeAString = true)), Seq.empty)
    )
    val schema =
      """
        |type Todo {
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
        |type Todo {
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
         |type Todo {
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
        |type Todo {
        |  title: String @default(value: "foo") @default(value: "bar")
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    val error1 = result.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("title"))
    error1.description should include(s"Directives must appear exactly once on a field.")
  }

  "fail if the old defaultValue directive appears on a field" in {
    val schema =
      """
        |type Todo {
        |  title: String @defaultValue(value: "foo")
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    val error1 = result.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("title"))
    error1.description should include(
      s"""You are using a '@defaultValue' directive. Prisma uses '@default(value: "Value as String")' to declare default values.""")
  }

  "fail if an id field does not specify @unique directive" in {
    val schema =
      """
        |type Todo {
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

  "fail if an id field does not match the valid types for a passive connector" in {
    val schema =
      """
        |type Todo {
        |  id: Float
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema, isActive = false).validate
    val error1 = result.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("id"))
    error1.description should include(s"The field `id` is reserved and has to have the format: id: ID! @unique or id: Int! @unique.")
  }

  "not fail if an id field is of type Int for a passive connector" in {
    val schema =
      """
        |type Todo {
        |  id: Int! @unique
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema, isActive = false).validate
    result should have(size(0))
  }

  "not fail if a model does not specify an id field at all" in {
    val schema =
      """
        |type Todo {
        |  title: String
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(0))
  }

  "fail if there is a duplicate enum in datamodel" in {
    val schema =
      """
        |type Todo {
        |  id: ID! @unique
        |}
        |
        |enum Privacy {
        |  A
        |  B
        |}
        |
        |enum Privacy {
        |  C
        |}
      """.stripMargin
    val result = SchemaSyntaxValidator(schema).validate
    result should have(size(1))
    val error1 = result.head
    error1.`type` should equal("Privacy")
    error1.field should equal(None)
    error1.description should include(s"The enum type `Privacy` is defined twice in the schema. Enum names must be unique.")
  }

  def missingDirectiveArgument(directive: String, argument: String) = {
    s"the directive `@$directive` but it's missing the required argument `$argument`"
  }
}
