extern crate datamodel;
use pretty_assertions::assert_eq;
use std::str;

#[test]
fn test_reformat_model() {
    let input = r#"
        model User { id Int @id }
    "#;

    let expected = r#"
model User {
  id Int @id
}"#;

    let mut buf = Vec::new();
    datamodel::ast::reformat::Reformatter::reformat_to(&input, &mut buf, 2);
    let actual = str::from_utf8(&buf).expect("unable to convert to string");
    assert_eq!(expected, actual);
}

#[test]
fn test_reformat_config() {
    let input = r#"
        datasource pg { provider = "postgres" }
    "#;

    let expected = r#"
datasource pg {
  provider = "postgres"
}"#;

    let mut buf = Vec::new();
    datamodel::ast::reformat::Reformatter::reformat_to(&input, &mut buf, 2);
    let actual = str::from_utf8(&buf).expect("unable to convert to string");
    assert_eq!(expected, actual);
}

#[test]
fn test_reformat_tabs() {
    let input = r#"
        datasource pg {\tprovider\t=\t"postgres"\t}
    "#;

    let expected = r#"
datasource pg {
  provider = "postgres"
}"#;

    let mut buf = Vec::new();
    // replaces \t placeholder with a real tab
    datamodel::ast::reformat::Reformatter::reformat_to(&input.replace("\\t", "\t"), &mut buf, 2);
    let actual = str::from_utf8(&buf).expect("unable to convert to string");
    assert_eq!(expected, actual);
}
