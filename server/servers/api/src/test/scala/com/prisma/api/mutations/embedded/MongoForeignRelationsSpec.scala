package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MongoForeignRelationsSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "Delete" should "take care of relations without foreign keys" in {
    val project = SchemaDsl.fromString() {
      """
        |type Child {
        |    id: ID! @unique
        |    name: String @unique
        |    parent: Parent
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    child: Child @mongoRelation(field: "children")
        |}
        |"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   child: {create:{name: "Daughter"}}
         |}){
         |  name,
         |  child{
         |    name
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createParent":{"name":"Dad","child":{"name":"Daughter"}}}}""")

    server.query(s"""mutation {deleteChild(where:{name: "Daughter"}){name}}""", project)

    server.query(s"""query {parents{name, child {name}}}""", project)

  }

  "Delete of something linked to on an embedded type" should "take care of relations without foreign keys " in {
    val project = SchemaDsl.fromString() {
      """
        |type Friend {
        |    name: String @unique
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    child: Child
        |}
        |
        |type Child @embedded{
        |    name: String @unique
        |    child: GrandChild
        |}
        |
        |type GrandChild @embedded{
        |    name: String @unique
        |    friend: Friend @mongoRelation(field:"friend")
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   child: {create:{
         |      name: "Daughter"
         |      child: {create:{
         |          name: "GrandSon"
         |          friend: {create:{
         |               name: "Friend"
         |          }}
         |      }}
         |   }}
         |}){
         |  name,
         |  child{
         |    name
         |    child{
         |       name
         |       friend{
         |          name
         |       }
         |    }
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createParent":{"name":"Dad","child":{"name":"Daughter","child":{"name":"GrandSon","friend":{"name":"Friend"}}}}}}""")

    server.query(s"""mutation {deleteFriend(where:{name: "Friend"}){name}}""", project)

    server.query(s"""query {parents{name, child {name}}}""", project)
  }

  "Delete of something linked to on an embedded type" should "take care of relations without foreign keys 2" in {
    val project = SchemaDsl.fromString() {
      """
        |type Friend {
        |    name: String @unique
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    child: Child
        |}
        |
        |type Child @embedded{
        |    name: String @unique
        |    child: GrandChild
        |}
        |
        |type GrandChild @embedded{
        |    name: String @unique
        |    friend: Friend! @mongoRelation(field:"friend")
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   child: {create:{
         |   name: "Daughter"
         |   child: {create:{
         |      name: "GrandSon"
         |      friend: {create:{
         |          name: "Friend"
         |          }}
         |      }}
         |   }}
         |}){
         |  name,
         |  child{
         |    name
         |    child{
         |       name
         |       friend{
         |          name
         |       }
         |    }
         |  }
         |}}""",
      project
    )

    val res2 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad2",
         |   child: {create:{
         |   name: "Daughter2"
         |   child: {create:{
         |      name: "GrandSon2"
         |      friend: {connect:{
         |             name: "Friend"
         |          }}
         |      }}
         |   }}
         |}){
         |  name,
         |  child{
         |    name
         |    child{
         |       name
         |       friend{
         |          name
         |       }
         |    }
         |  }
         |}}""",
      project
    )

    res2.toString should be(
      """{"data":{"createParent":{"name":"Dad2","child":{"name":"Daughter2","child":{"name":"GrandSon2","friend":{"name":"Friend"}}}}}}""")
  }

  "Dangling Ids" should "be ignored" in {
    val project = SchemaDsl.fromString() {
      """
        |type ZChild{
        |    name: String @unique
        |    parent: Parent
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    child: ZChild!
        |}"""
    }

    database.setup(project)

    val create = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   child: {create:{ name: "Daughter"}}
         |}){
         |  name,
         |  child{ name}
         |}}""",
      project
    )

    create.toString should be("""{"data":{"createParent":{"name":"Dad","child":{"name":"Daughter"}}}}""")

    val delete = server.query(s"""mutation {deleteParent(where: { name: "Dad" }){name}}""", project)

    delete.toString should be("""{"data":{"deleteParent":{"name":"Dad"}}}""")

    val create2 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad2",
         |   child: {connect:{name: "Daughter"}}
         |}){
         |  name,
         |  child{ name}
         |}}""",
      project
    )

    create2.toString should be("""{"data":{"createParent":{"name":"Dad2","child":{"name":"Daughter"}}}}""")
  }
}
