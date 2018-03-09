package com.prisma.deploy.database

import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.migration.validation.SchemaWarning
import com.prisma.shared.models._

import scala.concurrent.Future

case class DestructiveChanges(project: Project, nextSchema: Schema, steps: Vector[MigrationStep])(implicit val dependencies: DeployDependencies) {
  val clientDataResolver = dependencies.clientDbQueries(project)
  val previousSchema     = project.schema

  def generateWarnings: Future[Vector[SchemaWarning]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val res = steps.collect {
      case x: DeleteRelation => clientDataResolver.existsByRelation(x.name).collect { case true => SchemaWarning(x.name, "") }
      case x: DeleteField    => clientDataResolver.existsByModel(x.model).collect { case true   => SchemaWarning(x.name, x.name, "") }
//        case x: DeleteField    =>
//        case x: DeleteModel    =>
//        case x: DeleteEnum     =>
//        case x: UpdateRelation =>
//        case x: UpdateField    =>
//        case x: UpdateModel    =>
//        case x: UpdateEnum     =>
    }

    Future.sequence(res)
  }

}
