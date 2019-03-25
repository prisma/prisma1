use crate::{ModelRef, Relation, RelationField, ScalarField};
use once_cell::unsync::OnceCell;
use prisma_query::ast::Column;
use std::sync::Arc;

pub trait IntoSelectedFields {
    fn into_selected_fields(self, model: ModelRef, from_field: Option<Arc<RelationField>>) -> SelectedFields;
}

#[derive(Debug, Default)]
pub struct SelectedFields {
    scalar: Vec<SelectedScalarField>,
    pub relation: Vec<SelectedRelationField>,
    pub from_field: Option<Arc<RelationField>>,
    columns: OnceCell<Vec<Column>>,
}

#[derive(Debug)]
pub enum SelectedField {
    Scalar(SelectedScalarField),
    Relation(SelectedRelationField),
}

#[derive(Debug)]
pub struct SelectedScalarField {
    pub field: Arc<ScalarField>,
}

#[derive(Debug)]
pub struct SelectedRelationField {
    pub field: Arc<RelationField>,
    pub selected_fields: SelectedFields,
}

impl From<Arc<ScalarField>> for SelectedField {
    fn from(sf: Arc<ScalarField>) -> SelectedField {
        SelectedField::Scalar(SelectedScalarField { field: sf })
    }
}

impl From<Arc<ScalarField>> for SelectedFields {
    fn from(sf: Arc<ScalarField>) -> SelectedFields {
        SelectedFields::new(vec![SelectedField::from(sf)], None)
    }
}

impl From<Vec<Arc<ScalarField>>> for SelectedFields {
    fn from(sfs: Vec<Arc<ScalarField>>) -> SelectedFields {
        let fields = sfs.into_iter().map(SelectedField::from).collect();

        SelectedFields::new(fields, None)
    }
}

impl SelectedFields {
    pub const RELATED_MODEL_ALIAS: &'static str = "__RelatedModel__";
    pub const PARENT_MODEL_ALIAS: &'static str = "__ParentModel__";

    pub fn new(fields: Vec<SelectedField>, from_field: Option<Arc<RelationField>>) -> SelectedFields {
        let (scalar, relation) = fields.into_iter().fold((Vec::new(), Vec::new()), |mut acc, field| {
            match field {
                SelectedField::Scalar(sf) => acc.0.push(sf),
                SelectedField::Relation(sf) => acc.1.push(sf),
            }

            acc
        });

        let columns = OnceCell::new();

        SelectedFields {
            scalar,
            relation,
            from_field,
            columns,
        }
    }

    pub fn all_scalar(model: ModelRef, from_field: Option<Arc<RelationField>>) -> SelectedFields {
        let fields = model
            .fields()
            .scalar()
            .iter()
            .map(|field| SelectedField::Scalar(SelectedScalarField { field: field.clone() }))
            .collect();

        Self::new(fields, from_field)
    }

    pub fn add_scalar(&mut self, field: Arc<ScalarField>) {
        self.scalar.push(SelectedScalarField { field });
    }

    pub fn columns(&self) -> &[Column] {
        self.columns
            .get_or_init(|| {
                let mut result: Vec<Column> = self.scalar_non_list().iter().map(|f| f.as_column()).collect();

                if let Some(ref from_field) = self.from_field {
                    let relation = from_field.relation();

                    result.push(
                        relation
                            .column_for_relation_side(from_field.relation_side.opposite())
                            .alias(Self::RELATED_MODEL_ALIAS)
                            .table(Relation::TABLE_ALIAS.into()),
                    );

                    result.push(
                        relation
                            .column_for_relation_side(from_field.relation_side)
                            .alias(Self::PARENT_MODEL_ALIAS)
                            .table(Relation::TABLE_ALIAS.into()),
                    );
                };

                result
            })
            .as_slice()
    }

    pub fn needs_relation_fields(&self) -> bool {
        self.from_field.is_some()
    }

    pub fn scalar_non_list(&self) -> Vec<Arc<ScalarField>> {
        self.scalar
            .iter()
            .filter(|sf| !sf.field.is_list)
            .map(|sf| sf.field.clone())
            .collect()
    }
}
