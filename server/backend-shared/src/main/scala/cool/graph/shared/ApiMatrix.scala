package cool.graph.shared

import cool.graph.shared.models.{Field, Model, Project, Relation}

object ApiMatrixFactory {
  def apply(fn: Project => DefaultApiMatrix): ApiMatrixFactory = new ApiMatrixFactory {
    override def create(project: Project) = fn(project)
  }
}

trait ApiMatrixFactory {
  def create(project: Project): DefaultApiMatrix
}

case class DefaultApiMatrix(project: Project) {
  def includeModel(modelName: String): Boolean = {
    true
  }

  def filterModels(models: List[Model]): List[Model] = {
    models.filter(model => includeModel(model.name))
  }

  def filterModel(model: Model): Option[Model] = {
    filterModels(List(model)).headOption
  }

  def includeRelation(relation: Relation): Boolean = {
    includeModel(relation.getModelA_!(project).name) && includeModel(relation.getModelB_!(project).name)
  }

  def filterRelations(relations: List[Relation]): List[Relation] = {
    relations.filter(relation => includeRelation(relation))
  }

  def filterNonRequiredRelations(relations: List[Relation]): List[Relation] = {
    relations.filter(relation => {
      val aFieldRequired = relation.getModelAField(project).exists(_.isRequired)
      val bFieldRequired = relation.getModelBField(project).exists(_.isRequired)

      !aFieldRequired && !bFieldRequired
    })
  }

  def includeField(field: Field): Boolean = {
    field.isScalar || includeModel(field.relatedModel(project).get.name)
  }

  def filterFields(fields: List[Field]): List[Field] = {
    fields.filter(includeField)
  }
}
