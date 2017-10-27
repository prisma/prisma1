package cool.graph.system.schema.types

import cool.graph.shared.models

object PermissionQueryArguments {

  case class PermissionQueryArgument(group: String, name: String, typeIdentifier: models.TypeIdentifier.TypeIdentifier)

  private def defaultArguments = {
    List(
      PermissionQueryArgument("Authenticated User", "$user_id", models.TypeIdentifier.GraphQLID),
      PermissionQueryArgument("Current Node", "$node_id", models.TypeIdentifier.GraphQLID)
    )
  }

  def getCreateArguments(model: models.Model) = {
    val scalarPermissionQueryArgs = model.scalarFields
      .filter(_.name != "id")
      .map(scalarField => PermissionQueryArgument("Scalar Values", s"$$input_${scalarField.name}", scalarField.typeIdentifier))

    val singleRelationPermissionQueryArgs = model.singleRelationFields.map(singleRelationField =>
      PermissionQueryArgument("Relations", s"$$input_${singleRelationField.name}Id", models.TypeIdentifier.GraphQLID))

    scalarPermissionQueryArgs ++ singleRelationPermissionQueryArgs ++ defaultArguments
  }

  def getUpdateArguments(model: models.Model) = {
    val scalarPermissionQueryArgs = model.scalarFields
      .filter(_.name != "id")
      .map(scalarField => PermissionQueryArgument("Scalar Values", s"$$input_${scalarField.name}", scalarField.typeIdentifier))

    val singleRelationPermissionQueryArgs = model.singleRelationFields.map(singleRelationField =>
      PermissionQueryArgument("Relations", s"$$input_${singleRelationField.name}Id", models.TypeIdentifier.GraphQLID))

    val oldScalarPermissionQueryArgs = model.scalarFields
      .filter(_.name != "id")
      .map(scalarField => PermissionQueryArgument("Existing Scalar Values", s"$$node_${scalarField.name}", scalarField.typeIdentifier))

    scalarPermissionQueryArgs ++ oldScalarPermissionQueryArgs ++ singleRelationPermissionQueryArgs ++ defaultArguments
  }

  def getDeleteArguments(model: models.Model) = {
    val scalarPermissionQueryArgs = model.scalarFields
      .filter(_.name != "id")
      .map(scalarField => PermissionQueryArgument("Scalar Values", s"$$node_${scalarField.name}", scalarField.typeIdentifier))

    scalarPermissionQueryArgs ++ defaultArguments
  }

  def getReadArguments(model: models.Model) = {

    val scalarPermissionQueryArgs = model.scalarFields
      .filter(_.name != "id")
      .map(scalarField => PermissionQueryArgument("Scalar Values", s"$$node_${scalarField.name}", scalarField.typeIdentifier))

    scalarPermissionQueryArgs ++ defaultArguments
  }

  def getRelationArguments(relation: models.Relation, project: models.Project) = {

    List(
      PermissionQueryArgument("Authenticated User", "$user_id", models.TypeIdentifier.GraphQLID),
      PermissionQueryArgument("Relation", s"$$${relation.aName(project)}_id", models.TypeIdentifier.GraphQLID),
      PermissionQueryArgument("Relation", s"$$${relation.bName(project)}_id", models.TypeIdentifier.GraphQLID)
    )

  }
}
