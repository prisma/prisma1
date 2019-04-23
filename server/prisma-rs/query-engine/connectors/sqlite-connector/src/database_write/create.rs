use crate::mutaction::NestedActions;
use connector::ConnectorResult;
use prisma_models::{GraphqlId, ModelRef, PrismaArgs, PrismaListValue, RelationFieldRef};
use rusqlite::Transaction;

/// Functions to create new records to the database.
pub trait DatabaseCreate {
    /// Creates a new root record and any associated list records to the database.
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::{*, filter::*};
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
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User_cats (nodeId Text, position Integer, value Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.Site_tags (nodeId Text, position Integer, value Text);", NO_PARAMS).unwrap();
    /// let user = schema.find_model("User").unwrap();
    ///
    /// let mut args = PrismaArgs::new();
    /// args.insert("id", GraphqlId::from("id1"));
    /// args.insert("name", "Bob");
    ///
    /// let user_id = Sqlite::execute_create(
    ///     &trans,
    ///     Arc::clone(&user),
    ///     &args,
    ///     &[("cats", vec![])],
    /// ).unwrap();
    ///
    /// let name_field = user.fields().find_from_scalar("name").unwrap();
    ///
    /// assert_eq!(
    ///     user_id,
    ///     Sqlite::id_for(&trans, &NodeSelector::from((name_field, "Bob"))).unwrap(),
    /// );
    /// ```
    fn execute_create<T>(
        conn: &Transaction,
        model: ModelRef,
        non_list_args: &PrismaArgs,
        list_args: &[(T, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>
    where
        T: AsRef<str>;

    /// Creates a new nested item related to a parent, including any associated
    /// list values, and is connected with the `parent_id` to the parent record.
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::{*, filter::*, mutaction::*};
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
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.Site (id Text, name Text, user Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test._UserToSites (A Text, B Text, id Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User_cats (nodeId Text, position Integer, value Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.Site_tags (nodeId Text, position Integer, value Text);", NO_PARAMS).unwrap();
    /// # let user = schema.find_model("User").unwrap();
    /// # let site = schema.find_model("Site").unwrap();
    /// #
    /// # let mut args = PrismaArgs::new();
    /// # args.insert("id", GraphqlId::from("id1"));
    /// # args.insert("name", "Bob");
    /// #
    /// # let user_id = Sqlite::execute_create::<String>(
    /// #     &trans,
    /// #     Arc::clone(&user),
    /// #     &args,
    /// #     &[],
    /// # ).unwrap();
    /// #
    /// let relation_field = user.fields().find_from_relation_fields("sites").unwrap();
    ///
    /// let mut args = PrismaArgs::new();
    /// args.insert("id", GraphqlId::from("id2"));
    /// args.insert("name", "A Cat Blog");
    ///
    /// let actions = NestedCreateNode {
    ///    relation_field: Arc::clone(&relation_field),
    ///    non_list_args: args.clone(),
    ///    list_args: Vec::new(),
    ///    top_is_create: true,
    ///    nested_mutactions: NestedMutactions::default(),
    /// };
    ///
    /// let site_id = Sqlite::execute_nested_create(
    ///     &trans,
    ///     &user_id,
    ///     &actions,
    ///     Arc::clone(&relation_field),
    ///     &args,
    ///     &[("tags", vec![])],
    /// ).unwrap();
    ///
    /// let name_field = site.fields().find_from_scalar("name").unwrap();
    ///
    /// let from_parent = Sqlite::get_id_by_parent(
    ///     &trans,
    ///     relation_field,
    ///     &user_id,
    ///     &Some(NodeSelector::from((name_field, "A Cat Blog"))),
    /// ).unwrap();
    ///
    /// assert_eq!(
    ///     site_id,
    ///     from_parent,
    /// );
    /// ```
    fn execute_nested_create<T>(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(T, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>
    where
        T: AsRef<str>;
}
