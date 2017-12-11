package cool.graph.api

import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class Queries extends FlatSpec with Matchers with ApiBaseSpec {

  "schema" should "include simple API features" in {
    val schema = SchemaDsl()
    schema.model("Car").field("wheelCount", _.Int).field_!("name", _.String).field_!("createdAt", _.DateTime).field_!("updatedAt", _.DateTime)
    val project = schema.buildProject()

    database.setup(project)

    // MUTATIONS

    val newId = server.executeQuerySimple("""mutation { createCar(data: {wheelCount: 7, name: "Sleven"}){id} }""", project).pathAsString("data.createCar.id")
    server
      .executeQuerySimple(s"""mutation { updateCar(by: {id: "${newId}"} wheelCount: 8){wheelCount} }""", project)
      .pathAsLong("data.updateCar.wheelCount") should be(8)
    val idToDelete =
      server.executeQuerySimple("""mutation { createCar(data: {wheelCount: 7, name: "Sleven"}){id} }""", project).pathAsString("data.createCar.id")
    server.executeQuerySimple(s"""mutation { deleteCar(by: {id: "${idToDelete}"}){wheelCount} }""", project).pathAsLong("data.deleteCar.wheelCount") should be(
      7)

    // QUERIES

    server.executeQuerySimple("""{cars{wheelCount}}""", project).pathAsLong("data.cars.[0].wheelCount") should be(8)
    server.executeQuerySimple("""{carsConnection{edges{node{wheelCount}}}}""", project).pathAsLong("data.carsConnection.edges.[0].node.wheelCount") should be(8)
    server.executeQuerySimple(s"""{car(id:"${newId}"){wheelCount}}""", project).pathAsLong("data.car.wheelCount") should be(8)
    server.executeQuerySimple(s"""{node(id:"${newId}"){... on Car { wheelCount }}}""", project).pathAsLong("data.node.wheelCount") should be(8)
  }

  "schema" should "include old nested mutations" in {
    val schema = SchemaDsl()
    val car    = schema.model("Car").field("wheelCount", _.Int).field_!("name", _.String).field_!("createdAt", _.DateTime).field_!("updatedAt", _.DateTime)
    schema.model("Wheel").manyToOneRelation("car", "wheels", car).field_!("size", _.Int).field_!("createdAt", _.DateTime).field_!("updatedAt", _.DateTime)
    val project = schema.buildProject()

    database.setup(project)

    // MUTATIONS

    server
      .executeQuerySimple(
        """mutation { 
          |   createCar(data: {
          |     wheelCount: 7, 
          |     name: "Sleven", 
          |     wheels: { 
          |       create: [{size: 20}, {size: 19}]
          |     }
          |   }){
          |     wheels { size } 
          |     } 
          |}""".stripMargin,
        project
      )
      .pathAsLong("data.createCar.wheels.[0].size") should be(20)

    // QUERIES

    server.executeQuerySimple("""{cars{wheels{size}}}""", project).pathAsLong("data.cars.[0].wheels.[0].size") should be(20)
  }
}
