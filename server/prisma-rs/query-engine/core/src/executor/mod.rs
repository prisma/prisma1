//mod pipeline;
mod read;
mod write;

pub use read::ReadQueryExecutor;
pub use write::WriteQueryExecutor;

use crate::{
    query_builders::QueryBuilder,
    query_document::QueryDocument,
    result_ir::{Response, ResultIrBuilder},
    CoreError, CoreResult, OutputType, OutputTypeRef, QueryResult, QuerySchemaRef, ResultResolutionStrategy,
};
use connector::{
    filter::RecordFinder, Identifier, Query, ReadQuery, ReadQueryResult, SingleReadQueryResult, WriteQueryResult,
};
use prisma_models::{ModelRef, PrismaValue};
use std::borrow::Borrow;

/// Central query executor and main entry point into the query core.
pub struct QueryExecutor {
    read_executor: ReadQueryExecutor,
    write_executor: WriteQueryExecutor,
}

// Todo:
// - Partial execution semantics?
// - Do we need a clearer separation of queries coming from different query blocks? (e.g. 2 query { ... } in GQL)
// - ReadQueryResult should probably just be QueryResult
// - This is all temporary code until the larger query execution overhaul.
impl QueryExecutor {
    pub fn new(read_executor: ReadQueryExecutor, write_executor: WriteQueryExecutor) -> Self {
        QueryExecutor {
            read_executor,
            write_executor,
        }
    }

    /// Executes a query document, which involves parsing & validating the document,
    /// building queries and a query execution plan, and finally calling the connector APIs to
    /// resolve the queries and build reponses.
    pub fn execute(&self, query_doc: QueryDocument, query_schema: QuerySchemaRef) -> CoreResult<Vec<Response>> {
        // 1. Parse and validate query document (building)
        let queries = QueryBuilder::new(query_schema).build(query_doc)?;

        // 2. Build query plan
        // ...

        // 3. Execute query plan
        let results: Vec<QueryResult> = queries
            .into_iter()
            .map(|query| match query {
                (Query::Read(read), _) => self
                    .read_executor
                    .execute(read, vec![])
                    .map(|res| QueryResult::Read(res)),

                (Query::Write(write), strategy) => {
                    let model = write
                        .extract_model()
                        .expect("Expected root queries to have an associated model.");

                    let write_result = self.write_executor.execute(write)?;

                    match strategy {
                        ResultResolutionStrategy::CoerceInto(ref typ) => Self::coerce_result(write_result, typ),
                        ResultResolutionStrategy::Query(q) => match q {
                            Query::Read(ReadQuery::RecordQuery(mut rq)) => {
                                // Inject required information into the query and execute
                                rq.record_finder = Some(Self::to_record_finder(&write_result, model)?);
                                self.read_executor.read_one(rq).map(|res| QueryResult::Read(res))
                            }
                            _ => unreachable!(), // Invariant for now
                        },
                        ResultResolutionStrategy::None => unimplemented!(),
                    }
                }
            })
            .collect::<CoreResult<Vec<_>>>()?;

        // 4. Build IR response / Parse results into IR response
        // Ok(results
        //     .into_iter()
        //     .fold(ResultIrBuilder::new(), |builder, result| builder.add(result))
        //     .build())
        unimplemented!()
    }

    /// Attempts to coerce the given write result into the provided output type.
    fn coerce_result(result: WriteQueryResult, typ: &OutputTypeRef) -> CoreResult<QueryResult> {
        let value: PrismaValue = match result.identifier {
            Identifier::Id(id) => { // Requires
                let value: PrismaValue = id.into();

                unimplemented!()
            }
            Identifier::Count(c) => PrismaValue::from(c), // Requires object with one field that is usize / int / float, or single scalar type.
            Identifier::Record(r) => unimplemented!(), // Requires object. Try coercing all fields of the object.
            Identifier::None => unimplemented!(), // Null?
        };

        unimplemented!()
    }

    fn coerce_type(val: PrismaValue, typ: &OutputTypeRef) -> CoreResult<()> {

        match typ.borrow() {
                    OutputType::Object(o) => unimplemented!(),
                    OutputType::Opt(inner) => unimplemented!(),
                    OutputType::Enum(e) => unimplemented!(),
                    OutputType::List(inner) => unimplemented!(),
                    OutputType::Scalar(s) => unimplemented!(),
                };

        unimplemented!()
    }

    fn coerce_scalar() -> CoreResult<()> {
        unimplemented!()
    }

    /// Attempts to convert a write query result into a RecordFinder required for dependent queries.
    /// Assumes ID field is used as dependent field (which is true for now in the current execution model).
    fn to_record_finder(write_result: &WriteQueryResult, model: ModelRef) -> CoreResult<RecordFinder> {
        let id_field = model.fields().id();

        match &write_result.identifier {
            Identifier::Id(id) => Ok(RecordFinder::new(id_field, id)),
            Identifier::Record(r) => r
                .collect_id(&id_field.name)
                .map(|id_val| RecordFinder::new(id_field, id_val))
                .map_err(|err| err.into()),

            other => Err(CoreError::ConversionError(format!(
                "Impossible conversion of write query result {:?} to RecordFinder.",
                other
            ))),
        }
    }

    //    /// Legacy path
    //    /// Can be given a list of both ReadQueries and WriteQueries
    //    ///
    //    /// Will execute WriteQueries first, then all ReadQueries, while preserving order.
    //    pub fn exec_all(&self, queries: Vec<Query>) -> CoreResult<Vec<ReadQueryResult>> {
    //        // Give all queries to the pipeline module
    //        let mut pipeline = QueryPipeline::from(queries);
    //
    //        // Execute prefetch queries for destructive writes
    //        let (idx, queries): (Vec<_>, Vec<_>) = pipeline.prefetch().into_iter().unzip();
    //        let results = self.read_executor.execute(&queries)?;
    //        pipeline.store_prefetch(idx.into_iter().zip(results).collect());
    //
    //        // Execute write queries and generate required read queries
    //        let (idx, writes): (Vec<_>, Vec<_>) = pipeline.get_writes().into_iter().unzip();
    //        let results = self.write_executor.execute(writes)?;
    //        let (idx, reads): (Vec<_>, Vec<_>) = pipeline
    //            .process_writes(idx.into_iter().zip(results).collect())
    //            .into_iter()
    //            .unzip();
    //
    //        // Execute read queries created by write-queries
    //        let results = self.read_executor.execute(&reads)?;
    //        pipeline.store_reads(idx.into_iter().zip(results.into_iter()).collect());
    //
    //        // Now execute all remaining reads
    //        let (idx, queries): (Vec<_>, Vec<_>) = pipeline.get_reads().into_iter().unzip();
    //        let results = self.read_executor.execute(&queries)?;
    //        pipeline.store_reads(idx.into_iter().zip(results).collect());
    //
    //        // Consume pipeline into return value
    //        Ok(pipeline.consume())
    //    }

    /// Returns db name used in the executor.
    pub fn db_name(&self) -> String {
        self.write_executor.db_name.clone()
    }
}
