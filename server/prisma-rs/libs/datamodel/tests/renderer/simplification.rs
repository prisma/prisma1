#[test]
fn test_exclude_default_relation_names_from_rendering() {
    let input = r#"
        model Todo {
            id Int @id
        }

        model User {
            id Int @id
            todo Todo @relation("TodoToUser")
        }
    "#;

    let expected = r#"model Todo {
    id Int @id
}

model User {
    id Int @id
    todo Todo
}"#;

    let dml = datamodel::parse(input).unwrap();
    let rendered = datamodel::render(&dml).unwrap();

    print!("{}", rendered);

    assert_eq!(rendered, expected);
}

#[test]
fn test_exclude_to_fields_id() {
    let input = r#"
        model Todo {
            id Int @id
        }

        model User {
            id Int @id
            todo Todo @relation(references: [id])
        }
    "#;

    let expected = r#"model Todo {
    id Int @id
}

model User {
    id Int @id
    todo Todo
}"#;

    let dml = datamodel::parse(input).unwrap();
    let rendered = datamodel::render(&dml).unwrap();

    print!("{}", rendered);

    assert_eq!(rendered, expected);
}
