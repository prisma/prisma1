extern crate datamodel;

const DATAMODEL_STRING: &str = r#"model User {
    id Int @id
    createdAt DateTime
    email String @unique
    name String?
    posts Post[] @relation("author", onDelete: CASCADE)
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
fn test_parser_renderer_via_dml() {
    let dml = datamodel::parse(DATAMODEL_STRING).unwrap();
    let rendered = datamodel::render(&dml).unwrap();

    print!("{}", rendered);

    assert_eq!(DATAMODEL_STRING, rendered);
}

// TODO: Test that N:M relation names are correctly handled as soon as we
// get relation table support.
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
fn test_parser_renderer_many_to_many_via_dml() {
    let dml = datamodel::parse(MANY_TO_MANY_DATAMODEL).unwrap();
    let rendered = datamodel::render(&dml).unwrap();

    print!("{}", rendered);

    assert_eq!(rendered, MANY_TO_MANY_DATAMODEL);
}

const DATAMODEL_STRING_WITH_COMMENTS: &str = r#"/// Cool user model
model User {
    id Int @id
    /// Created at field
    createdAt DateTime
    email String @unique
    /// Name field.
    /// Multi line comment.
    name String?
    @@db("user")
}"#;

#[test]
fn test_parser_renderer_model_with_comments_via_dml() {
    let dml = datamodel::parse(DATAMODEL_STRING_WITH_COMMENTS).unwrap();
    let rendered = datamodel::render(&dml).unwrap();

    print!("{}", rendered);

    assert_eq!(rendered, DATAMODEL_STRING_WITH_COMMENTS);
}
