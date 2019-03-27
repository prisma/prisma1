package com.prisma.integration

import com.prisma.IgnoreSQLite
import com.prisma.ConnectorTag.{MySqlConnectorTag, PostgresConnectorTag, SQLiteConnectorTag}
import org.scalatest.{FlatSpec, Matchers}

class RenamingWithExistingDataSpec extends FlatSpec with Matchers with IntegrationBaseSpec {
  override def runOnlyForConnectors = Set(PostgresConnectorTag, MySqlConnectorTag, SQLiteConnectorTag)

  "Renaming a model" should "work" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  a: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A"}){a}}""", project)

    val schema1 =
      """type B @rename(oldName: "A"){
        |  a: String! @unique
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val bs = apiServer.query("""{bs{a}}""", updatedProject)
    bs.toString should be("""{"data":{"bs":[{"a":"A"}]}}""")
  }

  "Renaming a model with a scalar list " should "work" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  a: String! @unique
        |  ints: [Int!]!
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A"}){a}}""", project)

    val schema1 =
      """type B @rename(oldName: "A"){
        |  a: String! @unique
        |  ints: [Int!]!
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val bs = apiServer.query("""{bs{a}}""", updatedProject)
    bs.toString should be("""{"data":{"bs":[{"a":"A"}]}}""")
  }

  "Renaming a field" should "work" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  a: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A"}){a}}""", project)

    val schema1 =
      """type A {
        |  b: String! @unique @rename(oldName: "a")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val bs = apiServer.query("""{as{b}}""", updatedProject)
    bs.toString should be("""{"data":{"as":[{"b":"A"}]}}""")
  }

  "Renaming a relation with oldName on both sides" should "work" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "First")
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First")
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b:{create:{b:"B1"}}}){a}}""", project)

    val schema1 =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "Second", oldName: "First" )
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First", oldName:"First")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val as = apiServer.query("""{as{b{b}}}""", updatedProject)
    as.toString should be("""{"data":{"as":[{"b":{"b":"B1"}}]}}""")
  }

  "Renaming a model and field" should "work" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  a: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A"}){a}}""", project)

    val schema1 =
      """type B @rename(oldName: "A"){
        |  b: String! @unique @rename(oldName: "a")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val bs = apiServer.query("""{bs{b}}""", updatedProject)
    bs.toString should be("""{"data":{"bs":[{"b":"A"}]}}""")
  }

  "Renaming a model and a relation with oldName on both sides" should "work" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "First")
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First")
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b:{create:{b:"B1"}}}){a}}""", project)

    val schema1 =
      """type C @rename(oldName: "A") {
        |  a: String! @unique
        |  b: B @relation(name: "Second", oldName: "First" )
        |}
        |
        |type B {
        |  b: String @unique
        |  a: C @relation(name: "First", oldName:"First")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val as = apiServer.query("""{cs{b{b}}}""", updatedProject)
    as.toString should be("""{"data":{"cs":[{"b":{"b":"B1"}}]}}""")
  }

  "Renaming a field and a relation with oldName on both sides" should "work" taggedAs (IgnoreSQLite) in {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "First")
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First")
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b:{create:{b:"B1"}}}){a}}""", project)

    val schema1 =
      """type A  {
        |  a: String! @unique
        |  bNew: B @relation(name: "Second", oldName: "First" ) @rename(oldName: "b")
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First", oldName:"First")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val as = apiServer.query("""{as{bNew{b}}}""", updatedProject)
    as.toString should be("""{"data":{"as":[{"bNew":{"b":"B1"}}]}}""")
  }

  "Renaming models by switching the names of two existing models" should "error and ask to be split in two parts" in {

    val schema =
      """type A {
        |  id: ID! @unique
        |  a: String! @unique
        |}
        |
        |type B {
        |  id: ID! @unique
        |  b: String @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A"}){a}}""", project)
    apiServer.query("""mutation{createB(data:{b:"B"}){b}}""", project)

    val schema1 =
      """type B @rename(oldName: "A"){
        |  id: ID! @unique
        |  a: String! @unique
        |}
        |
        |type A @rename(oldName: "B"){
        |  id: ID! @unique
        |  b: String @unique
        |}"""

    val updatedProject = deployServer.deploySchemaThatMustError(project, schema1)

    updatedProject.toString() should be(
      """{"data":{"deploy":{"migration":null,"errors":[{"description":"You renamed type `A` to `B`. But that is the old name of type `A`. Please do this in two steps."},{"description":"You renamed type `B` to `A`. But that is the old name of type `B`. Please do this in two steps."}],"warnings":[]}}}""")
  }

  // these will be fixed when we implement a migration workflow

  "An accidental rename of a field on a new model" should "error" ignore {

    val schema =
      """type A {
        |  a: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A"}){a}}""", project)

    val schema1 =
      """type A {
        |  a: String! @unique
        |}
        |
        |type B {
        |  a: String! @unique @rename(oldName: "b")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val as = apiServer.query("""{as{a}}""", updatedProject)
    as.toString should be("""{"data":{"as":[{"a":"A"}]}}""")

    val bs = apiServer.query("""{bs{a}}""", updatedProject)
    bs.toString should be("""{"data":{"bs":[]}}""")

    true should be(false)
  }

  "Forgetting a rename directive on a model" should "not blow up" ignore {

    val schema =
      """type A {
        |  a: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type B @rename(oldName: "A"){
        |  a: String! @unique
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val schema2 =
      """type B @rename(oldName: "A"){
        |  a: String! @unique
        |}
        |
        |type C{
        |  c: String! @unique
        |}"""

    val updatedProject2 = deployServer.deploySchema(project, schema2)
  }

  "Forgetting a rename directive on a field" should "not blow up" ignore {

    val schema =
      """type A {
        |  a: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type B {
        |  b: String! @unique @rename(oldName: "a")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val schema2 =
      """type B {
        |  a: String! @unique b: String! @unique @rename(oldName: "a")
        |}
        |
        |type C{
        |  c: String! @unique
        |}"""

    val updatedProject2 = deployServer.deploySchema(project, schema2)
  }

  "Renaming a relation with oldName on both sides and forgetting to remove the oldName values" should "work" ignore {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "First")
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First")
        |}"""

    val (project, _) = setupProject(schema)

    val schema1 =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "Second", oldName: "First" )
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First", oldName:"First")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val schema2 =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "Second", oldName: "First" )
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First", oldName:"First")
        |}
        |
        |type C {
        |  b: String @unique
        |}"""

    val updatedProject2 = deployServer.deploySchema(project, schema2)

  }

  "Renaming fields by switching the names of two existing fields" should "work when there is existing data" ignore {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: String! @unique
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b:"B"}){a}}""", project)

    val schema1 =
      """type A {
        |  a: String! @unique @rename(oldName: "b")
        |  b: String! @unique @rename(oldName: "a")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val as = apiServer.query("""{as{a, b}}""", updatedProject)
    as.toString should be("""{"data":{"teams":[{"name":"Bayern","win":null},{"name":"Real","win":{"number":1}}]}}""")
  }

  "Renaming relations by switching the names of two existing relations" should "work even when there is existing data" ignore {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "First")
        |  b2: B @relation(name: "Second")
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First")
        |  a2: A @relation(name: "Second")
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b:{create:{b:"B1"}}, b2:{create: {b: "B2"}} }){a}}""", project)

    val schema1 =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "Second", oldName: "First" )
        |  b2: B @relation(name: "First", oldName: "Second")
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "Second", oldName: "First")
        |  a2: A @relation(name: "First", oldName: "Second")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val as = apiServer.query("""{as{b}}""", updatedProject)
    as.toString should be("""{"data":{"teams":[{"name":"Bayern","win":null},{"name":"Real","win":{"number":1}}]}}""")
    val bs = apiServer.query("""{bs{s}}""", updatedProject)
    bs.toString should be("""{"data":{"teams":[{"name":"Bayern","win":null},{"name":"Real","win":{"number":1}}]}}""")
  }

  "Renaming a relation but forgetting the oldName on one side" should "???" ignore {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "First")
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First")
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b:{create:{b:"B1"}}}){a}}""", project)

    val schema1 =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "Second", oldName: "First" )
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First")
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val as = apiServer.query("""{as{b{b}}}""", updatedProject)
    as.toString should be("""{"data":{"as":[{"b":{"b":"B1"}}]}}""")

    //do we want this kind of magic??
    true should be(false)
  }

  "Renaming a relation but forgetting the oldName on one side 2" should "???" ignore {

    val schema =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "First")
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "First")
        |}"""

    val (project, _) = setupProject(schema)

    apiServer.query("""mutation{createA(data:{a:"A", b:{create:{b:"B1"}}}){a}}""", project)

    val schema1 =
      """type A {
        |  a: String! @unique
        |  b: B @relation(name: "Second")
        |}
        |
        |type B {
        |  b: String @unique
        |  a: A @relation(name: "Second" , oldName: "First" )
        |}"""

    val updatedProject = deployServer.deploySchema(project, schema1)

    val as = apiServer.query("""{as{b{b}}}""", updatedProject)
    as.toString should be("""{"data":{"as":[{"b":{"b":"B1"}}]}}""")

    //do we want this kind of magic??
    true should be(false)
  }
}
