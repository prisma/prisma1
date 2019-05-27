extern crate datamodel;

use datamodel::{dml, source::SourceDefinition};

pub trait FieldAsserts {
    fn assert_base_type(&self, t: &dml::ScalarType) -> &Self;
    fn assert_enum_type(&self, en: &str) -> &Self;
    fn assert_relation_to(&self, t: &str) -> &Self;
    fn assert_relation_to_field(&self, t: &str) -> &Self;
    fn assert_arity(&self, arity: &dml::FieldArity) -> &Self;
    fn assert_with_db_name(&self, t: &str) -> &Self;
    fn assert_default_value(&self, t: dml::Value) -> &Self;
}

pub trait ModelAsserts {
    fn assert_has_field(&self, t: &str) -> &dml::Field;
    fn assert_is_embedded(&self, t: bool) -> &Self;
    fn assert_with_db_name(&self, t: &str) -> &Self;
}

pub trait EnumAsserts {
    fn assert_has_value(&self, t: &str) -> &Self;
}

pub trait SchemaAsserts {
    fn assert_has_model(&self, t: &str) -> &dml::Model;
    fn assert_has_enum(&self, t: &str) -> &dml::Enum;
}

impl FieldAsserts for dml::Field {
    fn assert_base_type(&self, t: &dml::ScalarType) -> &Self {
        if let dml::FieldType::Base(base_type) = &self.field_type {
            assert_eq!(base_type, t);
        } else {
            panic!("Scalar expected, but found {:?}", self.field_type);
        }

        return self;
    }

    fn assert_enum_type(&self, en: &str) -> &Self {
        if let dml::FieldType::Enum(enum_type) = &self.field_type {
            assert_eq!(enum_type, en);
        } else {
            panic!("Enum expected, but found {:?}", self.field_type);
        }

        return self;
    }

    fn assert_relation_to(&self, t: &str) -> &Self {
        if let dml::FieldType::Relation(info) = &self.field_type {
            assert_eq!(info.to, t);
        } else {
            panic!("Relation expected, but found {:?}", self.field_type);
        }

        return self;
    }

    fn assert_relation_to_field(&self, t: &str) -> &Self {
        if let dml::FieldType::Relation(info) = &self.field_type {
            assert_eq!(info.to_field.clone().expect("To field expected but not found."), t);
        } else {
            panic!("Relation expected, but found {:?}", self.field_type);
        }

        return self;
    }

    fn assert_arity(&self, arity: &dml::FieldArity) -> &Self {
        assert_eq!(self.arity, *arity);

        return self;
    }

    fn assert_with_db_name(&self, t: &str) -> &Self {
        assert_eq!(self.database_name, Some(String::from(t)));

        return self;
    }

    fn assert_default_value(&self, t: dml::Value) -> &Self {
        assert_eq!(self.default_value, Some(t));

        return self;
    }
}

impl SchemaAsserts for dml::Datamodel {
    fn assert_has_model(&self, t: &str) -> &dml::Model {
        self.find_model(&String::from(t))
            .expect(format!("Model {} not found", t).as_str())
    }
    fn assert_has_enum(&self, t: &str) -> &dml::Enum {
        self.find_enum(&String::from(t))
            .expect(format!("Enum {} not found", t).as_str())
    }
}

impl ModelAsserts for dml::Model {
    fn assert_has_field(&self, t: &str) -> &dml::Field {
        self.find_field(&String::from(t))
            .expect(format!("Field {} not found", t).as_str())
    }
    fn assert_is_embedded(&self, t: bool) -> &Self {
        assert_eq!(self.is_embedded, t);

        return self;
    }
    fn assert_with_db_name(&self, t: &str) -> &Self {
        assert_eq!(self.database_name, Some(String::from(t)));

        return self;
    }
}

impl EnumAsserts for dml::Enum {
    fn assert_has_value(&self, t: &str) -> &Self {
        let pred = String::from(t);
        self.values
            .iter()
            .find(|x| **x == pred)
            .expect(format!("Field {} not found", t).as_str());

        return self;
    }
}

#[allow(dead_code)] // Not sure why the compiler thinks this is never used.
pub fn parse(datamodel_string: &str) -> datamodel::Datamodel {
    parse_with_plugins(datamodel_string, vec![])
}

pub fn parse_with_plugins(datamodel_string: &str, source_definitions: Vec<Box<SourceDefinition>>) -> datamodel::Datamodel {
    match datamodel::parse_with_plugins(datamodel_string, source_definitions) {
        Ok(s) => s,
        Err(errs) => {
            for err in errs.to_iter() {
                err.pretty_print(&mut std::io::stderr().lock(), "", datamodel_string)
                    .unwrap();
            }
            panic!("Datamodel parsing failed. Please see error above.")
        }
    }
}
