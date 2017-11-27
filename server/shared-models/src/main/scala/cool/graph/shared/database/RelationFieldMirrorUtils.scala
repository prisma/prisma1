package cool.graph.shared.database

import cool.graph.shared.models.{Field, Project, Relation}

object RelationFieldMirrorUtils {
  def mirrorColumnName(project: Project, field: Field, relation: Relation): String = {
    val fieldModel = project.getModelByFieldId_!(field.id)
    val modelB     = relation.modelBId
    val modelA     = relation.modelAId
    fieldModel.id match {
      case `modelA` => s"A_${field.name}"
      case `modelB` => s"B_${field.name}"
    }
  }
}
