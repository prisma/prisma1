extern crate datamodel;

#[test]
fn test_parser_should_not_crash() {
    let dml = r#"
model User {
    id: ID @primary
    createdAt: DateTime
    email: String @unique
    name: String?
    role: Role
    posts: Post[] @onDelete(CASCADE)
    profile: Profile?
}
@db(name: "user")

model Profile {
    id: ID @primary
    user: User
    bio: String
}
@db("profile")

model Post {
    id: ID @primary
    createdAt: DateTime
    updatedAt: DateTime
    title: String @default("Default-Title")
    wasLiked: boolean @default(false)
    author: User @relation(name: "author")
    published: Boolean = false
    categories: Category[]
}
@db(name: "post")

model Category {
    id ID @primary
    name String
    posts Post[]
    cat CategoryEnum
}
@db(name: "category")

model PostToCategory {
    post: Post(id)
    category: Category
}
@db(name: "post_to_category")

enum CategoryEnum {
    A
    B
    C
}"#;

    datamodel::parser::parse(&String::from(dml));
}
