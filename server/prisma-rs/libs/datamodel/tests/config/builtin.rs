use crate::common::ErrorAsserts;
use datamodel::errors::ValidationError;

const DATAMODEL: &str = r#"
datasource pg1 {
    provider = "postgresql"
    url = "https://localhost/postgres1"
}

datasource pg2 {
    provider = "postgresql"
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
    let config = datamodel::load_configuration(DATAMODEL).unwrap();
    let rendered = datamodel::render_sources_to_json(&config.datasources);

    let expected = r#"[
  {
    "name": "pg1",
    "connectorType": "postgresql",
    "url": "https://localhost/postgres1",
    "config": {}
  },
  {
    "name": "pg2",
    "connectorType": "postgresql",
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
    let res = datamodel::load_configuration(INVALID_DATAMODEL);

    if let Err(error) = res {
        error.assert_is(ValidationError::SourceNotKnownError {
            source_name: String::from("AStrangeHalfMongoDatabase"),
            span: datamodel::ast::Span::new(33, 60),
        });
    } else {
        panic!("Expected error.")
    }
}

const ENABLED_DISABLED_SOURCE: &str = r#"
datasource chinook {
  provider = "sqlite"
  url = "file:../db/production.db"
  enabled = true
}

datasource chinook {
  provider = "sqlite"
  url = "file:../db/staging.db"
  enabled = false
}

"#;

#[test]
fn enable_disable_source_flag() {
    let config = datamodel::load_configuration(ENABLED_DISABLED_SOURCE).unwrap();

    assert_eq!(config.datasources.len(), 1);

    let source = &config.datasources[0];

    assert_eq!(source.name(), "chinook");
    assert_eq!(source.connector_type(), "sqlite");
    assert_eq!(source.url(), "file:../db/production.db");
}

const ENABLED_DISABLED_SOURCE_ENV: &str = r#"
datasource chinook {
  provider = "sqlite"
  url = "file:../db/production.db"
  enabled = env("PRODUCTION")
}

datasource chinook {
  provider = "sqlite"
  url = "file:../db/staging.db"
  enabled = env("STAGING")
}

"#;

#[test]
fn enable_disable_source_flag_from_env() {
    std::env::set_var("PRODUCTION", "false");
    std::env::set_var("STAGING", "true");

    let config = datamodel::load_configuration(ENABLED_DISABLED_SOURCE_ENV).unwrap();

    assert_eq!(config.datasources.len(), 1);

    let source = &config.datasources[0];

    assert_eq!(source.name(), "chinook");
    assert_eq!(source.connector_type(), "sqlite");
    assert_eq!(source.url(), "file:../db/staging.db");
}
