package com.prisma.deploy.connector.postgresql.database

import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models.{Field, Model}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{PositionedParameters, SQLActionBuilder}

object PostgresDeployDatabaseQueryBuilder {

  def existsByModel(projectId: String, model: Model): SQLActionBuilder = {
    sql"""select exists (select "${model.dbNameOfIdField}" from "#$projectId"."#${model.dbName}")"""
  }

  def existsByRelation(projectId: String, relationId: String): SQLActionBuilder = {
    sql"""select exists (select "id" from "#$projectId"."#$relationId")"""
  }

  def existsDuplicateByRelationAndSide(projectId: String, relationTableName: String, relationSide: RelationSide): SQLActionBuilder = {
    sql"""select exists (select Count(*)from "#$projectId"."#$relationTableName" Group by "#${relationSide.toString}" having Count(*) > 1)"""
  }

  def existsNullByModelAndScalarField(projectId: String, model: Model, fieldName: String) = {
    sql"""SELECT EXISTS(Select "#${model.dbNameOfIdField}" FROM "#$projectId"."#${model.dbName}"
          WHERE "#$projectId"."#${model.dbName}".#$fieldName IS NULL)"""
  }

  def existsDuplicateValueByModelAndField(projectId: String, model: Model, fieldName: String) = {
    sql"""SELECT EXISTS(
             Select Count(temp)
             FROM (
                  SELECT "#$fieldName"
                  FROM "#$projectId"."#${model.dbName}"
                  WHERE "#$fieldName" is not null
                  ) as temp
             GROUP BY temp
             HAVING COUNT(temp) > 1
          )"""
  }

  def existsNullByModelAndRelationField(projectId: String, model: Model, field: Field) = {
    val relationId   = field.relation.get.relationTableName
    val relationSide = field.relationSide.get.toString
    sql"""select EXISTS (
            select "#${model.dbNameOfIdField}" from "#$projectId"."#${model.dbName}"
            where "#${model.dbNameOfIdField}" Not IN
            (Select "#$projectId"."#$relationId"."#$relationSide" from "#$projectId"."#$relationId")
          )"""
  }

  def enumValueIsInUse(projectId: String, models: Vector[Model], enumName: String, value: String) = {

    val nameTuples = for {
      model <- models
      field <- model.fields
      if field.enum.isDefined && field.enum.get.name == enumName
    } yield {
      if (field.isList) ("nodeId", s"${model.name}_${field.name}", "value", value) else (model.dbNameOfIdField, model.name, field.name, value)
    }

    val checks: Vector[SQLActionBuilder] = nameTuples.map { tuple =>
      sql"""(Select Exists (
                  Select "#${tuple._1}"
                  From "#$projectId"."#${tuple._2}"
                  Where "#${tuple._3}" = ${tuple._4}) as existanceCheck)"""
    }

    val unionized = combineBy(checks, "Union")

    sql"""Select Exists (
               Select existanceCheck
               From(""" concat unionized concat sql""") as combined
               Where existanceCheck = TRUE )"""
  }

  def combineBy(actions: Iterable[SQLActionBuilder], combinator: String): Option[SQLActionBuilder] = actions.toList match {
    case Nil         => None
    case head :: Nil => Some(head)
    case _           => Some(actions.reduceLeft((a, b) => a concat sql"#$combinator" concat b))
  }

  implicit class SQLActionBuilderConcat(val a: SQLActionBuilder) extends AnyVal {
    def concat(b: SQLActionBuilder): SQLActionBuilder = {
      SQLActionBuilder(a.queryParts ++ " " ++ b.queryParts, (p: Unit, pp: PositionedParameters) => {
        a.unitPConv.apply(p, pp)
        b.unitPConv.apply(p, pp)
      })
    }
    def concat(b: Option[SQLActionBuilder]): SQLActionBuilder = b match {
      case Some(b) => a concat b
      case None    => a
    }

    def ++(b: SQLActionBuilder): SQLActionBuilder         = concat(b)
    def ++(b: Option[SQLActionBuilder]): SQLActionBuilder = concat(b)
  }
}
