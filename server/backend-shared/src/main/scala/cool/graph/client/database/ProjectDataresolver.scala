package cool.graph.client.database

import cool.graph.client.database.DatabaseQueryBuilder._
import cool.graph.shared.models._
import cool.graph.{DataItem, FilterElement, RequestContextTrait}
import scaldi._
import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.SQLActionBuilder
import slick.lifted.TableQuery
import slick.sql.{SqlAction, SqlStreamingAction}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProjectDataresolver(override val project: Project, override val requestContext: Option[RequestContextTrait])(implicit inj: Injector)
    extends DataResolver(project = project, requestContext = requestContext)
    with Injectable {

  def this(project: Project, requestContext: RequestContextTrait)(implicit inj: Injector) =
    this(project, Some(requestContext))

  def resolveByModel(model: Model, args: Option[QueryArguments] = None): Future[ResolverResult] = {
    val (query, resultTransform) = DatabaseQueryBuilder.selectAllFromModel(project.id, model.name, args)

    performWithTiming("resolveByModel", readonlyClientDatabase.run(readOnlyDataItem(query)))
      .map(_.toList.map(mapDataItem(model)(_)))
      .map(resultTransform(_))
  }

  def countByModel(model: Model, args: Option[QueryArguments] = None): Future[Int] = {
    val query = DatabaseQueryBuilder.countAllFromModel(project.id, model.name, args)
    performWithTiming("countByModel", readonlyClientDatabase.run(readOnlyInt(query))).map(_.head)
  }

  def existsByModelAndId(model: Model, id: String): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsByModelAndId(project.id, model.name, id)

    performWithTiming("existsByModelAndId", readonlyClientDatabase.run(readOnlyBoolean(query))).map(_.head)
  }

  def existsByModel(model: Model): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsByModel(project.id, model.name)

    performWithTiming("existsByModel", readonlyClientDatabase.run(readOnlyBoolean(query))).map(_.head)
  }

  def resolveByUnique(model: Model, key: String, value: Any): Future[Option[DataItem]] = {
    batchResolveByUnique(model, key, List(value)).map(_.headOption)
  }

  def resolveByUniqueWithoutValidation(model: Model, key: String, value: Any): Future[Option[DataItem]] = {
    batchResolveByUniqueWithoutValidation(model, key, List(value)).map(_.headOption)
  }

  def batchResolveByUnique(model: Model, key: String, values: List[Any]): Future[List[DataItem]] = {
    val query = DatabaseQueryBuilder.batchSelectFromModelByUnique(project.id, model.name, key, values)

    performWithTiming("batchResolveByUnique", readonlyClientDatabase.run(readOnlyDataItem(query)))
      .map(_.toList)
      .map(_.map(mapDataItem(model)))
  }

  def batchResolveByUniqueWithoutValidation(model: Model, key: String, values: List[Any]): Future[List[DataItem]] = {
    val query = DatabaseQueryBuilder.batchSelectFromModelByUnique(project.id, model.name, key, values)

    performWithTiming("batchResolveByUnique", readonlyClientDatabase.run(readOnlyDataItem(query)))
      .map(_.toList)
      .map(_.map(mapDataItemWithoutValidation(model)))
  }

  def resolveByGlobalId(globalId: String): Future[Option[DataItem]] = {
    if (globalId == "viewer-fixed") {
      return Future.successful(Some(DataItem(globalId, Map(), Some("Viewer"))))
    }

    val query: SqlAction[Option[String], NoStream, Read] = TableQuery(new ProjectRelayIdTable(_, project.id))
      .filter(_.id === globalId)
      .map(_.modelId)
      .take(1)
      .result
      .headOption

    readonlyClientDatabase
      .run(query)
      .map {
        case Some(modelId) =>
          val model = project.getModelById_!(modelId)
          resolveByUnique(model, "id", globalId).map(_.map(mapDataItem(model)).map(_.copy(typeName = Some(model.name))))
        case _ => Future.successful(None)
      }
      .flatMap(identity)
  }

  def resolveRelation(relationId: String, aId: String, bId: String): Future[ResolverResult] = {
    val (query, resultTransform) = DatabaseQueryBuilder.selectAllFromModel(
      project.id,
      relationId,
      Some(QueryArguments(None, None, None, None, None, Some(List(FilterElement("A", aId), FilterElement("B", bId))), None)))

    performWithTiming("resolveRelation",
                      readonlyClientDatabase
                        .run(
                          readOnlyDataItem(query)
                        )
                        .map(_.toList)
                        .map(resultTransform))
  }

  def resolveByRelation(fromField: Field, fromModelId: String, args: Option[QueryArguments]): Future[ResolverResult] = {
    val (query, resultTransform) =
      DatabaseQueryBuilder.batchSelectAllFromRelatedModel(project, fromField, List(fromModelId), args)

    performWithTiming(
      "resolveByRelation",
      readonlyClientDatabase
        .run(readOnlyDataItem(query))
        .map(_.toList.map(mapDataItem(fromField.relatedModel(project).get)))
        .map(resultTransform)
    )
  }

  def resolveByRelationManyModels(fromField: Field, fromModelIds: List[String], args: Option[QueryArguments]): Future[Seq[ResolverResult]] = {
    val (query, resultTransform) =
      DatabaseQueryBuilder
        .batchSelectAllFromRelatedModel(project, fromField, fromModelIds, args)

    performWithTiming(
      "resolveByRelation",
      readonlyClientDatabase
        .run(readOnlyDataItem(query))
        .map(_.toList.map(mapDataItem(fromField.relatedModel(project).get)))
        .map((items: List[DataItem]) => {
          val itemGroupsByModelId = items.groupBy(item => {
            item.userData
              .get(fromField.relationSide.get.toString)
              .flatten
          })

          fromModelIds.map(id => {
            itemGroupsByModelId.find(_._1.contains(id)) match {
              case Some((_, itemsForId)) => resultTransform(itemsForId).copy(parentModelId = Some(id))
              case None                  => ResolverResult(Seq.empty, parentModelId = Some(id))
            }
          })
        })
    )
  }

  def countByRelationManyModels(fromField: Field, fromNodeIds: List[String], args: Option[QueryArguments]): Future[List[(String, Int)]] = {

    val (query, _) = DatabaseQueryBuilder.countAllFromRelatedModels(project, fromField, fromNodeIds, args)

    performWithTiming("countByRelation", readonlyClientDatabase.run(readOnlyStringInt(query)).map(_.toList))
  }

  def itemCountForModel(model: Model): Future[Int] = {
    val query = DatabaseQueryBuilder.itemCountForTable(project.id, model.name)
    performWithTiming("itemCountForModel", readonlyClientDatabase.run(readOnlyInt(query)).map(_.head))
  }

  def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsNullByModelAndScalarField(project.id, model.name, field.name)

    performWithTiming("existsNullByModelAndScalarField", readonlyClientDatabase.run(readOnlyBoolean(query)).map(_.head))
  }

  def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsNullByModelAndRelationField(project.id, model.name, field)

    performWithTiming("existsNullByModelAndRelationField", readonlyClientDatabase.run(readOnlyBoolean(query)).map(_.head))
  }

  def itemCountForRelation(relation: Relation): Future[Int] = {
    val query = DatabaseQueryBuilder.itemCountForTable(project.id, relation.id)

    performWithTiming("itemCountForRelation", readonlyClientDatabase.run(readOnlyInt(query))).map(_.head)
  }

  // note: Explicitly mark queries generated from raw sql as readonly to make aurora endpoint selection work
  // see also http://danielwestheide.com/blog/2015/06/28/put-your-writes-where-your-master-is-compile-time-restriction-of-slick-effect-types.html
  private def readOnlyDataItem(query: SQLActionBuilder): SqlStreamingAction[Vector[DataItem], DataItem, Read] = {
    val action: SqlStreamingAction[Vector[DataItem], DataItem, Read] = query.as[DataItem]

    action
  }

  private def readOnlyInt(query: SQLActionBuilder): SqlStreamingAction[Vector[Int], Int, Read] = {
    val action: SqlStreamingAction[Vector[Int], Int, Read] = query.as[Int]

    action
  }

  private def readOnlyBoolean(query: SQLActionBuilder): SqlStreamingAction[Vector[Boolean], Boolean, Read] = {
    val action: SqlStreamingAction[Vector[Boolean], Boolean, Read] = query.as[Boolean]

    action
  }

  private def readOnlyStringInt(query: SQLActionBuilder): SqlStreamingAction[Vector[(String, Int)], (String, Int), Read] = {
    val action: SqlStreamingAction[Vector[(String, Int)], (String, Int), Read] = query.as[(String, Int)]

    action
  }
}
