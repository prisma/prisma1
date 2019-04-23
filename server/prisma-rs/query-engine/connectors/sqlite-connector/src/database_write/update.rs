use connector::{
    filter::{Filter, NodeSelector},
    ConnectorResult,
};
use prisma_models::*;
use rusqlite::Transaction;

/// Functions to update records in the database.
pub trait DatabaseUpdate {
    /// Updates one record and any associated list record in the database.
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
    /// # let user = schema.find_model("User").unwrap();
    /// # let mut args = PrismaArgs::new();
    /// # args.insert("id", GraphqlId::from("id1"));
    /// # args.insert("name", "Bob");
    /// #
    /// let user_id = Sqlite::execute_create(
    ///     &trans,
    ///     Arc::clone(&user),
    ///     &args,
    ///     &[("cats", vec![])],
    /// ).unwrap();
    ///
    /// let id_field = user.fields().id();
    ///
    /// let mut args = PrismaArgs::new();
    /// args.insert("name", "Alice");
    ///
    /// let selector = NodeSelector::from((id_field, user_id.clone()));
    ///
    /// let updated_id = Sqlite::execute_update(
    ///     &trans,
    ///     &selector,
    ///     &args,
    ///     &[("cats", vec![])],
    /// ).unwrap();
    ///
    /// let record = Sqlite::find_node(&trans, &selector).unwrap();
    ///
    /// assert_eq!(&PrismaValue::from("Alice"), record.get_field_value("name").unwrap());
    /// ```
    fn execute_update<T>(
        conn: &Transaction,
        node_selector: &NodeSelector,
        non_list_args: &PrismaArgs,
        list_args: &[(T, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>
    where
        T: AsRef<str>;

    /// Updates every record and any associated list records in the database
    /// matching the `Filter`.
    ///
    /// Returns the number of updated items, if successful.
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
    /// # let user = schema.find_model("User").unwrap();
    /// #
    /// for arg in ["Bob", "Bobby", "Alice"].into_iter() {
    ///     let mut args = PrismaArgs::new();
    ///     args.insert("id", GraphqlId::from(format!("id_{}", arg)));
    ///     args.insert("name", *arg);
    ///
    ///     Sqlite::execute_create(
    ///         &trans,
    ///         Arc::clone(&user),
    ///         &args,
    ///         &[("cats", vec![])],
    ///     ).unwrap();
    /// }
    ///
    /// let name_field = user.fields().find_from_scalar("name").unwrap();
    /// let finder = name_field.equals("Bobby");
    ///
    /// let mut args = PrismaArgs::new();
    /// args.insert("name", "Brooke");
    ///
    /// let count = Sqlite::execute_update_many(
    ///     &trans,
    ///     Arc::clone(&user),
    ///     &finder,
    ///     &args,
    ///     &[("cats", vec![])],
    /// ).unwrap();
    ///
    /// let id_field = user.fields().id();
    /// let selector = NodeSelector::from((id_field, "id_Bobby"));
    /// let record = Sqlite::find_node(&trans, &selector).unwrap();
    ///
    /// assert_eq!(&PrismaValue::from("Brooke"), record.get_field_value("name").unwrap());
    /// ```
    fn execute_update_many<T>(
        conn: &Transaction,
        model: ModelRef,
        filter: &Filter,
        non_list_args: &PrismaArgs,
        list_args: &[(T, PrismaListValue)],
    ) -> ConnectorResult<usize>
    where
        T: AsRef<str>;

    /// Updates a nested item related to the parent, including any associated
    /// list values.
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
    /// # let user_id = Sqlite::execute_create(
    /// #     &trans,
    /// #     Arc::clone(&user),
    /// #     &args,
    /// #     &[("cats", vec![])],
    /// # ).unwrap();
    /// #
    /// let relation_field = user.fields().find_from_relation_fields("sites").unwrap();
    ///
    /// let mut args = PrismaArgs::new();
    /// args.insert("id", GraphqlId::from("id2"));
    /// args.insert("name", "A Cat Blog");
    /// #
    /// # let actions = NestedCreateNode {
    /// #    relation_field: Arc::clone(&relation_field),
    /// #    non_list_args: args.clone(),
    /// #    list_args: Vec::new(),
    /// #    top_is_create: true,
    /// #    nested_mutactions: NestedMutactions::default(),
    /// # };
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
    /// let mut args = PrismaArgs::new();
    /// args.insert("name", "A Mouse Blog");
    ///
    /// let updated_id = Sqlite::execute_nested_update(
    ///     &trans,
    ///     &user_id,
    ///     &Some(NodeSelector::from((name_field, "A Cat Blog"))),
    ///     relation_field,
    ///     &args,
    ///     &[("tags", vec![])],
    /// ).unwrap();
    ///
    /// assert_eq!(
    ///     site_id,
    ///     updated_id,
    /// );
    ///
    /// let selector = NodeSelector::from((site.fields().id(), "id2"));
    /// let record = Sqlite::find_node(&trans, &selector).unwrap();
    ///
    /// assert_eq!(&PrismaValue::from("A Mouse Blog"), record.get_field_value("name").unwrap());
    /// ```
    fn execute_nested_update<T>(
        conn: &Transaction,
        parent_id: &GraphqlId,
        node_selector: &Option<NodeSelector>,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(T, PrismaListValue)],
    ) -> ConnectorResult<GraphqlId>
    where
        T: AsRef<str>;

    /// Updates nested items matching to filter, or if no filter is given, all
    /// nested items related to the given `parent_id`.
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
    /// #     &[("cats", vec![])],
    /// # ).unwrap();
    /// #
    /// let relation_field = user.fields().find_from_relation_fields("sites").unwrap();
    ///
    /// for arg in ["A Cat Blog", "A Car Blog", "A Shoe Blog"].into_iter() {
    ///     let mut args = PrismaArgs::new();
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
    ///         &[("tags", vec![])],
    ///     ).unwrap();
    /// }
    ///
    /// let name_field = site.fields().find_from_scalar("name").unwrap();
    /// let filter = name_field.starts_with("A Ca");
    ///
    /// let mut args = PrismaArgs::new();
    /// args.insert("name", "a'ca?");
    ///
    /// let count = Sqlite::execute_nested_update_many(
    ///     &trans,
    ///     &user_id,
    ///     &Some(filter),
    ///     relation_field,
    ///     &args,
    ///     &[("cats", vec![])],
    /// ).unwrap();
    ///
    /// assert_eq!(2, count);
    ///
    /// for id in ["id_A Cat Blog", "id_A Car Blog"].into_iter() {
    ///     let selector = NodeSelector::from((site.fields().id(), *id));
    ///     let record = Sqlite::find_node(&trans, &selector).unwrap();
    ///     assert_eq!(&PrismaValue::from("a'ca?"), record.get_field_value("name").unwrap());
    /// }
    /// ```
    fn execute_nested_update_many<T>(
        conn: &Transaction,
        parent_id: &GraphqlId,
        filter: &Option<Filter>,
        relation_field: RelationFieldRef,
        non_list_args: &PrismaArgs,
        list_args: &[(T, PrismaListValue)],
    ) -> ConnectorResult<usize>
    where
        T: AsRef<str>;

    /// Updates list args related to the given records.
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
    /// # let user = schema.find_model("User").unwrap();
    /// # let mut args = PrismaArgs::new();
    /// # args.insert("id", GraphqlId::from("id1"));
    /// # args.insert("name", "Bob");
    /// #
    /// let user_id = Sqlite::execute_create(
    ///     &trans,
    ///     Arc::clone(&user),
    ///     &args,
    ///     &[("cats", vec![PrismaValue::from("musti")])],
    /// ).unwrap();
    ///
    /// assert_eq!(1, Sqlite::count(&trans, "User_cats", ConditionTree::default()).unwrap());
    ///
    /// Sqlite::update_list_args(
    ///     &trans,
    ///     &[user_id],
    ///     user,
    ///     &[(
    ///         "cats",
    ///         vec![PrismaValue::from("musti"), PrismaValue::from("naukio")]
    ///     )],
    /// ).unwrap();
    ///
    /// assert_eq!(2, Sqlite::count(&trans, "User_cats", ConditionTree::default()).unwrap())
    /// ```
    fn update_list_args<T>(
        conn: &Transaction,
        ids: &[GraphqlId],
        model: ModelRef,
        list_args: &[(T, PrismaListValue)],
    ) -> ConnectorResult<()>
    where
        T: AsRef<str>;
}
