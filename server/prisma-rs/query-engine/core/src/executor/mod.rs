mod read;
mod write;

pub use read::ReadQueryExecutor;
pub use write::WriteQueryExecutor;

use crate::{
    query_builders::QueryBuilder,
    query_document::QueryDocument,
    response_ir::{Response, ResultIrBuilder},
    CoreError, CoreResult, QueryPair, QuerySchemaRef, ResultPair, ResultResolutionStrategy,
};
use connector::{ModelExtractor, Query, ReadQuery};

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
        let results: Vec<ResultPair> = self.execute_queries(queries)?;

        // 4. Build IR response / Parse results into IR response
        Ok(results
            .into_iter()
            .fold(ResultIrBuilder::new(), |builder, result| builder.push(result))
            .build())
    }

    fn execute_queries(&self, queries: Vec<QueryPair>) -> CoreResult<Vec<ResultPair>> {
        queries.into_iter().map(|query| self.execute_query(query)).collect()
    }

    fn execute_query(&self, query: QueryPair) -> CoreResult<ResultPair> {
        let (query, strategy) = query;
        let model_opt = query.extract_model();
        match query {
            Query::Read(read) => {
                let query_result = self.read_executor.execute(read, &[])?;

                Ok(match strategy {
                    ResultResolutionStrategy::Serialize(typ) => ResultPair::Read(query_result, typ),
                    ResultResolutionStrategy::Dependent(_) => unimplemented!(), // Dependent query exec. from read is not supported in this execution model.
                })
            }

            Query::Write(write) => {
                let query_result = self.write_executor.execute(write)?;

                match strategy {
                    ResultResolutionStrategy::Serialize(typ) => Ok(ResultPair::Write(query_result, typ)),
                    ResultResolutionStrategy::Dependent(dependent_pair) => match model_opt {
                        Some(model) => match *dependent_pair {
                            (Query::Read(ReadQuery::RecordQuery(mut rq)), strategy) => {
                                // Inject required information into the query and execute
                                rq.record_finder = Some(query_result.result.to_record_finder(model)?);

                                let dependent_pair = (Query::Read(ReadQuery::RecordQuery(rq)), strategy);
                                self.execute_query(dependent_pair)
                            }
                            _ => unreachable!(), // Invariant for now
                        },
                        None => Err(CoreError::ConversionError(
                            "Model required for dependent query execution".into(),
                        )),
                    },
                }
            }
        }
    }

    /// Returns db name used in the executor.
    pub fn db_name(&self) -> String {
        self.write_executor.db_name.clone()
    }
}
