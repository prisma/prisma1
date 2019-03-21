use failure::Fail;

#[derive(Debug)]
pub enum Missing {
    Model { name: String },
    Field { name: String, model: String },
    Relation { name: String },
    ScalarField { name: String, model: String },
    RelationField { name: String, model: String },
    FieldForRelation { relation: String, model: String },
    ModelForRelation { model_id: String, relation: String },
}

#[derive(Debug, Fail)]
pub enum DomainError {
    #[fail(display = "Couldn't find the requested resource.")]
    NotFound(Missing),
}
