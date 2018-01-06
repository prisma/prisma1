package cool.graph.deploy.server

import cool.graph.deploy.schema.InvalidToken
import cool.graph.shared.models.Project

import scala.util.{Failure, Success, Try}
import play.api.libs.json._

trait ClusterAuth {
  def verify(project: Project, authHeaderOpt: Option[String]): Try[Unit]
}

class ClusterAuthImpl(publicKey: Option[String]) extends ClusterAuth {
  override def verify(project: Project, authHeaderOpt: Option[String]): Try[Unit] = Try {
    publicKey match {
      case None =>
        println("warning: cluster authentication is disabled")
        println("To protect your cluster you should provide the environment variable 'CLUSTER_PUBLIC_KEY'")
        ()
      case Some(publicKey) =>
        authHeaderOpt match {
          case None => throw InvalidToken("'Authorization' header not provided")
          case Some(authHeader) =>
            import pdi.jwt.{Jwt, JwtAlgorithm, JwtOptions}

            val jwtOptions = JwtOptions(signature = true, expiration = true)
            val algorithms = Seq(JwtAlgorithm.RS256)
            println(authHeader)
            val claims = Jwt.decodeRaw(token = authHeader.stripPrefix("Bearer "), key = publicKey, algorithms = algorithms, options = jwtOptions)
            println(claims)

            claims match {
              case Failure(exception) => throw InvalidToken(s"claims are invalid: ${exception.getMessage}")
              case Success(claims) =>
                val grants = parseclaims(claims)

                val isSuccess = grants.exists(verifyGrant(project, _))

                if (isSuccess) {
                  ()
                } else {
                  throw InvalidToken(s"Token contained ${grants.length} grants but none satisfied the request")
                }
            }
        }
    }
  }

  private def verifyGrant(project: Project, grant: TokenGrant): Boolean = {
    val (workspace: String, service: String, stage: String) = grant.target.split("/").toVector match {
      case Vector(workspace, service, stage) => (workspace, service, stage)
      case Vector(service, stage)            => ("", service, stage)
      case invalid                           => throw InvalidToken(s"Contained invalid grant '${invalid}'")
    }

    if (service == "" || stage == "") {
      throw InvalidToken(s"Both service and stage must be defined in grant '${grant}'")
    }

    validateService(project, service) && validateStage(project, stage)
  }

  private def validateService(project: Project, servicePart: String) = servicePart match {
    case "*" => true
    case s   => project.projectId.name == s
  }

  private def validateStage(project: Project, stagePart: String) = stagePart match {
    case "*" => true
    case s   => project.projectId.stage == s
  }

  private def parseclaims(claims: String): Vector[TokenGrant] = {

    implicit val TokenGrantReads = Json.reads[TokenGrant]
    implicit val TokenDataReads  = Json.reads[TokenData]

    Json.parse(claims).asOpt[TokenData] match {
      case None         => throw InvalidToken(s"Failed to parse 'grants' claim in '${claims}'")
      case Some(claims) => claims.grants.toVector
    }
  }
}

case class TokenData(grants: Vector[TokenGrant])
case class TokenGrant(target: String, action: String)
