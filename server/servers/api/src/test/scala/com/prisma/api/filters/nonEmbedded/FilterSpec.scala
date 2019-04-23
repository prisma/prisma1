package com.prisma.api.filters.nonEmbedded

import com.prisma.IgnoreMongo
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class FilterSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val project: Project = SchemaDsl.fromStringV11() { """
                                                   |type User {
                                                   |  id: ID! @id
                                                   |  unique: Int! @unique
                                                   |  name: String
                                                   |  optional: String
                                                   |  ride: Vehicle @relation(link: INLINE)
                                                   |}
                                                   |
                                                   |type Vehicle {
                                                   |  id: ID! @id
                                                   |  unique: Int! @unique
                                                   |  brand: String
                                                   |  owner: User!
                                                   |  parked: Boolean
                                                   |}
                                                   |
                                                   |type ParkingLot {
                                                   |  id: ID! @id
                                                   |  unique: Int! @unique
                                                   |  area: String
                                                   |  size: Float
                                                   |  capacity: Int
                                                   |}""".stripMargin }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
    populate
  }

  "Queries" should "display all items if no filter is given" in {

    val filter = ""

    userUniques(filter) should be(Vector(1, 2, 3, 4))
    vehicleUniques(filter) should be(Vector(1, 2, 3))
    lotUniques(filter) should be(Vector(1, 2))
  }

  "Simple filter" should "work" in {

    val filter = """(where: {name: "John"})"""

    userUniques(filter) should be(Vector(4))
  }

  //todo Null and lists is weird

  "Using _in with null" should "return all nodes with null for that field" in {

    val filter = """(where: {optional_in: null})"""

    userUniques(filter) should be(Vector(1, 2, 3, 4))
  }

  "Using _in with [null]" should "return all nodes with null for that field" ignore {

    val filter = """(where: {optional_in: ["test", null]})"""

    userUniques(filter) should be(Vector(1, 2, 3, 4))
  }

  "Relation Null filter" should "work" ignore { // todo reenable

    val filter = "(where: {ride: null})"

    userUniques(filter) should be(Vector(4))
  }

  "AND filter" should "work" in {

    val filter = """(where: {AND:[{unique_gt: 2},{name_starts_with: "P"}]})"""

    userUniques(filter) should be(Vector())
  }

  "Empty AND filter" should "work" in {

    val filter = """(where: {AND:[]})"""

    userUniques(filter) should be(Vector(1, 2, 3, 4))
  }

  "OR filter" should "work" taggedAs (IgnoreMongo) in {

    val filter = """(where: {OR:[{unique_gt: 2},{name_starts_with: "P"}]})"""

    userUniques(filter) should be(Vector(1, 3, 4))
  }

  "Empty OR filter" should "work" taggedAs (IgnoreMongo) in {

    val filter = """(where: {OR:[]})"""

    userUniques(filter) should be(Vector())
  }

  "Empty NOT filter" should "work" taggedAs (IgnoreMongo) in {

    val filter = """(where: {NOT:[]})"""

    userUniques(filter) should be(Vector(1, 2, 3, 4))
  }

  "NOT filter" should "work" taggedAs (IgnoreMongo) in {

    val filter = """(where: {NOT:{name_starts_with: "P"}})"""

    userUniques(filter) should be(Vector(2, 3, 4))
  }

  "NOT filter" should "work as list" taggedAs (IgnoreMongo) in {

    val filter = """(where: {NOT:[{name_contains: "e"},{unique:1}]})"""

    userUniques(filter) should be(Vector(4))
  }

  "Nested filter" should "work" in {

    val filter = """(where: {ride:{brand_starts_with: "P"}})"""

    userUniques(filter) should be(Vector(1))
  }

  "Starts with filter" should "work" in {

    val filter = """(where: {name_starts_with: "P"})"""

    userUniques(filter) should be(Vector(1))
  }

  "Contains filter" should "work" in {

    val filter = """(where: {name_contains: "n"})"""

    userUniques(filter) should be(Vector(2, 4))
  }

  "Greater than filter" should "work with floats" in {

    val filter = """(where: {size_gt: 100.500000000001})"""

    lotUniques(filter) should be(Vector(1))
  }

  def userUniques(filter: String)    = server.query(s"{ users $filter{ unique } }", project).pathAsSeq("data.users").map(_.pathAsLong("unique")).toVector
  def vehicleUniques(filter: String) = server.query(s"{ vehicles $filter{ unique } }", project).pathAsSeq("data.vehicles").map(_.pathAsLong("unique")).toVector
  def lotUniques(filter: String) =
    server.query(s"{ parkingLots $filter{ unique } }", project).pathAsSeq("data.parkingLots").map(_.pathAsLong("unique")).toVector

  def populate: Unit = {
    server.query(
      s"""mutation createUser{createUser(
         |  data: {
         |    name: "Paul",
         |    unique:1,
         |    ride: {create: {brand: "Porsche",unique:1,parked: true}}
         |})
         |{id}
         |}
      """.stripMargin,
      project
    )

    server.query(
      s"""mutation createUser{createUser(
         |  data: {
         |    name: "Bernd",
         |    unique:2,
         |    ride: {create: {brand: "BMW",unique:2,parked: false}}
         |})
         |{id}
         |}
      """.stripMargin,
      project
    )

    server.query(
      s"""mutation createUser{createUser(
         |  data: {
         |    name: "Michael",
         |    unique:3,
         |    ride: {create: {brand: "Mercedes",unique:3,parked: true}}
         |})
         |{id}
         |}
      """.stripMargin,
      project
    )

    server.query(
      s"""mutation createUser{createUser(
         |  data: {
         |    name: "John",
         |    unique:4
         |})
         |{id}
         |}
      """.stripMargin,
      project
    )

    server.query(
      s"""mutation createParkingLot{createParkingLot(
         |  data: {
         |    area: "PrenzlBerg",
         |    unique:1, 
         |    capacity: 12, 
         |    size: 300.5
         |})
         |{id}
         |}
      """.stripMargin,
      project
    )

    server.query(
      s"""mutation createParkingLot{createParkingLot(
         |  data: {
         |    area: "Moabit",
         |    unique:2, 
         |    capacity: 34, 
         |    size: 100.5
         |})
         |{id}
         |}
      """.stripMargin,
      project
    )

  }
}
