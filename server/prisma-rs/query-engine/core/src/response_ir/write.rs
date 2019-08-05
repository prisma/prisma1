use super::*;
use crate::{CoreResult, OutputTypeRef};
use connector::{Identifier, WriteQueryResult};
use prisma_models::PrismaValue;

/// We just assume we ever get a single item back
pub fn serialize_write(result: WriteQueryResult, typ: &OutputTypeRef) -> CoreResult<Item> {
    match result.identifier {
        Identifier::Count(c) => {
            let mut map: IndexMap<String, Item> = IndexMap::new();

            map.insert("count".into(), Item::Value(PrismaValue::Int(c as i64)));
            Ok(Item::Map(map))
        }
        Identifier::Record(r) => {
            let obj = typ.as_object_type().unwrap();
            let mut map: IndexMap<String, Item> = IndexMap::new();

            let mut values = r.record.values;
            let mut fields = r.field_names;

            obj.get_fields().iter().for_each(|field| {
                if let Some(pos) = fields.iter().position(|f| &field.name == f) {
                    let val = values.remove(pos);
                    fields.remove(pos);

                    map.insert(field.name.clone(), Item::Value(val));
                }
            });

            Ok(Item::Map(map))
        }
        _ => unreachable!(),
    }
}

// /// Attempts to coerce the given write result into the provided output type.
// fn coerce_result(result: WriteQueryResult, typ: &OutputTypeRef) -> CoreResult<QueryResult> {
//     let value: PrismaValue = match result.identifier {
//         Identifier::Id(id) => id.into(),
//         Identifier::Count(c) => PrismaValue::from(c), // Requires object with one field that is usize / int / float, or single scalar type.
//         Identifier::Record(r) => unimplemented!(),    // Requires object. Try coercing all fields of the object.
//         Identifier::None => unimplemented!(),         // Null?
//     };

//     unimplemented!()
// }

// fn coerce_value_type(val: PrismaValue, typ: &OutputTypeRef) -> CoreResult<()> {
//     match typ.borrow() {
//         OutputType::Object(o) => unimplemented!(),
//         OutputType::Opt(inner) => unimplemented!(),
//         OutputType::Enum(e) => unimplemented!(),
//         OutputType::List(inner) => unimplemented!(),
//         OutputType::Scalar(s) => unimplemented!(),
//     };

//     unimplemented!()
// }

// fn coerce_scalar() -> CoreResult<()> {
//     unimplemented!()
// }
