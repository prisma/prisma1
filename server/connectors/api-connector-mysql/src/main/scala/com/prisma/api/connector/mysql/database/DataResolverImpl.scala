package com.prisma.api.connector.mysql.database

import com.prisma.api.connector.Types.DataItemFilterCollection
import com.prisma.api.connector._
import com.prisma.api.connector.mysql.Metrics
import com.prisma.api.connector.mysql.database.HelperTypes.ScalarListElement
import com.prisma.gc_values._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import slick.dbio.Effect.Read
import slick.dbio.NoStream
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.{MySQLProfile, SQLActionBuilder}
import slick.lifted.TableQuery
import slick.sql.{SqlAction, SqlStreamingAction}

import scala.collection.immutable.Seq
import scala.concurrent.Future

case class DataResolverImpl(
    project: Project,
    readonlyClientDatabase: MySQLProfile.backend.DatabaseDef
) extends DataResolver {
  import DatabaseQueryBuilder.{GetDataItem, GetScalarListValue}

  import scala.concurrent.ExecutionContext.Implicits.global

  protected def performWithTiming[A](name: String, f: => Future[A]): Future[A] = {
    Metrics.sqlQueryTimer.timeFuture(project.id, name) {
      f
    }
  }

  override def resolveByModel(model: Model, args: Option[QueryArguments] = None): Future[ResolverResult] = {
    val (query, resultTransform) = DatabaseQueryBuilder.selectAllFromTable(project.id, model.name, args)

    performWithTiming("resolveByModel", readonlyClientDatabase.run(readOnlyDataItem(query)))
      .map(_.toList.map(mapDataItem(model)(_)))
      .map(resultTransform(_))
  }

  override def countByModel(model: Model, where: DataItemFilterCollection): Future[Int] = countByModel(model, Some(where))

  override def countByModel(model: Model, where: Option[DataItemFilterCollection] = None): Future[Int] = {
    val query = DatabaseQueryBuilder.countAllFromModel(project, model, where)
    performWithTiming("countByModel", readonlyClientDatabase.run(readOnlyInt(query))).map(_.head)
  }

  override def resolveByUnique(where: NodeSelector): Future[Option[DataItem]] = {
    where.fieldValue match {
      case JsonGCValue(x) => batchResolveByUnique(where.model, where.field.name, List(where.fieldValueAsString)).map(_.headOption)
      case _              => batchResolveByUnique(where.model, where.field.name, List(where.unwrappedFieldValue)).map(_.headOption)
    }
  }

  override def loadModelRowsForExport(model: Model, args: Option[QueryArguments] = None): Future[ResolverResultNew[PrismaNode]] = {
    val query = DatabaseQueryBuilder.selectAllFromTableNew(project.id, model, args)
    performWithTiming("loadModelRowsForExport", readonlyClientDatabase.run(query))
  }

  override def loadListRowsForExport(model: Model, field: Field, args: Option[QueryArguments] = None): Future[ResolverResultNew[ScalarListValues]] = {
    import QueryArgumentsExtensions._
    val query                                = DatabaseQueryBuilder.selectAllFromListTable(project.id, model, field, args, None)
    val x: Future[Vector[ScalarListElement]] = performWithTiming("loadListRowsForExport", readonlyClientDatabase.run(query))
    x.map { scalarListElements =>
      val withoutExtraItem = args.get.dropExtraLimitItem(scalarListElements)

      val grouped: Map[Id, Vector[ScalarListElement]] = withoutExtraItem.groupBy(_.nodeId)
      val result = grouped.map {
        case (id, values) =>
          val gcValues = values.sortBy(_.position).map(_.value)
          ScalarListValues(id, ListGCValue(gcValues))
      }.toVector
      ResolverResultNew(result, hasNextPage = args.get.hasNext(scalarListElements.size), hasPreviousPage = args.get.hasPrevious(scalarListElements.size))
    }
  }

  override def loadRelationRowsForExport(relationId: String, args: Option[QueryArguments] = None): Future[ResolverResultNew[RelationNode]] = {
    val query = DatabaseQueryBuilder.selectAllFromRelationTable(project.id, relationId, args)
    performWithTiming("loadRelationRowsForExport", readonlyClientDatabase.run(query))
  }

  override def batchResolveByUnique(model: Model, key: String, values: List[Any]): Future[List[DataItem]] = {
    val query = DatabaseQueryBuilder.batchSelectFromModelByUnique(project.id, model.name, key, values)
    performWithTiming("batchResolveByUnique", readonlyClientDatabase.run(readOnlyDataItem(query))).map(_.toList).map(_.map(mapDataItem(model)))
  }

  override def batchResolveScalarList(model: Model, field: Field, nodeIds: Vector[String]): Future[Vector[ScalarListValues]] = {
    val query = DatabaseQueryBuilder.selectFromScalarList(project.id, model.name, field, nodeIds)
    val x     = performWithTiming("batchResolveScalarList", readonlyClientDatabase.run(query))

    x.map { scalarListElements =>
      val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
      grouped.map {
        case (id, values) =>
          val gcValues = values.sortBy(_.position).map(_.value)
          ScalarListValues(id, ListGCValue(gcValues))
      }.toVector
    }
  }

  override def resolveByGlobalId(globalId: String): Future[Option[DataItem]] = {
    if (globalId == "viewer-fixed") {
      return Future.successful(Some(DataItem(globalId, Map(), Some("Viewer"))))
    }

    val query: SqlAction[Option[String], NoStream, Read] = TableQuery(new ProjectRelayIdTable(_, project.id))
      .filter(_.id === globalId)
      .map(_.stableModelIdentifier)
      .take(1)
      .result
      .headOption

    readonlyClientDatabase
      .run(query)
      .flatMap {
        case Some(stableModelIdentifier) =>
          val model = project.schema.getModelByStableIdentifier_!(stableModelIdentifier.trim)
          resolveByUnique(NodeSelector.forId(model, globalId)).map(_.map(mapDataItem(model)))

        case _ =>
          Future.successful(None)
      }
  }

  override def resolveByRelationManyModels(fromField: Field, fromModelIds: List[String], args: Option[QueryArguments]): Future[Seq[ResolverResult]] = {
    val (query, resultTransform) = DatabaseQueryBuilder.batchSelectAllFromRelatedModel(project, fromField, fromModelIds, args)

    performWithTiming(
      "resolveByRelation",
      readonlyClientDatabase
        .run(readOnlyDataItem(query))
        .map(_.toList.map(mapDataItem(fromField.relatedModel(project.schema).get)))
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

  override def countByRelationManyModels(fromField: Field, fromNodeIds: List[String], args: Option[QueryArguments]): Future[List[(String, Int)]] = {
    val (query, _) = DatabaseQueryBuilder.countAllFromRelatedModels(project, fromField, fromNodeIds, args)
    performWithTiming("countByRelation", readonlyClientDatabase.run(readOnlyStringInt(query)).map(_.toList))
  }

  // note: Explicitly mark queries generated from raw sql as readonly to make aurora endpoint selection work
  // see also http://danielwestheide.com/blog/2015/06/28/put-your-writes-where-your-master-is-compile-time-restriction-of-slick-effect-types.html
  private def readOnlyDataItem(query: SQLActionBuilder): SqlStreamingAction[Vector[DataItem], DataItem, Read] = query.as[DataItem]

  private def readOnlyInt(query: SQLActionBuilder): SqlStreamingAction[Vector[Int], Int, Read] = query.as[Int]

  private def readOnlyStringInt(query: SQLActionBuilder): SqlStreamingAction[Vector[(String, Int)], (String, Int), Read] = query.as[(String, Int)]

  protected def mapDataItem(model: Model)(dataItem: DataItem): DataItem = mapDataItemHelper(model, dataItem)

  protected def mapScalarListValueWithoutValidation(model: Model, field: Field)(scalarListValue: ScalarListValue): ScalarListValue = {
    mapScalarListValueHelper(model, field, scalarListValue, validate = false)
  }

  private def mapDataItemHelper(model: Model, dataItem: DataItem, validate: Boolean = true): DataItem = {

    def isType(fieldName: String, typeIdentifier: TypeIdentifier) = model.fields.exists(f => f.name == fieldName && f.typeIdentifier == typeIdentifier)
    def isList(fieldName: String)                                 = model.fields.exists(f => f.name == fieldName && f.isList)

    val res = dataItem.copy(
      userData = dataItem.userData.map {
        case (f, Some(value: java.math.BigDecimal)) if isType(f, TypeIdentifier.Float) && !isList(f) =>
          (f, Some(value.doubleValue()))

        case (f, Some(value: String)) if isType(f, TypeIdentifier.Json) && !isList(f) =>
          DataResolverValidations(f, Some(value), model, validate).validateSingleJson(value)

        case (f, v) if isType(f, TypeIdentifier.Boolean) && !isList(f) =>
          DataResolverValidations(f, v, model, validate).validateSingleBoolean

        case (f, v) if isType(f, TypeIdentifier.Enum) && !isList(f) =>
          DataResolverValidations(f, v, model, validate).validateSingleEnum

        case (f, v) if isType(f, TypeIdentifier.Enum) =>
          DataResolverValidations(f, v, model, validate).validateListEnum

        case (f, v) =>
          (f, v)
      },
      typeName = Some(model.name)
    )

    res
  }

  private def mapScalarListValueHelper(model: Model, field: Field, listValue: ScalarListValue, validate: Boolean = true): ScalarListValue = {
    // Todo handle Json, it already seems to break earlier when casting the queryresult to a Vector

    val value = listValue.value match {
      case v: java.math.BigDecimal if field.typeIdentifier == TypeIdentifier.Float && field.isList =>
        v.doubleValue()

      case v: java.sql.Timestamp if field.typeIdentifier == TypeIdentifier.DateTime && field.isList =>
        DateTime.parse(v.toString, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").withZoneUTC())

      case v =>
        v
    }

    listValue.copy(value = value)
  }

  //  private def readOnlyScalarListValue(query: SQLActionBuilder): SqlStreamingAction[Vector[ScalarListValue], Any, Read] = query.as[ScalarListValue]

  //  override def existsByModelAndId(model: Model, id: String): Future[Boolean] = {
//    val query = DatabaseQueryBuilder.existsByModelAndId(project.id, model.name, id)
//    performWithTiming("existsByModelAndId", readonlyClientDatabase.run(readOnlyBoolean(query))).map(_.head)
//  }
//
//  override def existsByWhere(where: NodeSelector): Future[Boolean] = {
//    val query = DatabaseQueryBuilder.existsByWhere(project.id, where)
//    performWithTiming("existsByWhere", readonlyClientDatabase.run(readOnlyBoolean(query))).map(_.head)
//  }
//
//  override def existsByModel(model: Model): Future[Boolean] = {
//    val query = DatabaseQueryBuilder.existsByModel(project.id, model.name)
//    performWithTiming("existsByModel", readonlyClientDatabase.run(readOnlyBoolean(query))).map(_.head)
//  }

//  override def resolveByUniques(model: Model, uniques: Vector[NodeSelector]): Future[Vector[DataItem]] = {
//    val query = DatabaseQueryBuilder.selectFromModelsByUniques(project, model, uniques)
//    readonlyClientDatabase.run(readOnlyDataItem(query)).map(_.map(mapDataItem(model)))
//  }
//
//  override def resolveByUniqueWithoutValidation(model: Model, key: String, value: Any): Future[Option[DataItem]] = {
//    batchResolveByUniqueWithoutValidation(model, key, List(value)).map(_.headOption)
//  }

//  override def batchResolveByUniqueWithoutValidation(model: Model, key: String, values: List[Any]): Future[List[DataItem]] = {
//    val query = DatabaseQueryBuilder.batchSelectFromModelByUnique(project.id, model.name, key, values)
//    performWithTiming("batchResolveByUnique", readonlyClientDatabase.run(readOnlyDataItem(query))).map(_.toList).map(_.map(mapDataItemWithoutValidation(model)))
//  }
//  override def resolveRelation(relationId: String, aId: String, bId: String): Future[ResolverResult] = {
//    val (query, resultTransform) = DatabaseQueryBuilder.selectAllFromTable(
//      projectId = project.id,
//      tableName = relationId,
//      args = Some(QueryArguments(None, None, None, None, None, Some(List(FilterElement("A", aId), FilterElement("B", bId))), None))
//    )
//
//    performWithTiming("resolveRelation", readonlyClientDatabase.run(readOnlyDataItem(query)).map(_.toList).map(resultTransform))
//  }
//
//  override def resolveByRelation(fromField: Field, fromModelId: String, args: Option[QueryArguments]): Future[ResolverResult] = {
//    val (query, resultTransform) = DatabaseQueryBuilder.batchSelectAllFromRelatedModel(project, fromField, List(fromModelId), args)
//
//    performWithTiming(
//      "resolveByRelation",
//      readonlyClientDatabase
//        .run(readOnlyDataItem(query))
//        .map(_.toList.map(mapDataItem(fromField.relatedModel(project.schema).get)))
//        .map(resultTransform)
//    )
//  }

//  override def resolveByModelAndId(model: Model, id: Id): Future[Option[DataItem]] =
//    resolveByUnique(NodeSelector(model, model.getFieldByName_!("id"), GraphQLIdGCValue(id)))
//  override def resolveByModelAndIdWithoutValidation(model: Model, id: Id): Future[Option[DataItem]] = resolveByUniqueWithoutValidation(model, "id", id)

//  override def itemCountForModel(model: Model): Future[Int] = {
//    val query = DatabaseQueryBuilder.itemCountForTable(project.id, model.name)
//    performWithTiming("itemCountForModel", readonlyClientDatabase.run(readOnlyInt(query)).map(_.head))
//  }
//
//  override def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean] = {
//    val query = DatabaseQueryBuilder.existsNullByModelAndScalarField(project.id, model.name, field.name)
//    performWithTiming("existsNullByModelAndScalarField", readonlyClientDatabase.run(readOnlyBoolean(query)).map(_.head))
//  }
//
//  override def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean] = {
//    val query = DatabaseQueryBuilder.existsNullByModelAndRelationField(project.id, model.name, field)
//    performWithTiming("existsNullByModelAndRelationField", readonlyClientDatabase.run(readOnlyBoolean(query)).map(_.head))
//  }
//
//  override def itemCountForRelation(relation: Relation): Future[Int] = {
//    val query = DatabaseQueryBuilder.itemCountForTable(project.id, relation.id)
//    performWithTiming("itemCountForRelation", readonlyClientDatabase.run(readOnlyInt(query))).map(_.head)
//  }
//
//  override def itemCountsForAllModels(project: Project): Future[ModelCounts] = {
//    val x: Seq[Future[(Model, Int)]] = project.models.map { model =>
//      itemCountForModel(model).map { count =>
//        model -> count
//      }
//    }
//    Future.sequence(x).map(counts => ModelCounts(counts.toMap))
//  }

//  private def readOnlyBoolean(query: SQLActionBuilder): SqlStreamingAction[Vector[Boolean], Boolean, Read] = query.as[Boolean]

//  protected def mapDataItemWithoutValidation(model: Model)(dataItem: DataItem): DataItem = mapDataItemHelper(model, dataItem, validate = false)
//  private def unwrapGcValue(value: Any): Any = {
//    value match {
//      case x: GCValue => GCValueExtractor.fromGCValue(x)
//      case x          => x
//    }
//  }
}
