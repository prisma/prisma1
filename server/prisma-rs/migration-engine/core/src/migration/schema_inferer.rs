use prisma_models::prelude::*;

use serde_json;
use std::io::{Read, Write};
use std::process::Command;
use std::process::Stdio;
use std::sync::Arc;

pub trait SchemaInferer {
    fn infer(data_model: String) -> Arc<InternalDataModel>;
}

pub struct LegacySchemaInferer;

impl SchemaInferer for LegacySchemaInferer {
    fn infer(data_model: String) -> Arc<InternalDataModel> {
        let bin_path = "/Users/marcusboehm/R/github.com/prisma/prisma/server/images/schema-inferrer-bin/target/prisma-native-image/schema-inferrer-bin";
        let cmd = Command::new(bin_path)
            .stdin(Stdio::null())
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .spawn()
            .unwrap();
        let input = SchemaInfererBinInput {
            data_model: data_model,
            previous_schema: InternalDataModelTemplate::default(),
        };
        write!(cmd.stdin.unwrap(), "{}", serde_json::to_string(&input).unwrap()).unwrap();
        let mut buffer = String::new();
        let stdout = &mut cmd.stdout.unwrap();
        stdout.read_to_string(&mut buffer).unwrap();

        println!("received from the schema-inferrer-bin: {}", &buffer);

        let schema: InternalDataModelTemplate = serde_json::from_str(buffer.as_str()).expect("Deserializing the schema failed.");
        schema.build("".to_string())
    }
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct SchemaInfererBinInput {
    data_model: String,
    previous_schema: InternalDataModelTemplate,
}
