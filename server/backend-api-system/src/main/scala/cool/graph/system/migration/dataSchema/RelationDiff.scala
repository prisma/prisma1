package cool.graph.system.migration.dataSchema

import cool.graph.Types.Id
import cool.graph.shared.models.{Project, Relation}
import sangria.ast.{Document, StringValue}

object RelationDiff {
  // a schema is said to contain a relation if a @relation directive exists with correct name, or
  // a @relation with different name links the same fields
  def schemaContainsRelation(project: Project, schema: Document, relation: Relation): Boolean = {

    import DataSchemaAstExtensions._

    if (schema.containsRelation(relation.name)) {
      true
    } else {
      try {
        val leftModel = schema.objectType_!(relation.getModelA_!(project).name)
        val leftFieldRelationDirectiveName =
          leftModel
            .field_!(relation.getModelAField_!(project).name)
            .directive_!("relation")
            .argument_!("name")
            .value

        val rightModel = schema.objectType_!(relation.getModelB_!(project).name)
        val rightFieldRelationDirectiveName =
          rightModel
            .field_!(relation.getModelBField_!(project).name)
            .directive_!("relation")
            .argument_!("name")
            .value

        leftFieldRelationDirectiveName
          .asInstanceOf[StringValue]
          .value == rightFieldRelationDirectiveName.asInstanceOf[StringValue].value
      } catch {
        case e: Throwable => false
      }
    }
  }
  // project is said to contain relation if a relation with the name already exists
  // or the two fields are already linked by a relation with other name
  def projectContainsRelation(project: Project, addRelation: AddRelationAction): Boolean = {
    project.relations.exists { relation =>
      if (relation.name == addRelation.input.name) {
        true
      } else {
        try {
          val leftModelRelationId: Option[Id] = project
            .getModelById_!(addRelation.input.leftModelId)
            .getFieldByName_!(addRelation.input.fieldOnLeftModelName)
            .relation
            .map(_.id)
          val rightModelRelationId: Option[Id] = project
            .getModelById_!(addRelation.input.rightModelId)
            .getFieldByName_!(addRelation.input.fieldOnRightModelName)
            .relation
            .map(_.id)

          leftModelRelationId == rightModelRelationId
        } catch {
          case e: Throwable => false
        }
      }
    }
  }
}
