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

    "fail validation if the stages in grants don't match" in {
      val signGrant       = Some(JwtGrant("name/stage", "*"))
      val validationGrant = Some(JwtGrant("name/otherstage", "*"))
      val token           = auth.createToken(secrets.head, Some(3600), signGrant).get
      val validation      = auth.verifyToken(token, secrets, validationGrant)

      validation.failed.map(x => println(x.getMessage))
      validation.isSuccess should be(false)
    }

    "fail validation if the names in grants don't match" in {
      val signGrant       = Some(JwtGrant("name/stage", "*"))
      val validationGrant = Some(JwtGrant("name2/stage", "*"))
      val token           = auth.createToken(secrets.head, Some(3600), signGrant).get
      val validation      = auth.verifyToken(token, secrets, validationGrant)

      validation.failed.map(x => println(x.getMessage))
      validation.isSuccess should be(false)
    }

    "fail validation if the actions in grants don't match" in {
      val signGrant       = Some(JwtGrant("name/stage", "create"))
      val validationGrant = Some(JwtGrant("name/stage", "*"))
      val token           = auth.createToken(secrets.head, Some(3600), signGrant).get
      val validation      = auth.verifyToken(token, secrets, validationGrant)

      validation.failed.map(x => println(x.getMessage))
      validation.isSuccess should be(false)
    }

    "Why would anyone sign a JWT with emojis?" in {
      val secret = Vector("\uD83D\uDE80\uD83D\uDE31\uD83E\uDD26\uD83D\uDD1C\uD83D\uDD25")
      val token =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiJjamQ0cWl1emJsNXVrMDE1NG0wY3cxcWJ2IiwiaWF0IjoxNTM3MzY2NTIzLCJleHAiOjE1Mzk5NTg1MjN9.kWVlxIJwcBi3mzJNgnsT4H8dryLdEIz1jPY9HjCLtS4"
      val validation = auth.verifyToken(token, secret)

      validation.failed.map(x => println(x.getMessage))
      validation.isSuccess should be(true)
    }
  }
}
