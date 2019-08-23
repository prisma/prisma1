extern crate datamodel;
use pretty_assertions::assert_eq;

const DATAMODEL_STRING: &str = r#"model User {
  id        Int      @id
  createdAt DateTime
  email     String   @unique
  name      String?
  posts     Post[]   @relation(onDelete: CASCADE)
  profile   Profile?

  @@map("user")
}

model Profile {
  id   Int    @id
  user User
  bio  String

  @@map("profile")
}

model Post {
  id         Int              @id
  createdAt  DateTime
  updatedAt  DateTime
  title      String           @default("Default-Title")
  wasLiked   Boolean          @default(false)
  author     User             @relation("author")
  published  Boolean          @default(false)
  categories PostToCategory[]

  @@map("post")
}

model Category {
  id    Int              @id
  name  String
  posts PostToCategory[]
  cat   CategoryEnum

  @@map("category")
}

model PostToCategory {
  id       Int      @id
  post     Post
  category Category

  @@map("post_to_category")
}

model A {
  id Int @id
  b  B
}

model B {
  id Int @id
  a  A
}

enum CategoryEnum {
  A
  B
  C
}"#;

#[test]
fn test_parser_renderer_via_ast() {
    let ast = datamodel::parse_to_ast(DATAMODEL_STRING).expect("failed to parse");
    let rendered = datamodel::render_ast(&ast);

    print!("{}", rendered);

    assert_eq!(DATAMODEL_STRING, rendered);
}

const MANY_TO_MANY_DATAMODEL: &str = r#"model Blog {
  id        Int      @id
  name      String
  viewCount Int
  posts     Post[]
  authors   Author[] @relation("AuthorToBlogs")
}

model Author {
  id      Int     @id
  name    String?
  authors Blog[]  @relation("AuthorToBlogs")
}

model Post {
  id    Int      @id
  title String
  tags  String[]
  blog  Blog
}"#;

#[test]
fn test_parser_renderer_many_to_many_via_ast() {
    let ast = datamodel::parse_to_ast(MANY_TO_MANY_DATAMODEL).expect("failed to parse");
    let rendered = datamodel::render_ast(&ast);

    print!("{}", rendered);

    assert_eq!(rendered, MANY_TO_MANY_DATAMODEL);
}

const DATAMODEL_WITH_TYPES: &str = r#"type ID = Int @id

model Author {
  id      ID
  name    String?
  authors Blog[]  @relation("AuthorToBlogs")
}"#;

#[test]
fn test_parser_renderer_types_via_ast() {
    let ast = datamodel::parse_to_ast(DATAMODEL_WITH_TYPES).expect("failed to parse");
    let rendered = datamodel::render_ast(&ast);

    print!("{}", rendered);

    assert_eq!(rendered, DATAMODEL_WITH_TYPES);
}

const DATAMODEL_WITH_SOURCE: &str = r#"datasource pg1 {
  provider = "Postgres"
  url      = "https://localhost/postgres1"
}

model Author {
  id      ID
  name    String?
  authors Blog[]  @relation("AuthorToBlogs")
}"#;

#[test]
fn test_parser_renderer_sources_via_ast() {
    let ast = datamodel::parse_to_ast(DATAMODEL_WITH_SOURCE).expect("failed to parse");
    let rendered = datamodel::render_ast(&ast);

    print!("{}", rendered);

    assert_eq!(rendered, DATAMODEL_WITH_SOURCE);
}

const DATAMODEL_WITH_SOURCE_AND_COMMENTS: &str = r#"/// Super cool postgres source.
datasource pg1 {
  provider = "postgres"
  url      = "https://localhost/postgres1"
}

/// My author model.
model Author {
  id        Int      @id
  /// Name of the author.
  name      String?
  createdAt DateTime @default(now())
}"#;

#[test]
fn test_parser_renderer_sources_and_comments_via_ast() {
    let ast = datamodel::parse_to_ast(DATAMODEL_WITH_SOURCE_AND_COMMENTS).expect("failed to parse");
    let rendered = datamodel::render_ast(&ast);

    print!("{}", rendered);

    assert_eq!(rendered, DATAMODEL_WITH_SOURCE_AND_COMMENTS);
}

const DATAMODEL_WITH_TABS: &str = r#"/// Super cool postgres source.
datasource\tpg1\t{
\tprovider\t=\t\t"postgres"
\turl\t=\t"https://localhost/postgres1"
}
\t
///\tMy author\tmodel.
model\tAuthor\t{
\tid\tInt\t@id
\t/// Name of the author.
\t\tname\tString?
\tcreatedAt\tDateTime\t@default(now())
}"#;

const DATAMODEL_WITH_SPACES: &str = r#"/// Super cool postgres source.
datasource pg1 {
  provider = "postgres"
  url      = "https://localhost/postgres1"
}

/// My author\tmodel.
model Author {
  id        Int      @id
  /// Name of the author.
  name      String?
  createdAt DateTime @default(now())
}"#;

#[test]
fn test_parser_renderer_with_tabs() {
    // replaces \t placeholder with a real tab
    let tabbed_dm = DATAMODEL_WITH_TABS.replace("\\t", "\t");
    let ast = datamodel::parse_to_ast(&tabbed_dm).expect("failed to parse");
    let rendered = datamodel::render_ast(&ast);
    assert_eq!(rendered, DATAMODEL_WITH_SPACES.replace("\\t", "\t"));
}
