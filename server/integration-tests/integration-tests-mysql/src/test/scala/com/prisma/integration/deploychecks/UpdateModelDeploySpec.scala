package com.prisma.integration.deploychecks

import com.prisma.IgnoreSQLite
import com.prisma.integration.IntegrationBaseSpec
import org.scalatest.{FlatSpec, Matchers}

class UpdateModelDeploySpec extends FlatSpec with Matchers with IntegrationBaseSpec {
  "Updating a model by changing its name" should "succeed even when there are nodes" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        | name: String! @unique
        |}""".stripMargin

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{name:"A"}){name}}""", project).toString should be("""{"data":{"createA":{"name":"A"}}}""")

    val schema2 =
      """type B @rename(oldName: "A"){
        | name: String! @unique
        |}""".stripMargin

    deployServer.deploySchemaThatMustSucceed(project, schema2, 3)
  }

}
