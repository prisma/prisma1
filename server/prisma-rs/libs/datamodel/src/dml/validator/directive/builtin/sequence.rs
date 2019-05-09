use crate::dml;
use crate::dml::validator::directive::{Args, Error, DirectiveValidator};

pub struct SequenceDirectiveValidator { }

impl<Types: dml::TypePack> DirectiveValidator<dml::Field<Types>, Types> for SequenceDirectiveValidator {
    fn directive_name(&self) -> &'static str{ &"sequence" }
    fn validate_and_apply(&self, args: &Args, obj: &mut dml::Field<Types>) -> Option<Error> {

        // TODO: Handle fields according to tests: 
        // https://github.com/prisma/prisma/blob/master/server/servers/deploy/src/test/scala/com/prisma/deploy/migration/validation/SequenceDirectiveSpec.scala
        
        let mut seq = dml::Sequence {
            name: "".to_string(),
            allocation_size: 0,
            initial_value: 0
        };

        match args.arg("name").as_str() {
            Ok(name) => seq.name = name,
            Err(err) => return Some(err)
        }

        match args.arg("allocationSize").as_int() {
            Ok(allocation_size) => seq.allocation_size = allocation_size,
            Err(err) => return Some(err)
        }

        match args.arg("initialValie").as_int() {
            Ok(initial_value) => seq.initial_value = initial_value,
            Err(err) => return Some(err)
        }

        return None
    }
}