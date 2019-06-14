extern crate datamodel;

const DATAMODEL_STRING: &str = r#"model User {
  id        Int      @id
  createdAt DateTime
  email     String   @unique
  name      String?
  posts     Post[]   @relation("author", onDelete: CASCADE)
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
fn test_dmmf_roundtrip() {
    let dml = datamodel::parse(&DATAMODEL_STRING).unwrap();
    let dmmf = datamodel::dmmf::render_to_dmmf(&dml);
    let dml2 = datamodel::dmmf::parse_from_dmmf(&dmmf);
    let rendered = datamodel::render(&dml2).unwrap();

    println!("{}", rendered);

    assert_eq!(DATAMODEL_STRING, rendered);
}

const DATAMODEL_STRING_WITH_FUNCTIONS: &str = r#"model User {
  id        Int      @id
  createdAt DateTime @default(now())
  someId    String   @default(cuid()) @unique
}"#;

#[test]
fn test_dmmf_roundtrip_with_functions() {
    let dml = datamodel::parse(&DATAMODEL_STRING_WITH_FUNCTIONS).unwrap();
    let dmmf = datamodel::dmmf::render_to_dmmf(&dml);
    let dml2 = datamodel::dmmf::parse_from_dmmf(&dmmf);
    let rendered = datamodel::render(&dml2).unwrap();

    println!("{}", rendered);

    assert_eq!(DATAMODEL_STRING_WITH_FUNCTIONS, rendered);
}

const DATAMODEL_WITH_SOURCE: &str = r#"datasource pg1 {
  provider = "postgres"
  url      = "https://localhost/postgres1"
}

model Author {
  id        Int      @id
  name      String?
  createdAt DateTime @default(now())
}"#;

#[test]
fn test_dmmf_roundtrip_with_sources() {
    let rendered = dmmf_roundtrip(DATAMODEL_WITH_SOURCE);

    assert_eq!(DATAMODEL_WITH_SOURCE, rendered);
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
fn test_dmmf_roundtrip_with_sources_and_comments() {
    let rendered = dmmf_roundtrip(DATAMODEL_WITH_SOURCE_AND_COMMENTS);

    assert_eq!(DATAMODEL_WITH_SOURCE_AND_COMMENTS, rendered);
}

const DATAMODEL_WITH_GENERATOR: &str = r#"generator js {
  provider     = "javascript"
  output       = "./client"
  extra_config = "test"
}

model Author {
  id        Int      @id
  name      String?
  createdAt DateTime @default(now())
}"#;

#[test]
fn test_dmmf_roundtrip_with_generator() {
    let rendered = dmmf_roundtrip(DATAMODEL_WITH_GENERATOR);

    assert_eq!(DATAMODEL_WITH_GENERATOR, rendered);
}

fn dmmf_roundtrip(input: &str) -> String {
    let dml = datamodel::parse(input).unwrap();
    let config = datamodel::load_configuration(input).unwrap();

    let dmmf = datamodel::dmmf::render_to_dmmf(&dml);
    let mcf = datamodel::config_to_mcf_json(&config);

    let dml2 = datamodel::dmmf::parse_from_dmmf(&dmmf);
    let config = datamodel::config_from_mcf_json(&mcf);

    let rendered = datamodel::render_with_config(&dml2, &config).unwrap();

    println!("{}", rendered);

    rendered
}
