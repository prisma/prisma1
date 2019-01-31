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
  // escaping handler?
}

impl QueryBuilder {
  fn new(dialect: SQLDialect) -> QueryBuilder {
    match dialect {
      SQLDialect::SQLite => QueryBuilder {
        dialect: dialect,
      },
      _ => panic!("Unsupported dialect: {:?}", dialect),
    }
  }

  fn render(&self) -> String {
    unimplemented!();
  }
}

enum Step<'a> {
  SelectStep(SelectStep<'a>),
  FromStep(FromStep<'a>),
  ConditionStep(ConditionStep<'a>),
}

struct SelectStep<'a> {
  selectors: &'a [Column<'a>],
  parent_step: Option<Step<'a>>,
}

impl<'a> SelectStep<'a> {
  // multiple tables possible
  fn from(self, parts: Vec<&'a str>) -> FromStep {
    FromStep {
      table: TableQualifier { parts },
      parent_step: Some(&self.into_step()),
    }
  }

  fn into_step(self) -> Step<'a> {
    Step::SelectStep(self)
  }
}

struct FromStep<'a> {
  table: TableQualifier<'a>,
  parent_step: Option<&'a Step<'a>>,
}

impl<'a> FromStep<'a> {
  fn where_(self, condition: Condition<'a>) -> ConditionStep<'a> {
    let expression = ConditionExpression {
      ty: ConditionExpressionType::AND,
      condition,
    };

    ConditionStep { conditions: vec!(expression) }
  }
}

struct TableQualifier<'a> {
  parts: Vec<&'a str>,
  // alias
}

enum ConditionExpressionType {
  AND,
  OR,
}

struct ConditionExpression<'a> {
  ty: ConditionExpressionType,
  condition: Condition<'a>,
}

struct ConditionStep<'a> {
  conditions: Vec<ConditionExpression<'a>>
}

impl<'a> ConditionStep<'a> {
  fn and(mut self, condition: Condition<'a>) -> Self {
    self.conditions.push(ConditionExpression {
      ty: ConditionExpressionType::AND,
      condition,
    });

    self
  }
}

enum ConditionType {
  EQ,
  GT,
  GTE,
  LT,
  LTE,
  IN,
  IsNotNull,
  IsNull
}

enum Field<'a> {
  ColumnReference(Column<'a>),
  SqlBindParam,
}

struct Condition<'a> {
  ty: ConditionType,
  left: Field<'a>,
  right: Option<Field<'a>>
}

struct Column<'a> {
  parts: &'a [&'a str],
  alias: Option<&'a str>,
}

impl<'a> Column<'a> {
  fn eq(self, col: Column<'a>) -> Condition {
    Condition {
      ty: ConditionType::EQ,
      left: self.into_field(),
      right: Some(Field::ColumnReference(col))
    }
  }

  fn into_field(self) -> Field<'a> {
    Field::ColumnReference(self)
  }

  fn equal(self, field: Field<'a>) -> Condition<'a> {
    Condition {
      ty: ConditionType::EQ,
      left: self.into_field(),
      right: Some(field)
    }
  }

  // fn _in<T>(elems: &[T]) -> Condition {
  //   unimplemented!();
  // }
}

fn col<'a>(parts: &'a [&'a str]) -> Column<'a> {
  Column { parts, alias: None }
}

fn select<'a>(columns: &'a[Column<'a>]) -> SelectStep<'a> {
  SelectStep { selectors: columns, parent_step: None }
}

#[cfg(test)]
mod tests {
  use super::*;

  #[test]
  fn test() {
    let builder = QueryBuilder::new(SQLDialect::SQLite);

    select(&[col(&["testdb", "test"])])
      .from(vec!("testdb", "test"))
      .where_(col(&["a"]).equal(Field::SqlBindParam))
      .and(col(&["b"]).eq(col(&["c"])));
  }
}