package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector._
import com.prisma.gc_values.{GCValue, IdGCValue}
import com.prisma.shared.models.{Model, RelationField, ScalarField, Schema}
import org.jooq.{Record, SelectForUpdateStep}
import slick.jdbc.PositionedParameters

trait NodeManyQueries extends BuilderBase with FilterConditionBuilder with CursorConditionBuilder with OrderByClauseBuilder with LimitClauseBuilder {
  import slickDatabase.profile.api._

  private def modelQuery(model: Model, queryArguments: Option[QueryArguments]): SelectForUpdateStep[Record] = {

    val condition       = buildConditionForFilter(queryArguments.flatMap(_.filter))
    val cursorCondition = buildCursorCondition(queryArguments, model)
    val order           = orderByForModel(model, topLevelAlias, queryArguments)
    val limit           = limitClause(queryArguments)

    val base = sql
      .select()
      .from(modelTable(model).as(topLevelAlias))
      .where(condition, cursorCondition)
      .orderBy(order: _*)

    limit match {
      case Some(_) => base.limit(intDummy).offset(intDummy)
      case None    => base
    }
  }

  def getNodes(model: Model, args: Option[QueryArguments], overrideMaxNodeCount: Option[Int] = None): DBIO[ResolverResult[PrismaNode]] = {
    val query = modelQuery(model, args)
    queryToDBIO(query)(
      setParams = pp => SetParams.setQueryArgs(pp, args),
      readResult = { rs =>
        val result = rs.readWith(readsPrismaNode(model))
        ResolverResult(args, result)
      }
    )
  }

  def getRelatedNodes(fromField: RelationField,
                      fromNodeIds: Vector[IdGCValue],
                      args: Option[QueryArguments]): DBIO[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    if (isMySql && args.exists(_.isWithPagination)) {
      selectAllFromRelatedWithPaginationForMySQL(fromField, fromNodeIds, args)
    } else {
      val builder = RelatedModelsQueryBuilder(slickDatabase, schemaName, fromField, args, fromNodeIds)
      val query   = if (args.exists(_.isWithPagination)) builder.queryWithPagination else builder.queryWithoutPagination

      queryToDBIO(query)(
        setParams = { pp =>
          fromNodeIds.foreach(pp.setGcValue)
          args.foreach { arg =>
            arg.filter.foreach(filter => SetParams.setFilter(pp, filter))

            SetParams.setCursor(pp, arg)

            if (arg.isWithPagination) {
              val params = limitClauseForWindowFunction(args)
              pp.setInt(params._1)
              pp.setInt(params._2)
            }
          }
        },
        readResult = { rs =>
          val result              = rs.readWith(readPrismaNodeWithParent(fromField))
          val itemGroupsByModelId = result.groupBy(_.parentId)
          fromNodeIds.map { id =>
            itemGroupsByModelId.find(_._1 == id) match {
              case Some((_, itemsForId)) => ResolverResult(args, itemsForId, parentModelId = Some(id))
              case None                  => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
            }
          }
        }
      )
    }
  }

  private def selectAllFromRelatedWithPaginationForMySQL(
      fromField: RelationField,
      fromModelIds: Vector[IdGCValue],
      args: Option[QueryArguments]
  ): DBIO[Vector[ResolverResult[PrismaNodeWithParent]]] = {
    require(args.exists(_.isWithPagination))
    val builder = RelatedModelsQueryBuilder(slickDatabase, schemaName, fromField, args, fromModelIds)

    SimpleDBIO { ctx =>
      val baseQuery        = "(" + builder.mysqlHack.getSQL + ")"
      val distinctModelIds = fromModelIds.distinct
      val queries          = Vector.fill(distinctModelIds.size)(baseQuery)
      val query            = queries.mkString(" union all ")

      val ps = ctx.connection.prepareStatement(query)
      val pp = new PositionedParameters(ps)

      distinctModelIds.foreach { id =>
        pp.setGcValue(id)

        args.foreach { arg =>
          arg.filter.foreach(filter => SetParams.setFilter(pp, filter))

          SetParams.setCursor(pp, arg)

          if (arg.isWithPagination) {
            limitClause(args).foreach { params =>
              pp.setInt(params._1)
              pp.setInt(params._2)
            }
          }
        }
      }

      val rs                  = ps.executeQuery()
      val result              = rs.readWith(readPrismaNodeWithParent(fromField))
      val itemGroupsByModelId = result.groupBy(_.parentId)
      fromModelIds.map { id =>
        itemGroupsByModelId.find(_._1 == id) match {
          case Some((_, itemsForId)) => ResolverResult(args, itemsForId, parentModelId = Some(id))
          case None                  => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
        }
      }
    }
  }

  def getNodesByValuesForField(model: Model, field: ScalarField, values: Vector[GCValue]): DBIO[Vector[PrismaNode]] = {
    val queryArgs = Some(QueryArguments.withFilter(ScalarFilter(field, In(values))))
    val query     = modelQuery(model, queryArgs)
    queryToDBIO(query)(
      setParams = pp => SetParams.setQueryArgs(pp, queryArgs),
      readResult = _.readWith(readsPrismaNode(model))
    )
  }
}
