package cool.graph.subscriptions.schemas

import cool.graph.shared.models.{Model, ModelMutationType, Project}
import org.scalactic.{Bad, Good, Or}
import sangria.ast.Document
import sangria.parser.QueryParser
import sangria.validation.QueryValidator
import scaldi.Injector

import scala.util.{Failure, Success}

case class SubscriptionQueryError(errorMessage: String)

case class SubscriptionQueryValidator(project: Project)(implicit inj: Injector) {

  def validate(query: String): Model Or Seq[SubscriptionQueryError] = {
    queryDocument(query).flatMap(validate)
  }

  def validate(queryDoc: Document): Model Or Seq[SubscriptionQueryError] = {
    for {
      modelName <- modelName(queryDoc)
      model     <- modelFor(modelName)
      _         <- validateSubscriptionQuery(queryDoc, model)
    } yield model
  }

  def queryDocument(query: String): Document Or Seq[SubscriptionQueryError] = QueryParser.parse(query) match {
    case Success(doc) => Good(doc)
    case Failure(_)   => Bad(Seq(SubscriptionQueryError("The subscription query is invalid GraphQL.")))
  }

  def modelName(queryDoc: Document): String Or Seq[SubscriptionQueryError] =
    QueryTransformer.getModelNameFromSubscription(queryDoc) match {
      case Some(modelName) => Good(modelName)
      case None =>
        Bad(Seq(SubscriptionQueryError("The provided query doesn't include any known model name. Please check for the latest subscriptions API.")))
    }

  def modelFor(model: String): Model Or Seq[SubscriptionQueryError] = project.getModelByName(model) match {
    case Some(model) => Good(model)
    case None        => Bad(Seq(SubscriptionQueryError("The provided query doesn't include any known model name. Please check for the latest subscriptions API.")))
  }

  def validateSubscriptionQuery(queryDoc: Document, model: Model): Unit Or Seq[SubscriptionQueryError] = {
    val schema     = SubscriptionSchema(model, project, None, ModelMutationType.Created, None, true).build
    val violations = QueryValidator.default.validateQuery(schema, queryDoc)
    if (violations.nonEmpty) {
      Bad(violations.map(v => SubscriptionQueryError(v.errorMessage)))
    } else Good(())
  }
}
