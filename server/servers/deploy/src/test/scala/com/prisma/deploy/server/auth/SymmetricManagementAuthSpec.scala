package com.prisma.deploy.server.auth

import java.time.Instant

import org.scalatest.{FlatSpec, Matchers}

class SymmetricManagementAuthSpec extends FlatSpec with Matchers {

  val testSecret = "test"

  "Grant with wildcard for service and stage" should "give access to any service and stage" in {
    val auth = SymmetricManagementAuth(testSecret)
    val jwt  = createJwt("""[{"target": "*/*", "action": "*"}]""")

    auth.verify("service", "stage", Some(jwt)).isSuccess shouldBe true
  }

  "Grant with invalid target" should "not give access" in {
    val auth  = SymmetricManagementAuth(testSecret)
    val name  = "service"
    val stage = "stage"

    auth.verify(name, stage, Some(createJwt("""[{"target": "/*", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(name, stage, Some(createJwt("""[{"target": "*", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(name, stage, Some(createJwt("""[{"target": "abba", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(name, stage, Some(createJwt("""[{"target": "/*/*", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(name, stage, Some(createJwt("""[{"target": "*/*/*", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(name, stage, Some(createJwt("""[{"target": "", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(name, stage, Some(createJwt("""[{"target": "/", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(name, stage, Some(createJwt("""[{"target": "//", "action": "*"}]"""))).isSuccess shouldBe false
  }

  "Grant with wildcard for stage" should "give access to defined service only" in {
    val auth = SymmetricManagementAuth(testSecret)
    val jwt  = createJwt("""[{"target": "service/*", "action": "*"}]""")

    auth.verify("service", "stage", Some(jwt)).isSuccess shouldBe true
    auth.verify("otherService", "stage", Some(jwt)).isSuccess shouldBe false
  }

  "An expired token" should "not give access" in {
    val auth = SymmetricManagementAuth(testSecret)
    val jwt  = createJwt("""[{"target": "service/*", "action": "*"}]""", expiration = (Instant.now().toEpochMilli / 1000) - 5)

    auth.verify("service", "stage", Some(jwt)).isSuccess shouldBe false
    auth.verify("otherService", "stage", Some(jwt)).isSuccess shouldBe false
  }

  def createJwt(grants: String, expiration: Long = (Instant.now().toEpochMilli / 1000) + 5) = {
    import pdi.jwt.{Jwt, JwtAlgorithm}

    val claim = s"""{"grants": $grants, "exp": $expiration}"""
    Jwt.encode(claim = claim, algorithm = JwtAlgorithm.HS256, key = testSecret)
  }
}
