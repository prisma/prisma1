extern crate datamodel;

const DATAMODEL_STRING: &str = r#"
model User {
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
    categories Category[]
    @@db("post")
}

model Category {
    id Int @id
    name String
    posts Post[]
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
    b B @relation(references: [id])
}

model B {
    id Int @id
    a A
}

enum CategoryEnum {
    A
    B
    C
}
"#;

#[test]
fn test_parser_renderer_via_ast() {
    let ast = datamodel::parse_to_ast(DATAMODEL_STRING).unwrap();
    let rendered = datamodel::render_ast(&ast);

    assert_eq!(DATAMODEL_STRING, rendered);
}

#[test]
fn test_parser_renderer_via_dml() {
    let dml = datamodel::parse(DATAMODEL_STRING).unwrap();
    let rendered = datamodel::render(&dml).unwrap();

    print!("{}", rendered);

    assert_eq!(DATAMODEL_STRING, rendered);
}