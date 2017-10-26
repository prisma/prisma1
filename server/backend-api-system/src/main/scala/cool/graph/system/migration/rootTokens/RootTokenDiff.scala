package cool.graph.system.migration.rootTokens

import cool.graph.shared.models.Project

case class RootTokenDiff(project: Project, newRootTokens: Vector[String]) {
  val oldRootTokenNames: Vector[String] = project.rootTokens.map(_.name).toVector

  val addedRootTokens: Vector[String]   = newRootTokens diff oldRootTokenNames
  val removedRootTokens: Vector[String] = oldRootTokenNames diff newRootTokens

  val removedRootTokensIds: Vector[String] =
    removedRootTokens.map(rootToken => project.rootTokens.find(_.name == rootToken).getOrElse(sys.error("Logic error in RootTokenDiff")).id)
}
