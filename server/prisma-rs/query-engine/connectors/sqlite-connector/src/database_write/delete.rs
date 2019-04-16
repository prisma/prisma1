use crate::mutaction::NestedActions;
use connector::{
    filter::{Filter, NodeSelector},
    ConnectorResult,
};
use prisma_models::{GraphqlId, ModelRef, ProjectRef, RelationFieldRef, SingleNode};
use rusqlite::Transaction;

/// Functions to delete records from the database.
///
/// The functions are transactional and will do a rollback is handled in case of
/// an error.
pub trait DatabaseDelete {
    /// A top level delete that removes one record and should be called if the
    /// action is not nested in any other action in the query. Otherwise use
    /// `execute_nested_delete`. Violating any relations or a non-existing
    /// record will cause an error.
    ///
    /// Will return the deleted record if the delete was successful.
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::{*, filter::*};
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let mut conn = Connection::open_in_memory().unwrap();
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("./test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let trans = conn.transaction().unwrap();
    /// #
    /// # trans.execute("ATTACH DATABASE './test.db' AS 'test'", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User (id Text, name Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.Site (id Text, name Text, user Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test._UserToSites (A Text, B Text, id Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User_cats (nodeId Text, position Integer, value Text);", NO_PARAMS).unwrap();
    /// #
    /// # let user = schema.find_model("User").unwrap();
    /// #
    /// let mut args = PrismaArgs::new();
    /// args.insert("id", GraphqlId::from("id1"));
    /// args.insert("name", "Bob");
    ///
    /// Sqlite::execute_create(
    ///     &trans,
    ///     Arc::clone(&user),
    ///     &args,
    ///     &[],
    /// ).unwrap();
    ///
    /// assert_eq!(1, Sqlite::count(&trans, "User", ConditionTree::NoCondition).unwrap());
    ///
    /// let name_field = user.fields().find_from_scalar("name").unwrap();
    ///
    /// Sqlite::execute_delete(
    ///     &trans,
    ///     &NodeSelector::from((name_field, "Bob"))
    /// ).unwrap();
    ///
    /// assert_eq!(0, Sqlite::count(&trans, "User", ConditionTree::NoCondition).unwrap());
    /// ```
    fn execute_delete(conn: &Transaction, node_selector: &NodeSelector) -> ConnectorResult<SingleNode>;

    /// A top level delete that removes many records and should be called if the
    /// action is not nested in any other action in the query. Otherwise use
    /// `execute_nested_delete_many`. Violating any relations will cause an error.
    ///
    /// Will return the number records deleted.
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::{*, filter::*};
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let mut conn = Connection::open_in_memory().unwrap();
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("./test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let trans = conn.transaction().unwrap();
    /// #
    /// # trans.execute("ATTACH DATABASE './test.db' AS 'test'", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User (id Text, name Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.Site (id Text, name Text, user Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test._UserToSites (A Text, B Text, id Text);", NO_PARAMS).unwrap();
    /// # trans.execute("CREATE TABLE IF NOT EXISTS test.User_cats (nodeId Text, position Integer, value Text);", NO_PARAMS).unwrap();
    /// #
    /// # let user = schema.find_model("User").unwrap();
    /// #
    /// for arg in ["Bob", "Bobi", "Alice"].into_iter() {
    ///     let mut args = PrismaArgs::new();
    ///     args.insert("id", GraphqlId::from(format!("id_{}", *arg)));
    ///     args.insert("name", *arg);
    ///
    ///     Sqlite::execute_create(
    ///         &trans,
    ///         Arc::clone(&user),
    ///         &args,
    ///         &[],
    ///     ).unwrap();
    /// }
    ///
    /// assert_eq!(3, Sqlite::count(&trans, "User", ConditionTree::NoCondition).unwrap());
    ///
    /// let name_field = user.fields().find_from_scalar("name").unwrap();
    /// let filter = name_field.starts_with("Bob");
    ///
    /// assert_eq!(2, Sqlite::execute_delete_many(&trans, user, &filter).unwrap());
    /// assert_eq!(1, Sqlite::count(&trans, "User", ConditionTree::NoCondition).unwrap());
    /// ```
    fn execute_delete_many(conn: &Transaction, model: ModelRef, filter: &Filter) -> ConnectorResult<usize>;

    /// A nested delete that removes one item related to the given `parent_id`.
    /// If no `RecordFinder` is given, will delete the first item from the
    /// table.
    ///
    /// Errors thrown from domain violations:
    ///
    /// - Violating any relations where the deleted record is required
    /// - If the deleted record is not connected to the parent
    /// - The record does not exist
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::{*, filter::*, mutaction::*};
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let mut conn = Connection::open_in_memory().unwrap();
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("./test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let trans = conn.transaction().unwrap();
    /// #
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
    /// # let user_id = Sqlite::execute_create(
    /// #     &trans,
    /// #     Arc::clone(&user),
    /// #     &args,
    /// #     &[],
    /// # ).unwrap();
    /// #
    /// let relation_field = user.fields().find_from_relation_fields("sites").unwrap();
    /// let mut args = PrismaArgs::new();
    ///
    /// for arg in ["A Cat Blog", "A Car Blog"].into_iter() {
    ///     args.insert("id", GraphqlId::from(format!("id_{}", *arg)));
    ///     args.insert("name", *arg);
    ///
    ///     let create_actions = NestedCreateNode {
    ///        relation_field: Arc::clone(&relation_field),
    ///        non_list_args: args.clone(),
    ///        list_args: Vec::new(),
    ///        top_is_create: true,
    ///        nested_mutactions: NestedMutactions::default(),
    ///     };
    ///
    ///     Sqlite::execute_nested_create(
    ///         &trans,
    ///         &user_id,
    ///         &create_actions,
    ///         Arc::clone(&relation_field),
    ///         &args,
    ///         &[],
    ///     ).unwrap();
    /// }
    ///
    /// assert_eq!(2, Sqlite::count(&trans, "Site", ConditionTree::NoCondition).unwrap());
    ///
    /// let name_field = site.fields().find_from_scalar("name").unwrap();
    /// let selector = NodeSelector::from((name_field, "A Car Blog"));
    ///
    /// let delete_actions = NestedDeleteNode {
    ///    relation_field: Arc::clone(&relation_field),
    ///    where_: Some(selector.clone())
    /// };
    ///
    /// Sqlite::execute_nested_delete(
    ///     &trans,
    ///     &user_id,
    ///     &delete_actions,
    ///     &Some(selector),
    ///     relation_field,
    /// ).unwrap();
    ///
    /// assert_eq!(1, Sqlite::count(&trans, "Site", ConditionTree::NoCondition).unwrap());
    /// ```
    fn execute_nested_delete(
        conn: &Transaction,
        parent_id: &GraphqlId,
        nested_actions: &NestedActions,
        node_selector: &Option<NodeSelector>,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<()>;

    /// Removes nested items matching to filter, or if no filter is given, all
    /// nested items related to the given `parent_id`. An error will be thrown
    /// if any deleted record is required in a model.
    /// ```rust
    /// # use prisma_models::*;
    /// # use rusqlite::{Connection, NO_PARAMS};
    /// # use sqlite_connector::*;
    /// # use connector::{*, filter::*, mutaction::*};
    /// # use prisma_query::ast::*;
    /// # use serde_json;
    /// # use std::{fs::File, sync::Arc};
    /// #
    /// # let mut conn = Connection::open_in_memory().unwrap();
    /// # let tmp: SchemaTemplate = serde_json::from_reader(File::open("./test_schema.json").unwrap()).unwrap();
    /// # let schema = tmp.build(String::from("test"));
    /// # let trans = conn.transaction().unwrap();
    /// #
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
    /// # let user_id = Sqlite::execute_create(
    /// #     &trans,
    /// #     Arc::clone(&user),
    /// #     &args,
    /// #     &[],
    /// # ).unwrap();
    /// #
    /// let relation_field = user.fields().find_from_relation_fields("sites").unwrap();
    /// let mut args = PrismaArgs::new();
    ///
    /// for arg in ["A Cat Blog", "A Car Blog", "A Shoe Blog"].into_iter() {
    ///     args.insert("id", GraphqlId::from(format!("id_{}", *arg)));
    ///     args.insert("name", *arg);
    ///
    ///     let create_actions = NestedCreateNode {
    ///        relation_field: Arc::clone(&relation_field),
    ///        non_list_args: args.clone(),
    ///        list_args: Vec::new(),
    ///        top_is_create: true,
    ///        nested_mutactions: NestedMutactions::default(),
    ///     };
    ///
    ///     Sqlite::execute_nested_create(
    ///         &trans,
    ///         &user_id,
    ///         &create_actions,
    ///         Arc::clone(&relation_field),
    ///         &args,
    ///         &[],
    ///     ).unwrap();
    /// }
    ///
    /// assert_eq!(3, Sqlite::count(&trans, "Site", ConditionTree::NoCondition).unwrap());
    ///
    /// let name_field = site.fields().find_from_scalar("name").unwrap();
    /// let filter = name_field.starts_with("A Ca");
    ///
    /// Sqlite::execute_nested_delete_many(
    ///     &trans,
    ///     &user_id,
    ///     &Some(filter),
    ///     relation_field,
    /// ).unwrap();
    ///
    /// assert_eq!(1, Sqlite::count(&trans, "Site", ConditionTree::NoCondition).unwrap());
    /// ```
    fn execute_nested_delete_many(
        conn: &Transaction,
        parent_id: &GraphqlId,
        filter: &Option<Filter>,
        relation_field: RelationFieldRef,
    ) -> ConnectorResult<usize>;

    /// Truncates all tables from the project.
    fn execute_reset_data(conn: &Transaction, project: ProjectRef) -> ConnectorResult<()>;
}
