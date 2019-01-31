#[derive(Debug)]
pub enum SQLDialect {
  SQLite,
  MySQL,
  PostgreSQL,
}

#[derive(Debug)]
struct QueryBuilder {
  dialect: SQLDialect,
  // parameter binding type?
}

impl QueryBuilder {
  fn new(dialect: SQLDialect) -> QueryBuilder {
    match dialect {
      SQLDialect::SQLite => QueryBuilder {
        dialect: dialect,
        // placeholder: "?",
      },
      _ => panic!("Unsupported dialect: {:?}", dialect),
    }
  }

  fn render(&self) -> String {
    unimplemented!();
  }

  // fn select()
}

struct Condition {}

struct Value {}

struct Field<'a> {
  parts: Vec<&'a str>,
  alias: Option<&'a str>,
}

impl<'a> Field<'a> {
  fn eq(field: Field) -> Condition {
    unimplemented!();
  }

  fn equal(val: Value) -> Condition {
    unimplemented!();
  }
}

fn field<'a>(parts: Vec<&'a str>) -> Field<'a> {
  Field { parts, alias: None }
}
