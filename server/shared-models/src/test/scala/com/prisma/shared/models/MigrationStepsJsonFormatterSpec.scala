package com.prisma.shared.models

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

class MigrationStepsJsonFormatterSpec extends FlatSpec with Matchers {

  import MigrationStepsJsonFormatter._

  "CreateRelation" should "be readable in the old format" in {
    val json = Json.parse("""
        |{
        |    "name": "ListToTodo",
        |    "leftModelName": "List",
        |    "rightModelName": "Todo",
        |    "discriminator": "CreateRelation"
        |  }
      """.stripMargin)

    val create = json.as[CreateRelation]
    create.name should equal("ListToTodo")
    create.modelAName should equal("List")
    create.modelBName should equal("Todo")
    create.modelAOnDelete should equal(OnDelete.SetNull)
    create.modelBOnDelete should equal(OnDelete.SetNull)
  }

  "CreateRelation" should "be readable in the new format" in {
    val json = Json.parse("""
                            |{
                            |    "name": "ListToTodo",
                            |    "leftModelName": "List",
                            |    "rightModelName": "Todo",
                            |    "modelAOnDelete": "CASCADE",
                            |    "modelBOnDelete": "SET_NULL",
                            |    "discriminator": "CreateRelation"
                            |  }
                          """.stripMargin)

    val create = json.as[CreateRelation]
    create.name should equal("ListToTodo")
    create.modelAName should equal("List")
    create.modelBName should equal("Todo")
    create.modelAOnDelete should equal(OnDelete.Cascade)
    create.modelBOnDelete should equal(OnDelete.SetNull)
  }

  "UpdateRelation" should "be readable in the old format" in {
    val json = Json.parse("""
                            |{
                            |    "name": "ListToTodo",
                            |    "newName": "ListToTodoNew",
                            |    "modelAId": "List",
                            |    "modelBId": "Todo",
                            |    "discriminator": "UpdateRelation"
                            |  }
                          """.stripMargin)

    val create = json.as[UpdateRelation]
    create.name should equal("ListToTodo")
    create.newName should equal(Some("ListToTodoNew"))
    create.modelAId should equal(Some("List"))
    create.modelBId should equal(Some("Todo"))
    create.modelAOnDelete should equal(None)
    create.modelBOnDelete should equal(None)
  }

  "UpdateRelation" should "be readable in the new format" in {
    val json = Json.parse("""
                            |{
                            |    "name": "ListToTodo",
                            |    "newName": "ListToTodoNew",
                            |    "modelAId": "List",
                            |    "modelBId": "Todo",
                            |    "modelAOnDelete": "CASCADE",
                            |    "modelBOnDelete": "SET_NULL",
                            |    "discriminator": "UpdateRelation"
                            |  }
                          """.stripMargin)

    val create = json.as[UpdateRelation]
    create.name should equal("ListToTodo")
    create.newName should equal(Some("ListToTodoNew"))
    create.modelAId should equal(Some("List"))
    create.modelBId should equal(Some("Todo"))
    create.modelAOnDelete should equal(Some(OnDelete.Cascade))
    create.modelBOnDelete should equal(Some(OnDelete.SetNull))
  }
}
