use super::ConnectionLimit;

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct ExplicitConfig {
    pub connector: String,
    pub host: String,
    pub user: String,
    pub port: u16,

    pub raw_access: Option<bool>,
    pub ssl: Option<bool>,

    pub password: Option<String>,
    pub schema: Option<String>,
    pub database: Option<String>,
    pub management_schema: Option<String>,

    pooled: Option<bool>,
    connection_limit: Option<u32>,
    migrations: Option<bool>,
    active: Option<bool>,
}

impl ConnectionLimit for ExplicitConfig {
    fn connection_limit(&self) -> Option<u32> {
        self.connection_limit
    }

    fn pooled(&self) -> Option<bool> {
        self.pooled
    }
}
