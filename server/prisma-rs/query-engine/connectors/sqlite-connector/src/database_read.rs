use crate::SelectDefinition;
use connector::{
    filter::{Filter, NodeSelector},
    ConnectorResult,
};
use prisma_models::*;
use prisma_query::ast::*;
use rusqlite::{Row, Transaction};

pub trait DatabaseRead {
    /// Execute the `SELECT` and return a vector mapped with `F`.
    ///
    /// ```rust
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use prisma_query::ast::*;
    /// # let mut conn = Connection::open_in_memory().unwrap();
    /// # let trans = conn.transaction().unwrap();
    /// trans.execute(
    ///     "CREATE TABLE users (id, name Text);",
    ///     NO_PARAMS
    /// );
    ///
    /// trans.execute(
    ///     "INSERT INTO users (id, name) VALUES (1, 'Bob');",
    ///     NO_PARAMS
    /// );
    ///
    /// #[derive(Debug, PartialEq)]
    /// struct User {
    ///     id: i64,
    ///     name: String,
    /// };
    ///
    /// let select = Select::from_table("users")
    ///     .column("id")
    ///     .column("name");
    ///
    /// let users = Sqlite::query(&trans, select, |row| Ok(User {
    ///     id: row.get(0),
    ///     name: row.get(1),
    /// })).unwrap();
    ///
    /// assert_eq!(
    ///     vec![User { id: 1, name: String::from("Bob") }],
    ///     users,
    /// );
    /// ```
    fn query<F, T, S>(conn: &Transaction, query: S, f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>,
        S: Into<Select>;

    /// Count the records of the given query.
    ///
    /// ```rust
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use prisma_query::ast::*;
    /// # let mut conn = Connection::open_in_memory().unwrap();
    /// # let trans = conn.transaction().unwrap();
    /// trans.execute(
    ///     "CREATE TABLE users (id, name Text);",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// trans.execute(
    ///     "INSERT INTO users (id, name) VALUES (1, 'Bob');",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// trans.execute(
    ///     "INSERT INTO users (id, name) VALUES (2, 'Alice');",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// assert_eq!(
    ///     2,
    ///     Sqlite::count(&trans, "users", ConditionTree::default()).unwrap(),
    /// );
    ///
    /// assert_eq!(
    ///     1,
    ///     Sqlite::count(&trans, "users", "name".equals("Alice")).unwrap(),
    /// );
    /// ```
    fn count<C, T>(conn: &Transaction, table: T, into_args: C) -> ConnectorResult<usize>
    where
        C: Into<ConditionTree>,
        T: Into<Table>;

    /// Find all ids from the `Model` with the filter being true.
    ///
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::{*, filter::{ScalarCondition, ScalarFilter, Filter}};
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// # let mut conn = Connection::open_in_memory().unwrap();
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("./test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let trans = conn.transaction().unwrap();
    /// # trans.execute("ATTACH DATABASE './test.db' AS 'test'", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User (id Text, name Text);", NO_PARAMS).unwrap();
    /// trans.execute(
    ///     "INSERT INTO test.User (id, name) VALUES ('id1', 'Bob'), ('id2', 'Alice');",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// let model = schema.find_model("User").unwrap();
    /// let name_field = model.fields().find_from_scalar("name").unwrap();
    ///
    /// let find_bob = ScalarFilter {
    ///     field: Arc::clone(&name_field),
    ///     condition: ScalarCondition::Equals(PrismaValue::from("Bob")),
    /// };
    ///
    /// let find_alice = ScalarFilter {
    ///     field: name_field,
    ///     condition: ScalarCondition::Equals(PrismaValue::from("Alice")),
    /// };
    ///
    /// let filter = Filter::or(vec![
    ///     Filter::from(find_bob),
    ///     Filter::from(find_alice),
    /// ]);
    ///
    /// let ids = Sqlite::ids_for(&trans, model, filter).unwrap();
    ///
    /// assert_eq!(
    ///     vec![GraphqlId::from("id1"), GraphqlId::from("id2")],
    ///     ids,
    /// );
    /// ```
    fn ids_for<T>(conn: &Transaction, model: ModelRef, into_args: T) -> ConnectorResult<Vec<GraphqlId>>
    where
        T: SelectDefinition;

    /// Find the id for the given selector.
    ///
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::{*, filter::NodeSelector};
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// # let mut conn = Connection::open_in_memory().unwrap();
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("./test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let trans = conn.transaction().unwrap();
    /// # trans.execute("ATTACH DATABASE './test.db' AS 'test'", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User (id Text, name Text);", NO_PARAMS).unwrap();
    /// trans.execute(
    ///     "INSERT INTO test.User (id, name) VALUES ('id1', 'Bob');",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// let model = schema.find_model("User").unwrap();
    /// let name_field = model.fields().find_from_scalar("name").unwrap();
    /// let find_bob = NodeSelector::new(Arc::clone(&name_field), "Bob");
    ///
    /// assert_eq!(
    ///     GraphqlId::from("id1"),
    ///     Sqlite::id_for(&trans, &find_bob).unwrap(),
    /// );
    /// ```
    fn id_for(conn: &Transaction, node_selector: &NodeSelector) -> ConnectorResult<GraphqlId>;

    /// Find the node for the given selector, selecting all scalar fields.
    ///
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::{*, filter::NodeSelector};
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// # let mut conn = Connection::open_in_memory().unwrap();
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("./test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let trans = conn.transaction().unwrap();
    /// # trans.execute("ATTACH DATABASE './test.db' AS 'test'", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User (id Text, name Text);", NO_PARAMS).unwrap();
    /// trans.execute(
    ///     "INSERT INTO test.User (id, name) VALUES ('id1', 'Bob');",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// let model = schema.find_model("User").unwrap();
    /// let name_field = model.fields().find_from_scalar("name").unwrap();
    ///
    /// let find_bob = NodeSelector::new(Arc::clone(&name_field), "Bob");
    /// let single_node = Sqlite::find_node(&trans, &find_bob).unwrap();
    ///
    /// assert_eq!(
    ///     vec![String::from("id"), String::from("name")],
    ///     single_node.field_names,
    /// );
    ///
    /// let expected_id = PrismaValue::from(GraphqlId::from("id1"));
    /// let expected_name = PrismaValue::from("Bob");
    ///
    /// assert_eq!(
    ///     vec![expected_id, expected_name],
    ///     single_node.node.values,
    /// );
    /// ```
    fn find_node(conn: &Transaction, node_selector: &NodeSelector) -> ConnectorResult<SingleNode>;

    /// Find a child of a parent. Will return an error if no child found with
    /// the given parameters. A more restrictive version of `get_ids_by_parents`.
    fn get_id_by_parent(
        conn: &Transaction,
        parent_field: RelationFieldRef,
        parent_id: &GraphqlId,
        selector: &Option<NodeSelector>,
    ) -> ConnectorResult<GraphqlId>;

    /// Find all children node id's with the given parent id's, optionally given
    /// a `Filter` for extra filtering.
    ///
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::{*, filter::Filter};
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// # let mut conn = Connection::open_in_memory().unwrap();
    /// #
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("./test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let trans = conn.transaction().unwrap();
    /// # trans.execute("ATTACH DATABASE './test.db' AS 'test'", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User (id Text, name Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.Site (id Text, name Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test._UserToSites (A Text, B Text);", NO_PARAMS);
    /// trans.execute(
    ///     "INSERT INTO test.User (id, name) VALUES ('user1', 'Bob');",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// trans.execute(
    ///     "INSERT INTO test.Site (id, name) VALUES ('site1', 'Blog');",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// trans.execute(
    ///     "INSERT INTO test.Site (id, name) VALUES ('site2', 'Cats');",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// trans.execute(
    ///     "INSERT INTO test._UserToSites (A, B) VALUES ('site1', 'user1');",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// trans.execute(
    ///     "INSERT INTO test._UserToSites (A, B) VALUES ('site2', 'user1');",
    ///     NO_PARAMS
    /// ).unwrap();
    ///
    /// let model = schema.find_model("Site").unwrap();
    /// let rel_model = schema.find_model("User").unwrap();
    /// let name_field = model.fields().find_from_scalar("name").unwrap();
    ///
    /// let rf = rel_model.fields().find_from_relation_fields("sites").unwrap();
    ///
    /// let ids = Sqlite::get_ids_by_parents(
    ///     &trans,
    ///     rf,
    ///     vec![&GraphqlId::from("user1")],
    ///     Some(name_field.equals("Cats")),
    /// ).unwrap();
    ///
    /// assert_eq!(
    ///     vec![GraphqlId::from("site2")],
    ///     ids,
    /// );
    /// ```
    fn get_ids_by_parents<T>(
        conn: &Transaction,
        parent_field: RelationFieldRef,
        parent_id: Vec<&GraphqlId>,
        selector: Option<T>,
    ) -> ConnectorResult<Vec<GraphqlId>>
    where
        T: Into<Filter>;
}
