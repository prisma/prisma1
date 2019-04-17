//! Json serialisation endpoint from IR

use crate::{PrismaError, PrismaResult};
use core::ir::{Item, Response, ResponseSet};
use indexmap::IndexMap;
use prisma_models::{GraphqlId, PrismaValue};
use serde_json::{Map, Number, Value};

type JsonMap = Map<String, Value>;
type JsonVec = Vec<Value>;

macro_rules! envelope {
    ($name:ident, $producer:expr) => {{
        let mut m = JsonMap::new();
        m.insert($name, $producer);
        Value::Object(m)
    }};
}

pub fn serialize(resp: ResponseSet) -> Value {
    let mut map = Map::new();

    let vals: Vec<Value> = resp
        .into_iter()
        .map(|res| match res {
            Response::Data(name, Item::List(list)) => envelope!(name, Value::Array(serialize_list(list))),
            Response::Data(name, Item::Map(map)) => envelope!(name, Value::Object(serialize_map(map))),
            _ => unreachable!(),
        })
        .collect();

    map.insert(
        "data".into(),
        if vals.len() == 1 {
            vals.first().unwrap().clone()
        } else {
            Value::Array(vals)
        },
    );

    Value::Object(map)
}

macro_rules! match_serialize {
    ($val:ident) => {
        match $val {
            Item::List(l) => Value::Array(serialize_list(l)),
            Item::Map(m) => Value::Object(serialize_map(m)),
            Item::Value(v) => serialize_prisma_value(v).unwrap(),
        }
    };
}

/// Recursively serialize query results
fn serialize_map(map: IndexMap<String, Item>) -> JsonMap {
    map.into_iter().fold(JsonMap::new(), |mut map, (k, v)| {
        map.insert(k, match_serialize!(v));
        map
    })
}

fn serialize_list(list: Vec<Item>) -> JsonVec {
    list.into_iter().fold(JsonVec::new(), |mut vec, i| {
        vec.push(match_serialize!(i));
        vec
    })
}

fn serialize_prisma_value(value: PrismaValue) -> PrismaResult<Value> {
    Ok(match value {
        PrismaValue::String(x) => Value::String(x.clone()),
        PrismaValue::Float(x) => Value::Number(match Number::from_f64(x) {
            Some(num) => num,
            None => return Err(PrismaError::SerializationError("`f64` number was invalid".into())),
        }),
        PrismaValue::Boolean(x) => Value::Bool(x),
        PrismaValue::DateTime(_) => unimplemented!(),
        PrismaValue::Enum(x) => Value::String(x.clone()),
        PrismaValue::Json(x) => serde_json::from_str(&x)?,
        PrismaValue::Int(x) => Value::Number(match Number::from_f64(x as f64) {
            Some(num) => num,
            None => return Err(PrismaError::SerializationError("`f64` number was invalid".into())),
        }),
        PrismaValue::Relation(_) => unimplemented!(),
        PrismaValue::Null => Value::Null,
        PrismaValue::Uuid(x) => Value::String(x.to_hyphenated().to_string()),
        PrismaValue::GraphqlId(x) => serialize_graphql_id(&x)?,
        PrismaValue::List(_) => unimplemented!(),
    })
}

fn serialize_graphql_id(id: &GraphqlId) -> PrismaResult<Value> {
    Ok(match id {
        GraphqlId::String(x) => Value::String(x.clone()),
        GraphqlId::Int(x) => Value::Number(match serde_json::Number::from_f64(*x as f64) {
            Some(num) => num,
            None => return Err(PrismaError::SerializationError("`f64` number was invalid".into())),
        }),
        GraphqlId::UUID(x) => Value::String(x.to_hyphenated().to_string()),
    })
}
