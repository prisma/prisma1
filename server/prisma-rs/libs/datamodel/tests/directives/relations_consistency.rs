use crate::common::*;

#[test]
fn should_add_back_relations() {
    let dml = r#"
    model User {
        id Int @id
        posts Post[]
    }

    model Post {
        post_id Int @id
    }
    "#;

    let schema = parse(dml);
    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_relation_to_fields(&["id"])
        .assert_arity(&datamodel::dml::FieldArity::Optional);

    let user_model = schema.assert_has_model("User");
    user_model
        .assert_has_field("posts")
        .assert_relation_to("Post")
        .assert_relation_to_fields(&[])
        .assert_arity(&datamodel::dml::FieldArity::List);
}

#[test]
fn must_add_to_fields_on_the_right_side_for_one_to_one_relations() {
    // the to fields are always added to model with the lower name in lexicographic order
    let dml = r#"
    model User1 {
      id         String @default(cuid()) @id @unique
      referenceA User2
    }
    
    model User2 {
      id         String @default(cuid()) @id @unique
      referenceB User1
    }
    
    model User3 {
      id         String @default(cuid()) @id @unique
      referenceB User4
    }
    
    model User4 {
      id         String @default(cuid()) @id @unique
      referenceA User3
    }
    "#;

    let schema = parse(dml);

    schema
        .assert_has_model("User1")
        .assert_has_field("referenceA")
        .assert_relation_to_fields(&["id"]);

    schema
        .assert_has_model("User2")
        .assert_has_field("referenceB")
        .assert_relation_to_fields(&[]);

    schema
        .assert_has_model("User3")
        .assert_has_field("referenceB")
        .assert_relation_to_fields(&["id"]);

    schema
        .assert_has_model("User4")
        .assert_has_field("referenceA")
        .assert_relation_to_fields(&[]);
}

#[test]
fn must_add_to_fields_correctly_for_implicit_back_relations_for_one_to_one_relations() {
    // Post is lower that User. So the to_fields should be stored in Post.
    let dml = r#"
    model User {
        user_id Int  @id
        post    Post 
    }

    model Post {
        post_id Int @id
    }
    "#;

    let schema = dbg!(parse(dml));

    schema
        .assert_has_model("User")
        .assert_has_field("post")
        .assert_relation_to_fields(&[]);
    schema
        .assert_has_model("Post")
        .assert_has_field("user")
        .assert_relation_to_fields(&["user_id"]);
}

#[test]
fn must_add_to_fields_on_both_sides_for_many_to_many_relations() {
    let dml = r#"
    model User {
        user_id Int    @id
        posts   Post[] 
    }

    model Post {
        post_id Int    @id
        users   User[]
    }
    "#;

    let schema = parse(dml);

    schema
        .assert_has_model("User")
        .assert_has_field("posts")
        .assert_relation_to_fields(&["post_id"]);
    schema
        .assert_has_model("Post")
        .assert_has_field("users")
        .assert_relation_to_fields(&["user_id"]);
}

#[test]
fn must_add_to_fields_on_both_sides_for_one_to_many_relations() {
    let dml = r#"
    model User {
        user_id Int    @id
        posts   Post[] 
    }

    model Post {
        post_id Int    @id
        user    User
    }
    "#;

    let schema = parse(dml);

    schema
        .assert_has_model("User")
        .assert_has_field("posts")
        .assert_relation_to_fields(&[]);
    schema
        .assert_has_model("Post")
        .assert_has_field("user")
        .assert_relation_to_fields(&["user_id"]);

    // prove that lexicographic order does not have an influence.
    let dml = r#"
    model User {
        user_id Int    @id
        post    Post 
    }

    model Post {
        post_id Int    @id
        users   User[]
    }
    "#;

    let schema = parse(dml);

    schema
        .assert_has_model("User")
        .assert_has_field("post")
        .assert_relation_to_fields(&["post_id"]);
    schema
        .assert_has_model("Post")
        .assert_has_field("users")
        .assert_relation_to_fields(&[]);
}

#[test]
fn should_not_add_back_relation_fields_for_many_to_many_relations() {
    // Equal name for both fields was a bug trigger.
    let dml = r#"
model Blog {
  id Int @id
  authors Author[]
}

model Author {
  id Int @id
  authors Blog[]
}
    "#;

    let schema = parse(dml);

    let author_model = schema.assert_has_model("Author");
    author_model
        .assert_has_field("authors")
        .assert_relation_to("Blog")
        .assert_relation_name("AuthorToBlog")
        .assert_relation_to_fields(&["id"])
        .assert_arity(&datamodel::dml::FieldArity::List);

    author_model.assert_has_field("id");

    let blog_model = schema.assert_has_model("Blog");
    blog_model
        .assert_has_field("authors")
        .assert_relation_to("Author")
        .assert_relation_name("AuthorToBlog")
        .assert_relation_to_fields(&["id"])
        .assert_arity(&datamodel::dml::FieldArity::List);

    blog_model.assert_has_field("id");

    // Assert nothing else was generated.
    // E.g. no erronous back relations.
    assert_eq!(author_model.fields().count(), 2);
    assert_eq!(blog_model.fields().count(), 2);
}

#[test]
fn should_add_back_relations_for_more_complex_cases() {
    let dml = r#"
    model User {
        id Int @id
        posts Post[]
    }

    model Post {
        post_id Int @id
        comments Comment[]
        categories PostToCategory[]
    }

    model Comment {
        comment_id Int @id
    }

    model Category {
        category_id Int @id
        posts PostToCategory[]
    }

    model PostToCategory {
        id Int @id
        post Post
        category Category
        @@map("post_to_category")
    }
    "#;

    let schema = parse(dml);

    // PostToUser

    // Forward
    schema
        .assert_has_model("Post")
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_relation_to_fields(&["id"])
        .assert_relation_name("PostToUser")
        .assert_is_generated(true)
        .assert_arity(&datamodel::dml::FieldArity::Optional);

    // Backward
    schema
        .assert_has_model("User")
        .assert_has_field("posts")
        .assert_relation_to("Post")
        .assert_relation_to_fields(&[])
        .assert_relation_name("PostToUser")
        .assert_is_generated(false)
        .assert_arity(&datamodel::dml::FieldArity::List);

    // Comments

    // Forward
    schema
        .assert_has_model("Comment")
        .assert_has_field("post")
        .assert_relation_to("Post")
        .assert_relation_to_fields(&["post_id"])
        .assert_relation_name("CommentToPost")
        .assert_is_generated(true)
        .assert_arity(&datamodel::dml::FieldArity::Optional);

    // Backward
    schema
        .assert_has_model("Post")
        .assert_has_field("comments")
        .assert_relation_to("Comment")
        .assert_relation_to_fields(&[])
        .assert_relation_name("CommentToPost")
        .assert_is_generated(false)
        .assert_arity(&datamodel::dml::FieldArity::List);

    // CategoryToPostToCategory

    // Backward
    schema
        .assert_has_model("Category")
        .assert_has_field("posts")
        .assert_relation_to("PostToCategory")
        .assert_relation_to_fields(&[])
        .assert_relation_name("CategoryToPostToCategory")
        .assert_is_generated(false)
        .assert_arity(&datamodel::dml::FieldArity::List);

    // Forward
    schema
        .assert_has_model("PostToCategory")
        .assert_has_field("category")
        .assert_relation_to("Category")
        .assert_relation_to_fields(&["category_id"])
        .assert_relation_name("CategoryToPostToCategory")
        .assert_is_generated(false)
        .assert_arity(&datamodel::dml::FieldArity::Required);

    // PostToPostToCategory

    // Backward
    schema
        .assert_has_model("Post")
        .assert_has_field("categories")
        .assert_relation_to("PostToCategory")
        .assert_relation_to_fields(&[])
        .assert_relation_name("PostToPostToCategory")
        .assert_is_generated(false)
        .assert_arity(&datamodel::dml::FieldArity::List);

    // Forward
    schema
        .assert_has_model("PostToCategory")
        .assert_has_field("post")
        .assert_relation_to("Post")
        .assert_relation_to_fields(&["post_id"])
        .assert_relation_name("PostToPostToCategory")
        .assert_is_generated(false)
        .assert_arity(&datamodel::dml::FieldArity::Required);
}

#[test]
fn should_add_to_fields_on_the_correct_side_tie_breaker() {
    let dml = r#"
    model User {
        user_id Int @id
        post Post
    }

    model Post {
        post_id Int @id
        user User
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model
        .assert_has_field("post")
        .assert_relation_to("Post")
        .assert_relation_to_fields(&[]);

    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_relation_to_fields(&["user_id"]);
}

#[test]
fn should_add_to_fields_on_the_correct_side_list() {
    let dml = r#"
    model User {
        id Int @id
        post Post[]
    }

    model Post {
        post_id Int @id
        user User
    }
    "#;

    let schema = parse(dml);
    let user_model = schema.assert_has_model("User");
    user_model
        .assert_has_field("post")
        .assert_relation_to("Post")
        .assert_relation_to_fields(&[]);

    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("user")
        .assert_relation_to("User")
        .assert_relation_to_fields(&["id"]);
}

#[test]
fn should_camel_case_back_relation_field_name() {
    let dml = r#"
    model OhWhatAUser {
        id Int @id
        posts Post[]
    }

    model Post {
        post_id Int @id
    }
    "#;

    let schema = parse(dml);
    let post_model = schema.assert_has_model("Post");
    post_model
        .assert_has_field("ohWhatAUser")
        .assert_relation_to("OhWhatAUser");
}

#[test]
fn should_add_self_back_relation_fields_on_defined_side() {
    let dml = r#"
    model Human {
        id Int @id
        son Human?
    }
    "#;

    let schema = parse(dml);
    let model = schema.assert_has_model("Human");
    model
        .assert_has_field("son")
        .assert_relation_to("Human")
        .assert_relation_to_fields(&[]);

    model
        .assert_has_field("human")
        .assert_relation_to("Human")
        .assert_relation_to_fields(&["id"]);
}

#[test]
fn should_add_embed_ids_on_self_relations() {
    let dml = r#"
    model Human {
        id Int @id
        father Human?
        son Human?
    }
    "#;

    let schema = parse(dml);
    let model = schema.assert_has_model("Human");
    model
        .assert_has_field("son")
        .assert_relation_to("Human")
        .assert_relation_to_fields(&[]);

    model
        .assert_has_field("father")
        .assert_relation_to("Human")
        // Fieldname tie breaker.
        .assert_relation_to_fields(&["id"]);
}

#[test]
fn should_not_get_confused_with_complicated_self_relations() {
    let dml = r#"
    model Human {
        id Int @id
        wife Human? @relation("Marrige")
        husband Human? @relation("Marrige")
        father Human?
        son Human?
        children Human[] @relation("Offspring")
        parent Human? @relation("Offspring")
    }
    "#;

    let schema = parse(dml);
    let model = schema.assert_has_model("Human");
    model
        .assert_has_field("son")
        .assert_relation_to("Human")
        .assert_relation_to_fields(&[]);

    model
        .assert_has_field("father")
        .assert_relation_to("Human")
        // Fieldname tie breaker.
        .assert_relation_to_fields(&["id"]);

    model
        .assert_has_field("wife")
        .assert_relation_to("Human")
        .assert_relation_name("Marrige")
        .assert_relation_to_fields(&[]);

    model
        .assert_has_field("husband")
        .assert_relation_to("Human")
        .assert_relation_name("Marrige")
        .assert_relation_to_fields(&["id"]);

    model
        .assert_has_field("children")
        .assert_relation_to("Human")
        .assert_relation_name("Offspring")
        .assert_relation_to_fields(&[]);

    model
        .assert_has_field("parent")
        .assert_relation_to("Human")
        .assert_relation_name("Offspring")
        .assert_relation_to_fields(&["id"]);
}
