package cool.graph.deploy

import cool.graph.deploy.server.{ClusterAuth, ClusterAuthImpl}
import cool.graph.shared.models.{Project, Schema}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}

class ClusterAuthSpec extends FlatSpec with Matchers {
  "Grant with wildcard for workspace, service and stage" should "give access to any service and stage" in {
    val auth = new ClusterAuthImpl(Some(publicKey))
    val jwt  = createJwt("""[{"target": "*/*/*", "action": "*"}]""")

    auth.verify(Project("service@stage", "", schema = Schema()), None).isSuccess shouldBe false
    auth.verify(Project("service@stage", "", schema = Schema()), Some(jwt)).isSuccess shouldBe true
  }

  "Grant with wildcard for service and stage" should "give access to any service and stage" in {
    val auth = new ClusterAuthImpl(Some(publicKey))
    val jwt  = createJwt("""[{"target": "*/*", "action": "*"}]""")

    auth.verify(Project("service@stage", "", schema = Schema()), Some(jwt)).isSuccess shouldBe true
  }

  "Grant with invalid target" should "not give access" in {
    val auth    = new ClusterAuthImpl(Some(publicKey))
    val project = Project("service@stage", "", schema = Schema())

    auth.verify(project, Some(createJwt("""[{"target": "/*", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(project, Some(createJwt("""[{"target": "*", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(project, Some(createJwt("""[{"target": "abba", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(project, Some(createJwt("""[{"target": "/*/*/*", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(project, Some(createJwt("""[{"target": "*/*/*/*", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(project, Some(createJwt("""[{"target": "", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(project, Some(createJwt("""[{"target": "/", "action": "*"}]"""))).isSuccess shouldBe false
    auth.verify(project, Some(createJwt("""[{"target": "//", "action": "*"}]"""))).isSuccess shouldBe false
  }

  "Grant with wildcard for stage" should "give access to defined service only" in {
    val auth = new ClusterAuthImpl(Some(publicKey))
    val jwt  = createJwt("""[{"target": "service/*", "action": "*"}]""")

    auth.verify(Project("service@stage", "", schema = Schema()), Some(jwt)).isSuccess shouldBe true
    auth.verify(Project("otherService@stage", "", schema = Schema()), Some(jwt)).isSuccess shouldBe false
  }

  val privateKey =
    """-----BEGIN RSA PRIVATE KEY-----
MIIEogIBAAKCAQEAqRYN98U6f6iLQObNWYlckG/6ro5pF3zApFv/bQBi1V169roi
bHHgwA1/FN0bHJXi+LqAZZQXNORHcNqXnLlqSkrA7ElxgEn6UbsCDKjOo3+ogrJS
K/RrsAFEdlctbMoBpBSNHabvEN4dERu3ahmFkxmbuqmMynD2znUKUTkr4j4aMjED
wkbHUxSVWyzy9ZF6sOFU/H0beQOLdC61VZtZHMNYYxdMTQb4mFpEN2faNhChYcg8
C5xL7C0DeOYR05HYGLrn59i2RldXx4AKs978qQR5IBPJHVVMiSntY0eW4/4A9HoZ
fjQYG5R4jyzp4NiChXRRGZhy7cvn3K7AmGsq0QIDAQABAoIBAB2FPjcN9iKnmHhi
U2PYeZK2GjwznOF+5FtNvJCZSqgZxAgjgzMPxr+BG7jWyY76FEB8v0H80vhnCpoH
cATq0kXaO0iFog1V3SA72CXBqyIcfZ0j6PjHma2G6x8GJWYi9pphBCozJPX68XQ1
NJaPkiSWifvS8kO96TkucfiwVJsRFvkJNNx38o+0wDUaSLt6R9spsvE/QImIgH2c
lRMDKz66T/SWhLNuloiqJWf2rlb+HYtwROP0n2k39T/cv1surbDf6BJhjHD151YC
mTNTBJ69+XWaUXJRjunDIgrYp9A7A6yZJhh+UYwdLzya5w+A0nOyT9R0ubZpf0IY
CR6YxSkCgYEA9XRsa/MSZq/Zx5vM0+yJrNKR4V0CdPUpkjfi/r1bMsz+zdPGDO8O
P9OkraT8l+Pw3tk0sqZI8ERgwiajsZ7WDiE/NTI2WaoB/Lafi4zRdIFT50Y+u08x
dE4H2l9w0fxi8i7Oc4en/Bij4pP7OfJDLFzBMg4w1W1t+wwVlZXpIhMCgYEAsFm1
bQnCyRX6PC/+ZYSOMhIA6uX/JwMXAi62mXklS6Ic5vFwGCtgzhW7UHNF1+sx3t5c
fXF2iy5HKnWcHKOSd6+sLuCONP9w9U/tckRbhyGXPPl5QOR9wmjRtlfyz5/P84eW
YLO66hBNinDr44REHvlLiqUZhvfQwRsS5go0/AsCgYApDF9VbkEVizMQfq2yg0xC
6rQazEMs7BMXsOD1WRV3WXEDWvc0EoZ/hhV0NLNJc4VEv25gsg5goA7OaUfW3IlP
s5+udcdBF31dlez4mYQtx7MQal7zVDshCCuoCW4EsACcH9fG2ljtf/FoYvcQqcMy
GBD3HghsqPBLm6nAamGioQKBgHEMUP1hMHjvmcZTjeVOIEmAuQ3b+sDrfihsAapI
utvNRHHXfGBCDoCN9dIQ00kjAIk6SlgwECoQtJZHZpgFU7Nd7ibu1LqstaDMaA7E
O1hY9DamRlCPKP8jaqxVnNX0QL6AwKmlDcFWSh7hXJYxB+pDLWXniIG5Ax2HWYoW
KPkZAoGAJUhdn/u9Kc81X1ls7myLnYcGkhiYPGSjnawHl38UcI05RnYH81wTtNzA
ui/J74jgPKib7p1WJEFMAGjO7SahlpWl2D2l/HasS7CvkQgvpiqGjOnupdeahwBO
Gn3QrFoGciUYS3ZS83kVtCNJneLoo2dhMOMMW/FEqbj8S4fQrH0=
-----END RSA PRIVATE KEY-----"""
  val publicKey =
    """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqRYN98U6f6iLQObNWYlc
kG/6ro5pF3zApFv/bQBi1V169roibHHgwA1/FN0bHJXi+LqAZZQXNORHcNqXnLlq
SkrA7ElxgEn6UbsCDKjOo3+ogrJSK/RrsAFEdlctbMoBpBSNHabvEN4dERu3ahmF
kxmbuqmMynD2znUKUTkr4j4aMjEDwkbHUxSVWyzy9ZF6sOFU/H0beQOLdC61VZtZ
HMNYYxdMTQb4mFpEN2faNhChYcg8C5xL7C0DeOYR05HYGLrn59i2RldXx4AKs978
qQR5IBPJHVVMiSntY0eW4/4A9HoZfjQYG5R4jyzp4NiChXRRGZhy7cvn3K7AmGsq
0QIDAQAB
-----END PUBLIC KEY-----"""

  def createJwt(grants: String) = {
    import pdi.jwt.{Jwt, JwtAlgorithm}

    val claim = s"""{"grants": $grants}"""

    Jwt.encode(claim = claim, algorithm = JwtAlgorithm.RS256, key = privateKey)
  }
}
