package cool.graph.deploy.migration

import cool.graph.deploy.migration.mutactions._
import cool.graph.shared.models._

case class MigrationStepMapper(projectId: String) {

  // todo: I think this knows too much about previous and next. It should just know how to apply steps to previous.
  // todo: Ideally, the interface would just have a (previous)project and a step, maybe?
  def mutactionFor(previousSchema: Schema, nextSchema: Schema, step: MigrationStep): Option[ClientSqlMutaction] = step match {
    case x: CreateModel =>
      Some(CreateModelTable(projectId, x.name))

    case x: DeleteModel =>
      val model                = previousSchema.getModelByName_!(x.name)
      val scalarListFieldNames = model.scalarListFields.map(_.name).toVector
      Some(DeleteModelTable(projectId, x.name, scalarListFieldNames))

    case x: UpdateModel =>
      val model                = nextSchema.getModelByName_!(x.newName)
      val scalarListFieldNames = model.scalarListFields.map(_.name).toVector
      Some(RenameModelTable(projectId, previousName = x.name, nextName = x.newName, scalarListFieldsNames = scalarListFieldNames))

    case x: CreateField =>
      // todo I think those validations should be somewhere else, preferably preventing a step being created
      val model = nextSchema.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)
      if (ReservedFields.isReservedFieldName(field.name) || !field.isScalar) {
        None
      } else {
        if (field.isList) {
          Some(CreateScalarListTable(projectId, model.name, field.name, field.typeIdentifier))
        } else {
          Some(CreateColumn(projectId, model, field))
        }
      }

    case x: DeleteField =>
      val model = previousSchema.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)
      if (field.isList) {
        Some(DeleteScalarListTable(projectId, model.name, field.name, field.typeIdentifier))
      } else {
        Some(DeleteColumn(projectId, model, field))
      }

    case x: UpdateField =>
      val model         = nextSchema.getModelByName_!(x.model)
      val nextField     = nextSchema.getFieldByName_!(x.model, x.finalName)
      val previousField = previousSchema.getFieldByName_!(x.model, x.name)

      if (previousField.isList) {
        // todo: also handle changing to/from scalar list
        Some(UpdateScalarListTable(projectId, model, model, previousField, nextField))
      } else {
        Some(UpdateColumn(projectId, model, previousField, nextField))
      }

    case _: EnumMigrationStep =>
      None

    case x: CreateRelation =>
      val relation = nextSchema.getRelationByName_!(x.name)
      Some(CreateRelationTable(projectId, nextSchema, relation))

    case x: DeleteRelation =>
      val relation = previousSchema.getRelationByName_!(x.name)
      Some(DeleteRelationTable(projectId, nextSchema, relation))
  }
}

//case class MigrationApplierImpl(clientDatabase: DatabaseDef)(implicit ec: ExecutionContext) extends MigrationApplier {
//  override def applyMigration(previousProject: Project, nextProject: Project, migration: Migration): Future[MigrationApplierResult] = {
//    val initialProgress = MigrationProgress(pendingSteps = migration.steps, appliedSteps = Vector.empty, isRollingback = false)
//    recurse(previousProject, nextProject, initialProgress)
//  }
//
//  def recurse(previousProject: Project, nextProject: Project, progress: MigrationProgress): Future[MigrationApplierResult] = {
//    if (!progress.isRollingback) {
//      recurseForward(previousProject, nextProject, progress)
//    } else {
//      recurseForRollback(previousProject, nextProject, progress)
//    }
//  }
//
//  def recurseForward(previousProject: Project, nextProject: Project, progress: MigrationProgress): Future[MigrationApplierResult] = {
//    if (progress.pendingSteps.nonEmpty) {
//      val (step, newProgress) = progress.popPending
//
//      val result = for {
//        _ <- applyStep(previousProject, nextProject, step)
//        x <- recurse(previousProject, nextProject, newProgress)
//      } yield x
//
//      result.recoverWith {
//        case exception =>
//          println("encountered exception while applying migration. will roll back.")
//          exception.printStackTrace()
//          recurseForRollback(previousProject, nextProject, newProgress.markForRollback)
//      }
//    } else {
//      Future.successful(MigrationApplierResult(succeeded = true))
//    }
//  }
//
//  def recurseForRollback(previousProject: Project, nextProject: Project, progress: MigrationProgress): Future[MigrationApplierResult] = {
//    if (progress.appliedSteps.nonEmpty) {
//      val (step, newProgress) = progress.popApplied
//
//      for {
//        _ <- unapplyStep(previousProject, nextProject, step).recover { case _ => () }
//        x <- recurse(previousProject, nextProject, newProgress)
//      } yield x
//    } else {
//      Future.successful(MigrationApplierResult(succeeded = false))
//    }
//  }
//
//  def applyStep(previousProject: Project, nextProject: Project, step: MigrationStep): Future[Unit] = {
//    migrationStepToMutaction(previousProject, nextProject, step).map(executeClientMutaction).getOrElse(Future.successful(()))
//  }
//
//  def unapplyStep(previousProject: Project, nextProject: Project, step: MigrationStep): Future[Unit] = {
//    migrationStepToMutaction(previousProject, nextProject, step).map(executeClientMutactionRollback).getOrElse(Future.successful(()))
//  }
//
//  def executeClientMutaction(mutaction: ClientSqlMutaction): Future[Unit] = {
//    for {
//      statements <- mutaction.execute
//      _          <- clientDatabase.run(statements.sqlAction)
//    } yield ()
//  }
//
//  def executeClientMutactionRollback(mutaction: ClientSqlMutaction): Future[Unit] = {
//    for {
//      statements <- mutaction.rollback.get
//      _          <- clientDatabase.run(statements.sqlAction)
//    } yield ()
//  }
//}
