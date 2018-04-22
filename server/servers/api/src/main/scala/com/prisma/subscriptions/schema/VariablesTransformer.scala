package com.prisma.subscriptions.schema

import com.prisma.shared.models.ModelMutationType.ModelMutationType
import play.api.libs.json._

object VariablesTransformer {
  def transformVariables(variables: JsValue, mutation: ModelMutationType, updatedFields: Option[List[String]]): JsValue = {
    val mutationName  = mutation.toString
    val isUpdate      = mutationName == "UPDATED"
    lazy val fieldSet = updatedFields.get.toSet

    def removeUpdated(input: JsObject): JsObject = {
      JsObject(input.fields.map {
        case ("mutation_in", JsString(action))                                         => ("boolean", JsBoolean(action == mutationName))
        case ("mutation_in", JsArray(values))                                          => ("boolean", JsBoolean(values.map(_.as[JsString].value).contains(mutationName)))
        case ("updatedFields_contains", JsString(field)) if isUpdate                   => ("boolean", JsBoolean(fieldSet.contains(field)))
        case ("updatedFields_contains_every", JsArray(values)) if isUpdate             => ("boolean", JsBoolean(values.map(_.as[JsString].value).toSet.subsetOf(fieldSet)))
        case ("updatedFields_contains_some", JsArray(values)) if isUpdate              => ("boolean", JsBoolean(values.map(_.as[JsString].value).exists(fieldSet.contains)))
        case (key, obj: JsObject)                                                      => (key, removeUpdated(obj))
        case (key, JsArray(eles)) if eles.nonEmpty && eles.head.isInstanceOf[JsObject] => (key, JsArray(eles.map(v => removeUpdated(v.as[JsObject]))))
        case (key, arr: JsArray)                                                       => (key, arr)
        case (key, leafJsValue)                                                        => (key, leafJsValue)
      })
    }

    removeUpdated(variables.as[JsObject])
  }

}
