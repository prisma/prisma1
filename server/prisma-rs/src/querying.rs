use std::rc::Rc;

use crate::{
    models::{Model, ScalarField, Project},
    protobuf::prisma::QueryArguments,
    PrismaValue,
};

pub struct NodeSelector<'a> {
    pub project: Project,
    pub database: &'a str,
    pub model: Rc<Model>,
    pub query_arguments: &'a QueryArguments,
    pub selected_fields: &'a [&'a ScalarField],
}

impl<'a> NodeSelector<'a> {
    pub fn new(
        project: Project,
        model: Rc<Model>,
        query_arguments: &'a QueryArguments,
        selected_fields: &'a [&'a ScalarField],
    ) -> NodeSelector<'a> {
        NodeSelector {
            database,
            model,
            query_arguments,
            selected_fields,
        }
    }
}
