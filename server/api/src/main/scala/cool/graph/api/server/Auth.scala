package cool.graph.api.server

import cool.graph.api.schema.APIErrors.InvalidToken
import cool.graph.shared.models.Project

import scala.util.Try

trait Auth {
  def verify(project: Project, authHeaderOpt: Option[String]): Try[Unit]
}

object AuthImpl extends Auth {
  override def verify(project: Project, authHeaderOpt: Option[String]): Try[Unit] = Try {
    if (project.secrets.isEmpty) {
      ()
    } else {
      authHeaderOpt match {
        case Some(authHeader) =>
          import pdi.jwt.{Jwt, JwtAlgorithm, JwtOptions}

          val isValid = project.secrets.exists { secret =>
            val jwtOptions = JwtOptions(signature = true, expiration = false)
            val algorithms = Seq(JwtAlgorithm.HS256)
            val claims     = Jwt.decodeRaw(token = authHeader.stripPrefix("Bearer "), key = secret, algorithms = algorithms, options = jwtOptions)

            // todo: also verify claims in accordance with https://github.com/graphcool/framework/issues/1365

            claims.isSuccess
          }

          if (!isValid) throw InvalidToken()

        case None => throw InvalidToken()
      }
    }
  }
}
