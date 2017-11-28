package cool.graph.api.mutations.definitions

import cool.graph.api.schema.SchemaArgument
import cool.graph.shared.models.{Model, Project, Relation}
import sangria.schema

sealed trait RelationDefinition extends ClientMutationDefinition {
  def argumentGroupName: String
  def relation: Relation
  def project: Project

  val aName = relation.aName(project) + "Id"
  val bName = relation.bName(project) + "Id"
  val scalarArgs = List(
    SchemaArgument(aName, schema.IDType, None),
    SchemaArgument(bName, schema.IDType, None)
  )

  override def getSchemaArguments(model: Model): List[SchemaArgument] = scalarArgs
}

case class AddToRelationDefinition(relation: Relation, project: Project) extends RelationDefinition {

  override val argumentGroupName = s"AddTo${relation.name}"
}

case class RemoveFromRelationDefinition(relation: Relation, project: Project) extends RelationDefinition {

  override val argumentGroupName = s"RemoveFrom${relation.name}"
}

case class SetRelationDefinition(relation: Relation, project: Project) extends RelationDefinition {

  override val argumentGroupName = s"Set${relation.name}"
}

case class UnsetRelationDefinition(relation: Relation, project: Project) extends RelationDefinition {

  override val argumentGroupName = s"Unset${relation.name}"
}
