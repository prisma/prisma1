use crate::SelectDefinition;
use connector::{filter::NodeSelector, mutaction::*, ConnectorResult};
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
    /// # use connector::*;
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

    /// Find all ids from the `Model` with the filter being true.
    ///
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::*;
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
    ///     Sqlite::id_for(&trans, model, &find_bob).unwrap(),
    /// );
    /// ```
    fn id_for(conn: &Transaction, model: ModelRef, node_selector: &NodeSelector) -> ConnectorResult<GraphqlId>;

    /// Find all children node id's with the given parent id's, optionally given
    /// a `NodeSelector` block for extra filtering.
    ///
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::*;
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
    /// let find_cats = NodeSelector::new(Arc::clone(&name_field), "Cats");
    ///
    /// let rf = rel_model.fields().find_from_relation_fields("sites").unwrap();
    ///
    /// let ids = Sqlite::get_ids_by_parents(
    ///     &trans,
    ///     rf,
    ///     vec![GraphqlId::from("user1")],
    ///     &Some(find_cats),
    /// ).unwrap();
    ///
    /// assert_eq!(
    ///     vec![GraphqlId::from("site2")],
    ///     ids,
    /// );
    /// ```
    fn get_ids_by_parents(
        conn: &Transaction,
        parent_field: RelationFieldRef,
        parent_id: Vec<GraphqlId>,
        selector: &Option<NodeSelector>,
    ) -> ConnectorResult<Vec<GraphqlId>>;
}

pub trait DatabaseWrite {
    /// Execute a single statement in the database.
    fn execute_one<T>(conn: &Transaction, query: T) -> ConnectorResult<()>
    where
        T: Into<Query>;

    /// Execute a multiple statements in the database.
    fn execute_many<T>(conn: &Transaction, queries: Vec<T>) -> ConnectorResult<()>
    where
        T: Into<Query>;

    /// Creates a new record to the database.
    ///
    /// Example: A single record with list arguments. The return value is the
    /// `id` of the created parent record:
    ///
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::*;
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
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User_cats (nodeId Integer, position Integer, value Text);", NO_PARAMS).unwrap();
    /// #
    /// let model = schema.find_model("User").unwrap();
    ///
    /// let mut args = PrismaArgs::default();
    /// args.insert("id", GraphqlId::from("id1"));
    /// args.insert("name", "John");
    ///
    /// let list_args = vec![
    ///     (
    ///         String::from("cats"),
    ///         vec![
    ///             PrismaValue::from("Musti"),
    ///             PrismaValue::from("Naukio")
    ///         ]
    ///     )
    /// ];
    ///
    /// assert_eq!(
    ///    GraphqlId::from("id1"),
    ///    Sqlite::create(&trans, model, args, list_args).unwrap(),
    /// );
    ///
    /// assert_eq!(
    ///     1,
    ///     Sqlite::count(&trans, "User", ConditionTree::default()).unwrap()
    /// );
    ///
    /// assert_eq!(
    ///     2,
    ///     Sqlite::count(&trans, "User_cats", ConditionTree::default()).unwrap()
    /// )
    /// ```
    fn create(
        conn: &Transaction,
        model: ModelRef,
        non_list_args: PrismaArgs,
        list_args: Vec<(String, PrismaListValue)>,
    ) -> ConnectorResult<GraphqlId>;

    /// Creates a new record to the database, connects it to the parent
    /// record and returning the child's id.
    ///
    /// Example. A single nested record with list arguments:
    ///
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::*;
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
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test._UserToSites (A Text, B Text, id Text);", NO_PARAMS);
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.Site_tags (nodeId Integer, position Integer, value Text);", NO_PARAMS).unwrap();
    /// #
    /// let user = schema.find_model("User").unwrap();
    /// let site = schema.find_model("Site").unwrap();
    ///
    /// let mut user_args = PrismaArgs::default();
    /// user_args.insert("id", GraphqlId::from("id1"));
    /// user_args.insert("name", "John");
    ///
    /// let parent_id = Sqlite::create(
    ///     &trans,
    ///     user.clone(),
    ///     user_args,
    ///     Vec::new(),
    /// ).unwrap();
    ///
    /// let mut site_args = PrismaArgs::default();
    /// site_args.insert("id", GraphqlId::from("id1"));
    /// site_args.insert("name", "A Blog");
    ///
    /// let list_args = vec![
    ///     (
    ///         String::from("tags"),
    ///         vec![
    ///             PrismaValue::from("cursing"),
    ///             PrismaValue::from("adult")
    ///         ]
    ///     )
    /// ];
    ///
    /// let rel_field = user
    ///     .fields()
    ///     .find_from_relation_fields("sites")
    ///     .unwrap();
    ///
    /// let child_id = Sqlite::create_and_connect(
    ///     &trans,
    ///     &parent_id,
    ///     rel_field,
    ///     site_args,
    ///     list_args,
    /// ).unwrap();
    ///
    /// assert_eq!(
    ///     1,
    ///     Sqlite::count(&trans, "Site", ConditionTree::default()).unwrap()
    /// );
    ///
    /// assert_eq!(
    ///     2,
    ///     Sqlite::count(&trans, "Site_tags", ConditionTree::default()).unwrap()
    /// )
    /// ```
    fn create_and_connect(
        conn: &Transaction,
        parent_id: &GraphqlId,
        model: RelationFieldRef,
        non_list_args: PrismaArgs,
        list_args: Vec<(String, PrismaListValue)>,
    ) -> ConnectorResult<GraphqlId>;

    fn update_list_args(
        conn: &Transaction,
        ids: Vec<GraphqlId>,
        model: ModelRef,
        list_args: Vec<(String, PrismaListValue)>,
    ) -> ConnectorResult<()>;

    fn update_node<T>(conn: &Transaction, id: GraphqlId, mutaction: &T) -> ConnectorResult<GraphqlId>
    where
        T: SharedUpdateLogic;

    fn update_nodes(conn: &Transaction, ids: Vec<GraphqlId>, mutaction: &UpdateNodes) -> ConnectorResult<usize>;
}
