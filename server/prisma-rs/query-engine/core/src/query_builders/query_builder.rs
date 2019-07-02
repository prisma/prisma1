use crate::{query_document::{
    QueryValue,
    Operation,
    QueryDocument,
    ReadOperation
}, Argument, CoreError, CoreResult, EnumType, Field, InputObjectTypeStrongRef, InputType, IntoArc, QuerySchemaRef, QueryValidationError, ScalarType, OperationTag, OutputType, FieldRef, ObjectTypeStrongRef};
use chrono::prelude::*;
use connector::Query;
use prisma_models::{GraphqlId, PrismaValue, ModelRef};
use std::{collections::BTreeMap, result::Result};
use uuid::Uuid;
use crate::query_document::Selection;

type QueryBuilderResult<T> = Result<T, QueryValidationError>;

pub struct QueryBuilder {
    pub query_schema: QuerySchemaRef,
}

impl QueryBuilder {
    /// Builds queries from a query document.
    pub fn build(self, query_doc: QueryDocument) -> CoreResult<Vec<Query>> {
        query_doc
            .operations
            .into_iter()
            .map(|op| self.map_operation(op).map_err(|err| err.into()))
            .collect()
    }

    /// Maps an operation to a query
    fn map_operation(&self, operation: Operation) -> QueryBuilderResult<Query> {
        match operation {
            Operation::Read(read_op) => self.map_read(read_op),
            Operation::Write(write_op) => unimplemented!(),
        }
    }

    /// Maps a read operation to a query.
    fn map_read(&self, read_op: ReadOperation) -> QueryBuilderResult<Query> {
        // Parse and validate the incoming read operation against the query object.
        let parsed = self.parse_object(&read_op.selections, self.query_schema.query())?;


        // Special treatment on read root: all fields map to a model operation.
        // This means: Find matching schema field (which is a bit redundant here) for each operation build a query.


        // every read op maps to one root read query.

        unimplemented!()
    }

    /// Parses and validates a set of selections against a schema (output) object.
    // Todo empty selection set: Does that fail at the parser level already?
    fn parse_object(&self, selections: &Vec<Selection>, object: ObjectTypeStrongRef) -> QueryBuilderResult<ParsedObject> {
        // Todo WIP
        selections
            .into_iter()
            .map(
                |selection| match self.query_schema.find_query_field(selection.name.as_ref()) {
                    Some(ref field) => {
                        // Parse and validate all provided arguments
                        let parsed_arguments = self.parse_arguments(field, &selection.arguments).map_err(|err| {
                            QueryValidationError::FieldValidationError {
                                field_name: field.name.clone(),
                                reason: Box::new(err),
                                on_object: "Query".into(),
                            }})?;


                        // Based on the operation that is present on the root read field, build the query.
                        let field_operation = field.operation.as_ref().expect("Expect Query and Mutation object fields to always have an associated ");

                        // Validate that sub selection set is only selecting fields that are allowed.


                        // - Parse RecordFinder for read-one operation
                        // - Parse QueryArguments for read-many operation
                        // - Output fields are the actual reads

//                        match field_operation.operation {
//                            OperationTag::FindOne => Ok(ReadQueryBuilder::One(
//                                OneBuilder::new().setup(Arc::clone(&model), field),
//                            )),
//                            OperationTag::FindMany => Ok(ReadQueryBuilder::Many(
//                                ManyBuilder::new().setup(Arc::clone(&model), field),
//                            )),
//                            _ => Err(CoreError::LegacyQueryValidationError(format!(
//                                "Invalid root operation on Query: {:?}",
//                                operation
//                            ))),
//                        }

                        // the builders get:
                        // - the parsed args
                        // - the selections
                        // - whatever input needed, for example the model it needs to operate on

                        unimplemented!()
                    }

                    None => Err(QueryValidationError::FieldValidationError {
                        field_name: selection.name,
                        reason: Box::new(QueryValidationError::FieldNotFoundError),
                        on_object: "Query".into(),
                    }),
                },
            )
            .collect::<QueryBuilderResult<Vec<Query>>>()?;

        unimplemented!()
    }

    /// Parses and validates selection arguments against a schema defined field.
    fn parse_arguments(&self, schema_field: &FieldRef, given_arguments: &Vec<(String, QueryValue)>) -> QueryBuilderResult<Vec<(String, ParsedInputValue)>> {
        schema_field
            .arguments
            .iter()
            .map(|schema_arg| {
                // Match schema field to a field in the incoming document
                let selection_arg: Option<&(String, QueryValue)> = given_arguments
                    .iter()
                    .find(|given_argument| given_argument.0 == schema_arg.name);

                // Parse the query value into a list / object / PrismaValue.
                // If the field was not found previously, None will be handed into the
                // parsing, which also checks if this is valid in context of the schema
                // (field is optional or not).
                self
                    .parse_input_value(selection_arg.map(|x| &x.1), &schema_arg.argument_type)
                    .map(|val| (schema_arg.name.clone(), val))
                    .map_err(|err| QueryValidationError::ArgumentValidationError {
                        argument: schema_arg.name.clone(),
                        inner: Box::new(err),
                    })
            })
            .collect::<Vec<QueryBuilderResult<(String, ParsedInputValue)>>>().into_iter().collect()
    }

    /// Parses and validates a QueryValue against an InputType, recursively.
    /// Some(value) indicates that a value is present in the query doc, None indicates that no value was provided.
    /// Special case is Some(Null). In that case an explicit null was provided, which is, however, treated
    /// the same as None during validation.
    #[rustfmt::skip]
    fn parse_input_value(&self, value: Option<&QueryValue>, input_type: &InputType) -> QueryBuilderResult<ParsedInputValue> {
        match (value, input_type) {
            (None, InputType::Opt(inner))                         => self.parse_input_value(value, inner),
            (Some(QueryValue::Null), InputType::Opt(inner))       => self.parse_input_value(value, inner),
            (None, _)                                             => Err(QueryValidationError::RequiredValueNotSetError),
            (Some(val), InputType::Scalar(scalar))                => self.parse_scalar(val, scalar).map(|pv| ParsedInputValue::Single(pv)),
//            (Some(val), InputType::Enum(et))                      => self.parse_scalar(val, scalar),

            (Some(QueryValue::Object(_)), InputType::List(_))     => Err(QueryValidationError::ValueTypeMismatchError { have: value.unwrap().clone(), want: input_type.clone() }),
            (Some(QueryValue::List(values)), InputType::List(l))  => self.parse_list(values.iter().collect(), l).map(|pv| ParsedInputValue::Single(pv)),
            (Some(val), InputType::List(l))                       => self.parse_list(vec![val], l).map(|pv| ParsedInputValue::Single(pv)),

            (Some(QueryValue::Object(o)), InputType::Object(obj)) => self.parse_input_object(o, obj.into_arc()).map(|btree| ParsedInputValue::Map(btree)),
            (Some(qv), InputType::Object(obj))                    => Err(QueryValidationError::ValueTypeMismatchError { have: qv.clone(), want: input_type.clone() }),
            _                                                     => unreachable!(),
        }
    }

    /// Attempts to convert given query value into a concrete PrismaValue based on given scalar type.
    /// Only callable for non-null scalar values.
    #[rustfmt::skip]
    fn parse_scalar(&self, value: &QueryValue, scalar_type: &ScalarType) -> QueryBuilderResult<PrismaValue> {
        match (value, scalar_type) {
            (QueryValue::Null, _)                         => Ok(PrismaValue::Null),
            (QueryValue::String(s), ScalarType::String)   => Ok(PrismaValue::String(s.clone())),
            (QueryValue::String(s), ScalarType::DateTime) => Self::parse_datetime(s).map(|dt| PrismaValue::DateTime(dt)),
            (QueryValue::String(s), ScalarType::Json)     => Self::parse_json(s).map(|j| PrismaValue::Json(j)),
            (QueryValue::String(s), ScalarType::UUID)     => Self::parse_uuid(s).map(|u| PrismaValue::Uuid(u)),
            (QueryValue::Int(i), ScalarType::Int)         => Ok(PrismaValue::Int(*i)),
            (QueryValue::Float(f), ScalarType::Float)     => Ok(PrismaValue::Float(*f)),
            (QueryValue::Boolean(b), ScalarType::Boolean) => Ok(PrismaValue::Boolean(*b)),
            (QueryValue::Enum(e), ScalarType::Enum(et))   => match et.value_for(e).and_then(|val| val.as_string()) {
                                                                Some(val) => Ok(PrismaValue::Enum(val)),
                                                                None => Err(QueryValidationError::ValueParseError(format!("Enum value '{}' is invalid for enum type {}.", e, et.name)))
                                                             },

            // Possible ID combinations TODO UUID ids are not encoded in any useful way in the schema.
            (QueryValue::String(s), ScalarType::ID)       => Self::parse_uuid(s).map(|u| PrismaValue::Uuid(u)).or_else(|_| Ok(PrismaValue::String(s.clone()))),
            (QueryValue::Int(i), ScalarType::ID)          => Ok(PrismaValue::GraphqlId(GraphqlId::Int(*i as usize))),

            // Remainder of combinations is invalid
            (qv, st)                                      => Err(QueryValidationError::ValueTypeMismatchError { have: qv.clone(), want: InputType::Scalar(scalar_type.clone()) }),
        }
    }

    fn parse_datetime(s: &str) -> QueryBuilderResult<DateTime<Utc>> {
        let fmt = "%Y-%m-%dT%H:%M:%S%.3f";
        Utc.datetime_from_str(s.trim_end_matches("Z"), fmt)
            .map(|dt| DateTime::<Utc>::from_utc(dt.naive_utc(), Utc))
            .map_err(|err| {
                QueryValidationError::ValueParseError(format!(
                    "Invalid DateTime: {} DateTime must adhere to format: %Y-%m-%dT%H:%M:%S%.3f",
                    err
                ))
            })
    }

    fn parse_json(s: &str) -> QueryBuilderResult<serde_json::Value> {
        serde_json::from_str(s).map_err(|err| QueryValidationError::ValueParseError(format!("Invalid json: {}", err)))
    }

    fn parse_uuid(s: &str) -> QueryBuilderResult<Uuid> {
        Uuid::parse_str(s).map_err(|err| QueryValidationError::ValueParseError(format!("Invalid UUID: {}", err)))
    }

    fn parse_list(&self, values: Vec<&QueryValue>, value_type: &InputType) -> QueryBuilderResult<PrismaValue> {
        let values: Vec<ParsedInputValue> = values
            .into_iter()
            .map(|val| self.parse_input_value(Some(val), value_type))
            .collect::<QueryBuilderResult<Vec<ParsedInputValue>>>()?;

        let values: Vec<PrismaValue> = values
            .into_iter()
            .map(|val| match val {
                ParsedInputValue::Single(inner) => inner,
                _ => unreachable!(), // Objects represent relations, which are handled separately and can't occur for scalar lists.
            })
            .collect();

        Ok(PrismaValue::List(Some(values)))
    }

    /// Parses and validates an input object recursively.
    fn parse_input_object(
        &self,
        object: &BTreeMap<String, QueryValue>,
        schema_object: InputObjectTypeStrongRef,
    ) -> QueryBuilderResult<BTreeMap<String, ParsedInputValue>> {
        schema_object
            .get_fields()
            .iter()
            .map(|field| {
                // Find field in the passed object
                match object.iter().find(|(k, v)| *k == &field.name) {
                    Some((k, v)) => self
                        .parse_input_value(Some(v), &field.field_type)
                        .map(|parsed| (k.clone(), parsed)),

                    None => {
                        // Find default value and use that one if the field can't be found.
                        match field.default_value.clone().map(|def| (&field.name, def)) {
                            Some((k, v)) => Ok((k.clone(), ParsedInputValue::Single(v))),
                            None => Err(QueryValidationError::FieldNotFoundError),
                        }
                    }
                }
            })
            .collect::<QueryBuilderResult<Vec<(String, ParsedInputValue)>>>()
            .map(|tuples| tuples.into_iter().collect())
    }
}

// Todo: Naming sucks
struct ParsedObject {
    pub fields: BTreeMap<String, Vec<ParsedArgument>>
}

struct ParsedArgument {
    pub name: String,
    pub value: ParsedValue,
}

enum ParsedInputValue {
    Single(PrismaValue),
    Map(BTreeMap<String, ParsedInputValue>),
}
