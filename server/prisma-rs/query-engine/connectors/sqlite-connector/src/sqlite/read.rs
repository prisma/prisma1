use crate::{query_builder::QueryBuilder, DatabaseRead, SelectDefinition, Sqlite};
use connector::*;
use prisma_models::*;
use prisma_query::{
    ast::*,
    visitor::{self, *},
};
use rusqlite::{Row, Transaction};

impl DatabaseRead for Sqlite {
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
    fn query<F, T, S>(conn: &Transaction, query: S, mut f: F) -> ConnectorResult<Vec<T>>
    where
        F: FnMut(&Row) -> ConnectorResult<T>,
        S: Into<Select>,
    {
        let (query_sql, params) = dbg!(visitor::Sqlite::build(query.into()));

        let res: ConnectorResult<Vec<T>> = conn
            .prepare(&query_sql)?
            .query_map(&params, |row| f(row))?
            .map(|row_res| row_res.unwrap())
            .collect();

        Ok(res?)
    }

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
    ///     Sqlite::count(&trans, "users", ConditionTree::single("name".equals("Alice"))).unwrap(),
    /// );
    /// ```
    fn count<C, T>(conn: &Transaction, table: T, conditions: C) -> ConnectorResult<usize>
    where
        C: Into<ConditionTree>,
        T: Into<Table>,
    {
        let select = Select::from_table(table)
            .value(count(asterisk()))
            .so_that(conditions.into());

        let (sql, params) = dbg!(visitor::Sqlite::build(select));

        let res = conn
            .prepare(&sql)?
            .query_map(&params, |row| Self::fetch_int(row))?
            .map(|r| r.unwrap())
            .next()
            .unwrap_or(0);

        Ok(res as usize)
    }

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
    fn ids_for<T>(conn: &Transaction, model: ModelRef, into_select: T) -> ConnectorResult<Vec<GraphqlId>>
    where
        T: SelectDefinition,
    {
        let select = {
            let selected_fields = SelectedFields::from(model.fields().id());
            QueryBuilder::get_nodes(model, &selected_fields, into_select)
        };

        let ids = Self::query(conn, select, |row| {
            let id: GraphqlId = row.get(0);
            Ok(id)
        })?;

        Ok(ids)
    }

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
    fn id_for(conn: &Transaction, model: ModelRef, node_selector: &NodeSelector) -> ConnectorResult<GraphqlId> {
        let opt_id = Self::ids_for(conn, model, node_selector.clone())?.into_iter().next();

        opt_id.ok_or_else(|| ConnectorError::NodeNotFoundForWhere(NodeSelectorInfo::from(node_selector)))
    }

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
        parent_ids: Vec<GraphqlId>,
        selector: &Option<NodeSelector>,
    ) -> ConnectorResult<Vec<GraphqlId>> {
        let related_model = parent_field.related_model();
        let relation = parent_field.relation();
        let child_id_field = relation.column_for_relation_side(parent_field.relation_side.opposite());
        let parent_id_field = relation.column_for_relation_side(parent_field.relation_side);

        let subselect = Select::from_table(relation.relation_table())
            .column(child_id_field)
            .so_that(parent_id_field.in_selection(parent_ids));

        let conditions = related_model.fields().id().db_name().in_selection(subselect);

        let conditions = match selector {
            Some(ref node_selector) => {
                conditions.and(node_selector.field.as_column().equals(node_selector.value.clone()))
            }
            None => conditions.into(),
        };

        let select = Select::from_table(related_model.table())
            .column(related_model.fields().id().as_column())
            .so_that(conditions);

        Self::query(conn, select, |row| {
            let id: GraphqlId = row.get(0);
            Ok(id)
        })
    }
}
