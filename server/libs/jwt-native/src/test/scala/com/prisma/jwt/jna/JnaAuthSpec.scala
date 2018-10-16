package com.prisma.jwt.jna

import com.prisma.jwt.{Algorithm, Auth}
import org.scalatest.{FlatSpec, Matchers}

class JnaAuthSpec extends FlatSpec with Matchers {
  "HS256 JWT auth" should "work as expected" in {
    val auth    = Auth.jna(algorithm = Algorithm.HS256)
    val secrets = Vector("secret", "1234567890")

//    val tokenWithExp    = auth.createToken(secrets.head, Some(3600)).get
    val tokenWithoutExp = auth.createToken(secrets.last, None).get
//    val expiredToken    = auth.createToken(secrets.head, Some(-5)).get

    println("/////////////////")

//    val verify1Result = auth.verifyToken(tokenWithExp, secrets)
    val verify2Result = auth.verifyToken(tokenWithoutExp, secrets)
//    val verify3Result = auth.verifyToken("someinvalidtoken", secrets)
//    val verify4Result = auth.verifyToken(tokenWithExp, Vector("invalidsecret"))
//    val verify5Result = auth.verifyToken(expiredToken, secrets)

//    println(verify1Result)
    println(verify2Result)
//    println(verify3Result)
//    println(verify4Result)
//    println(verify5Result)
//
//    verify1Result.isSuccess should be(true)
    verify2Result.isSuccess should be(true)
//    verify3Result.isFailure should be(true)
//    verify4Result.isFailure should be(true)
//    verify5Result.isFailure should be(true)
  }
}
