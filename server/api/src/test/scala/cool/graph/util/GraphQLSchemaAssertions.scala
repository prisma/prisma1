package cool.graph.util

import org.scalatest.matchers.{MatchResult, Matcher}

import scala.util.{Failure, Success, Try}

object GraphQLSchemaAssertions extends GraphQLSchemaAssertions

trait GraphQLSchemaAssertions {
  implicit class SchemaAssertions(schemaString: String) {
    val mutationStart = "type Mutation {"
    val queryStart    = "type Query {"
    val objectEnd     = "}"

    def mustContainMutation(name: String): String    = mustContainField(definition(mutationStart), name)
    def mustNotContainMutation(name: String): String = mustNotContainField(definition(mutationStart), name)

    def mustContainQuery(name: String): String    = mustContainField(definition(queryStart), name)
    def mustNotContainQuery(name: String): String = mustNotContainField(definition(queryStart), name)

    def mustContainTypeSignature(signature: String) = schemaString

    private def mustContainField(typeDef: String, field: String): String = {
      val theField = typeDef.lines.map(_.trim).find { line =>
        line.startsWith(field + "(")
      }
      theField match {
        case Some(field) => field
        case None        => sys.error(s"Could not find the field $field in this definition: $typeDef")
      }
    }

    private def mustNotContainField(typeDef: String, field: String): String = {
      val theField = typeDef.lines.map(_.trim).find { line =>
        line.startsWith(field + "(")
      }
      theField match {
        case Some(field) => field
        case None        => sys.error(s"Could not find the field $field in this definition: $typeDef")
      }
    }

    def mustContainInputType(name: String): String = definition(s"input $name {")

    def mustContainType(name: String): String = definition(s"type $name {")

    private def definition(start: String): String = {
      val startOfDefinition = schemaString.lines.dropWhile(_ != start)
      if (startOfDefinition.isEmpty) {
        sys.error(s"The schema did not contain the definition [$start] in the schema: $schemaString")
      }

      val definitionWithOutClosingBrace = startOfDefinition.takeWhile(_ != objectEnd).mkString(start = "", sep = "\n", end = "\n")
      definitionWithOutClosingBrace + objectEnd
    }
  }
}

trait GraphQLSchemaMatchers {

  sealed trait TopLevelSchemaElement {
    val start: String
  }

  object Mutation extends TopLevelSchemaElement {
    val start = "type Mutation {"
  }

  object Query extends TopLevelSchemaElement {
    val start = "type Query {"
  }

  case class Type(name: String, interface: String = "") extends TopLevelSchemaElement {
    val start = {
      if (interface.isEmpty) {
        s"type $name {"
      } else {
        s"type $name implements $interface {"
      }
    }
  }

  case class Enum(name: String) extends TopLevelSchemaElement {
    val start = s"enum $name {"
  }

  class SchemaMatcher(element: TopLevelSchemaElement, expectationOnObject: Option[String] = None) extends Matcher[String] {
    val objectEnd = "}"

    def apply(schema: String) = {
      val result = findObject(schema, element.start).flatMap(findOnObject(_, expectationOnObject))

      MatchResult(
        matches = result.isSuccess,
        result.failed.map(_.getMessage).getOrElse(""),
        result.getOrElse("")
      )
    }

    // Returns an object from the schema
    private def findObject(schema: String, objStart: String): Try[String] = {
      val startOfDefinition = schema.lines.dropWhile(_ != objStart)
      if (startOfDefinition.isEmpty) {
        Failure(new Exception(s"The schema did not contain the definition [${element.start}] in the schema: $schema"))
      } else {
        val definitionWithOutClosingBrace = startOfDefinition.takeWhile(_ != objectEnd).mkString(start = "", sep = "\n", end = "\n")
        Success(definitionWithOutClosingBrace + objectEnd)
      }
    }

    private def findOnObject(obj: String, expectation: Option[String]): Try[String] = {
      obj.lines.map(_.trim).find { line =>
        line.startsWith(expectation.getOrElse(""))
      } match {
        case Some(line) => Success(line)
        case None       => Failure(new Exception(s"Could not find $expectation on object: $obj"))
      }
    }
  }

  def containQuery(expectedQuery: String)               = new SchemaMatcher(Query, Some(expectedQuery))
  def containMutation(expectedMutation: String)         = new SchemaMatcher(Query, Some(expectedMutation))
  def containType(name: String, interface: String = "") = new SchemaMatcher(Type(name, interface))
  def containEnum(name: String)                         = new SchemaMatcher(Enum(name))

  def containField(typeName: String, fieldDef: String) = new SchemaMatcher(Type(typeName), Some(fieldDef))

  //containsTypeWithField(typename, fieldname)
}
