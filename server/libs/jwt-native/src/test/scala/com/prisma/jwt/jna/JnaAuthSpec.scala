package com.prisma.jwt.jna

import com.prisma.jwt.{Algorithm, Auth, JwtGrant}
import org.scalatest.{Matchers, WordSpec}

class JnaAuthSpec extends WordSpec with Matchers {
  "HS256 JWT auth" should {
    val auth    = Auth.jna(algorithm = Algorithm.HS256)
    val secrets = Vector("secret", "1234567890", "\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02")

    "sign and validate a simple token" in {
      val token      = auth.createToken(secrets.head, None).get
      val validation = auth.verifyToken(token, secrets)

      validation.isSuccess should be(true)
    }

    "sign and validate a simple token with utf-8" in {
      val token      = auth.createToken(secrets.last, None).get
      val validation = auth.verifyToken(token, secrets)

      println(token)

      validation.failed.map(x => println(x.getMessage))
      validation.isSuccess should be(true)
    }

    "sign and validate a token with expiration" in {
      val token      = auth.createToken(secrets.head, Some(3600)).get
      val validation = auth.verifyToken(token, secrets)

      validation.isSuccess should be(true)
    }

    "sign and validate a token with a grant" in {
      val grant      = Some(JwtGrant("*/*", "*"))
      val token      = auth.createToken(secrets.head, None, grant).get
      val validation = auth.verifyToken(token, secrets, grant)

      validation.isSuccess should be(true)
    }

    "sign and validate a token with expiration and a grant" in {
      val grant      = Some(JwtGrant("*/*", "*"))
      val token      = auth.createToken(secrets.head, Some(3600), grant).get
      val validation = auth.verifyToken(token, secrets, grant)

      validation.isSuccess should be(true)
    }

    "fail validation if the token is invalid" in {
      val validation = auth.verifyToken("some_invalid_token\uD83D\uDE02", secrets)

      validation.failed.map(x => println(x.getMessage))
      validation.isSuccess should be(false)
    }

    "fail validation if the secret is invalid" in {
      val token      = auth.createToken(secrets.head, Some(3600)).get
      val validation = auth.verifyToken(token, Vector("invalid_secret\uD83D\uDE02"))

      validation.failed.map(x => println(x.getMessage))
      validation.isSuccess should be(false)
    }

    //      val tokenWithoutExp = auth.createToken(secrets.last, None).get
    //      val expiredToken    = auth.createToken(secrets.head, Some(-5)).get
    //
    //      val verify1Result = auth.verifyToken(tokenWithExp, secrets)
    //      val verify2Result = auth.verifyToken(tokenWithoutExp, secrets)
    //      val verify3Result = auth.verifyToken("someinvalidtoken", secrets)
    //      val verify4Result = auth.verifyToken(tokenWithExp, Vector("invalidsecret"))
    //      val verify5Result = auth.verifyToken(expiredToken, secrets)
    //      val verify6Result = auth.verifyToken(tokenWithExp, secrets, Some(JwtGrant("*/*", "*")))
    //
    //      println(verify1Result)
    //      println(verify2Result)
    //      println(verify3Result)
    //      println(verify4Result)
    //      println(verify5Result)
    //      println(verify6Result)
    //
    //      verify1Result.isSuccess should be(true)
    //      verify2Result.isSuccess should be(true)
    //      verify3Result.isFailure should be(true)
    //      verify4Result.isFailure should be(true)
    //      verify5Result.isFailure should be(true)
    //      verify6Result.isFailure should be(true)
  }
}
