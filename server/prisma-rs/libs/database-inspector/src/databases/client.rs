// TODO: This will be replaced by prisma-query.
pub trait DatabaseClient {
    fn query(&self, query: &str, variables: ()) -> ();
}
