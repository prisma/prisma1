package cool.graph.auth

import pdi.jwt.{Jwt, JwtAlgorithm, JwtOptions}

trait Auth {
  def verify(secrets: Vector[String], authHeader: String): AuthResult
}

sealed trait AuthResult {
  def isSuccess: Boolean
}
object AuthSuccess extends AuthResult {
  override def isSuccess = true
}
object AuthFailure extends AuthResult {
  override def isSuccess = false
}

object AuthImpl extends Auth {
  private val jwtOptions = JwtOptions(signature = true, expiration = false)
  private val algorithms = Seq(JwtAlgorithm.HS256)

  override def verify(secrets: Vector[String], authHeader: String): AuthResult = {
    val isValid = secrets.exists { secret =>
      val claims = Jwt.decodeRaw(token = authHeader.stripPrefix("Bearer "), key = secret, algorithms = algorithms, options = jwtOptions)
      // todo: also verify claims in accordance with https://github.com/graphcool/framework/issues/1365
      claims.isSuccess
    }
    if (isValid || secrets.isEmpty) AuthSuccess else AuthFailure
  }
}
