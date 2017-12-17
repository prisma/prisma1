package cool.graph.api.database

import cool.graph.api.ApiDependencies
import cool.graph.api.database.DatabaseQueryBuilder._
import cool.graph.api.mutations.NodeSelector
import cool.graph.api.schema.APIErrors
import cool.graph.gc_values.GCValue
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models._
import cool.graph.util.gc_value.GCDBValueConverter
import slick.dbio.Effect.Read
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.{MySQLProfile, SQLActionBuilder}
import slick.lifted.TableQuery
import slick.sql.{SqlAction, SqlStreamingAction}
import spray.json._

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DataResolver(project: Project, useMasterDatabaseOnly: Boolean = false)(implicit apiDependencies: ApiDependencies) {

  def masterClientDatabase: MySQLProfile.backend.DatabaseDef = apiDependencies.databases.master
  def readonlyClientDatabase: MySQLProfile.backend.DatabaseDef =
    if (useMasterDatabaseOnly) apiDependencies.databases.master
    else apiDependencies.databases.readOnly

  protected def performWithTiming[A](name: String, f: => Future[A]): Future[A] = {
    f
    //    val begin = System.currentTimeMillis()
    //    sqlQueryTimer.time(project.id, name) {
    //      f andThen {
    //        case x =>
    //          requestContext.foreach(_.logSqlTiming(Timing(name, System.currentTimeMillis() - begin)))
    //          x
    //      }
    //    }
  }

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
    batchResolveByUnique(model, key, List(unwrapGcValue(value))).map(_.headOption)
  }

  def resolveByUniques(model: Model, uniques: Vector[NodeSelector]): Future[Vector[DataItem]] = {
    val query = DatabaseQueryBuilder.selectFromModelsByUniques(project, model, uniques)
    readonlyClientDatabase.run(readOnlyDataItem(query)).map(_.map(mapDataItem(model)))
  }

  def resolveByUniqueWithoutValidation(model: Model, key: String, value: Any): Future[Option[DataItem]] = {
    batchResolveByUniqueWithoutValidation(model, key, List(value)).map(_.headOption)
  }

  def loadModelRowsForExport(model: Model, args: Option[QueryArguments] = None): Future[ResolverResult] = {
    val (query, resultTransform) = DatabaseQueryBuilder.selectAllFromModel(project.id, model.name, args, overrideMaxNodeCount = Some(1001))

    performWithTiming("loadModelRowsForExport", readonlyClientDatabase.run(readOnlyDataItem(query)))
      .map(_.toList.map(mapDataItem(model)(_)))
      .map(resultTransform(_))
  }

  def loadRelationRowsForExport(relationId: String, args: Option[QueryArguments] = None): Future[ResolverResult] = {
    val (query, resultTransform) = DatabaseQueryBuilder.selectAllFromModel(project.id, relationId, args, overrideMaxNodeCount = Some(1001))

    performWithTiming("loadRelationRowsForExport", readonlyClientDatabase.run(readOnlyDataItem(query))).map(_.toList).map(resultTransform(_))
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
          val model = project.getModelById_!(modelId.trim)
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

  def resolveByModelAndId(model: Model, id: Id): Future[Option[DataItem]]                  = resolveByUnique(model, "id", id)
  def resolveByModelAndIdWithoutValidation(model: Model, id: Id): Future[Option[DataItem]] = resolveByUniqueWithoutValidation(model, "id", id)

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

  def itemCountsForAllModels(project: Project): Future[ModelCounts] = {
    val x: Seq[Future[(Model, Int)]] = project.models.map { model =>
      itemCountForModel(model).map { count =>
        model -> count
      }
    }
    Future.sequence(x).map(counts => ModelCounts(counts.toMap))
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

  def runOnClientDatabase[A](name: String, sqlAction: DBIOAction[A, NoStream, Effect.All]): Future[A] =
    performWithTiming(name, masterClientDatabase.run(sqlAction))

  protected def mapDataItem(model: Model)(dataItem: DataItem): DataItem = {
    mapDataItemHelper(model, dataItem)
  }
  protected def mapDataItemWithoutValidation(model: Model)(dataItem: DataItem): DataItem = {
    mapDataItemHelper(model, dataItem, validate = false)
  }

  private def mapDataItemHelper(model: Model, dataItem: DataItem, validate: Boolean = true): DataItem = {

    def isType(fieldName: String, typeIdentifier: TypeIdentifier) = model.fields.exists(f => f.name == fieldName && f.typeIdentifier == typeIdentifier)
    def isList(fieldName: String)                                 = model.fields.exists(f => f.name == fieldName && f.isList)

    val res = dataItem.copy(userData = dataItem.userData.map {
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
    })

    res
  }

  private def unwrapGcValue(value: Any): Any = {
    value match {
      case x: GCValue => GCDBValueConverter().fromGCValue(x)
      case x          => x
    }
  }
}

case class ModelCounts(countsMap: Map[Model, Int]) {
  def countForName(name: String): Int = {
    val model = countsMap.keySet.find(_.name == name).getOrElse(sys.error(s"No count found for model $name"))
    countsMap(model)
  }
}

case class ResolverResult(items: Seq[DataItem], hasNextPage: Boolean = false, hasPreviousPage: Boolean = false, parentModelId: Option[String] = None)

case class DataResolverValidations(f: String, v: Option[Any], model: Model, validate: Boolean) {

  private val field: Field = model.getFieldByName_!(f)

  private def enumOnFieldContainsValue(field: Field, value: Any): Boolean = {
    val enum = field.enum.getOrElse(sys.error("Field should have an Enum"))
    enum.values.contains(value)
  }

  def validateSingleJson(value: String) = {
    def parseJson = Try(value.parseJson) match {
      case Success(json) ⇒ Some(json)
      case Failure(_)    ⇒ if (validate) throw APIErrors.ValueNotAValidJson(f, value) else None
    }
    (f, parseJson)
  }

  def validateSingleBoolean = {
    (f, v.map {
      case v: Boolean => v
      case v: Integer => v == 1
      case v: String  => v.toBoolean
    })
  }

  def validateSingleEnum = {
    val validatedEnum = v match {
      case Some(value) if enumOnFieldContainsValue(field, value) => Some(value)
      case Some(_)                                               => if (validate) throw APIErrors.StoredValueForFieldNotValid(field.name, model.name) else None
      case _                                                     => None
    }
    (f, validatedEnum)
  }

  def validateListEnum = {
    def enumListValueValid(input: Any): Boolean = {
      val inputWithoutWhitespace = input.asInstanceOf[String].replaceAll(" ", "")

      inputWithoutWhitespace match {
        case "[]" =>
          true

        case _ =>
          val values        = inputWithoutWhitespace.stripPrefix("[").stripSuffix("]").split(",")
          val invalidValues = values.collect { case value if !enumOnFieldContainsValue(field, value.stripPrefix("\"").stripSuffix("\"")) => value }
          invalidValues.isEmpty
      }
    }

    val validatedEnumList = v match {
      case Some(x) if enumListValueValid(x) => Some(x)
      case Some(_)                          => if (validate) throw APIErrors.StoredValueForFieldNotValid(field.name, model.name) else None
      case _                                => None
    }
    (f, validatedEnumList)
  }
}
