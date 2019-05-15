use super::ConnectionLimit;
use std::path::PathBuf;

#[derive(Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct FileConfig {
    pub connector: String,
    pub database_file: String,

    #[serde(default)]
    pub test_mode: bool,
    pub raw_access: Option<bool>,

    pub connection_limit: Option<u32>,
    pub pooled: Option<bool>,
    pub schema: Option<String>,
    pub management_schema: Option<String>,

    migrations: Option<bool>,
    active: Option<bool>,
}

impl FileConfig {
    /// The database name for file based config is derived from the file name minus the extension
    pub fn db_name(&self) -> String {
        let path = PathBuf::from(&self.database_file);
        let file_name: String = path
            .file_name()
            .expect("Expected `database_file` path to end in a file name.")
            .to_owned()
            .into_string()
            .unwrap();

        match path.extension() {
            Some(ext) => file_name
                .trim_end_matches(ext.to_str().unwrap())
                .trim_end_matches(".")
                .to_owned(),
            None => file_name,
        }
    }
}

impl ConnectionLimit for FileConfig {
    fn connection_limit(&self) -> Option<u32> {
        self.connection_limit
    }

    fn pooled(&self) -> Option<bool> {
        self.pooled
    }
}
