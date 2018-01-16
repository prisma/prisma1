package cool.graph.stub

import scala.collection.SortedMap
import scala.util.parsing.json.{JSONType, JSON}

object StubMatching {
  sealed trait MatchResult {
    def isMatch: Boolean
    def rank: Int
    def stub: Stub
    def noMatchMessage: String
  }
  case class Match(rank: Int, stub: Stub) extends MatchResult {
    def isMatch = rank > 0

    override def noMatchMessage: String = throw new NoSuchElementException("Match.noMatchMessage")
  }
  case class DoesNotMatch(rank: Int, reason: NoMatchReason) extends MatchResult {
    def isMatch = false
    def stub    = throw new NoSuchElementException("DoesNotMatch.stub")

    override def noMatchMessage: String = reason.message
  }
  trait NoMatchReason {
    def message: String
  }
  case class MethodDoesNotMatch(stub: Stub, request: StubRequest) extends NoMatchReason {
    override def message: String = s"expected request method [${stub.httpMethod}], but got: [${request.httpMethod}}]"
  }
  case class PathDoesNotMatch(stub: Stub, request: StubRequest) extends NoMatchReason {
    override def message: String = s"expected request path [${stub.path}], but got: [${request.path}]"
  }
  case class QueryStringDoesNotMatch(missingParams: SortedMap[String, Any]) extends NoMatchReason {
    override def message: String = s"request is missing the following params ${missingParams.toList}"
  }
  case class BodyDoesNotMatch(stub: Stub, request: StubRequest) extends NoMatchReason {
    override def message: String = s"expected request body [${stub.body}], but got: [${request.body}]"
  }

  def matchStubs(stubRequest: StubRequest, stubs: List[Stub]): List[MatchResult] = {
    val sortedCandidates: List[MatchResult] = stubs
      .map { stub =>
        StubMatching.matchStub(stub, stubRequest)
      }
      .sortBy(_.rank)
      .reverse
    sortedCandidates
  }

  def matchStub(stub: Stub, request: StubRequest): MatchResult = {
    val methodMatches    = doesMethodMatch(stub, request)
    val pathMatches      = doesPathMatch(stub, request)
    val queryParamsMatch = doesQueryStringMatch(stub, request)
    val bodyMatches      = doesBodyMatch(stub, request)
    val matches          = List(methodMatches, pathMatches, queryParamsMatch, bodyMatches)

    val minimalRequirements = matches.forall(_.isLeft)
    val score               = matches.map(_.left.getOrElse(0)).sum

    if (minimalRequirements) {
      Match(rank = score, stub = stub)
    } else {
      val firstNoMatchReason = matches.find(_.isRight).get.right.get
      DoesNotMatch(rank = score, reason = firstNoMatchReason)
    }
  }

  def doesMethodMatch(stub: Stub, request: StubRequest): Either[Int, MethodDoesNotMatch] = {
    if (stub.httpMethod.equalsIgnoreCase(request.httpMethod)) {
      Left(1)
    } else {
      Right(MethodDoesNotMatch(stub, request))
    }
  }

  def doesPathMatch(stub: Stub, request: StubRequest): Either[Int, PathDoesNotMatch] = {
    if (stub.path.equalsIgnoreCase(request.path)) {
      Left(1)
    } else {
      Right(PathDoesNotMatch(stub, request))
    }
  }

  def doesQueryStringMatch(stub: Stub, request: StubRequest): Either[Int, QueryStringDoesNotMatch] = {
    val score = request.queryMap.foldLeft(0) {
      case (acc, requestQueryParam) =>
        val stubContainsQueryParam = queryContainsPair(stub.querySortedMap, requestQueryParam)
        if (stubContainsQueryParam) {
          acc + 1
        } else {
          acc
        }
    }
    if (requestContainsAllStubParams(stub, request)) {
      Left(score)
    } else {
      val missingParams = stub.querySortedMap -- request.querySortedMap.keys
      Right(QueryStringDoesNotMatch(missingParams))
    }
  }

  def requestContainsAllStubParams(stub: Stub, request: StubRequest): Boolean = {
    stub.queryMap.forall { stubQueryParam =>
      queryContainsPair(request.querySortedMap, stubQueryParam)
    }
  }

  def queryContainsPair(queryPairs: Iterable[(String, Any)], testPair: (String, Any)): Boolean = {
    queryPairs.exists { currentPair =>
      currentPair.toString == testPair.toString
    }
  }

  def doesBodyMatch(stub: Stub, request: StubRequest): Either[Int, BodyDoesNotMatch] = {
    request.isPostOrPatch && stub.shouldCheckBody match {
      case true =>
        val simpleEquals = request.body == stub.body
        lazy val jsonEquals = {
          val requestJson: Option[JSONType] = JSON.parseRaw(request.body)
          val stubJson: Option[JSONType]    = JSON.parseRaw(stub.body)

          (requestJson, stubJson) match {
            case (Some(a), Some(b)) => a == b
            case _                  => false
          }
        }
        if (simpleEquals || jsonEquals) {
          Left(1)
        } else {
          Right(BodyDoesNotMatch(stub, request))
        }
      case _ => // We only check the body if it's a post or patch
        Left(0)
    }
  }
}
