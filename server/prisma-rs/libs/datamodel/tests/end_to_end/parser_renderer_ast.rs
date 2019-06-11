extern crate datamodel;

const DATAMODEL_STRING: &str = r#"model User {
    id Int @id
    createdAt DateTime
    email String @unique
    name String?
    posts Post[] @relation(onDelete: CASCADE)
    profile Profile?
    @@db("user")
}

model Profile {
    id Int @id
    user User
    bio String
    @@db("profile")
}

model Post {
    id Int @id
    createdAt DateTime
    updatedAt DateTime
    title String @default("Default-Title")
    wasLiked Boolean @default(false)
    author User @relation("author")
    published Boolean @default(false)
    categories PostToCategory[]
    @@db("post")
}

model Category {
    id Int @id
    name String
    posts PostToCategory[]
    cat CategoryEnum
    @@db("category")
}

model PostToCategory {
    id Int @id
    post Post
    category Category
    @@db("post_to_category")
}

model A {
    id Int @id
    b B
}

model B {
    id Int @id
    a A
}

enum CategoryEnum {
    A
    B
    C
}"#;

#[test]
fn test_parser_renderer_via_ast() {
    let ast = datamodel::parse_to_ast(DATAMODEL_STRING).unwrap();
    let rendered = datamodel::render_ast(&ast);

    assert_eq!(DATAMODEL_STRING, rendered);
}

const MANY_TO_MANY_DATAMODEL: &str = r#"model Blog {
    id Int @id
    name String
    viewCount Int
    posts Post[]
    authors Author[] @relation("AuthorToBlogs")
}

model Author {
    id Int @id
    name String?
    authors Blog[] @relation("AuthorToBlogs")
}

model Post {
    id Int @id
    title String
    tags String[]
    blog Blog
}"#;

#[test]
fn test_parser_renderer_many_to_many_via_ast() {
    let ast = datamodel::parse_to_ast(MANY_TO_MANY_DATAMODEL).unwrap();
    let rendered = datamodel::render_ast(&ast);

    print!("{}", rendered);

    assert_eq!(rendered, MANY_TO_MANY_DATAMODEL);
}

const DATAMODEL_WITH_TYPES: &str = r#"type ID = Int @id

model Author {
    id ID
    name String?
    authors Blog[] @relation("AuthorToBlogs")
}"#;

#[test]
fn test_parser_renderer_types_via_ast() {
    let ast = datamodel::parse_to_ast(DATAMODEL_WITH_TYPES).unwrap();
    let rendered = datamodel::render_ast(&ast);

    print!("{}", rendered);

    assert_eq!(rendered, DATAMODEL_WITH_TYPES);
}

const DATAMODEL_WITH_SOURCE: &str = r#"source pg1 {
    type = "Postgres"
    url = "https://localhost/postgres1"
}

model Author {
    id ID
    name String?
    authors Blog[] @relation("AuthorToBlogs")
}"#;

#[test]
fn test_parser_renderer_sources_via_ast() {
    let ast = datamodel::parse_to_ast(DATAMODEL_WITH_SOURCE).unwrap();
    let rendered = datamodel::render_ast(&ast);

    print!("{}", rendered);

    assert_eq!(rendered, DATAMODEL_WITH_SOURCE);
}

const DATAMODEL_WITH_SOURCE_AND_COMMENTS: &str = r#"/// Super cool postgres source.
source pg1 {
    type = "postgres"
    url = "https://localhost/postgres1"
}

/// My author model.
model Author {
    id Int @id
    /// Name of the author.
    name String?
    createdAt DateTime @default(now())
}"#;

#[test]
fn test_parser_renderer_sources_and_comments_via_ast() {
    let ast = datamodel::parse_to_ast(DATAMODEL_WITH_SOURCE_AND_COMMENTS).unwrap();
    let rendered = datamodel::render_ast(&ast);

    print!("{}", rendered);

    assert_eq!(rendered, DATAMODEL_WITH_SOURCE_AND_COMMENTS);
}
