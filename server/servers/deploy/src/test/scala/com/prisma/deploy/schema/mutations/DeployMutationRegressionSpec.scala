package com.prisma.deploy.schema.mutations

import com.prisma.IgnoreSQLite
import com.prisma.deploy.specutils.ActiveDeploySpecBase
import com.prisma.shared.models.ConnectorCapability.LegacyDataModelCapability
import com.prisma.shared.models.{ConnectorCapability, MigrationId, MigrationStatus}
import org.scalatest.{FlatSpec, Matchers}

class DeployMutationRegressionSpec extends FlatSpec with Matchers with ActiveDeploySpecBase {

  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(LegacyDataModelCapability)

  val projectPersistence   = testDependencies.projectPersistence
  val migrationPersistence = testDependencies.migrationPersistence

  "DeployMutation" should "succeed for regression #1490 (1/2)" in {
    val (project, migration) = setupProject("""
                                              |type Post {
                                              |  id: ID! @unique
                                              |  version: [PostVersion] @relation(name: "PostVersion")
                                              |}
                                              |
                                              |type PostVersion {
                                              |  id: ID! @unique
                                              |  post: Post! @relation(name: "PostVersion")
                                              |  postContent: [PostContents] @relation(name: "PostOnPostContents")
                                              |}
                                              |
                                              |type PostContents {
                                              |  id: ID! @unique
                                              |  postVersion: PostVersion! @relation(name: "PostOnPostContents")
                                              |}
                                            """.stripMargin)

    migration.errors should be(empty)
    migration.status shouldEqual MigrationStatus.Success
  }

  "DeployMutation" should "succeed for regression #1490 (2/2)" in {
    val (project, migration) = setupProject("""
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

    migration.errors should be(empty)
    migration.status shouldEqual MigrationStatus.Success
  }

  "DeployMutation" should "succeed for regression #1420" in {
    val (project, initialMigration) = setupProject("""
                                                     |type User {
                                                     |  id: ID! @unique
                                                     |
                                                     |  createdAt: DateTime!
                                                     |  updatedAt: DateTime!
                                                     |
                                                     |  repositories: [Repository] @relation(name: "UserRepository")
                                                     |}
                                                     |
                                                     |type Repository {
                                                     |  id: ID! @unique
                                                     |
                                                     |  name: String!
                                                     |  owner: User! @relation(name: "UserRepository")
                                                     |}
                                                   """.stripMargin)

    initialMigration.errors should be(empty)
    initialMigration.status shouldEqual MigrationStatus.Success

    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val nextSchema =
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
                                 |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(nextSchema)}}){
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

  "DeployMutation" should "succeed for regression #1436" taggedAs (IgnoreSQLite) in {
    val (project, initialMigration) = setupProject("""
                                                     |type Post {
                                                     |  id: ID! @unique
                                                     |  createdAt: DateTime!
                                                     |  updatedAt: DateTime!
                                                     |  isPublished: Boolean! # @default(value: false)
                                                     |  title: String!
                                                     |  text: String!
                                                     |  author: User! @relation(name: "UserPosts")
                                                     |}
                                                     |
                                                     |type User {
                                                     |  id: ID! @unique
                                                     |  email: String! @unique
                                                     |  password: String!
                                                     |  name: String!
                                                     |  posts: [Post] @relation(name: "UserPosts")
                                                     |}
                                                   """.stripMargin)

    initialMigration.errors should be(empty)
    initialMigration.status shouldEqual MigrationStatus.Success

    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val nextSchema =
      """
        |type Update {
        |  id: ID! @unique
        |  createdAt: DateTime!
        |  updatedAt: DateTime!
        |  text: String!
        |  creator: User! @relation(name: "UserUpdates")
        |}
        |
        |type User {
        |  id: ID! @unique
        |  email: String! @unique
        |  firstName: String!
        |  lastName: String!
        |  profilePicture: String!
        |  providerId: String!
        |  provider: Provider!
        |  updates: [Update] @relation(name: "UserUpdates")
        |}
        |
        |enum Provider {
        |  Facebook
        |  Instagram
        |}
      """.stripMargin

    val result = server.query(s"""
                                 |mutation {
                                 |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(nextSchema)}}){
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

  "DeployMutation" should "succeed for regression #1426" in {
    val (project, initialMigration) = setupProject("""
                                                     |type Post {
                                                     |  id: ID! @unique
                                                     |  createdAt: DateTime!
                                                     |  updatedAt: DateTime!
                                                     |  isPublished: Boolean! # @default(value: false)
                                                     |  title: String!
                                                     |  text: String!
                                                     |  author: User! @relation(name: "UserPosts")
                                                     |}
                                                     |
                                                     |type User {
                                                     |  id: ID! @unique
                                                     |  email: String! @unique
                                                     |  password: String!
                                                     |  name: String!
                                                     |  posts: [Post] @relation(name: "UserPosts")
                                                     |}
                                                   """.stripMargin)

    initialMigration.errors should be(empty)
    initialMigration.status shouldEqual MigrationStatus.Success

    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val nextSchema =
      """
        |type Post {
        |  id: ID! @unique
        |  createdAt: DateTime!
        |  updatedAt: DateTime!
        |  isPublished: Boolean! # @default(value: false)
        |  title: String!
        |  text: String!
        |  author: User! @relation(name: "UserPosts")
        |}
        |
        |type User {
        |  id: ID! @unique
        |  email: String! @unique
        |  password: String!
        |  name: String!
        |  posts: [Post] @relation(name: "UserPosts")
        |  customString: String
        |}
      """.stripMargin

    val result = server.query(s"""
                                 |mutation {
                                 |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(nextSchema)}}){
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

  "DeployMutation" should "succeed for regression #1466" in {
    val (project, initialMigration) = setupProject("""
                                                     |type Post {
                                                     |  id: ID! @unique
                                                     |  createdAt: DateTime!
                                                     |  updatedAt: DateTime!
                                                     |  isPublished: Boolean! # @default(value: false)
                                                     |  title: String!
                                                     |  text: String!
                                                     |  author: User! @relation(name: "UserPosts")
                                                     |}
                                                     |
                                                     |type User {
                                                     |  id: ID! @unique
                                                     |  email: String! @unique
                                                     |  password: String!
                                                     |  name: String!
                                                     |  posts: [Post] @relation(name: "UserPosts")
                                                     |}
                                                   """.stripMargin)

    initialMigration.errors should be(empty)
    initialMigration.status shouldEqual MigrationStatus.Success

    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val nextSchema =
      """
        |type Post {
        |  id: ID! @unique
        |  isPublished: Boolean! # @default(value: false)
        |  title: String!
        |  text: String!
        |}
      """.stripMargin

    val result = server.query(s"""
                                 |mutation {
                                 |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(nextSchema)}}){
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

  "DeployMutation" should "succeed for regression #1532" in {
    val (project, initialMigration) = setupProject("""
                                                     |type User {
                                                     |  id: ID! @unique
                                                     |  name: String!
                                                     |}
                                                     |
                                                     |type Post {
                                                     |  id: ID! @unique
                                                     |  title: String
                                                     |}
                                                   """.stripMargin)

    initialMigration.errors should be(empty)
    initialMigration.status shouldEqual MigrationStatus.Success

    val nameAndStage = testDependencies.projectIdEncoder.fromEncodedString(project.id)
    val nextSchema =
      """
        |type User {
        |  id: ID! @unique
        |  name: String!
        |}
        |
        |type Post {
        |  id: ID! @unique
        |  title: String
        |  users: [User]
        |}
      """.stripMargin

    val result = server.query(s"""
                                 |mutation {
                                 |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(nextSchema)}}){
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

    val nextSchema2 =
      """
        |type User {
        |  id: ID! @unique
        |  name: String!
        |}
        |
        |type Post {
        |  id: ID! @unique
        |  title: String
        |}
      """.stripMargin

    val result2 = server.query(s"""
                                 |mutation {
                                 |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(nextSchema2)}}){
                                 |    migration {
                                 |      revision
                                 |    }
                                 |    errors {
                                 |      description
                                 |    }
                                 |  }
                                 |}
      """.stripMargin)

    val revision2  = result2.pathAsLong("data.deploy.migration.revision")
    val migration2 = migrationPersistence.byId(MigrationId(project.id, revision2.toInt)).await.get

    migration2.errors should be(empty)
    migration2.status shouldEqual MigrationStatus.Success
  }
}
