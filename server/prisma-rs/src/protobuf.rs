mod envelope;
mod input;
mod interface;
mod query_arguments;

pub mod prelude;

use crate::models::{ModelRef, ProjectRef};
pub use envelope::ProtoBufEnvelope;
pub use input::*;
pub use interface::{ExternalInterface, ProtoBufInterface};
use prisma_query::ast::*;

pub mod prisma {
    include!(concat!(env!("OUT_DIR"), "/prisma.rs"));
}

use crate::{Error as CrateError, PrismaValue};
use prelude::*;

impl RpcResponse {
    pub fn header() -> Header {
        Header {
            type_name: String::from("RpcResponse"),
        }
    }

    pub fn empty() -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Result(prisma::Result { value: None })),
        }
    }

    pub fn ok(result: NodesResult) -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Result(prisma::Result {
                value: Some(result::Value::NodesResult(result)),
            })),
        }
    }

    pub fn error(error: CrateError) -> RpcResponse {
        RpcResponse {
            header: Self::header(),
            response: Some(rpc::Response::Error(ProtoError {
                value: Some(error.into()),
            })),
        }
    }
}

impl SelectedField {
    pub fn is_scalar(&self) -> bool {
        match self.field {
            Some(selected_field::Field::Scalar(_)) => true,
            _ => false,
        }
    }

    pub fn is_relational(&self) -> bool {
        match self.field {
            Some(selected_field::Field::Relational { .. }) => true,
            _ => false,
        }
    }
}

impl Node {
    pub fn get(&self, index: usize) -> Option<&PrismaValue> {
        self.values
            .get(index)
            .and_then(|ref vc| vc.prisma_value.as_ref())
    }

    pub fn len(&self) -> usize {
        self.values.len()
    }
}

impl ValueContainer {
    pub fn is_null_value(&self) -> bool {
        match self.prisma_value {
            Some(PrismaValue::Null(_)) => true,
            _ => false,
        }
    }
}

impl RelationFilter {
    pub fn as_condition_tree(self, project: &ProjectRef, model: ModelRef) -> ConditionTree {
        let alias = "Alias"; // todo define constants somewhere
        let model_id_col = Column::from((alias, model.db_name()));
        let condition = relation_filter::Condition::from_i32(self.condition).unwrap();

        // inStatementForRelationCondition(
        //   jooqField = modelIdColumn(alias, relationFilter.field.model),
        //   condition = relationFilter.condition,
        //   subSelect = relationFilterSubSelect(alias, relationFilter)
        // )

        let relation_field = self.field.field;
        let relation_field = model.fields().find_from_relation(&relation_field);
        // let relation = project.schema.relation.... todo
        let alias = format!("{}_{}", "todo", alias); // todo need related model logic, see RelationField relatedModel_!
        let invert_condition_of_subselect = match condition {
            relation_filter::Condition::EveryRelatedNode => true,
            _ => false,
        };

        match self.nested_filter.type_ {
            Some(filter::Type::Relation(nested)) => {
                // let nested_condition =
                unimplemented!()
            }
            _ => unimplemented!(),
        }

        unimplemented!()
    }

    fn inStatementForRelationCondition(
        column: Column,
        condition: relation_filter::Condition,
        sub_select: Select,
    ) -> Expression {
        match condition {
            relation_filter::Condition::EveryRelatedNode => column.in_selection(sub_select),
            relation_filter::Condition::NoRelatedNode => true,
            relation_filter::Condition::AtLeastOneRelatedNode => true,
            relation_filter::Condition::ToOneRelatedNode => true,
        };

        unimplemented!()
    }

    // private def inStatementForRelationCondition(jooqField: Field[AnyRef], condition: RelationCondition, subSelect: SelectConditionStep[_]) = {
    //     condition match {
    //     case EveryRelatedNode      => jooqField.notIn(subSelect)
    //     case NoRelatedNode         => jooqField.notIn(subSelect)
    //     case AtLeastOneRelatedNode => jooqField.in(subSelect)
    //     case ToOneRelatedNode      => jooqField.in(subSelect)
    //     }
    // }
}

//   private def relationFilterSubSelect(alias: String, relationFilter: RelationFilter): SelectConditionStep[Record1[AnyRef]] = {
//     // this skips intermediate tables when there is no condition on them. so the following will not join with the album table but join the artist-album relation with the album-track relation
//     // artists(where:{albums_some:{tracks_some:{condition}}})
//     //
//     // the following query contains an implicit andFilter around the two nested ones and will not be improved at the moment
//     // albums(where: {Tracks_some:{ MediaType:{Name_starts_with:""}, Genre:{Name_starts_with:""}}})
//     // the same is true for explicit AND, OR, NOT with more than one nested relationfilter. they do not profit from skipping intermediate tables at the moment
//     // these cases could be improved as well at the price of higher code complexity

//     val relationField              = relationFilter.field
//     val relation                   = relationField.relation
//     val newAlias                   = relationField.relatedModel_!.dbName + "_" + alias
//     val invertConditionOfSubSelect = relationFilter.condition == EveryRelatedNode

//     relationFilter.nestedFilter match {
//       case nested: RelationFilter =>
//         val condition = inStatementForRelationCondition(
//           jooqField = relationColumn(relation, relationField.oppositeRelationSide),
//           condition = nested.condition,
//           subSelect = relationFilterSubSelect(newAlias, nested)
//         )
//         sql
//           .select(relationColumn(relation, relationField.relationSide))
//           .from(relationTable(relation))
//           .where(condition.invert(invertConditionOfSubSelect))

//       case nested =>
//         val condition = buildConditionForFilter(nested, newAlias)
//         sql
//           .select(relationColumn(relation, relationField.relationSide))
//           .from(relationTable(relation))
//           .innerJoin(modelTable(relationField.relatedModel_!).as(newAlias))
//           .on(modelIdColumn(newAlias, relationField.relatedModel_!).eq(relationColumn(relation, relationField.oppositeRelationSide)))
//           .where(condition.invert(invertConditionOfSubSelect))
//     }
//   }
