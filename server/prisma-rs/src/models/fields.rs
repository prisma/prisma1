use crate::{
    error::Error,
    models::{Field, RelationField, Renameable, ScalarField},
    PrismaResult,
};
use std::collections::BTreeSet;

#[derive(Debug)]
pub struct Fields {
    pub all: Vec<Field>,
}

impl Fields {
    pub fn new(all: Vec<Field>) -> Fields {
        Fields { all }
    }

    pub fn scalar(&self) -> Vec<&ScalarField> {
        self.all.iter().fold(Vec::new(), Self::scalar_filter)
    }

    pub fn find_many_from_all(&self, names: &BTreeSet<String>) -> Vec<&Field> {
        self.all
            .iter()
            .filter(|field| names.contains(field.db_name()))
            .collect()
    }

    pub fn find_many_from_scalar(&self, names: &BTreeSet<String>) -> Vec<&ScalarField> {
        self.all
            .iter()
            .filter(|field| names.contains(field.db_name()))
            .fold(Vec::new(), Self::scalar_filter)
    }

    pub fn find_many_from_relation(&self, names: &BTreeSet<String>) -> Vec<&RelationField> {
        self.all
            .iter()
            .filter(|field| names.contains(field.db_name()))
            .fold(Vec::new(), Self::relation_filter)
    }

    pub fn find_from_all(&self, name: &str) -> PrismaResult<&Field> {
        self.all
            .iter()
            .find(|field| field.db_name() == name)
            .ok_or_else(|| Error::InvalidInputError(format!("Field not found: {}", name)))
    }

    pub fn find_from_scalar(&self, name: &str) -> PrismaResult<&ScalarField> {
        self.all
            .iter()
            .find(|field| field.db_name() == name)
            .and_then(|field| {
                if let Field::Scalar(scalar_field) = field {
                    Some(scalar_field)
                } else {
                    None
                }
            })
            .ok_or_else(|| Error::InvalidInputError(format!("Field not found: {}", name)))
    }

    pub fn find_from_relation(&self, name: &str) -> PrismaResult<&RelationField> {
        self.all
            .iter()
            .find(|field| field.db_name() == name)
            .and_then(|field| {
                if let Field::Relation(relation_field) = field {
                    Some(relation_field)
                } else {
                    None
                }
            })
            .ok_or_else(|| Error::InvalidInputError(format!("Field not found: {}", name)))
    }

    fn scalar_filter<'a>(mut acc: Vec<&'a ScalarField>, field: &'a Field) -> Vec<&'a ScalarField> {
        if let Field::Scalar(scalar_field) = field {
            acc.push(scalar_field);
        };

        acc
    }

    fn relation_filter<'a>(
        mut acc: Vec<&'a RelationField>,
        field: &'a Field,
    ) -> Vec<&'a RelationField> {
        if let Field::Relation(relation_field) = field {
            acc.push(relation_field);
        };

        acc
    }
}
