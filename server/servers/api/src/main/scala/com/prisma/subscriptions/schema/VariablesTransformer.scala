package com.prisma.subscriptions.schema

import com.prisma.shared.models.ModelMutationType
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import play.api.libs.json._

object VariablesTransformer {
  def transformVariables(variables: JsValue, mutation: ModelMutationType, updatedFields: Set[String]): JsValue = {
    val mutationName = mutation match {
      case ModelMutationType.Created => "CREATED"
      case ModelMutationType.Updated => "UPDATED"
      case ModelMutationType.Deleted => "DELETED"
    }

    def convert(input: JsObject): JsObject = {
      JsObject(input.fields.map {
        case ("updatedFields_contains", JsString(field))                                       => ("boolean", JsBoolean(updatedFields.contains(field)))
        case ("updatedFields_contains_every", JsArray(values))                                 => ("boolean", JsBoolean(values.map(_.as[JsString].value).toSet.subsetOf(updatedFields)))
        case ("updatedFields_contains_some", JsArray(values))                                  => ("boolean", JsBoolean(values.map(_.as[JsString].value).exists(updatedFields.contains)))
        case ("mutation_in", JsString(action))                                                 => ("boolean", JsBoolean(action == mutationName))
        case ("mutation_in", JsArray(values))                                                  => ("boolean", JsBoolean(values.map(_.as[JsString].value).contains(mutationName)))
        case (key: String, obj: JsObject)                                                      => (key, convert(obj))
        case (key: String, JsArray(eles)) if eles.nonEmpty && eles.head.isInstanceOf[JsObject] => (key, JsArray(eles.map(v => convert(v.as[JsObject]))))
        case (key: String, arr: JsArray)                                                       => (key, arr)
        case (key: String, leafJsValue)                                                        => (key, leafJsValue)
      })
    }

    convert(variables.as[JsObject])
  }

}
