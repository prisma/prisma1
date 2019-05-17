package com.prisma.api.schema

import com.prisma.api.connector._

object Optimizations {

  trait Optimizer {
    def optimize(filter: Filter): Filter

  }

  object FilterOptimizer extends Optimizer {

    override def optimize(filter: Filter): Filter = {
      val one   = LogicalOpt.transform(filter)
      val two   = InlineOpt.transform(one)
      val three = SameRelationFilterOpt.transform(two)
      three
    }

    trait Optimization {
      def transform(filter: Filter): Filter
    }

    object LogicalOpt extends Optimization {
      override def transform(filter: Filter): Filter = {
        filter match {
          case AndFilter(filters) if filters.length == 1   => transform(filters.head)
          case AndFilter(filters)                          => AndFilter(filters.map(transform))
          case OrFilter(filters) if filters.length == 1    => transform(filters.head)
          case OrFilter(filters)                           => OrFilter(filters.map(transform))
          case NotFilter(filters)                          => NotFilter(filters.map(transform))
          case RelationFilter(rf, nestedFilter, condition) => RelationFilter(rf, transform(nestedFilter), condition)
          case x                                           => x
        }
      }
    }

    object InlineOpt extends Optimization {

      //For Mongo this could also handle rf_some{id: "id"} => ScalarListFilter(ScalarListField, ListContains("id"))
      override def transform(filter: Filter): Filter = {
        filter match {
          case AndFilter(filters) => AndFilter(filters.map(transform))
          case OrFilter(filters)  => OrFilter(filters.map(transform))
          case NotFilter(filters) => NotFilter(filters.map(transform))
          case RelationFilter(rf, ScalarFilter(sf, scalarCondition), _) if rf.relationIsInlinedInParent && !rf.isList && sf.isId =>
            ScalarFilter(rf.scalarCopy, scalarCondition)
          case RelationFilter(rf, nestedFilter, cond) => RelationFilter(rf, transform(nestedFilter), cond)
          case x                                      => x
        }
      }
    }

    object SameRelationFilterOpt extends Optimization {

      override def transform(filter: Filter): Filter = {
        filter match {
          case AndFilter(
              Vector(RelationFilter(rf1, ScalarFilter(sf1, cond1), AtLeastOneRelatedNode),
                     RelationFilter(rf2, ScalarFilter(sf2, cond2), AtLeastOneRelatedNode))) if rf1 == rf2 && sf1 == sf2 && cond1.sameAs(cond2) =>
            RelationFilter(rf1, OrFilter(Vector(ScalarFilter(sf1, cond1), ScalarFilter(sf1, cond2))), AtLeastOneRelatedNode)
          case _ => filter
        }
      }
    }

  }

}
