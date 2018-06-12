package com.prisma.api.connector.mysql.database

import java.sql.{PreparedStatement, ResultSet}

import com.prisma.api.connector._
import com.prisma.gc_values._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.{Function => _, _}
import slick.dbio.DBIOAction
import slick.dbio.Effect.Read
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.{DatabaseMeta, MTable}
import slick.jdbc.{SQLActionBuilder, _}
import slick.sql.SqlStreamingAction

import scala.concurrent.ExecutionContext

case class MySqlApiDatabaseQueryBuilder(project: Project)(implicit ec: ExecutionContext) {
  import JdbcExtensions._
  import MySqlQueryArgumentsExtensions._
  import MySqlSlickExtensions._

  def getResultForModel(model: Model): GetResult[PrismaNode] = GetResult { ps: PositionedResult =>
    getPrismaNode(model, ps)
  }

  private def getPrismaNode(model: Model, ps: PositionedResult) = {
    val data = model.scalarNonListFields.map(field => field.name -> ps.rs.getGcValue(field.name, field.typeIdentifier))

    PrismaNode(id = ps.rs.getId, data = RootGCValue(data: _*), Some(model.name))
  }

  def getResultForModelAndRelationSide(model: Model, side: String, oppositeSide: String): GetResult[PrismaNodeWithParent] = GetResult { ps: PositionedResult =>
    val node       = getPrismaNode(model, ps)
    val firstSide  = ps.rs.getParentId(side)
    val secondSide = ps.rs.getParentId(oppositeSide)
    val parentId   = if (firstSide == node.id) secondSide else firstSide

    PrismaNodeWithParent(parentId, node)
  }

  implicit object GetRelationNode extends GetResult[RelationNode] {
    override def apply(ps: PositionedResult): RelationNode = RelationNode(ps.rs.getAsID("A"), ps.rs.getAsID("B"))
  }

  implicit object GetRelationCount extends GetResult[(CuidGCValue, Int)] {
    override def apply(ps: PositionedResult): (CuidGCValue, Int) = (ps.rs.getId, ps.rs.getInt("Count"))
  }

  def getResultForScalarListField(field: ScalarField): GetResult[ScalarListElement] = GetResult { ps: PositionedResult =>
    val resultSet = ps.rs
    val nodeId    = resultSet.getString("nodeId")
    val position  = resultSet.getInt("position")
    val value     = resultSet.getGcValue("value", field.typeIdentifier)
    ScalarListElement(nodeId, position, value)
  }

  private def whereOrderByLimitCommands(args: Option[QueryArguments], overrideMaxNodeCount: Option[Int], tableName: String, forList: Boolean = false) = {
    val (where, orderBy, limit) = extractQueryArgs(project.id, alias = ALIAS, tableName, args, None, overrideMaxNodeCount, forList)
    sql"" ++ prefixIfNotNone("where", where) ++ prefixIfNotNone("order by", orderBy) ++ prefixIfNotNone("limit", limit)
  }

  def selectAllFromTable(model: Model, args: Option[QueryArguments], overrideMaxNodeCount: Option[Int] = None) = {
    val query = sql"""select * from `#${project.id}`.`#${model.name}` as `#${ALIAS}` """ ++ whereOrderByLimitCommands(args, overrideMaxNodeCount, model.name)
    query.as[PrismaNode](getResultForModel(model)).map(args.get.resultTransform)
  }

  def selectAllFromRelationTable(relationId: String, args: Option[QueryArguments], overrideMaxNodeCount: Option[Int] = None) = {
    val query = sql"""select * from `#${project.id}`.`#$relationId` as `#${ALIAS}` """ ++ whereOrderByLimitCommands(args, overrideMaxNodeCount, relationId)
    query.as[RelationNode].map(args.get.resultTransform)
  }

  def selectAllFromListTable(model: Model,
                             field: ScalarField,
                             args: Option[QueryArguments],
                             overrideMaxNodeCount: Option[Int] = None): DBIOAction[ResolverResult[ScalarListValues], NoStream, Effect] = {
    val tableName = s"${model.name}_${field.name}"
    val query     = sql"""select * from `#${project.id}`.`#$tableName`  as `#${ALIAS}` """ ++ whereOrderByLimitCommands(args, overrideMaxNodeCount, tableName, true)

    query.as[ScalarListElement](getResultForScalarListField(field)).map { scalarListElements =>
      val res = args.get.resultTransform(scalarListElements)
      val convertedValues =
        res.nodes
          .groupBy(_.nodeId)
          .map { case (id, values) => ScalarListValues(CuidGCValue(id), ListGCValue(values.sortBy(_.position).map(_.value))) }
          .toVector
      res.copy(nodes = convertedValues)
    }
  }

  def countAllFromTable(table: String, whereFilter: Option[Filter]): DBIOAction[Int, NoStream, Effect] = {
    val query = sql"select count(*) from `#${project.id}`.`#$table`" ++ whereFilterAppendix(project.id, table, whereFilter)
    query.as[Int].map(_.head)
  }

  def batchSelectFromModelByUnique(model: Model, fieldName: String, values: Vector[GCValue]) = {
    val query = sql"select * from `#${project.id}`.`#${model.name}` where `#$fieldName` in (" ++ combineByComma(values.map(v => sql"$v")) ++ sql")"
    query.as[PrismaNode](getResultForModel(model))
  }

  import com.prisma.slick.NewJdbcExtensions._
  import com.prisma.api.connector.mysql.database.JdbcExtensions._

  def batchSelectFromModelByUniqueSimple(model: Model, fieldName: String, values: Vector[GCValue]): SimpleDBIO[Vector[PrismaNode]] =
    SimpleDBIO[Vector[PrismaNode]] { x =>
      val query                 = s"select * from `${project.id}`.`${model.name}` where `$fieldName` in ${queryPlaceHolders(values)}"
      val ps: PreparedStatement = x.connection.prepareStatement(query).setValues(values)
      val rs: ResultSet         = ps.executeQuery()
      rs.as[PrismaNode](readsPrismaNode(model))
    }

  def readsPrismaNode(model: Model): ReadsResultSet[PrismaNode] = ReadsResultSet { rs =>
    val data = model.scalarNonListFields.map(field => field.name -> rs.getGcValue(field.name, field.typeIdentifier))
    PrismaNode(id = rs.getId, data = RootGCValue(data: _*), Some(model.name))
  }

  implicit val setGcValue: SetParam[GCValue] = new SetParam[GCValue] {
    override def apply(ps: PreparedStatement, index: Int, value: GCValue): Unit = ps.setGcValue(index, value)
  }

  def selectFromScalarList(modelName: String, field: ScalarField, nodeIds: Vector[CuidGCValue]): DBIOAction[Vector[ScalarListValues], NoStream, Effect] = {
    val query = sql"select nodeId, position, value from `#${project.id}`.`#${modelName}_#${field.name}` where nodeId in (" ++ combineByComma(
      nodeIds.map(v => sql"$v")) ++ sql")"

    query.as[ScalarListElement](getResultForScalarListField(field)).map { scalarListElements =>
      val grouped: Map[Id, Vector[ScalarListElement]] = scalarListElements.groupBy(_.nodeId)
      grouped.map {
        case (id, values) =>
          val gcValues = values.sortBy(_.position).map(_.value)
          ScalarListValues(CuidGCValue(id), ListGCValue(gcValues))
      }.toVector
    }
  }

  def batchSelectAllFromRelatedModel(fromField: RelationField,
                                     fromModelIds: Vector[CuidGCValue],
                                     args: Option[QueryArguments]): DBIOAction[Vector[ResolverResult[PrismaNodeWithParent]], NoStream, Effect] = {

    val relatedModel         = fromField.relatedModel_!
    val fieldTable           = fromField.relatedModel_!.name
    val unsafeRelationId     = fromField.relation.relationTableName
    val modelRelationSide    = fromField.relationSide.toString
    val oppositeRelationSide = fromField.oppositeRelationSide.toString

    val (conditionCommand, orderByCommand, limitCommand) =
      extractQueryArgs(project.id,
                       fieldTable,
                       fieldTable,
                       args,
                       defaultOrderShortcut = Some(s"""`${project.id}`.`$unsafeRelationId`.$oppositeRelationSide"""),
                       None)

    def createQuery(id: String, modelRelationSide: String, fieldRelationSide: String) = {
      sql"""(select `#${project.id}`.`#$fieldTable`.*, `#${project.id}`.`#$unsafeRelationId`.A as __Relation__A,  `#${project.id}`.`#$unsafeRelationId`.B as __Relation__B
            from `#${project.id}`.`#$fieldTable`
           inner join `#${project.id}`.`#$unsafeRelationId`
           on `#${project.id}`.`#$fieldTable`.id = `#${project.id}`.`#$unsafeRelationId`.#$fieldRelationSide
           where `#${project.id}`.`#$unsafeRelationId`.#$modelRelationSide = '#$id' """ ++
        prefixIfNotNone("and", conditionCommand) ++
        prefixIfNotNone("order by", orderByCommand) ++
        prefixIfNotNone("limit", limitCommand) ++ sql")"
    }

    val query =
      fromModelIds.view.zipWithIndex.foldLeft(sql"")((a, b) => a ++ unionIfNotFirst(b._2) ++ createQuery(b._1.value, modelRelationSide, oppositeRelationSide))

    query
      .as[PrismaNodeWithParent](getResultForModelAndRelationSide(relatedModel, modelRelationSide, oppositeRelationSide))
      .map { items =>
        val itemGroupsByModelId = items.groupBy(_.parentId)
        fromModelIds
          .map(id =>
            itemGroupsByModelId.find(_._1 == id) match {
              case Some((_, itemsForId)) => args.get.resultTransform(itemsForId).copy(parentModelId = Some(id))
              case None                  => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
          })
      }
  }

  def unionIfNotFirst(index: Int): SQLActionBuilder = if (index == 0) sql"" else sql"union all "

// used in tests only

  def getTables: DBIOAction[Vector[String], NoStream, Read] = {
    for {
      metaTables <- MTable.getTables(cat = Some(project.id), schemaPattern = None, namePattern = None, types = None)
    } yield metaTables.map(table => table.name.name)
  }

  def getSchemas: DBIOAction[Vector[String], NoStream, Read] = {
    for {
      catalogs <- DatabaseMeta.getCatalogs
    } yield catalogs
  }

  def itemCountForTable(modelName: String) = { // todo use count all from model
    sql"SELECT COUNT(*) AS Count FROM `#${project.id}`.`#$modelName`"
  }

  def existsByModel(modelName: String): SQLActionBuilder = { //todo also replace in tests with count
    sql"select exists (select `id` from `#${project.id}`.`#$modelName`)"
  }
}
