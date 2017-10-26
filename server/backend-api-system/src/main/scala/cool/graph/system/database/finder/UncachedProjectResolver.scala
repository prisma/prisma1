package cool.graph.system.database.finder

import cool.graph.shared.models
import cool.graph.shared.models.ProjectWithClientId
import cool.graph.system.database.tables._
import cool.graph.system.database.{AllDataForProject, DbToModelMapper}
import cool.graph.system.metrics.SystemMetrics
import cool.graph.{RequestContextTrait, Timing}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.lifted.QueryBase

import scala.concurrent.Future

object UncachedProjectResolverMetrics {
  import SystemMetrics._

  val readFromDatabaseTimer = defineTimer("readFromDatabaseTimer")
}

object UncachedProjectResolver {
  def apply(internalDatabase: DatabaseDef, requestContext: RequestContextTrait): UncachedProjectResolver = {
    UncachedProjectResolver(internalDatabase, Some(requestContext))
  }
}

case class UncachedProjectResolver(
    internalDatabase: DatabaseDef,
    requestContext: Option[RequestContextTrait] = None
) extends ProjectResolver {
  import DbQueriesForUncachedProjectResolver._
  import UncachedProjectResolverMetrics._
  import scala.concurrent.ExecutionContext.Implicits.global

  override def resolve(projectIdOrAlias: String): Future[Option[models.Project]] = resolveProjectWithClientId(projectIdOrAlias).map(_.map(_.project))

  override def resolveProjectWithClientId(projectIdOrAlias: String): Future[Option[models.ProjectWithClientId]] = {
    val project: Future[Option[Project]] = runQuery(projectQuery(projectIdOrAlias)).map(_.headOption)

    val allDataForProject: Future[Option[AllDataForProject]] = project.flatMap {
      case Some(project) => gatherAllDataForProject(project).map(Some(_))
      case None          => Future.successful(Option.empty)
    }

    val asModel: Future[Option[ProjectWithClientId]] = allDataForProject.map(_.map { allDataForProject =>
      val project = DbToModelMapper.createProject(allDataForProject)
      models.ProjectWithClientId(project, allDataForProject.project.clientId)
    })

    asModel
  }

  private def gatherAllDataForProject(project: Project): Future[AllDataForProject] = performWithTiming("resolveProjectWithClientId.gatherAllDataForProject") {
    readFromDatabaseTimer.timeFuture() {
      for {
        _ <- Future.successful(())
        // execute all queries in parallel
        fieldsFuture              = runQuery(fieldsForProjectQuery(project.id))
        rootsFuture               = runQuery(patQuery(project.id))
        actionsFuture             = runQuery(actionQuery(project.id))
        seatsFuture               = runQuery(seatQuery(project.id))
        packageDefinitionsFuture  = runQuery(packageDefinitionQuery(project.id))
        enumsFuture               = runQuery(enumQuery(project.id))
        featureTogglesFuture      = runQuery(featureTogglesQuery(project.id))
        functionsFuture           = runQuery(functionsQuery(project.id))
        projectDatabaseFuture     = runQuery(projectDatabasesQuery(project.projectDatabaseId)).map(_.head)
        fieldConstraintsFuture    = runQuery(fieldConstraintsQuery(project.id))
        modelsFuture              = runQuery(modelsForProjectQuery(project.id))
        relationsAndMirrorsFuture = runQuery(relationAndFieldMirrorQuery(project.id))
        integrationsFuture        = runQuery(integrationQuery(project.id))

        // gather the first results we need for the next queries
        models              <- modelsFuture
        relationsAndMirrors <- relationsAndMirrorsFuture
        integrations        <- integrationsFuture

        // trigger next queries in parallel
        modelIds                  = models.map(_.id)
        relationIds               = relationsAndMirrors.map(_._1.id).distinct
        integrationIds            = integrations.map(_.id).toList
        modelPermissionsFuture    = runQuery(modelAndPermissionQuery(modelIds))
        relationPermissionsFuture = runQuery(relationPermissionQuery(relationIds))
        auth0IntegrationsFuture   = runQuery(auth0IntegrationQuery(integrationIds))
        digitsIntegrationsFuture  = runQuery(digitsIntegrationQuery(integrationIds))
        algoliaIntegrationsFuture = runQuery(algoliaIntegrationQuery(integrationIds))

        // then gather all results
        fields              <- fieldsFuture
        roots               <- rootsFuture
        actions             <- actionsFuture
        seats               <- seatsFuture
        packageDefinitions  <- packageDefinitionsFuture
        enums               <- enumsFuture
        featureToggles      <- featureTogglesFuture
        functions           <- functionsFuture
        projectDatabase     <- projectDatabaseFuture
        fieldConstraints    <- fieldConstraintsFuture
        modelPermissions    <- modelPermissionsFuture
        relationPermissions <- relationPermissionsFuture
        auth0Integrations   <- auth0IntegrationsFuture
        digitsIntegrations  <- digitsIntegrationsFuture
        algoliaIntegrations <- algoliaIntegrationsFuture

      } yield {
        AllDataForProject(
          project = project,
          models = models,
          fields = fields,
          relations = relationsAndMirrors.map(_._1).distinct,
          relationFieldMirrors = relationsAndMirrors.flatMap(_._2).distinct,
          rootTokens = roots.distinct,
          actions = actions.map(_._1).distinct,
          actionTriggerMutationModels = actions.flatMap(_._2).distinct,
          actionTriggerMutationRelations = actions.flatMap(_._3).distinct,
          actionHandlerWebhooks = actions.flatMap(_._4).distinct,
          integrations = integrations,
          modelPermissions = modelPermissions.map(_._1).distinct,
          modelPermissionFields = modelPermissions.flatMap(_._2).distinct,
          relationPermissions = relationPermissions,
          auth0s = auth0Integrations,
          digits = digitsIntegrations,
          algolias = algoliaIntegrations.map(_._1).distinct,
          algoliaSyncQueries = algoliaIntegrations.flatMap(_._2),
          seats = seats.distinct,
          packageDefinitions = packageDefinitions.distinct,
          enums = enums.distinct,
          featureToggles = featureToggles.toList,
          functions = functions.toList,
          fieldConstraints = fieldConstraints,
          projectDatabase = projectDatabase
        )
      }
    }
  }

  private def runQuery[T](query: QueryBase[T]): Future[T] = internalDatabase.run(query.result)

  private def performWithTiming[A](name: String)(f: => Future[A]): Future[A] = {
    val begin  = System.currentTimeMillis()
    val result = f
    result onComplete { _ =>
      val timing = Timing(name, System.currentTimeMillis() - begin)
      requestContext.foreach(_.logTimingWithoutCloudwatch(timing, _.RequestMetricsSql))
    }
    result
  }
}

object DbQueriesForUncachedProjectResolver {
  import Tables._

  def projectQuery(projectIdOrAlias: String): Query[ProjectTable, Project, Seq] = {
    val query = for {
      project <- Projects if project.id === projectIdOrAlias || project.alias === projectIdOrAlias
    } yield project
    query.take(1)
  }

  def modelsForProjectQuery(projectId: String): Query[ModelTable, Model, Seq] = {
    for {
      model <- Models if model.projectId === projectId
    } yield model
  }

  def fieldsForProjectQuery(projectId: String): Query[FieldTable, Field, Seq] = {
    for {
      model <- modelsForProjectQuery(projectId)
      field <- Fields if field.modelId === model.id
    } yield field
  }

  def relationAndFieldMirrorQuery(projectId: String): QueryBase[Seq[(Relation, Option[RelationFieldMirror])]] = {
    for {
      ((r: RelationTable), frm) <- Relations joinLeft RelationFieldMirrors on (_.id === _.relationId)
      if r.projectId === projectId
    } yield (r, frm)
  }

  def patQuery(projectId: String): QueryBase[Seq[RootToken]] = {
    for {
      pat <- RootTokens if pat.projectId === projectId
    } yield pat
  }

  def actionQuery(
      projectId: String): QueryBase[Seq[(Action, Option[ActionTriggerMutationModel], Option[ActionTriggerMutationRelation], Option[ActionHandlerWebhook])]] = {
    for {
      ((((a: ActionTable), atmm), atrm), atwh) <- Actions joinLeft ActionTriggerMutationModels on (_.id === _.actionId) joinLeft ActionTriggerMutationRelations on (_._1.id === _.actionId) joinLeft ActionHandlerWebhooks on (_._1._1.id === _.actionId)
      if a.projectId === projectId
    } yield (a, atmm, atrm, atwh)
  }

  def integrationQuery(projectId: String): QueryBase[Seq[Integration]] = {
    for {
      integration <- Integrations
      if integration.projectId === projectId
    } yield integration
  }

  def modelAndPermissionQuery(modelIds: Seq[String]): QueryBase[Seq[(ModelPermission, Option[ModelPermissionField])]] = {
    for {
      ((mp: ModelPermissionTable), mpf) <- ModelPermissions joinLeft ModelPermissionFields on (_.id === _.modelPermissionId)
      if mp.modelId.inSet(modelIds)
    } yield (mp, mpf)
  }

  def auth0IntegrationQuery(integrationIds: Seq[String]): QueryBase[Seq[IntegrationAuth0]] = {
    for {
      a <- IntegrationAuth0s if a.integrationId.inSet(integrationIds)
    } yield a
  }

  def digitsIntegrationQuery(integrationIds: Seq[String]): QueryBase[Seq[IntegrationDigits]] = {
    for {
      d <- IntegrationDigits if d.integrationId.inSet(integrationIds)
    } yield d
  }

  def algoliaIntegrationQuery(integrationIds: Seq[String]): QueryBase[Seq[(SearchProviderAlgolia, Option[AlgoliaSyncQuery])]] = {
    for {
      ((a: SearchProviderAlgoliaTable), as) <- SearchProviderAlgolias joinLeft AlgoliaSyncQueries on (_.id === _.searchProviderAlgoliaId)
      if a.integrationId.inSet(integrationIds)
    } yield (a, as)
  }

  def seatQuery(projectId: String): QueryBase[Seq[(Seat, Option[Client])]] = {
    for {
      (s: SeatTable, c) <- Seats joinLeft Clients on (_.clientId === _.id)
      if s.projectId === projectId
    } yield (s, c)
  }

  def fieldConstraintsQuery(projectId: String): QueryBase[Seq[FieldConstraint]] = {
    for {
      field      <- fieldsForProjectQuery(projectId)
      constraint <- FieldConstraints if constraint.fieldId === field.id
    } yield constraint
  }

  def relationPermissionQuery(relationIds: Seq[String]): QueryBase[Seq[RelationPermission]] = {
    for {
      relationPermission <- RelationPermissions if relationPermission.relationId.inSet(relationIds)
    } yield relationPermission
  }

  def packageDefinitionQuery(projectId: String): QueryBase[Seq[PackageDefinition]] = {
    for {
      packageDefinition <- PackageDefinitions if packageDefinition.projectId === projectId
    } yield packageDefinition
  }

  def enumQuery(projectId: String): QueryBase[Seq[Enum]] = {
    for {
      enum <- Enums
      if enum.projectId === projectId
    } yield enum
  }

  def featureTogglesQuery(projectId: String): QueryBase[Seq[FeatureToggle]] = {
    for {
      featureToggle <- FeatureToggles
      if featureToggle.projectId === projectId
    } yield featureToggle
  }

  def functionsQuery(projectId: String): QueryBase[Seq[Function]] = {
    for {
      function <- Functions
      if function.projectId === projectId
    } yield function
  }

  def projectDatabasesQuery(projectDatabaseId: String): QueryBase[Seq[ProjectDatabase]] = {
    for {
      projectDatabase <- ProjectDatabases
      if projectDatabase.id === projectDatabaseId
    } yield projectDatabase
  }
}
