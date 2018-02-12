package com.prisma.auth

import org.scalatest.{FlatSpec, Matchers}

class AuthSpec extends FlatSpec with Matchers {
  val auth = AuthImpl

  "expiration" should "be encoded and verified correctly" in {
    val secrets = Vector("super-duper-secret")
    val token   = auth.createToken(secrets, expirationInSeconds = 3)
    auth.verify(secrets, token) should be(AuthSuccess)
    Thread.sleep(1000)
    auth.verify(secrets, token) should be(AuthSuccess)
    Thread.sleep(2000) // 3 seconds passed
    auth.verify(secrets, token) should be(AuthFailure)
  }
}
