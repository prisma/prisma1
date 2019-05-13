extern crate datamodel;

use datamodel::dml;
use datamodel::Validator;

pub trait FieldAsserts {
    fn assert_base_type(&self, t: &dml::ScalarType) -> &Self;
    fn assert_enum_type(&self, en: &str) -> &Self;
    fn assert_relation_to(&self, t: &str) -> &Self;
    fn assert_relation_to_field(&self, t: &str) -> &Self;
    fn assert_arity(&self, arity: &dml::FieldArity) -> &Self;
}

pub trait ModelAsserts<Types: dml::TypePack> {
    fn assert_has_field(&self, t: &str) -> &dml::Field<Types>;
    fn assert_is_embedded(&self, t: bool) -> &Self;
}

pub trait EnumAsserts {
    fn assert_has_value(&self, t: &str) -> &Self;
}

pub trait SchemaAsserts<Types: dml::TypePack> {
    fn assert_has_model(&self, t: &str) -> &dml::Model<Types>;
    fn assert_has_enum(&self, t: &str) -> &dml::Enum<Types>;
}

impl<Types: dml::TypePack> FieldAsserts for dml::Field<Types> {
    fn assert_base_type(&self, t: &dml::ScalarType) -> &Self {
        if let dml::FieldType::Base(base_type) = &self.field_type {
            assert_eq!(base_type, t);
        } else {
            panic!("Scalar expected, but found {:?}", self.field_type);
        }

        return self
    }

    fn assert_enum_type(&self, en: &str) -> &Self {
        if let dml::FieldType::Enum(enum_type) = &self.field_type {
            assert_eq!(enum_type, en);
        } else {
            panic!("Enum expected, but found {:?}", self.field_type);
        }

        return self
    }

    fn assert_relation_to(&self, t: &str) -> &Self {
        if let dml::FieldType::Relation(info) = &self.field_type {
            assert_eq!(info.to, t);
        } else {
            panic!("Relation expected, but found {:?}", self.field_type);
        }

        return self
    }

    fn assert_relation_to_field(&self, t: &str) -> &Self {
        if let dml::FieldType::Relation(info) = &self.field_type {
            assert_eq!(info.to_field, t);
        } else {
            panic!("Relation expected, but found {:?}", self.field_type);
        }

        return self
    }

    fn assert_arity(&self, arity: &dml::FieldArity) -> &Self {
        assert_eq!(self.arity, *arity);

        return self
    }
}

impl<Types: dml::TypePack> SchemaAsserts<Types> for dml::Schema<Types> {
    fn assert_has_model(&self, t: &str) -> &dml::Model<Types> {
        self.find_model(&String::from(t)).expect(format!("Model {} not found", t).as_str())
    }
    fn assert_has_enum(&self, t: &str) -> &dml::Enum<Types> {
        self.find_enum(&String::from(t)).expect(format!("Enum {} not found", t).as_str())
    }
}

impl<Types: dml::TypePack> ModelAsserts<Types> for dml::Model<Types> {
    fn assert_has_field(&self, t: &str) -> &dml::Field<Types> {
        self.find_field(&String::from(t)).expect(format!("Field {} not found", t).as_str())
    }
    fn assert_is_embedded(&self, t: bool) -> &Self {
        assert_eq!(self.is_embedded, t);

        return self
    }
}

impl<Types: dml::TypePack> EnumAsserts for dml::Enum<Types> {
    fn assert_has_value(&self, t: &str) -> &Self {
        let pred = String::from(t);
        self.values.iter().find(|x| **x == pred).expect(format!("Field {} not found", t).as_str());

        return self
    }
}

pub fn parse_and_validate(input: &str) -> dml::Schema<dml::BuiltinTypePack> {
    let ast = datamodel::parser::parse(&String::from(input));
    let validator = datamodel::validator::BaseValidator::<dml::BuiltinTypePack, dml::validator::EmptyAttachmentValidator>::new();
    validator.validate(&ast)
}