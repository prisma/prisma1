use crate::common::ErrorAsserts;
use datamodel::errors::ValidationError;

const DATAMODEL: &str = r#"
datasource pg1 {
    provider = "postgres"
    url = "https://localhost/postgres1"
}

datasource pg2 {
    provider = "postgres"
    url = "https://localhost/postgres2"
}

datasource sqlite1 {
    provider = "sqlite"
    url = "https://localhost/sqlite1"
}

datasource mysql1 {
    provider = "mysql"
    url = "https://localhost/mysql"
}
"#;

#[test]
fn serialize_builtin_sources_to_dmmf() {
    let sources = datamodel::load_data_source_configuration(DATAMODEL).unwrap();
    let rendered = datamodel::render_sources_to_json(&sources);

    let expected = r#"[
  {
    "name": "pg1",
    "connectorType": "postgres",
    "url": "https://localhost/postgres1",
    "config": {}
  },
  {
    "name": "pg2",
    "connectorType": "postgres",
    "url": "https://localhost/postgres2",
    "config": {}
  },
  {
    "name": "sqlite1",
    "connectorType": "sqlite",
    "url": "https://localhost/sqlite1",
    "config": {}
  },
  {
    "name": "mysql1",
    "connectorType": "mysql",
    "url": "https://localhost/mysql",
    "config": {}
  }
]"#;

    print!("{}", &rendered);

    assert_eq!(rendered, expected);
}

const INVALID_DATAMODEL: &str = r#"
datasource pg1 {
    provider = "AStrangeHalfMongoDatabase"
    url = "https://localhost/postgres1"
}
"#;

#[test]
fn fail_to_load_sources_for_invalid_source() {
    let res = datamodel::load_data_source_configuration(INVALID_DATAMODEL);

    if let Err(error) = res {
        error.assert_is(ValidationError::SourceNotKnownError {
            source_name: String::from("AStrangeHalfMongoDatabase"),
            span: datamodel::ast::Span::new(1, 102),
        });
    } else {
        panic!("Expected error.")
    }
}

const CRASHING_SOURCE: &str = r#"
datasource chinook {
  provider = "sqlite"
  url = "file:../db/Chinook.db"
}
"#;

#[test]
fn load_a_sqlite_source() {
    let sources = datamodel::load_data_source_configuration(CRASHING_SOURCE).unwrap();

    assert_eq!(sources.len(), 1);

    let source = &sources[0];

    assert_eq!(source.name(), "chinook");
    assert_eq!(source.connector_type(), "sqlite");
    assert_eq!(source.url(), "file:../db/Chinook.db");
}
