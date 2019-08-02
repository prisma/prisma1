
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

