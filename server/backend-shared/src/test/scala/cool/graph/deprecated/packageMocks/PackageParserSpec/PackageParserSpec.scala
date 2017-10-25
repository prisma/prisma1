package cool.graph.deprecated.packageMocks.PackageParserSpec

import cool.graph.deprecated.packageMocks.PackageParser
import org.scalatest.{FlatSpec, Matchers}

class PackageParserSpec extends FlatSpec with Matchers {
  "PackageParser" should "work" in {
    val packageYaml =
      """
        |name: anonymous-auth-provider
        |
        |functions:
        |  authenticateAnonymousUser:
        |    schema: >
        |      type input {
        |        secret: String!
        |      }
        |      type output {
        |        token: String!
        |      }
        |    type: webhook
        |    url: https://some-webhook
        |
        |interfaces:
        |  AnonymousUser:
        |    schema: >
        |      interface AnonymousUser {
        |        secret: String
        |        isVerified: Boolean!
        |      }
        |
        |# This is configured by user when installing
        |install:
        |  - type: mutation
        |    binding: functions.authenticateAnonymousUser
        |    name: authenticateAnonymousCustomer
        |  - type: interface
        |    binding: interfaces.AnonymousUser
        |    onType: Customer
        |
      """.stripMargin

    val importedPackage = PackageParser.parse(packageYaml)

    println(importedPackage)
  }
}
