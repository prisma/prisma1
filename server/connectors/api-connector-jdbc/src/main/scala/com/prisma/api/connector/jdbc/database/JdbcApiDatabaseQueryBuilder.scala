package com.prisma.api.connector.jdbc.database

import java.sql.ResultSet

import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Function => _, _}
import slick.jdbc._
import scala.language.existentials

import scala.concurrent.ExecutionContext

case class JdbcApiDatabaseQueryBuilder(
    project: Project,
    schemaName: String,
    slickDatabase: SlickDatabase
)(implicit ec: ExecutionContext)
    extends BuilderBase {

  import com.prisma.slick.NewJdbcExtensions._
  import slickDatabase.profile.api._

  val relayIdTableQuery = {
    val bla = RelayIdTableWrapper(slickDatabase.profile)
    TableQuery(new bla.SlickTable(_, project.id))
  }

  def selectByGlobalId(idGCValue: IdGCValue): DBIO[Option[PrismaNode]] = {
    val modelNameForId: DBIO[Option[String]] = relayIdTableQuery
      .filter(_.id === idGCValue.value.toString)
      .map(_.stableModelIdentifier)
      .take(1)
      .result
      .headOption

    for {
      stableModelIdentifier <- modelNameForId
      result <- stableModelIdentifier match {
                 case Some(stableModelIdentifier) =>
                   val model = project.schema.getModelByStableIdentifier_!(stableModelIdentifier.trim)
                   selectById(model, idGCValue)
                 case None =>
                   DBIO.successful(None)
               }
    } yield result
  }

  def selectById(model: Model, idGcValue: IdGCValue): DBIO[Option[PrismaNode]] = {
    batchSelectFromModelByUnique(model, model.idField_!, Vector(idGcValue)).map(_.headOption)
  }

  def selectAllFromTable(
      model: Model,
      args: Option[QueryArguments],
      overrideMaxNodeCount: Option[Int] = None
  ): DBIO[ResolverResult[PrismaNode]] = {
    SimpleDBIO[ResolverResult[PrismaNode]] { ctx =>
      val jooqBuilder = JooqModelQueryBuilder(slickDatabase, schemaName, model, args)
      val ps          = ctx.connection.prepareStatement(jooqBuilder.queryString)
      SetParams.setQueryArgs(ps, args)
      val rs: ResultSet              = ps.executeQuery()
      val result: Vector[PrismaNode] = rs.as[PrismaNode](readsPrismaNode(model))
      ResolverResult(args, result)
    }
  }

  def batchSelectAllFromRelatedModel(
      schema: Schema,
      fromField: RelationField,
      fromNodeIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): DBIO[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    if (isMySql && args.exists(_.isWithPagination)) {
      selectAllFromRelatedWithPaginationForMySQL(schema, fromField, fromNodeIds, args)
    } else {
      SimpleDBIO { ctx =>
        val builder = JooqRelatedModelsQueryBuilder(slickDatabase, schemaName, fromField, args, fromNodeIds)
        val query   = if (args.exists(_.isWithPagination)) builder.queryStringWithPagination else builder.queryStringWithoutPagination

        val ps = ctx.connection.prepareStatement(query)

        // injecting params
        val pp = new PositionedParameters(ps)
        fromNodeIds.foreach(pp.setGcValue)
        val filter = args.flatMap(_.filter)
        filter.foreach(filter => SetParams.setFilter(pp, filter))

        if (args.get.after.isDefined) {
          pp.setString(args.get.after.get)
          pp.setString(args.get.after.get)
        }

        if (args.get.before.isDefined) {
          pp.setString(args.get.before.get)
          pp.setString(args.get.before.get)
        }

        if (args.exists(_.isWithPagination)) {
          val params = limitClauseForWindowFunction(args)
          pp.setInt(params._1)
          pp.setInt(params._2)
        }

        // executing
        val rs: ResultSet       = ps.executeQuery()
        val model               = fromField.relatedModel_!
        val parent              = fromField.model
        val result              = rs.as[PrismaNodeWithParent](readPrismaNodeWithParent(fromField))
        val itemGroupsByModelId = result.groupBy(_.parentId)
        fromNodeIds.map { id =>
          itemGroupsByModelId.find(_._1 == id) match {
            case Some((_, itemsForId)) => ResolverResult(args, itemsForId, parentModelId = Some(id))
            case None                  => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
          }
        }
      }
    }
  }

  def selectAllFromRelatedWithPaginationForMySQL(
      schema: Schema,
      fromField: RelationField,
      fromModelIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): DBIO[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    require(args.exists(_.isWithPagination))
    val builder = JooqRelatedModelsQueryBuilder(slickDatabase, schemaName, fromField, args, fromModelIds)

    SimpleDBIO { ctx =>
      val baseQuery        = "(" + builder.mysqlHack.getSQL + ")"
      val distinctModelIds = fromModelIds.distinct
      val queries          = Vector.fill(distinctModelIds.size)(baseQuery)
      val query            = queries.mkString(" union all ")
      println(query)

      val ps          = ctx.connection.prepareStatement(query)
      val pp          = new PositionedParameters(ps)
      val filter      = args.flatMap(_.filter)
      val limitParams = limitClause(args)

      distinctModelIds.foreach { id =>
        pp.setGcValue(id)
        filter.foreach { filter =>
          SetParams.setFilter(pp, filter)
        }
        if (args.get.after.isDefined) {
          pp.setString(args.get.after.get)
          pp.setString(args.get.after.get)
        }

        if (args.get.before.isDefined) {
          pp.setString(args.get.before.get)
          pp.setString(args.get.before.get)
        }
        if (args.exists(_.isWithPagination)) {
          limitParams.foreach { params =>
            pp.setInt(params._1)
            pp.setInt(params._2)
          }
        }
      }

      val rs                  = ps.executeQuery()
      val result              = rs.as[PrismaNodeWithParent](readPrismaNodeWithParent(fromField))
      val itemGroupsByModelId = result.groupBy(_.parentId)
      fromModelIds.map { id =>
        itemGroupsByModelId.find(_._1 == id) match {
          case Some((_, itemsForId)) => ResolverResult(args, itemsForId, parentModelId = Some(id))
          case None                  => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
        }
      }
    }
  }

  def selectAllFromRelationTable(
      relation: Relation,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[RelationNode]] = {

    SimpleDBIO[ResolverResult[RelationNode]] { ctx =>
      val builder = JooqRelationQueryBuilder(slickDatabase, schemaName, relation, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()
      val result        = rs.as(readRelation(relation))
      ResolverResult(result)
    }
  }

  def selectAllFromListTable(
      model: Model,
      field: ScalarField,
      args: Option[QueryArguments]
  ): DBIO[ResolverResult[ScalarListValues]] = {

    SimpleDBIO[ResolverResult[ScalarListValues]] { ctx =>
      val builder = JooqScalarListQueryBuilder(slickDatabase, schemaName, field, args)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setQueryArgs(ps, args)
      val rs: ResultSet = ps.executeQuery()

      val result = rs.as(readsScalarListField(field))

      val convertedValues = result
        .groupBy(_.nodeId)
        .map { case (id, values) => ScalarListValues(CuidGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }
        .toVector

      ResolverResult(convertedValues)
    }
  }

  def selectFromScalarList(modelName: String, field: ScalarField, nodeIds: Vector[IdGCValue]): DBIO[Vector[ScalarListValues]] = {
    SimpleDBIO[Vector[ScalarListValues]] { ctx =>
      val builder = JooqScalarListByUniquesQueryBuilder(slickDatabase, schemaName, field, nodeIds)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      val pp      = new PositionedParameters(ps)
      nodeIds.foreach(pp.setGcValue)

      val rs                 = ps.executeQuery()
      val scalarListElements = rs.as(readsScalarListField(field))

      val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
      grouped.map { case (id, values) => ScalarListValues(CuidGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }.toVector
    }
  }

  def countAllFromTable(table: String, whereFilter: Option[Filter]): DBIO[Int] = {
    SimpleDBIO[Int] { ctx =>
      val builder = JooqCountQueryBuilder(slickDatabase, schemaName, table, whereFilter)
      val ps      = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setFilter(new PositionedParameters(ps), whereFilter)
      val rs = ps.executeQuery()
      rs.next()
      rs.getInt(1)
    }
  }

  def batchSelectFromModelByUnique(model: Model, field: ScalarField, values: Vector[GCValue]): DBIO[Vector[PrismaNode]] = {
    SimpleDBIO { ctx =>
      val queryArgs = Some(QueryArguments.withFilter(ScalarFilter(field, In(values))))
      val builder   = JooqModelQueryBuilder(slickDatabase, schemaName, model, queryArgs)
      val ps        = ctx.connection.prepareStatement(builder.queryString)
      SetParams.setQueryArgs(ps, queryArgs)
      val rs: ResultSet = ps.executeQuery()
      rs.as(readsPrismaNode(model))
    }
  }
}
