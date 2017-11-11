package cool.graph.util

import cool.graph.client.mutactions.AddDataItemToManyRelation
import cool.graph.shared.models.{Field, Model}

object PrettyStrings {
  implicit class PrettyAddDataItemToManyRelation(rel: AddDataItemToManyRelation) {
    def pretty: String = {
      s"${rel.fromModel.name}.${rel.fromField.name} from id [${rel.fromId}] to id [${rel.toId}]"
    }
  }

  implicit class PrettyModel(model: Model) {
    def prettyFields: String = {
      model.fields.foldLeft(s"fields of model ${model.name}") { (acc, field) =>
        acc + "\n" + field.pretty
      }
    }
  }

  implicit class PrettyField(field: Field) {
    def pretty: String = {
      s"${field.name} isScalar:${field.isScalar} isList:${field.isList} isRelation:${field.isRelation} isRequired:${field.isRequired}"
    }
  }
}
