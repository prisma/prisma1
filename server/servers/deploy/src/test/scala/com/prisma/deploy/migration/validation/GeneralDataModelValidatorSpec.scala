package com.prisma.deploy.migration.validation

import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.gc_values.{EnumGCValue, StringGCValue}
import com.prisma.shared.models.ConnectorCapability._
import com.prisma.shared.models.FieldBehaviour._
import com.prisma.shared.models.{OnDelete, RelationStrategy, TypeIdentifier}
import org.scalatest.{Matchers, WordSpecLike}

class GeneralDataModelValidatorSpec extends WordSpecLike with Matchers with DeploySpecBase with DataModelValidationSpecBase {

  "succeed in the simplest case" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |}
      """.stripMargin
    val dataModel = validate(dataModelString)
    dataModel.type_!("Todo").field_!("title").typeIdentifier should equal(TypeIdentifier.String)
  }

  "fail if if is syntactically incorrect" in {
    val dataModelString =
      """
        |type Todo  {
        |  title: String
        |  isDone
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString).head
    error.`type` should equal("Global")
    error.description should include("There's a syntax error in the data model.")
  }

  "fail if a type is missing" in {
    // the relation directive is there as this used to cause an exception
    val dataModelString =
      """
        |type Todo  {
        |  id: ID! @id
        |  title: String
        |  owner: User @relation(name: "Test", onDelete: CASCADE)
        |}
      """.stripMargin
    val error = validateThatMustError(dataModelString)
    error should have(size(1))
    error.head.`type` should equal("Todo")
    error.head.field should equal(Some("owner"))
    error.head.description should equal("The field `owner` has the type `User` but there's no type or enum declaration with that name.")
  }

  "fail if schema refers to a type that is not there" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String
        |  comments: [Comment]
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    val error = errors.head
    error.`type` should equal("Todo")
    error.field should equal(Some("comments"))
    error.description should include("no type or enum declaration with that name")
  }

  "fail if the values in an enum declaration don't begin uppercase" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String @one @two(a:"")
        |  status: TodoStatus
        |}
        |enum TodoStatus {
        |  active
        |  done
        |}
      """.stripMargin

    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    val error1 = errors.head
    error1.`type` should equal("TodoStatus")
    error1.field should equal(None)
    error1.description should include("uppercase")
  }

  "fail if a directive appears more than once on a field" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String @default(value: "foo") @default(value: "bar")
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    println(errors)
    errors should have(size(1))
    val error1 = errors.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("title"))
    error1.description should include(s"Directives must appear exactly once on a field.")
  }

  "fail if an id field does not match the valid types for a passive connector" in {
    val dataModelString =
      """
        |type Todo {
        |  id: Float! @id
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString, Set(IntIdCapability, UuidIdCapability))
    val error1 = errors.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("id"))
    error1.description should include(s"The field `id` is marked as id must have one of the following types: `ID!`,`UUID!`,`Int!`.")
  }

  "not fail if an id field is of type Int for a passive connector" in {
    val dataModelString =
      """
        |type Todo {
        |  myId: Int! @id
        |}
      """.stripMargin
    val dataModel = validate(dataModelString, Set(IntIdCapability))
    dataModel.type_!("Todo").scalarField_!("myId").behaviour should contain(IdBehaviour(IdStrategy.Auto))
  }

  "fail if there is a duplicate enum in datamodel" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
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
    val errors = validateThatMustError(dataModelString)
    errors should have(size(1))
    val error1 = errors.head
    error1.`type` should equal("Privacy")
    error1.field should equal(None)
    error1.description should include(s"The enum type `Privacy` is defined twice in the schema. Enum names must be unique.")
  }

  "fail if there are duplicate fields in a type" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |  title: String!
        |  TITLE: String!
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    errors should have(size(2))
    val error1 = errors.head
    error1.`type` should equal("Todo")
    error1.field should equal(Some("title"))
    error1.description should include(s"The type `Todo` has a duplicate fieldName. The detection of duplicates is performed case insensitive.")
    val error2 = errors(1)
    error2.`type` should equal("Todo")
    error2.field should equal(Some("TITLE"))
    error2.description should include(s"The type `Todo` has a duplicate fieldName. The detection of duplicates is performed case insensitive.")
  }

  "fail if there are duplicate types" in {
    val dataModelString =
      """
        |type Todo {
        |  id: ID! @id
        |}
        |
        |type TODO {
        |  id: ID! @id
        |}
      """.stripMargin
    val errors = validateThatMustError(dataModelString)
    val error1 = errors.head
    error1.`type` should equal("Todo")
    error1.field should equal(None)
    error1.description should include(s"The name of the type `Todo` occurs more than once. The detection of duplicates is performed case insensitive.")
  }

  "enum types must be detected" in {
    val dataModelString =
      """
        |enum Status {
        |  A,
        |  B
        |}
      """.stripMargin

    val dataModel = validate(dataModelString)
    val enum      = dataModel.enum_!("Status")
    enum.values should equal(Vector("A", "B"))
  }
}
