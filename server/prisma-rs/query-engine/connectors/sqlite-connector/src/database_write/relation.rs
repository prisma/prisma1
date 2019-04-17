use crate::mutaction::NestedActions;
use connector::{filter::NodeSelector, ConnectorResult};
use prisma_models::{GraphqlId, RelationFieldRef};
use rusqlite::Transaction;

/// Functions to connect and disconnect records in the database.
pub trait DatabaseRelation {
    /// Connect a record to the parent.
    ///
    /// When nested with a create, will have special behaviour in some cases:
    ///
    /// | action                               | p is a list | p is required | c is list | c is required |
    /// | ------------------------------------ | ----------- | ------------- | --------- | ------------- |
    /// | relation violation                   | false       | true          | false     | true          |
    /// | check if connected to another parent | false       | true          | false     | false         |
    ///
    /// When nesting to an action that is not a create:
    ///
    /// | action                               | p is a list | p is required | c is list | c is required |
    /// | ------------------------------------ | ----------- | ------------- | --------- | ------------- |
    /// | relation violation                   | false       | true          | false     | true          |
    /// | check if connected to another parent | false       | true          | false     | false         |
    /// | check if parent has another child    | false       | true          | false     | false         |
    ///
    /// If none of the checks fail, the record will be disconnected to the
    /// previous relation before connecting to the given parent.
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
    /// # let mut args = PrismaArgs::new();
    /// # args.insert("id", GraphqlId::from("id1"));
    /// # args.insert("name", "A Blog");
    /// #
    /// # let site_id = Sqlite::execute_create::<String>(
    /// #     &trans,
    /// #     Arc::clone(&site),
    /// #     &args,
    /// #     &[],
    /// # ).unwrap();
    /// #
    /// let relation_field = user.fields().find_from_relation_fields("sites").unwrap();
    /// let name_field = site.fields().find_from_scalar("name").unwrap();
    /// let node_selector = NodeSelector::from((Arc::clone(&name_field), "A Blog"));
    ///
    /// let action = NestedConnect {
    ///     relation_field: Arc::clone(&relation_field),
    ///     where_: node_selector,
    ///     top_is_create: false,
    /// };
    ///
    /// Sqlite::execute_connect(
    ///     &trans,
    ///     &user_id,
    ///     &action,
    ///     &action.where_,
    ///     Arc::clone(&relation_field),
    /// ).unwrap();
    ///
    /// let from_parent = Sqlite::get_id_by_parent(
    ///     &trans,
    ///     relation_field,
    ///     &user_id,
    ///     &Some(NodeSelector::from((name_field, "A Blog"))),
    /// ).unwrap();
    ///
    /// assert_eq!(
    ///     site_id,
    ///     from_parent,
    /// );
    /// ```
    fn execute_connect(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        node_selector: &NodeSelector,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<()>;

    /// Disconnect a record from the parent.
    ///
    /// The following cases will lead to a relation violation error:
    ///
    /// | p is a list | p is required | c is list | c is required |
    /// | ----------- | ------------- | --------- | ------------- |
    /// | false       | true          | false     | true          |
    /// | false       | true          | false     | false         |
    /// | false       | false         | false     | true          |
    /// | true        | false         | false     | true          |
    /// | false       | true          | true      | false         |
    ///
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
    /// # let mut args = PrismaArgs::new();
    /// # args.insert("id", GraphqlId::from("id1"));
    /// # args.insert("name", "A Blog");
    /// #
    /// # let site_id = Sqlite::execute_create::<String>(
    /// #     &trans,
    /// #     Arc::clone(&site),
    /// #     &args,
    /// #     &[],
    /// # ).unwrap();
    /// #
    /// # let relation_field = user.fields().find_from_relation_fields("sites").unwrap();
    /// let name_field = site.fields().find_from_scalar("name").unwrap();
    /// let node_selector = NodeSelector::from((Arc::clone(&name_field), "A Blog"));
    /// #
    /// # let action = NestedConnect {
    /// #     relation_field: Arc::clone(&relation_field),
    /// #     where_: node_selector.clone(),
    /// #     top_is_create: false,
    /// # };
    /// #
    /// # Sqlite::execute_connect(
    /// #     &trans,
    /// #     &user_id,
    /// #     &action,
    /// #     &action.where_,
    /// #     Arc::clone(&relation_field),
    /// # ).unwrap();
    /// #
    /// let action = NestedDisconnect {
    ///     relation_field: Arc::clone(&relation_field),
    ///     where_: Some(node_selector.clone()),
    /// };
    ///
    /// Sqlite::execute_disconnect(
    ///     &trans,
    ///     &user_id,
    ///     &action,
    ///     &Some(node_selector.clone()),
    /// ).unwrap();
    ///
    /// let from_parent = Sqlite::get_ids_by_parents(
    ///     &trans,
    ///     relation_field,
    ///     vec![&user_id],
    ///     Some(node_selector),
    /// ).unwrap();
    ///
    /// assert!(from_parent.is_empty());
    /// ```
    fn execute_disconnect(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        node_selector: &Option<NodeSelector>,
    ) -> ConnectorResult<()>;

    /// Connects multiple records into the parent. Rules from `execute_connect`
    /// apply.
    fn execute_set(
        conn: &Transaction,
        parent_id: &GraphqlId,
        actions: &NestedActions,
        node_selectors: &Vec<NodeSelector>,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<()>;
}
