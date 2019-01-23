use std::{
    collections::BTreeMap,
};


#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct DatabaseWithUsernameAndPassword {
    pub migrations: bool,
    pub host: String,
    pub port: u16,
    pub user: String,
    pub password: String,
    pub raw_access: bool,
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct DatabaseWithUri {
    pub uri: String,
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct DatabaseWithFile {
    pub file: String,
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase", tag = "connector")]
pub enum PrismaDatabase {
    Postgres(DatabaseWithUsernameAndPassword),
    Mysql(DatabaseWithUsernameAndPassword),
    Mongo(DatabaseWithUri),
    Sqlite(DatabaseWithFile)
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct PrismaConfig {
    pub port: i16,
    pub databases: BTreeMap<String, PrismaDatabase>,
}
