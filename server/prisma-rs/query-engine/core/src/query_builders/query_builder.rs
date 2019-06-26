use crate::query_ir::QueryValue;
use crate::{
    query_ir::{Operation, QueryDocument, ReadOperation},
    CoreError, CoreResult, InputType, QuerySchemaRef,
};
use chrono::format::Parsed;
use connector::Query;
use prisma_models::PrismaValue;
use std::collections::BTreeMap;

pub struct QueryBuilder {
    pub query_schema: QuerySchemaRef,
}

impl QueryBuilder {
    pub fn build(self, query_doc: QueryDocument) -> CoreResult<Vec<Query>> {
        query_doc
            .operations
            .into_iter()
            .map(|op| self.map_operation(op))
            .collect()
    }

    fn map_operation(&self, operation: Operation) -> CoreResult<Query> {
        match operation {
            Operation::Read(read_op) => self.map_read(read_op),
            Operation::Write(write_op) => unimplemented!(),
        }
    }

    fn map_read(&self, read_op: ReadOperation) -> CoreResult<Query> {
        let test: Vec<Query> = read_op
            .selections
            .into_iter()
            .map(
                |selection| match self.query_schema.find_query_field(selection.name.as_ref()) {
                    Some(field) => {
                        let parsed_args = field
                            .arguments
                            .iter()
                            .map(|schema_arg| {
                                // If argument is not present but is not optional -> error
                                // If argument is present but not in the list -> error

                                // Might be an fn on the field
                                match selection
                                    .arguments
                                    .iter()
                                    .find(|selection_arg| selection_arg.0 == schema_arg.name)
                                {
                                    None => self
                                        .parse_value(None, &schema_arg.argument_type)
                                        .map(|val| (schema_arg.name.clone(), val)),

                                    Some(selection_arg) => self
                                        .parse_value(Some(&selection_arg.1), &schema_arg.argument_type)
                                        .map(|val| (selection_arg.0.clone(), val)),
                                }
                            })
                            .collect::<Vec<CoreResult<(String, PrismaValue)>>>();

                        unimplemented!()
                    }
                    None => Err(CoreError::QueryValidationError(format!(
                        "Field '{}' not found on query schema object 'Query'.",
                        selection.name
                    ))),
                },
            )
            .collect::<CoreResult<Vec<Query>>>()?;

        // every read op maps to one root read query.

        unimplemented!()
    }

    /// Parses and validates a QueryValue against an InputType
    /// Some(value) indicates that a value is present, None indicates that no value was provided.
    fn parse_value(&self, value: Option<&QueryValue>, input_type: &InputType) -> CoreResult<PrismaValue> {
        match input_type {
            InputType::Enum(et) => unimplemented!(),
            InputType::List(inner) => unimplemented!(), // single value of matching type will be coerced to list with one element
            InputType::Object(obj) => unimplemented!(),
            InputType::Opt(inner) => unimplemented!(),
            InputType::Scalar(scalar) => unimplemented!(),
        }
    }
}

enum ParsedValue {
    Single(PrismaValue),
    Map(BTreeMap<String, ParsedValue>),
}
