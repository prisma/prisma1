package cool.graph.client.database

import cool.graph.Types.Id
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models._
import cool.graph.{DataItem, RequestContextTrait, Timing}
import scaldi._
import slick.dbio.{DBIOAction, Effect, NoStream}
import spray.json._

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

abstract class DataResolver(val project: Project, val requestContext: Option[RequestContextTrait])(implicit inj: Injector) extends Injectable with Cloneable {

  def this(project: Project, requestContext: RequestContextTrait)(implicit inj: Injector) =
    this(project: Project, Some(requestContext))

  def copy(project: Project = project, requestContext: Option[RequestContextTrait] = requestContext): DataResolver =
    this match {
      case _: ProjectDataresolver => new ProjectDataresolver(project, requestContext)
    }

  // todo: find a better pattern for this
  private var useMasterDatabaseOnly = false
  def enableMasterDatabaseOnlyMode  = useMasterDatabaseOnly = true

  val globalDatabaseManager = inject[GlobalDatabaseManager]
  def masterClientDatabase  = globalDatabaseManager.getDbForProject(project).master
  def readonlyClientDatabase =
    if (useMasterDatabaseOnly) globalDatabaseManager.getDbForProject(project).master
    else globalDatabaseManager.getDbForProject(project).readOnly

  protected def performWithTiming[A](name: String, f: Future[A]): Future[A] = {
    val begin = System.currentTimeMillis()
    f andThen {
      case x =>
        requestContext.foreach(_.logSqlTiming(Timing(name, System.currentTimeMillis() - begin)))
        x
    }
  }
  def resolveByModel(model: Model, args: Option[QueryArguments] = None): Future[ResolverResult]

  def countByModel(model: Model, args: Option[QueryArguments] = None): Future[Int]

  def existsByModel(model: Model): Future[Boolean]

  def existsByModelAndId(model: Model, id: String): Future[Boolean]

  def resolveByUnique(model: Model, key: String, value: Any): Future[Option[DataItem]]
  def resolveByUniqueWithoutValidation(model: Model, key: String, value: Any): Future[Option[DataItem]]

  def batchResolveByUnique(model: Model, key: String, values: List[Any]): Future[List[DataItem]]

  /**
    * Resolves a DataItem by its global id. As this method has no knowledge about which model table to query it has to do an additional
    * lookup from the id to the actual model table. This is stored in the _relayId table. Therefore this needs one more lookup.
    * So if possible rather use resolveByModelAndId which does not have this cost..
    */
  def resolveByGlobalId(id: String): Future[Option[DataItem]]

  def resolveByModelAndId(model: Model, id: Id): Future[Option[DataItem]]                  = resolveByUnique(model, "id", id)
  def resolveByModelAndIdWithoutValidation(model: Model, id: Id): Future[Option[DataItem]] = resolveByUniqueWithoutValidation(model, "id", id)

  def resolveRelation(relationId: String, aId: String, bId: String): Future[ResolverResult]

  def resolveByRelation(fromField: Field, fromModelId: String, args: Option[QueryArguments]): Future[ResolverResult]

  def resolveByRelationManyModels(fromField: Field, fromModelIds: List[String], args: Option[QueryArguments]): Future[Seq[ResolverResult]]

  def countByRelationManyModels(fromField: Field, fromModelIds: List[String], args: Option[QueryArguments]): Future[List[(String, Int)]]

  def itemCountForModel(model: Model): Future[Int]

  def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean]

  def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean]

  def itemCountsForAllModels(project: Project): Future[ModelCounts] = {
    val x: Seq[Future[(Model, Int)]] = project.models.map { model =>
      itemCountForModel(model).map { count =>
        model -> count
      }
    }
    Future.sequence(x).map(counts => ModelCounts(counts.toMap))
  }

  def itemCountForRelation(relation: Relation): Future[Int]

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
      case Failure(_)    ⇒ if (validate) throw UserAPIErrors.ValueNotAValidJson(f, value) else None
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
      case Some(_)                                               => if (validate) throw UserAPIErrors.StoredValueForFieldNotValid(field.name, model.name) else None
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
      case Some(_)                          => if (validate) throw UserAPIErrors.StoredValueForFieldNotValid(field.name, model.name) else None
      case _                                => None
    }
    (f, validatedEnumList)
  }
}
