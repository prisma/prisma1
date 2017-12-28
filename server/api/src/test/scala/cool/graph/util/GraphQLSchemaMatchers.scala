package cool.graph.util

import org.scalatest.matchers.{MatchResult, Matcher}
import scala.util.{Failure, Success, Try}

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

  case class InputType(name: String, interface: String = "") extends TopLevelSchemaElement {
    val start = {
      if (interface.isEmpty) {
        s"input $name {"
      } else {
        s"input $name implements $interface {"
      }
    }
  }

  case class Enum(name: String) extends TopLevelSchemaElement {
    val start = s"enum $name {"
  }

  case class MatchError(error: String, negatedError: String) extends Throwable

  class SchemaMatcher(element: TopLevelSchemaElement, expectationsOnObject: Vector[String] = Vector.empty) extends Matcher[String] {
    val objectEnd = "}"

    def apply(schema: String) = {
      val result = findObject(schema, element.start).flatMap { obj =>
        val expectationResults: Seq[Try[String]] = expectationsOnObject.map(expectation => findOnObject(obj, expectation))
        expectationResults.find(_.isFailure) match {
          case Some(failed) => failed
          case None         => Success(s"$element meets expectations $expectationsOnObject")
        }
      }

      // todo negated messages need to be better thought through
      result match {
        case Success(msg) =>
          MatchResult(
            matches = result.isSuccess,
            rawFailureMessage = msg,
            rawNegatedFailureMessage = s"[Negated] $msg"
          )

        case Failure(err: MatchError) =>
          MatchResult(
            matches = result.isSuccess,
            rawFailureMessage = err.error,
            rawNegatedFailureMessage = err.negatedError
          )

        case Failure(err: Throwable) =>
          MatchResult(
            matches = result.isSuccess,
            rawFailureMessage = s"Failed with unknown error $err",
            rawNegatedFailureMessage = s"[Negated] Failed with unknown error $err"
          )
      }
    }

    // Returns an object from the schema
    private def findObject(schema: String, objStart: String): Try[String] = {
      val startOfDefinition = schema.lines.dropWhile(!_.startsWith(objStart))

      if (startOfDefinition.isEmpty) {
        Failure(
          MatchError(
            s"The schema did not contain the definition [${element.start}] in the schema: $schema",
            s"The schema contains the definition [${element.start}]"
          ))
      } else {
        val definitionWithOutClosingBrace = startOfDefinition.takeWhile(_ != objectEnd).mkString(start = "", sep = "\n", end = "\n")
        Success(definitionWithOutClosingBrace + objectEnd)
      }
    }

    private def findOnObject(obj: String, expectation: String): Try[String] = {
      obj.lines.map(_.trim).find { line =>
        line.startsWith(expectation)
      } match {
        case Some(line) => Success(line)
        case None =>
          Failure(
            MatchError(
              s"Could not find $expectation on object: $obj",
              s"Found $expectation on object: $obj"
            ))
      }
    }
  }

  def containQuery(expectedQuery: String)                                                           = new SchemaMatcher(Query, Vector(constrainExpectation(expectedQuery)))
  def containMutation(expectedMutation: String)                                                     = new SchemaMatcher(Mutation, Vector(constrainExpectation(expectedMutation)))
  def containType(name: String, interface: String = "", fields: Vector[String] = Vector.empty)      = new SchemaMatcher(Type(name, interface), fields)
  def containInputType(name: String, interface: String = "", fields: Vector[String] = Vector.empty) = new SchemaMatcher(InputType(name, interface), fields)
  def containEnum(name: String)                                                                     = new SchemaMatcher(Enum(name))

  // Ensures that singular and pluralized queries/mutations don't match each other, for example
  private def constrainExpectation(expectation: String): String = {
    if (expectation.contains("(")) {
      expectation
    } else {
      expectation + "("
    }
  }
}
