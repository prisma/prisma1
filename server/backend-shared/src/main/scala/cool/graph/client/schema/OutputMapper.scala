package cool.graph.client.schema

import cool.graph.DataItem
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models.{Model, Relation}
import sangria.schema.{Args, ObjectType}

abstract class OutputMapper {
  type R
  def nodePaths(model: Model): List[List[String]]
  def mapCreateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, R]

  def mapUpdateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, R]

  def mapSubscriptionOutputType[C](model: Model,
                                   objectType: ObjectType[C, DataItem],
                                   updatedFields: Option[List[String]] = None,
                                   mutation: ModelMutationType = cool.graph.shared.models.ModelMutationType.Created,
                                   previousValues: Option[DataItem] = None,
                                   dataItem: Option[R] = None): ObjectType[C, R]

  def mapUpdateOrCreateOutputType[C](model: Model, objectType: ObjectType[C, DataItem]): ObjectType[C, R]

  def mapDeleteOutputType[C](model: Model, objectType: ObjectType[C, DataItem], onlyId: Boolean = false): ObjectType[C, R]

  def mapAddToRelationOutputType[C](relation: Relation,
                                    fromModel: Model,
                                    fromField: cool.graph.shared.models.Field,
                                    toModel: Model,
                                    objectType: ObjectType[C, DataItem],
                                    payloadName: String): ObjectType[C, R]

  def mapRemoveFromRelationOutputType[C](relation: Relation,
                                         fromModel: Model,
                                         fromField: cool.graph.shared.models.Field,
                                         toModel: Model,
                                         objectType: ObjectType[C, DataItem],
                                         payloadName: String): ObjectType[C, R]

  def mapResolve(item: DataItem, args: Args): R
}
