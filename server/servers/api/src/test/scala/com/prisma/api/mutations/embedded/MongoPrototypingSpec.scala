package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MongoPrototypingSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "Simple unique index" should "work" in {

    val project = SchemaDsl.fromString() {
      """
        |type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 11111,
         |   name: "Top"
         |}){
         |  unique,
         |  name
         |}}""",
      project
    )

    server.queryThatMustFail(
      s"""mutation {
         |   createTop(data: {
         |   unique: 11111,
         |   name: "Top"
         |}){
         |  unique,
         |  name
         |}}""",
      project,
      3010,
      errorContains = """A unique constraint would be violated on Top. Details: Field name = unique"""
    )
  }

  //Fixme https://jira.mongodb.org/browse/SERVER-1068
  "Unique indexes on embedded types" should "work" ignore {
    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    children: [Child]
        |}
        |
        |type Child @embedded{
        |    name: String @unique
        |}
        |"""
    }

    database.setup(project)

    val create1 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create: [{ name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create1.toString should be("""{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter"}]}}}""")

    val create2 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad2",
         |   children: {create: [{ name: "Daughter"}, { name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create2.toString should be("""{"data":{"createParent":{"name":"Dad2","children":[{"name":"Daughter"},{"name":"Daughter"}]}}}""")

    val create3 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create: [{ name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create3.toString should be("""{"data":{"createParent":{"name":"Dad2","children":[{"name":"Daughter"},{"name":"Daughter"}]}}}""")

    val update1 = server.query(
      s"""mutation {
         |   updateParent(
         |   where: {name: "Dad"}
         |   data: {
         |      children: {create: [{ name: "Daughter2"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    update1.toString should be("""{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"}]}}}""")

    val update2 = server.query(
      s"""mutation {
         |   updateParent(
         |   where: {name: "Dad"}
         |   data: {
         |      children: {create: [{ name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    update2.toString should be("""{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"}]}}}""")
  }

}
