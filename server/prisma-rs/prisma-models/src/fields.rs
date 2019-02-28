use crate::{Field, RelationField, RelationSide, ScalarField};
use prisma_common::{error::Error, PrismaResult};
use std::{
    collections::BTreeSet,
    sync::{Arc, Weak},
};

#[derive(Debug)]
pub struct Fields {
    pub all: Vec<Field>,
    id: Weak<ScalarField>,
    scalar: Vec<Weak<ScalarField>>,
    relation: Vec<Weak<RelationField>>,
}

impl Fields {
    pub fn new(all: Vec<Field>) -> Fields {
        let scalar = all.iter().fold(Vec::new(), Self::scalar_filter);
        let relation = all.iter().fold(Vec::new(), Self::relation_filter);
        let id = Self::find_id(&all);

        Fields {
            all,
            id,
            scalar,
            relation,
        }
    }

    fn find_id(all: &[Field]) -> Weak<ScalarField> {
        all.iter()
            .fold(None, |acc, field| match field {
                Field::Scalar(sf) if sf.is_id() => Some(Arc::downgrade(sf)),
                _ => acc,
            })
            .expect("No id field defined!")
    }

    pub fn id(&self) -> Arc<ScalarField> {
        self.id.upgrade().unwrap()
    }

    pub fn scalar(&self) -> Vec<Arc<ScalarField>> {
        self.scalar
            .iter()
            .map(|field| field.upgrade().unwrap())
            .collect()
    }

    pub fn relation(&self) -> Vec<Arc<RelationField>> {
        self.relation
            .iter()
            .map(|field| field.upgrade().unwrap())
            .collect()
    }

    pub fn find_many_from_all(&self, names: &BTreeSet<String>) -> Vec<&Field> {
        self.all
            .iter()
            .filter(|field| names.contains(field.db_name().as_ref()))
            .collect()
    }

    pub fn find_many_from_scalar(&self, names: &BTreeSet<String>) -> Vec<Arc<ScalarField>> {
        self.scalar
            .iter()
            .map(|field| field.upgrade().unwrap())
            .filter(|field| names.contains(field.db_name()))
            .collect()
    }

    pub fn find_many_from_relation(&self, names: &BTreeSet<String>) -> Vec<Arc<RelationField>> {
        self.relation
            .iter()
            .map(|field| field.upgrade().unwrap())
            .filter(|field| names.contains(&field.db_name()))
            .collect()
    }

    pub fn find_from_all(&self, name: &str) -> PrismaResult<&Field> {
        self.all
            .iter()
            .find(|field| field.db_name() == name)
            .ok_or_else(|| Error::InvalidInputError(format!("Field not found: {}", name)))
    }

    pub fn find_from_scalar(&self, name: &str) -> PrismaResult<Arc<ScalarField>> {
        self.scalar()
            .iter()
            .find(|field| field.db_name() == name)
            .cloned()
            .ok_or_else(|| Error::InvalidInputError(format!("Field not found: {}", name)))
    }

    pub fn find_from_relation(&self, name: &str) -> PrismaResult<Arc<RelationField>> {
        self.relation()
            .iter()
            .find(|field| field.db_name() == name)
            .cloned()
            .ok_or_else(|| Error::InvalidInputError(format!("Field not found: {}", name)))
    }

    fn scalar_filter(mut acc: Vec<Weak<ScalarField>>, field: &Field) -> Vec<Weak<ScalarField>> {
        if let Field::Scalar(scalar_field) = field {
            acc.push(Arc::downgrade(scalar_field));
        };

        acc
    }

    fn relation_filter<'a>(
        mut acc: Vec<Weak<RelationField>>,
        field: &'a Field,
    ) -> Vec<Weak<RelationField>> {
        if let Field::Relation(relation_field) = field {
            acc.push(Arc::downgrade(relation_field));
        };

        acc
    }
}
