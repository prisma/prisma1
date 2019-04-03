use crate::{ModelRef, Relation, RelationField, ScalarField, TypeIdentifier};
use once_cell::unsync::OnceCell;
use prisma_query::ast::Column;
use std::sync::Arc;

pub trait IntoSelectedFields {
    fn into_selected_fields(self, model: ModelRef, from_field: Option<Arc<RelationField>>) -> SelectedFields;
}

#[derive(Debug, Default, Clone)]
pub struct SelectedFields {
    pub scalar: Vec<SelectedScalarField>,
    pub relation: Vec<SelectedRelationField>,

    /// FIXME: naming
    pub from_field: Option<Arc<RelationField>>,
    columns: OnceCell<Vec<Column>>,
}

#[derive(Debug, Clone)]
pub enum SelectedField {
    Scalar(SelectedScalarField),
    Relation(SelectedRelationField),
}

#[derive(Debug, Clone)]
pub struct SelectedScalarField {
    pub field: Arc<ScalarField>,

    /// Denotes whether or not a field was selected implicitly,
    /// meaning it needs to be selected from the database, but not shown in the actual result set.
    pub implicit: bool,
}

#[derive(Debug, Clone)]
pub struct SelectedRelationField {
    pub field: Arc<RelationField>,
    pub selected_fields: SelectedFields,
}

impl From<Arc<ScalarField>> for SelectedField {
    fn from(sf: Arc<ScalarField>) -> SelectedField {
        SelectedField::Scalar(SelectedScalarField {
            field: sf,
            implicit: false,
        })
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

    pub fn get_implicit_fields(&self) -> Vec<&SelectedScalarField> {
        self.scalar.iter().filter(|sf| sf.implicit).collect()
    }

    pub fn add_scalar(&mut self, field: Arc<ScalarField>, implicit: bool) {
        self.columns = OnceCell::new();
        self.scalar.push(SelectedScalarField { field, implicit });
    }

    pub fn columns(&self) -> &[Column] {
        self.columns
            .get_or_init(|| {
                let mut result: Vec<Column> = self.scalar_non_list().iter().map(|f| f.as_column()).collect();

                for rf in self.relation_inlined().iter() {
                    result.push(rf.as_column());
                }

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

    pub fn names(&self) -> Vec<String> {
        self.columns().iter().map(|c| c.name.clone()).collect()
    }

    pub fn type_identifiers(&self) -> Vec<TypeIdentifier> {
        let mut result: Vec<TypeIdentifier> = self.scalar_non_list().iter().map(|sf| sf.type_identifier).collect();

        for rf in self.relation_inlined().iter() {
            result.push(rf.type_identifier);
        }

        result
    }

    pub fn model(&self) -> ModelRef {
        let field = self
            .scalar
            .first()
            .expect("Expected at least one scalar field to be present");

        field.field.model()
    }

    fn relation_inlined(&self) -> Vec<Arc<RelationField>> {
        self.relation
            .iter()
            .filter(|rf| rf.field.relation_is_inlined_in_parent())
            .map(|sf| sf.field.clone())
            .collect()
    }

    fn scalar_non_list(&self) -> Vec<Arc<ScalarField>> {
        self.scalar
            .iter()
            .filter(|sf| !sf.field.is_list)
            .map(|sf| sf.field.clone())
            .collect()
    }
}
