package com.prisma.api.filters.nonEmbedded

import com.prisma.IgnoreMongo
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.{JoinRelationLinksCapability, SupportsExistingDatabasesCapability}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class SelfRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def doNotRunForCapabilities = Set(SupportsExistingDatabasesCapability)
  override def runOnlyForCapabilities  = Set(JoinRelationLinksCapability)

  val project = SchemaDsl.fromString() {
    """type Human{
    |   id: ID! @unique
    |   name: String
    |   wife: Human @relation(name: "Marriage")
    |   husband: Human @relation(name: "Marriage")
    |   daughters: [Human] @relation(name:"Offspring")
    |   father: Human @relation(name:"Offspring")
    |   stepdaughters: [Human] @relation(name:"Cuckoo")
    |   mother: Human @relation(name:"Cuckoo")
    |   fans: [Human] @relation(name:"Admirers")
    |   rockstars: [Human] @relation(name:"Admirers")
    |   singer: Human @relation(name:"Team")
    |   bandmembers: [Human] @relation(name:"Team")
    |   title: Song
    |}
    |type Song{
    |   id: ID! @unique
    |   title: String
    |   creator: Human 
    |}""".stripMargin
  }

  //All Queries run against the same data that is only set up once before all testcases

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
    setupRockRelations
  }

  "Filter Queries along self relations" should "succeed with one level " in {
    val filterKurt = s"""query{songs (
                                    where: {
                                      creator: {
                                          name: "kurt"
                                            }
                                        }
                                      ) {
                                        title
                                      }
                                    }""".stripMargin

    server.query(filterKurt, project, dataContains = "{\"songs\":[{\"title\":\"My Girl\"},{\"title\":\"Gasag\"}]}")
  }

  "Filter Queries along self relations" should "succeed with two levels" in {

    val filterFrances = s"""query{songs (
                                    where: {
                                      creator: {
                                        daughters_some: {
                                          name: "frances"
                                            }
                                          }
                                        }
                                      ) {
                                        title
                                      }
                                    }""".stripMargin

    server.query(filterFrances, project, dataContains = "{\"songs\":[{\"title\":\"My Girl\"}]}")
  }

  "Filter Queries along OneToOne self relations" should "succeed with two levels 2" in {

    val filterWife = s"""query{songs (where: {
                                          creator :{
                                              wife: {
                                                    name: "yoko"
                                                  }
                                                }
                                              }
                                            ) {
                                              title
                                            }
                                          }""".stripMargin

    server.query(filterWife, project, dataContains = "{\"songs\":[{\"title\":\"Imagine\"}]}")
  }

  "Filter Queries along OneToOne self relations" should "succeed with null filter" in {

    val filterWifeNull = s"""query{songs (
                                          where: {
                                            creator: {
                                              wife: null
                                                }
                                              }
                                            ) {
                                              title
                                            }
                                          }""".stripMargin

    server.query(filterWifeNull, project, dataContains = "{\"songs\":[{\"title\":\"Bicycle\"},{\"title\":\"Gasag\"}]}")
  }

  "Filter Queries along OneToOne self relations" should "succeed with {} filter" in {

    val filterWifeNull = s"""query{songs (
                                          where: {
                                            creator: {
                                              wife: {}
                                                }
                                              }
                                            ) {
                                              title
                                            }
                                          }""".stripMargin

    server.query(filterWifeNull, project, dataContains = "{\"songs\":[{\"title\":\"My Girl\"},{\"title\":\"Imagine\"}]}")
  }

  "Filter Queries along OneToMany self relations" should "fail with  null filter" taggedAs (IgnoreMongo) in {

    val filterDaughterNull = s"""query{songs (
                                          where: {
                                            creator: {
                                              daughters_none: null
                                                }
                                              }
                                            ) {
                                              title
                                            }
                                          }""".stripMargin

    server.queryThatMustFail(filterDaughterNull, project, errorCode = 3033)
  }

  "Filter Queries along OneToMany self relations" should "succeed with empty filter {}" in {

    val filterDaughter = s"""query{songs (
                                          where: {
                                            creator: {
                                              daughters_some: {}
                                                }
                                              }
                                            ) {
                                              title
                                            }
                                          }""".stripMargin

    server.query(filterDaughter, project, dataContains = "{\"songs\":[{\"title\":\"My Girl\"}]}")
  }

  //ManyToMany

  "Filter Queries along ManyToMany self relations" should "succeed with valid filter _some" in {

    val filterGroupies = s"""query{songs (
                                          where: {
                                            creator: {
                                              fans_some: {
                                                    name: "groupie1"
                                                  }
                                                }
                                              }
                                            ) {
                                              title
                                            }
                                          }""".stripMargin

    server.query(filterGroupies, project, dataContains = "{\"songs\":[{\"title\":\"My Girl\"},{\"title\":\"Imagine\"}]}")
  }

  "Filter Queries along ManyToMany self relations" should "succeed with valid filter _none" taggedAs (IgnoreMongo) in {

    val filterGroupies = s"""query{songs (
                                          where: {
                                            creator: {
                                              fans_none: {
                                                    name: "groupie1"
                                                  }
                                                }
                                              }
                                            ) {
                                              title
                                            }
                                          }""".stripMargin

    server.query(filterGroupies, project, dataContains = "{\"songs\":[{\"title\":\"Bicycle\"},{\"title\":\"Gasag\"}]}")
  }

  "Filter Queries along ManyToMany self relations" should "succeed with valid filter _every" taggedAs (IgnoreMongo) in {

    val filterGroupies = s"""query{songs (
                                          where: {
                                            creator: {
                                              fans_every: {
                                                    name: "groupie1"
                                                  }
                                                }
                                              }
                                            ) {
                                              title
                                            }
                                          }""".stripMargin

    server.query(filterGroupies, project, dataContains = "{\"songs\":[{\"title\":\"Imagine\"},{\"title\":\"Bicycle\"},{\"title\":\"Gasag\"}]}")
  }

  "Filter Queries along ManyToMany self relations" should "give an error with null" taggedAs (IgnoreMongo) in {

    val filterGroupies = s"""query{songs (
                                          where: {
                                            creator: {
                                              fans_every: {
                                                    fans_some: null
                                                  }
                                                }
                                              }
                                            ) {
                                              title
                                            }
                                          }""".stripMargin

    server.queryThatMustFail(filterGroupies, project, errorCode = 3033)
  }

  "Filter Queries along ManyToMany self relations" should "succeed with {} filter _some" in {

    val filterGroupies = s"""query{songs (
                                          where: {
                                            creator: {
                                              fans_some: {}
                                                }
                                              }
                                            ) {
                                              title
                                            }
                                          }""".stripMargin

    server.query(filterGroupies, project, dataContains = "{\"songs\":[{\"title\":\"My Girl\"},{\"title\":\"Imagine\"}]}")
  }

  "Filter Queries along ManyToMany self relations" should "succeed with {} filter _none" taggedAs (IgnoreMongo) in {

    val filterGroupies = s"""query{humans(
                                          where: {fans_none: {}
                                                }
                                            ) {
                                              name
                                            }
                                          }""".stripMargin

    server.query(
      filterGroupies,
      project,
      dataContains =
        "{\"humans\":[{\"name\":\"paul\"},{\"name\":\"dave\"},{\"name\":\"groupie1\"},{\"name\":\"groupie2\"},{\"name\":\"frances\"},{\"name\":\"courtney\"},{\"name\":\"yoko\"},{\"name\":\"freddy\"},{\"name\":\"kurt\"}]}"
    )
  }

  "Filter Queries along ManyToMany self relations" should "succeed with {} filter _every" taggedAs (IgnoreMongo) in {

    val filterGroupies = s"""query{humans(
                                          where: {fans_every: {}
                                                }
                                            ) {
                                              name
                                            }
                                          }""".stripMargin

    server.query(
      filterGroupies,
      project,
      dataContains =
        "{\"humans\":[{\"name\":\"paul\"},{\"name\":\"dave\"},{\"name\":\"groupie1\"},{\"name\":\"groupie2\"},{\"name\":\"frances\"},{\"name\":\"courtney\"},{\"name\":\"kurt\"},{\"name\":\"yoko\"},{\"name\":\"john\"},{\"name\":\"freddy\"},{\"name\":\"kurt\"}]}"
    )
  }

  //Many to one

  "Filter Queries along ManyToOne self relations" should "succeed valid filter" in {

    val filterSingers = s"""query{humans(
                                          where: {singer:{
                                                     name: "kurt"
                                                  }
                                                }
                                            ) {
                                              name
                                            }
                                          }""".stripMargin

    server.query(filterSingers, project, dataContains = "{\"humans\":[{\"name\":\"dave\"}]}")
  }

  "Filter Queries along ManyToOne self relations" should "succeed with {} filter" in {

    val filterSingers = s"""query{humans(
                                          where: {singer:{}
                                                }
                                            ) {
                                              name
                                            }
                                          }""".stripMargin

    server.query(filterSingers, project, dataContains = "{\"humans\":[{\"name\":\"paul\"},{\"name\":\"dave\"}]}")
  }

  "Filter Queries along ManyToOne self relations" should "succeed with null filter" in {

    val filterSingers = s"""query{humans(
                                          where: {singer: null
                                                }
                                            ) {
                                              name
                                            }
                                          }""".stripMargin

    server.query(
      filterSingers,
      project,
      dataContains =
        "{\"humans\":[{\"name\":\"groupie1\"},{\"name\":\"groupie2\"},{\"name\":\"frances\"},{\"name\":\"courtney\"},{\"name\":\"kurt\"},{\"name\":\"yoko\"},{\"name\":\"john\"},{\"name\":\"freddy\"},{\"name\":\"kurt\"}]}"
    )
  }

  override def beforeEach() = {} // do not delete dbs on each run

  private def setupRockRelations = {

    val paul = server.query("""mutation{createHuman(data:{name: "paul"}){id}}""", project).pathAsString("data.createHuman.id")

    val dave = server.query("""mutation{createHuman(data:{name: "dave"}){id}}""", project).pathAsString("data.createHuman.id")

    val groupie1 = server.query("""mutation{createHuman(data:{name: "groupie1"}){id}}""", project).pathAsString("data.createHuman.id")

    val groupie2 = server.query("""mutation{createHuman(data:{name: "groupie2"}){id}}""", project).pathAsString("data.createHuman.id")

    val frances = server.query("""mutation{createHuman(data:{name: "frances"}){id}}""", project).pathAsString("data.createHuman.id")

    val courtney = server
      .query(s"""mutation{createHuman(data:{name: "courtney",stepdaughters: {connect: [{id: "$frances"}]}}){id}}""", project)
      .pathAsString("data.createHuman.id")

    val kurtc = server
      .query(
        s"""mutation{createHuman(data:{name: "kurt",
         |wife: {connect: { id: "$courtney"}},
         |daughters: {connect: [{ id: "$frances"}]},
         |fans: {connect: [{id: "$groupie1"}, {id: "$groupie2"}]},
         |bandmembers: {connect:[{id: "$dave"}]}}){id}}""".stripMargin,
        project
      )
      .pathAsString("data.createHuman.id")

    val mygirl =
      server.query(s"""mutation{createSong(data:{title: "My Girl", creator: {connect: { id: "$kurtc"}}}){id}}""", project).pathAsString("data.createSong.id")

    val yoko = server.query(s"""mutation{createHuman(data:{name: "yoko"}){id}}""", project).pathAsString("data.createHuman.id")

    val john = server
      .query(
        s"""mutation{createHuman(data:{name: "john",
         |wife: {connect: { id: "$yoko"}}
         |fans: {connect:[{id: "$groupie1"}]}
         |bandmembers: {connect: [{id: "$paul"}]}}){id}}""".stripMargin,
        project
      )
      .pathAsString("data.createHuman.id")

    val imagine =
      server.query(s"""mutation{createSong(data:{title: "Imagine", creator: {connect: { id: "$john"}}}){id}}""", project).pathAsString("data.createSong.id")

    val freddy = server.query(s"""mutation{createHuman(data:{name: "freddy"}){id}}""", project).pathAsString("data.createHuman.id")

    val bicycle =
      server.query(s"""mutation{createSong(data:{title: "Bicycle", creator: {connect: { id: "$freddy"}}}){id}}""", project).pathAsString("data.createSong.id")

    val kurtk = server.query(s"""mutation{createHuman(data:{name: "kurt"}){id}}""", project).pathAsString("data.createHuman.id")

    val gasag =
      server.query(s"""mutation{createSong(data:{title: "Gasag", creator: {connect: { id: "$kurtk"}}}){id}}""", project).pathAsString("data.createSong.id")
  }
}
