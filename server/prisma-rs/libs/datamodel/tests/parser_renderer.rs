extern crate datamodel;

#[test]
fn test_parser_should_parse_and_renderer_should_render() {
    let dml = r#"
model User {
    id ID @id
    createdAt DateTime
    email String @unique
    name String?
    role Role
    posts Post[] @onDelete(CASCADE)
    profile Profile?
    @@db(name: "user")
}

model Profile {
    id ID @id
    user User
    bio String
    @@db("profile")
}

model Post {
    id ID @id
    createdAt DateTime
    updatedAt DateTime
    title String @default("Default-Title")
    wasLiked boolean @default(false)
    author User @relation(name: "author")
    published Boolean @default(false)
    categories Category[]
    @@db(name: "post")
}

model Category {
    id ID @id
    name String
    posts Post[]
    cat CategoryEnum
    @@db(name: "category")
}

model PostToCategory {
    post Post
    category Category
    @@db(name: "post_to_category")
}

enum CategoryEnum {
    A
    B
    C
}
"#;

    let ast = datamodel::parser::parse(&String::from(dml)).expect("Failed to parse");

    let mut buffer = std::io::Cursor::new(Vec::<u8>::new());
    let mut renderer = datamodel::renderer::Renderer::new(&mut buffer);

    renderer.render(&ast);

    let rendered = String::from_utf8(buffer.into_inner()).unwrap();

    assert_eq!(dml, rendered);
}
