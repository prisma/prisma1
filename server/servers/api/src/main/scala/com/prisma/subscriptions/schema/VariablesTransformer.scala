package com.prisma.subscriptions.schema

import com.prisma.shared.models.ModelMutationType.ModelMutationType
import play.api.libs.json._

object VariablesTransformer {
  def transformVariables(variables: JsValue, mutation: ModelMutationType, updatedFields: Option[List[String]]): JsValue = {
    val mutationName = mutation.toString

    def mutationIn(input: JsObject): JsObject = {
      JsObject(input.fields.map {
        case ("mutation_in", JsString(action))                                                 => ("boolean", JsBoolean(action == mutationName))
        case ("mutation_in", JsArray(values))                                                  => ("boolean", JsBoolean(values.map(_.as[JsString].value).contains(mutationName)))
        case (key: String, obj: JsObject)                                                      => (key, mutationIn(obj))
        case (key: String, JsArray(eles)) if eles.nonEmpty && eles.head.isInstanceOf[JsObject] => (key, JsArray(eles.map(v => mutationIn(v.as[JsObject]))))
        case (key: String, arr: JsArray)                                                       => (key, arr)
        case (key: String, leafJsValue)                                                        => (key, leafJsValue)
      })
    }

    mutationName == "UPDATED" match {
      case false =>
        mutationIn(variables.as[JsObject])

      case true =>
        val removedMutationIn = mutationIn(variables.as[JsObject])
        val fieldSet          = updatedFields.get.toSet

        def removedUpdatedFields(input: JsObject): JsObject = {
          JsObject(input.fields.map {
            case ("updatedFields_contains", JsString(field))       => ("boolean", JsBoolean(fieldSet.contains(field)))
            case ("updatedFields_contains_every", JsArray(values)) => ("boolean", JsBoolean(values.map(_.as[JsString].value).toSet.subsetOf(fieldSet)))
            case ("updatedFields_contains_some", JsArray(values))  => ("boolean", JsBoolean(values.map(_.as[JsString].value).exists(fieldSet.contains)))
            case ("mutation_in", JsString(action))                 => ("boolean", JsBoolean(action == mutationName))
            case ("mutation_in", JsArray(values))                  => ("boolean", JsBoolean(values.map(_.as[JsString].value).contains(mutationName)))
            case (key: String, obj: JsObject)                      => (key, removedUpdatedFields(obj))
            case (key: String, JsArray(eles)) if eles.nonEmpty && eles.head.isInstanceOf[JsObject] =>
              (key, JsArray(eles.map(v => removedUpdatedFields(v.as[JsObject]))))
            case (key: String, arr: JsArray) => (key, arr)
            case (key: String, leafJsValue)  => (key, leafJsValue)
          })
        }

        removedUpdatedFields(removedMutationIn)
    }

  }

}
