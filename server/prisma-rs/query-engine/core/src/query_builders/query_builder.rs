use crate::query_ir::QueryValue;
use crate::{
    query_ir::{Operation, QueryDocument, ReadOperation},
    Argument, CoreError, CoreResult, EnumType, Field, InputObjectTypeStrongRef, InputType, IntoArc, QuerySchemaRef,
    QueryValidationError, ScalarType,
};
use chrono::prelude::*;
use connector::Query;
use prisma_models::{GraphqlId, PrismaValue};
use std::{collections::BTreeMap, result::Result};
use uuid::Uuid;

type QueryBuilderResult<T> = Result<T, QueryValidationError>;

pub struct QueryBuilder {
    pub query_schema: QuerySchemaRef,
}

impl QueryBuilder {
    pub fn build(self, query_doc: QueryDocument) -> CoreResult<Vec<Query>> {
        query_doc
            .operations
            .into_iter()
            .map(|op| self.map_operation(op).map_err(|err| err.into()))
            .collect()
    }

    fn map_operation(&self, operation: Operation) -> QueryBuilderResult<Query> {
        match operation {
            Operation::Read(read_op) => self.map_read(read_op),
            Operation::Write(write_op) => unimplemented!(),
        }
    }

    fn map_read(&self, read_op: ReadOperation) -> QueryBuilderResult<Query> {
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
                                let selection_arg: Option<&(String, QueryValue)> = selection
                                    .arguments
                                    .iter()
                                    .find(|selection_arg| selection_arg.0 == schema_arg.name);

                                let parsed_value = self
                                    .parse_value(selection_arg.map(|x| &x.1), &schema_arg.argument_type)
                                    .map(|val| (schema_arg.name.clone(), val))
                                    .map_err(|err| QueryValidationError::ArgumentValidationError {
                                        argument: schema_arg.name.clone(),
                                        inner: Box::new(err),
                                    })?;

                                //                                {
                                //                                    None => self
                                //                                        .parse_value(None, &schema_arg.argument_type)
                                //                                        .map(|val| (schema_arg.name.clone(), val)),
                                //
                                //                                    Some(selection_arg) => self
                                //                                        .parse_value(Some(&selection_arg.1), &schema_arg.argument_type)
                                //                                        .map(|val| (selection_arg.0.clone(), val)),
                                //                                }
                                unimplemented!()
                            })
                            .collect::<Vec<CoreResult<(String, PrismaValue)>>>();

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

        // every read op maps to one root read query.

        unimplemented!()
    }

    /// Todo: Handle defaults!
    /// Parses and validates a QueryValue against an InputType, recursively.
    /// Some(value) indicates that a value is present in the query, None indicates that no value was provided.
    /// Special case is Some(Null). In that case an explicit null was provided, which is, however, treated
    /// the same as None during validation.
    #[rustfmt::skip]
    fn parse_value(&self, value: Option<&QueryValue>, input_type: &InputType) -> QueryBuilderResult<ParsedValue> {
        match (value, input_type) {
            (None, InputType::Opt(inner))                         => self.parse_value(value, inner),
            (Some(QueryValue::Null), InputType::Opt(inner))       => self.parse_value(value, inner),
            (None, _)                                             => Err(QueryValidationError::RequiredValueNotSetError),
            (Some(val), InputType::Scalar(scalar))                => self.parse_scalar(val, scalar).map(|pv| ParsedValue::Single(pv)),
//            (Some(val), InputType::Enum(et))                      => self.parse_scalar(val, scalar),

            (Some(QueryValue::Object(_)), InputType::List(_))     => Err(QueryValidationError::ValueTypeMismatchError { have: value.unwrap().clone(), want: input_type.clone() }),
            (Some(QueryValue::List(values)), InputType::List(l))  => self.parse_list(values.iter().collect(), l).map(|pv| ParsedValue::Single(pv)),
            (Some(val), InputType::List(l))                       => self.parse_list(vec![val], l).map(|pv| ParsedValue::Single(pv)),

            (Some(QueryValue::Object(o)), InputType::Object(obj)) => self.parse_object(o, obj.into_arc()).map(|btree| ParsedValue::Map(btree)),
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
        let values: Vec<ParsedValue> = values
            .into_iter()
            .map(|val| self.parse_value(Some(val), value_type))
            .collect::<QueryBuilderResult<Vec<ParsedValue>>>()?;

        let values: Vec<PrismaValue> = values
            .into_iter()
            .map(|val| match val {
                ParsedValue::Single(inner) => inner,
                _ => unreachable!(), // Objects are relations, which are handled separately and can't occur for scalar lists.
            })
            .collect();

        Ok(PrismaValue::List(Some(values)))
    }

    fn parse_object(
        &self,
        object: &BTreeMap<String, QueryValue>,
        schema_object: InputObjectTypeStrongRef,
    ) -> QueryBuilderResult<BTreeMap<String, ParsedValue>> {
        schema_object
            .get_fields()
            .iter()
            .map(|field| {
                // Find field in the passed object
                // Check type
                // If not present - take default of the input field TODO this needs to be implemented on the schema building as well!
                // The type check will fail in the parse_value - nothing needs to be done here. Simply gather all the fields and return
                unimplemented!()
            })
            .collect()
    }
}

enum ParsedValue {
    Single(PrismaValue),
    Map(BTreeMap<String, ParsedValue>),
}
