package com.prisma.deploy.migration
import com.prisma.deploy.connector.DatabaseSchema
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.deploy.specutils.PassiveDeploySpecBase
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability}
import com.prisma.shared.models.ConnectorCapability.{IntIdCapability, IntrospectionCapability, MigrationsCapability, RelationLinkTableCapability}
import org.scalatest.{Matchers, WordSpecLike}

class DatabaseSchemaValidatorSpec extends WordSpecLike with Matchers with PassiveDeploySpecBase {
  override def doNotRunForCapabilities: Set[ConnectorCapability] = Set.empty
  override def runOnlyForCapabilities                            = Set(MigrationsCapability)

  "it should error if a table is missing" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "a_wrong_table_name"){
         |  id: Int! @id
         |  title: Int!
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(empty)
    error.description should be("Could not find the table for the model Blog in the database.")
  }

  "it should error if a column is missing" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: Int!
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(Some("title"))
    error.description should be("Could not find the column for the field title in the database.")
  }

  "it should error if a column exists but it has the wrong type" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   title text
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   title mediumtext,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: Int!
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(Some("title"))
    error.description should be(
      "The underlying column for the field title has an incompatible type. The field has type `Int` and the column has type `String`.")
  }

  "it should succeed if an inline relation field exists" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   author int references author(id)
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  author: Author! @relation(link: INLINE)
         |}
         |
         |type Author @db(name: "author"){
         |  id: Int! @id
         |}
       """.stripMargin

    deploy(dataModel, ConnectorCapabilities(IntIdCapability))
  }

  "it should error if the column for an inline relation field is missing" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  author: Author! @relation(link: INLINE)
         |}
         |
         |type Author @db(name: "author"){
         |  id: Int! @id
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(Some("author"))
    error.description should be("Could not find the column for the inline relation field author in the database.")
  }

  "it should error if the foreign key constraint for an inline relation field is referencing the wrong table" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE wrong_table (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   author int references wrong_table(id)
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  author: Author! @relation(link: INLINE)
         |}
         |
         |type Author @db(name: "author"){
         |  id: Int! @id
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(Some("author"))
    error.description should be(
      "The column for the inline relation field author is not referencing the right table. It should reference the table of model `Author` but is referencing the table `wrong_table`.")
  }

  "it should error if the foreign key constraint for an inline relation field is referencing a column which is not the id" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY,
         |   nick int UNIQUE
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   author int references author(nick)
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  author: Author! @relation(link: INLINE)
         |}
         |
         |type Author @db(name: "author"){
         |  id: Int! @id
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(Some("author"))
    error.description should be(
      "The column for the inline relation field author is not referencing a valid column. Those columns must always reference the column of the id field of related model. So it should reference `id` instead of `nick`.")
  }

  "it should error if a @linkTable is missing in the database" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY,
         |   nick int UNIQUE
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   author int references author(nick)
         |);
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  authors: [Author] @relation(name:"BlogToAuthor")
         |}
         |
         |type Author @db(name: "author"){
         |  id: Int! @id
         |}
         |
         |type BlogToAuthor @linkTable @db(name:"blog_to_author"){
         |  id: ID! @id
         |  blog: Blog!
         |  author: Author!
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability, RelationLinkTableCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("BlogToAuthor")
    error.field should be(empty)
    error.description should be("Could not find the table `blog_to_author` for the relation `BlogToAuthor` in the database.")
  }

  "it should error if the id column of a @linkTable is missing in the database" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY,
         |   nick int UNIQUE
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   author int references author(nick)
         | );
         | CREATE TABLE blog_to_author (
         |   blog int references blog(id),
         |   author int references author(id)
         | )
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  authors: [Author] @relation(name:"BlogToAuthor")
         |}
         |
         |type Author @db(name: "author"){
         |  id: Int! @id
         |}
         |
         |type BlogToAuthor @linkTable @db(name:"blog_to_author"){
         |  id: ID! @id
         |  blog: Blog!
         |  author: Author!
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability, RelationLinkTableCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("BlogToAuthor")
    error.field should be(Some("id"))
    error.description should be("The link table BlogToAuthor is missing the column `id`.")
  }

  "it should succeed if the @linkTable matches the relation table in database" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY,
         |   nick int UNIQUE
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   author int references author(nick)
         | );
         | CREATE TABLE blog_to_author (
         |   blog int references blog(id),
         |   author int references author(id)
         | )
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  authors: [Author] @relation(name:"BlogToAuthor")
         |}
         |
         |type Author @db(name: "author"){
         |  id: Int! @id
         |}
         |
         |type BlogToAuthor @linkTable @db(name:"blog_to_author"){
         |  blog: Blog!
         |  author: Author!
         |}
       """.stripMargin

    deploy(dataModel, ConnectorCapabilities(IntIdCapability, RelationLinkTableCapability))
  }

  "it should error if a column misses a foreign key constraint" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY,
         |   nick int UNIQUE
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   author int references author(nick)
         | );
         | CREATE TABLE blog_to_author (
         |   blog int,
         |   author int
         | )
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  authors: [Author] @relation(name:"BlogToAuthor")
         |}
         |
         |type Author @db(name: "author"){
         |  id: Int! @id
         |}
         |
         |type BlogToAuthor @linkTable @db(name:"blog_to_author"){
         |  blog: Blog!
         |  author: Author!
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability, RelationLinkTableCapability))
    errors should have(size(2))
    val (error1, error2) = (errors(0), errors(1))
    error1.`type` should be("BlogToAuthor")
    error1.field should be(Some("author"))
    error1.description should be("The column `author` for the relation table `blog_to_author` is missing a foreign key constraint.")
    error2.`type` should be("BlogToAuthor")
    error2.field should be(Some("blog"))
    error2.description should be("The column `blog` for the relation table `blog_to_author` is missing a foreign key constraint.")
  }

  "it should error if a column of a relation table is referencing the wrong table" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY,
         |   nick int UNIQUE
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   author int references author(nick)
         | );
         | CREATE TABLE wrong_table(
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog_to_author (
         |   blog int references wrong_table(id),
         |   author int references wrong_table(id)
         | )
       """.stripMargin
    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  authors: [Author] @relation(name:"BlogToAuthor")
         |}
         |
         |type Author @db(name: "author"){
         |  id: Int! @id
         |}
         |
         |type BlogToAuthor @linkTable @db(name:"blog_to_author"){
         |  blog: Blog!
         |  author: Author!
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability, RelationLinkTableCapability))
    errors should have(size(2))
    val (error1, error2) = (errors(0), errors(1))
    error1.`type` should be("BlogToAuthor")
    error1.field should be(Some("author"))
    error1.description should be(
      "The column `author` of the relation table `blog_to_author` is not referencing the right table. It should reference the table of model `Author` but is referencing the table `wrong_table`.")
    error2.`type` should be("BlogToAuthor")
    error2.field should be(Some("blog"))
    error2.description should be(
      "The column `blog` of the relation table `blog_to_author` is not referencing the right table. It should reference the table of model `Blog` but is referencing the table `wrong_table`.")
  }

  private def deployThatMustError(dataModel: String, capabilities: ConnectorCapabilities): Vector[DeployError] = {
    val actualCapabilities = ConnectorCapabilities(capabilities.capabilities ++ Set(IntrospectionCapability))
    val result             = deployThatMustError(dataModel, actualCapabilities, noMigration = true)
    println(result)
    result
  }

  private def deploy(dataModel: String, capabilities: ConnectorCapabilities): DatabaseSchema = {
    val actualCapabilities = ConnectorCapabilities(capabilities.capabilities ++ Set(IntrospectionCapability))
    val result             = deploy(dataModel, actualCapabilities, noMigration = true)
    result
  }
}
