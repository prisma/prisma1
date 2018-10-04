package com.prisma.subscriptions.schema

import com.prisma.shared.models.ModelMutationType.ModelMutationType
import play.api.libs.json._

object VariablesTransformer {
  def evaluateInMemoryFilters(
      variables: JsValue,
      mutation: ModelMutationType,
      updatedFields: Set[String]
  ): (Boolean, JsValue) = {
    val mutationName = mutation.toString
    val isUpdate     = mutationName == "UPDATED"

    def collectInMemoryFilters(variables: JsObject): Boolean = {
      val values: Seq[Boolean] = variables.fields.collect {
        case ("mutation_in", JsString(action))                                       => action == mutationName
        case ("mutation_in", JsArray(values))                                        => values.map(_.as[JsString].value).contains(mutationName)
        case ("updatedFields_contains", JsString(field)) if isUpdate                 => updatedFields.contains(field)
        case ("updatedFields_contains_every", JsArray(values)) if isUpdate           => values.map(_.as[JsString].value).toSet.subsetOf(updatedFields)
        case ("updatedFields_contains_some", JsArray(values)) if isUpdate            => values.map(_.as[JsString].value).exists(updatedFields.contains)
        case (_, obj: JsObject)                                                      => collectInMemoryFilters(obj)
        case (_, JsArray(eles)) if eles.nonEmpty && eles.head.isInstanceOf[JsObject] => eles.map(v => collectInMemoryFilters(v.as[JsObject])).forall(_ == true)
      }
      values.forall(_ == true)
    }

    def removeInMemoryFilters(json: JsValue): Option[JsObject] = {
      val variables = json.as[JsObject]
      val obj = JsObject(
        variables.value
          .flatMap({
            case ("mutation_in", _)                                                        => None
            case ("updatedFields_contains", _)                                             => None
            case ("updatedFields_contains_every", _)                                       => None
            case ("updatedFields_contains_some", _)                                        => None
            case (key, obj: JsObject)                                                      => removeInMemoryFilters(obj).map(key -> _)
            case (key, JsArray(eles)) if eles.nonEmpty && eles.head.isInstanceOf[JsObject] => Some(key -> JsArray(eles.flatMap(removeInMemoryFilters)))
            case x                                                                         => Some(x)
          })
          .filter {
            case (_, obj: JsObject) => obj.fields.nonEmpty // filter out empty objects
            case _                  => true
          }
      )

      if (obj.fields.isEmpty) None else Some(obj)
    }

    val matches      = collectInMemoryFilters(variables.as[JsObject])
    val newVariables = removeInMemoryFilters(variables)
    (matches, newVariables.getOrElse(JsObject.empty))
  }

}
