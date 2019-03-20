use super::{ConnectionLimit, WithMigrations};
use url::Url;

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct ConnectionStringConfig {
    #[serde(with = "url_serde")]
    pub uri: Url,

    pub database: Option<String>,
    pub connection_limit: Option<u32>,
    pub pooled: Option<bool>,
    pub schema: Option<String>,
    pub management_schema: Option<String>,

    migrations: Option<bool>,
    active: Option<bool>,
}

impl WithMigrations for ConnectionStringConfig {
    fn migrations(&self) -> Option<bool> {
        self.migrations
    }

    fn is_active(&self) -> Option<bool> {
        self.active
    }
}

impl ConnectionLimit for ConnectionStringConfig {
    fn connection_limit(&self) -> Option<u32> {
        self.connection_limit
    }

    fn pooled(&self) -> Option<bool> {
        self.pooled
    }
}
