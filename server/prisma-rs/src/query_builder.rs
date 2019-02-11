use crate::{
    models::{Model, ScalarField, Project},
    protobuf::prisma::{QueryArguments, SelectedField},
};

use sql::{grammar::statement::Select, prelude::*};

const TOP_LEVEL_ALIAS:        &str = "Alias";
const RELATION_TABLE_ALIAS:   &str = "RelationTable";
const INT_DUMMY:              &str = 1;
const STRING_DUMMY:           &str = "";
const RELATED_MODEL_ALIAS:    &str = "__RelatedModel__";
const PARENT_MODEL_ALIAS:     &str = "__ParentModel__";
const ROW_NUMBER_ALIAS:       &str = "prismaRowNumberAlias";
const BASE_TABLE_ALIAS:       &str = "prismaBaseTableAlias";
const ROW_NUMBER_TABLE_ALIAS: &str = "prismaRowNumberTableAlias";
const NODE_ID_FIELD_NAME:     &str = "nodeId";
const POSITION_FIELD_NAME:    &str = "position";
const VALUE_FIELD_NAME:       &str = "value";
const PLACEHOLDER:            &str = "?";
const RELAY_TABLE_NAME:       &str = "_RelayId";

pub struct QueryBuilder<'a> {
    project: &'a Project,
    filter_condition_builder: FilterConditionBuilder<'a>
}

impl<'a> QueryBuilder<'a> {
    pub fn new(project: &'a project) -> QueryBuilder<'a> {
        let filter_condition_builder = FilterConditionBuilder::new(project);

        QueryBuilder {
            project,
            filter_condition_builder,
        }
    }
}
