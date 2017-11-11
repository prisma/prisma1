package cool.graph.functions.lambda

import cool.graph.shared.functions.lambda.LambdaFunctionEnvironment
import org.scalatest.{FlatSpec, Matchers}
import spray.json.{JsObject, JsString}

class LambdaLogsSpec extends FlatSpec with Matchers {
  "Logs parsing for lambda" should "return the correct aggregation of lines" in {
    val testString =
      """
        |START RequestId:	fb6c1b70-afef-11e7-b988-db72e0053f77	Version: $LATEST
        |2017-10-13T08:24:50.856Z	fb6c1b70-afef-11e7-b988-db72e0053f77	getting event {}
        |2017-10-13T08:24:50.856Z	fb6c1b70-afef-11e7-b988-db72e0053f77	requiring event => {
        |  return {
        |    data: {
        |      message: "msg"
        |    }
        |  }
        |}
        |2017-10-13T08:24:50.857Z	fb6c1b70-afef-11e7-b988-db72e0053f77	{"errorMessage":"Cannot read property 'name' of undefined","errorType":"TypeError","stackTrace":["module.exports.event (/var/task/src/hello2.js:6:47)","executeFunction (/var/task/src/hello2-lambda.js:14:19)","exports.handle (/var/task/src/hello2-lambda.js:9:3)"]}
        |END RequestId: fb6c1b70-afef-11e7-b988-db72e0053f77
        |REPORT RequestId: fb6c1b70-afef-11e7-b988-db72e0053f77	Duration: 1.10 ms	Billed Duration: 100 ms	Memory Size: 128 MB	Max Memory Used: 26 MB
      """.stripMargin

    val testString2 =
      """
        |2017-10-23T10:05:04.839Z	a426c566-b7d9-11e7-a701-7b78cbef51e9	20
        |2017-10-23T10:05:04.839Z	a426c566-b7d9-11e7-a701-7b78cbef51e9	null
        |2017-10-23T10:05:04.839Z	a426c566-b7d9-11e7-a701-7b78cbef51e9	{ big: 'OBJECT' }
      """.stripMargin

    val logs = LambdaFunctionEnvironment.parseLambdaLogs(testString)
    logs should contain(JsObject("2017-10-13T08:24:50.856Z" -> JsString("getting event {}")))
    logs should contain(
      JsObject("2017-10-13T08:24:50.856Z" -> JsString("requiring event => {\n  return {\n    data: {\n      message: \"msg\"\n    }\n  }\n}")))
    logs should contain(JsObject("2017-10-13T08:24:50.857Z" -> JsString(
      """{"errorMessage":"Cannot read property 'name' of undefined","errorType":"TypeError","stackTrace":["module.exports.event (/var/task/src/hello2.js:6:47)","executeFunction (/var/task/src/hello2-lambda.js:14:19)","exports.handle (/var/task/src/hello2-lambda.js:9:3)"]}""")))

    val logs2 = LambdaFunctionEnvironment.parseLambdaLogs(testString2)

    logs.length shouldEqual 3

    logs2.length shouldEqual 3
    logs2 should contain(JsObject("2017-10-23T10:05:04.839Z" -> JsString("20")))
    logs2 should contain(JsObject("2017-10-23T10:05:04.839Z" -> JsString("null")))
    logs2 should contain(JsObject("2017-10-23T10:05:04.839Z" -> JsString("{ big: 'OBJECT' }")))
  }
}
