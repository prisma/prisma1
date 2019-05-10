package com.prisma.deploy.migration
import com.prisma.deploy.connector.DatabaseSchema
import com.prisma.deploy.migration.validation.DeployError
import com.prisma.deploy.specutils.PassiveDeploySpecBase
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability}
import com.prisma.shared.models.ConnectorCapability._
import org.scalatest.{Matchers, WordSpecLike}

class DatabaseSchemaValidatorSpec extends WordSpecLike with Matchers with PassiveDeploySpecBase {
  override def runOnlyForCapabilities = Set(MigrationsCapability)

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
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.blog (
         |   id INTEGER PRIMARY KEY
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
    error.description should be("Could not find the table for the model `Blog` in the database.")
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
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.blog (
         |   id INTEGER PRIMARY KEY NOT NULL
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
    error.description should be("Could not find the column for the field `title` in the database.")
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
         |   id int NOT NULL, PRIMARY KEY(id),
         |   title mediumtext
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL,
         |   title mediumtext
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
      "The underlying column for the field `title` has an incompatible type. The field has type `Int` and the column has type `String`.")
  }

  "it should error if a column is required but the field is not required" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   title text NOT NULL
         |);
       """.stripMargin

    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id),
         |   title mediumtext NOT NULL
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL,
         |   title mediumtext NOT NULL
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: String
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(Some("title"))
    error.description should be(
      "The underlying column for the field `title` is required but the field is declared optional. Please declare it as required: `title: String!`.")
  }

  "it should error if a column is not required but the field is required" in {
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
         |   id int NOT NULL, PRIMARY KEY(id),
         |   title mediumtext
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL,
         |   title mediumtext
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  title: String!
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(Some("title"))
    error.description should be(
      "The underlying column for the field `title` is optional but the field is declared required. Please declare it as optional by removing the `!`: `title: String`.")
  }

  "it should succeed if an id column has the right type" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id varchar(25) PRIMARY KEY
         |);
       """.stripMargin

    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id char(25), PRIMARY KEY(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.blog (
         |   id char(25) PRIMARY KEY NOT NULL
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: ID! @id
         |}
       """.stripMargin

    deploy(dataModel, ConnectorCapabilities(IntIdCapability))
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
         | CREATE TABLE author (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id),
         |   author int, FOREIGN KEY (author) REFERENCES author(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author (
         |   id int PRIMARY KEY NOT NULL
         | );
         |
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL,
         |   author int,
         |   FOREIGN KEY (author) REFERENCES author(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  author: Author @relation(link: INLINE)
         |}
         |
         |type Author @db(name: "author"){
         |  id: Int! @id
         |}
       """.stripMargin

    deploy(dataModel, ConnectorCapabilities(IntIdCapability))
  }

  "it should error if an inline relation field is required but the column is optional" in {
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
         | CREATE TABLE author (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id),
         |   author int, FOREIGN KEY (author) REFERENCES author(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author (
         |   id int PRIMARY KEY NOT NULL
         | );
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL,
         |   author int,
         |   FOREIGN KEY (author) REFERENCES author(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
      "The underlying column for the field `author` is optional but the field is declared required. Please declare it as optional by removing the `!`: `author: Author`.")
  }

  "it should error if an inline relation field is optional but the column is required" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY,
         |   author int NOT NULL references author(id)
         |);
       """.stripMargin

    val mysql =
      s"""
         | CREATE TABLE author (
         |   id int NOT NULL,
         |   PRIMARY KEY(id)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id),
         |   author int NOT NULL, FOREIGN KEY (author) REFERENCES author(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author (
         |   id int PRIMARY KEY NOT NULL
         | );
         |
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL,
         |   author int NOT NULL,
         |   FOREIGN KEY (author) REFERENCES author(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  author: Author @relation(link: INLINE)
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
      "The underlying column for the field `author` is required but the field is declared optional. Please declare it as required: `author: Author!`.")
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
         | CREATE TABLE author(
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author(
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE $projectId.blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
    error.description should be("Could not find the column for the inline relation field `author` in the database.")
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
         | CREATE TABLE author (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE wrong_table (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id),
         |   author int, FOREIGN KEY (author) REFERENCES wrong_table(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author (
         |   id int PRIMARY KEY NOT NULL
         | );
         | CREATE TABLE $projectId.wrong_table (
         |   id int PRIMARY KEY NOT NULL
         | );
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL,
         |   author int, FOREIGN KEY (author) REFERENCES wrong_table(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
      "The column for the inline relation field `author` is not referencing the right table. It should reference the table of model `Author` but is referencing the table `wrong_table`.")
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
         | CREATE TABLE author(
         |   id int NOT NULL,
         |   nick int, UNIQUE KEY(nick)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id),
         |   author int, FOREIGN KEY (author) REFERENCES author(nick)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author(
         |   id int PRIMARY KEY NOT NULL,
         |   nick int UNIQUE
         | );
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL,
         |   author int, FOREIGN KEY (author) REFERENCES author(nick)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
      "The column for the inline relation field `author` is not referencing a valid column. Those columns must always reference the column of the id field of related model. So it should reference `id` instead of `nick`.")
  }

  "it should error if a @relationTable is missing in the database" in {
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
         | CREATE TABLE author(
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author(
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE $projectId.blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
         |type BlogToAuthor @relationTable @db(name:"blog_to_author"){
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

  "it should error if the id column of a @relationTable is missing in the database" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog_to_author (
         |   blog int references blog(id),
         |   author int references author(id)
         | )
       """.stripMargin

    val mysql =
      s"""
         | CREATE TABLE author (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog_to_author(
         |   author int, FOREIGN KEY (author) REFERENCES author(id),
         |   blog int, FOREIGN KEY (blog) REFERENCES blog(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author (
         |   id int PRIMARY KEY NOT NULL
         | );
         |
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL
         | );
         |
         | CREATE TABLE $projectId.blog_to_author(
         |   author int,
         |   blog int,
         |   FOREIGN KEY (blog) REFERENCES blog(id),
         |   FOREIGN KEY (author) REFERENCES author(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
         |type BlogToAuthor @relationTable @db(name:"blog_to_author"){
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
    error.description should be("The link table `BlogToAuthor` is missing the column `id`.")
  }

  "it should succeed if the @relationTable matches the relation table in database" in {
    val postgres =
      s"""
         | CREATE TABLE author (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog_to_author (
         |   blog int references blog(id),
         |   author int references author(id)
         | );
       """.stripMargin

    val mysql =
      s"""
         | CREATE TABLE author(
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog_to_author(
         |   author int, FOREIGN KEY (author) REFERENCES author(id),
         |   blog int, FOREIGN KEY (blog) REFERENCES blog(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author(
         |   id int PRIMARY KEY NOT NULL
         | );
         |
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL
         | );
         |
         | CREATE TABLE $projectId.blog_to_author(
         |   author int,
         |   blog int,
         |   FOREIGN KEY (author) REFERENCES author(id),
         |   FOREIGN KEY (blog) REFERENCES blog(id)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
         |type BlogToAuthor @relationTable @db(name:"blog_to_author"){
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
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog_to_author (
         |   blog int,
         |   author int
         | )
       """.stripMargin

    val mysql =
      s"""
         | CREATE TABLE author(
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog_to_author(
         |   author int,
         |   blog int
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author(
         |   id int PRIMARY KEY NOT NULL
         | );
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL
         | );
         | CREATE TABLE $projectId.blog_to_author(
         |   author int,
         |   blog int
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
         |type BlogToAuthor @relationTable @db(name:"blog_to_author"){
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
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
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
         | CREATE TABLE author (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE wrong_table(
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog_to_author(
         |   blog int, FOREIGN KEY (blog) REFERENCES wrong_table(id),
         |   author int, FOREIGN KEY (author) REFERENCES wrong_table(id)
         | )
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.author (
         |   id int PRIMARY KEY NOT NULL
         | );
         |
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL
         | );
         |
         | CREATE TABLE $projectId.wrong_table(
         |   id int PRIMARY KEY NOT NULL
         | );
         |
         | CREATE TABLE $projectId.blog_to_author(
         |   blog int,
         |   author int,
         |   FOREIGN KEY (blog) REFERENCES wrong_table(id),
         |   FOREIGN KEY (author) REFERENCES wrong_table(id)
         | )
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

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
         |type BlogToAuthor @relationTable @db(name:"blog_to_author"){
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

  "it should succeed if the underlying table for a legacy scalar list field does exist" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog_tags (
         |   "nodeId" varchar(25) NOT NULL,
         |   "position" int4 NOT NULL,
         |   "value" text NOT NULL,
         |   PRIMARY KEY ("nodeId","position")
         | )
       """.stripMargin

    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog_tags (
         |   `nodeId` char(25) NOT NULL,
         |   `position` int NOT NULL,
         |   `value` mediumtext NOT NULL,
         |   PRIMARY KEY (`nodeId`,`position`)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL
         | );
         | CREATE TABLE $projectId.blog_tags (
         |   `nodeId` char(25) NOT NULL,
         |   `position` int NOT NULL,
         |   `value` mediumtext NOT NULL,
         |   PRIMARY KEY (`nodeId`,`position`)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  tags: [Json] @scalarList(strategy: RELATION)
         |}
       """.stripMargin

    deploy(dataModel, ConnectorCapabilities(IntIdCapability, NonEmbeddedScalarListCapability))
  }

  "it should error if the underlying table for a legacy scalar list field does not exist" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         | );
       """.stripMargin

    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  tags: [String] @scalarList(strategy: RELATION)
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability, NonEmbeddedScalarListCapability))
    errors should have(size(1))
    val error = errors.head
    error.`type` should be("Blog")
    error.field should be(Some("tags"))
    error.description should be("Could not find the underlying table for the scalar list field `tags`.")
  }

  "it should error if the columns in underlying table for a legacy scalar list field have the wrong type" in {
    val postgres =
      s"""
         | CREATE TABLE blog (
         |   id SERIAL PRIMARY KEY
         | );
         | CREATE TABLE blog_tags (
         |   "nodeId" boolean NOT NULL,
         |   "position" boolean NOT NULL,
         |   "value" boolean NOT NULL,
         |   PRIMARY KEY ("nodeId","position")
         | );
       """.stripMargin

    val mysql =
      s"""
         | CREATE TABLE blog (
         |   id int NOT NULL, PRIMARY KEY(id)
         | );
         | CREATE TABLE blog_tags (
         |   `nodeId` bool NOT NULL,
         |   `position` bool NOT NULL,
         |   `value` bool NOT NULL,
         |   PRIMARY KEY (`nodeId`,`position`)
         | );
       """.stripMargin

    val sqlite =
      s"""
         | CREATE TABLE $projectId.blog (
         |   id int PRIMARY KEY NOT NULL
         | );
         | CREATE TABLE $projectId.blog_tags (
         |   `nodeId` bool NOT NULL,
         |   `position` bool NOT NULL,
         |   `value` bool NOT NULL,
         |   PRIMARY KEY (`nodeId`,`position`)
         | );
       """.stripMargin

    setup(SQLs(postgres = postgres, mysql = mysql, sqlite = sqlite))

    val dataModel =
      s"""
         |type Blog @db(name: "blog"){
         |  id: Int! @id
         |  tags: [String] @scalarList(strategy: RELATION)
         |}
       """.stripMargin

    val errors = deployThatMustError(dataModel, ConnectorCapabilities(IntIdCapability, NonEmbeddedScalarListCapability))
    errors should have(size(3))
    errors.forall(_.`type` == "Blog") should be(true)
    errors.forall(_.field.contains("tags")) should be(true)
    val (error1, error2, error3) = (errors(0), errors(1), errors(2))
    error1.description should be(
      "The column `nodeId` in the underlying table for the scalar list field `tags` has the wrong type. It has the type `Boolean` but it should have the type `String`.")
    error2.description should be(
      "The column `position` in the underlying table for the scalar list field `tags` has the wrong type. It has the type `Boolean` but it should have the type `Int`.")
    error3.description should be(
      "The column `value` in the underlying table for the scalar list field `tags` has the wrong type. It has the type `Boolean` but it should have the type `String`.")
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
