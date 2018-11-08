package com.prisma.subscriptions.schema

import com.prisma.shared.models.ModelMutationType
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import sangria.ast.OperationType.Subscription
import sangria.ast._
import sangria.visitor.VisitorCommand

object QueryTransformer {

  def evaluateInMemoryFilters(query: Document, mutationType: ModelMutationType, updatedFields: Set[String]): (Boolean, Document) = {
    val (mutationInMatches, query1)     = evaluateMutationInFilter(query, mutationType)
    val (updatedFieldsContains, query2) = evaluateUpdatedFieldsInFilter(query1, updatedFields)
    (mutationInMatches && updatedFieldsContains, query2)
  }

  private def evaluateMutationInFilter(query: Document, mutationType: ModelMutationType): (Boolean, Document) = {
    val mutationName = mutationType.toString
    var accumulator  = true
    val newQuery = AstVisitor.visit(
      query,
      AstVisitor {
        case ObjectField("mutation_in", EnumValue(value, _, _), _, _) =>
          val matches = mutationName == value
          accumulator = accumulator && matches
          VisitorCommand.Delete

        case ObjectField("mutation_in", ListValue(values, _, _), _, _) =>
          val matches = values.asInstanceOf[Vector[EnumValue]].exists(_.value == mutationName)
          accumulator = accumulator && matches
          VisitorCommand.Delete
      }
    )
    (accumulator, newQuery)
  }

  private def evaluateUpdatedFieldsInFilter(query: Document, updatedFields: Set[String]): (Boolean, Document) = {
    var accumulator = true
    val visitor: AstVisitor = AstVisitor {
      case ObjectField(fieldName @ ("updatedFields_contains_every" | "updatedFields_contains_some"), ListValue(values, _, _), _, _) =>
        values match {
          case (x: StringValue) +: _ =>
            val list      = values.asInstanceOf[Vector[StringValue]]
            val valuesSet = list.map(_.value).toSet

            fieldName match {
              case "updatedFields_contains_every" =>
                val containsEvery = valuesSet.subsetOf(updatedFields)
                accumulator = accumulator && containsEvery
                VisitorCommand.Delete

              case "updatedFields_contains_some" =>
                // is one of the fields in the list included in the updated fields?
                val containsSome = valuesSet.exists(updatedFields.contains)
                accumulator = accumulator && containsSome
                VisitorCommand.Delete

              case _ =>
                VisitorCommand.Continue
            }

          case _ =>
            VisitorCommand.Continue
        }

      case ObjectField("updatedFields_contains", StringValue(value, _, _, _, _), _, _) =>
        val contains = updatedFields.contains(value)
        accumulator = accumulator && contains
        VisitorCommand.Delete
    }
    val newQuery = AstVisitor.visit(
      query,
      visitor
    )

    (accumulator, newQuery)
  }

  def getModelNameFromSubscription(query: Document): Option[String] = {
    var modelName: Option[String] = None

    AstVisitor.visit(
      query,
      onEnter = (node: AstNode) => {
        node match {
          case OperationDefinition(Subscription, _, _, _, selections, _, _, _) =>
            selections match {
              case (x: Field) +: _ => modelName = Some(x.name.capitalize)
              case _               =>
            }

          case _ =>
        }
        VisitorCommand.Continue
      },
      onLeave = _ => {
        VisitorCommand.Continue
      }
    )
    modelName
  }

  def getMutationTypesFromSubscription(query: Document): Set[ModelMutationType] = {

    var mutations: Set[ModelMutationType] = Set.empty

    AstVisitor.visit(
      query,
      onEnter = (node: AstNode) => {
        node match {
          case ObjectField("mutation_in", ListValue(values, _, _), _, _) =>
            values match {
              case (x: EnumValue) +: xs =>
                val list = values.asInstanceOf[Vector[EnumValue]]
                list.foreach(mutation => {
                  mutation.value match {
                    case "CREATED" => mutations += ModelMutationType.Created
                    case "DELETED" => mutations += ModelMutationType.Deleted
                    case "UPDATED" => mutations += ModelMutationType.Updated
                  }
                })

              case _ =>
            }

          case _ =>
        }
        VisitorCommand.Continue
      },
      onLeave = (node) => {
        VisitorCommand.Continue
      }
    )

    if (mutations.isEmpty) mutations ++= Set(ModelMutationType.Created, ModelMutationType.Deleted, ModelMutationType.Updated)

    mutations
  }
}
