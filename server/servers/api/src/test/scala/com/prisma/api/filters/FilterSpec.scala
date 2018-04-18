package com.prisma.api.filters

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class FilterSpec extends FlatSpec with Matchers with ApiBaseSpec {

  val project: Project = SchemaDsl.fromString() { """
                                                   |type User {
                                                   |  id: ID! @unique
                                                   |  unique: Int! @unique
                                                   |  name: String
                                                   |  ride: Vehicle
                                                   |}
                                                   |
                                                   |type Vehicle {
                                                   |  id: ID! @unique
                                                   |  unique: Int! @unique
                                                   |  brand: String
                                                   |  owner: User!
                                                   |  parked: Boolean
                                                   |}
                                                   |
                                                   |type ParkingLot {
                                                   |  id: ID! @unique
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

  "Relation Null filter" should "work" in {

    val filter = "(where: {ride: null})"

    userUniques(filter) should be(Vector(4))
  }

  "AND filter" should "work" in {

    val filter = """(where: {AND:[{unique_gt: 2},{name_starts_with: "P"}]})"""

    userUniques(filter) should be(Vector())
  }

  "OR filter" should "work" in {

    val filter = """(where: {OR:[{unique_gt: 2},{name_starts_with: "P"}]})"""

    userUniques(filter) should be(Vector(1, 3, 4))
  }

  "NOT filter" should "work" in {

    val filter = """(where: {NOT:{name_starts_with: "P"}})"""

    userUniques(filter) should be(Vector(1, 3, 4))
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
