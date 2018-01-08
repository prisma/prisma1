package cool.graph.deploy.database.schema.mutations

import cool.graph.deploy.specutils.DeploySpecBase
import cool.graph.shared.models.{MigrationId, MigrationStatus, Project, ProjectId}
import org.scalatest.{FlatSpec, Matchers}

class DeployMutationRegressionSpec extends FlatSpec with Matchers with DeploySpecBase {

  val projectPersistence   = testDependencies.projectPersistence
  val migrationPersistence = testDependencies.migrationPersistence

  "DeployMutation" should "succeed for regression #1490 (1/2)" in {
    val project = setupProject("""
      |type Post {
      |  id: ID! @unique
      |  version: [PostVersion!]! @relation(name: "PostVersion")
      |}
      |
      |type PostVersion {
      |  id: ID! @unique
      |  post: Post! @relation(name: "PostVersion")
      |  postContent: [PostContents!]! @relation(name: "PostOnPostContents")
      |}
      |
      |type PostContents {
      |  id: ID! @unique
      |  postVersion: PostVersion! @relation(name: "PostOnPostContents")
      |}
    """.stripMargin)

    val migration = migrationPersistence.loadAll(project.id).await.last

    migration.errors should be(empty)
    migration.status shouldEqual MigrationStatus.Success
  }

  "DeployMutation" should "succeed for regression #1490 (2/2)" in {
    val project = setupProject("""
      |type Post {
      |  id: ID! @unique
      |  pin: Pin @relation(name: "PinCaseStudy")
      |}
      |
      |type Pin {
      |  id: ID! @unique
      |  caseStudy: Post @relation(name: "PinCaseStudy")
      |}
    """.stripMargin)

//    val revision  = result.pathAsLong("data.deploy.migration.revision")
//    val migration = migrationPersistence.byId(MigrationId(project.id, revision.toInt)).await.get
//
//    migration.errors should be(empty)
//    migration.status shouldEqual MigrationStatus.Success
  }

  "DeployMutation" should "succeed for regression #1420" in {
    val project      = setupProject("""
        |type User {
        |  id: ID! @unique
        |
        |  createdAt: DateTime!
        |  updatedAt: DateTime!
        |
        |  repositories: [Repository!]! @relation(name: "UserRepository")
        |}
        |
        |type Repository {
        |  id: ID! @unique
        |
        |  name: String!
        |  owner: User! @relation(name: "UserRepository")
        |}
      """.stripMargin)
    val nameAndStage = ProjectId.fromEncodedString(project.id)
    val schema =
      """
         |type User {
         |  id: ID! @unique
         |
         |  createdAt: DateTime!
         |  updatedAt: DateTime!
         |
         |  githubUserId: String! @unique
         |
         |  name: String!
         |  bio: String!
         |  public_repos: Int!
         |  public_gists: Int!
         |}
      """.stripMargin

    val result = server.query(s"""
         |mutation {
         |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema)}}){
         |    migration {
         |      revision
         |    }
         |    errors {
         |      description
         |    }
         |  }
         |}
      """.stripMargin)

    val revision  = result.pathAsLong("data.deploy.migration.revision")
    val migration = migrationPersistence.byId(MigrationId(project.id, revision.toInt)).await.get

    migration.errors should be(empty)
    migration.status shouldEqual MigrationStatus.Success
  }

  "DeployMutation" should "succeed for regression #1420" in {
    val project = setupProject("""
                                      |type User {
                                      |  id: ID! @unique
                                      |
                                      |  createdAt: DateTime!
                                      |  updatedAt: DateTime!
                                      |
                                      |  repositories: [Repository!]! @relation(name: "UserRepository")
                                      |}
                                      |
                                      |type Repository {
                                      |  id: ID! @unique
                                      |
                                      |  name: String!
                                      |  owner: User! @relation(name: "UserRepository")
                                      |}
                                    """.stripMargin)
//    val nameAndStage = ProjectId.fromEncodedString(project.id)
//    val schema =
//      """
//        |type User {
//        |  id: ID! @unique
//        |
//        |  createdAt: DateTime!
//        |  updatedAt: DateTime!
//        |
//        |  githubUserId: String! @unique
//        |
//        |  name: String!
//        |  bio: String!
//        |  public_repos: Int!
//        |  public_gists: Int!
//        |}
//      """.stripMargin
//
//    val result = server.query(s"""
//                                 |mutation {
//                                 |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema)}}){
//                                 |    migration {
//                                 |      revision
//                                 |    }
//                                 |    errors {
//                                 |      description
//                                 |    }
//                                 |  }
//                                 |}
//      """.stripMargin)

//    val revision  = result.pathAsLong("data.deploy.migration.revision")
//    val migration = migrationPersistence.byId(MigrationId(project.id, revision.toInt)).await.get
//
//    migration.errors should be(empty)
//    migration.status shouldEqual MigrationStatus.Success
  }

  def deploySchema(project: Project, schema: String) = {
    val nameAndStage = ProjectId.fromEncodedString(project.id)
    server.query(s"""
      |mutation {
      |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(schema)}}){
      |    migration {
      |      steps {
      |        type
      |      }
      |    }
      |    errors {
      |      description
      |    }
      |  }
      |}""".stripMargin)
  }
}
