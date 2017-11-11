package cool.graph.client

import cool.graph.RequestContextTrait
import sangria.schema.Context

class MutationQueryWhitelist {
  private var fields: Set[String]       = Set()
  private var paths: List[List[String]] = List(List())
  private var _isMutationQuery          = false

  def registerWhitelist[C <: RequestContextTrait](mutationName: String, pathsToNode: List[List[String]], inputWrapper: Option[String], ctx: Context[C, _]) = {
    _isMutationQuery = true

    fields = inputWrapper match {
      case Some(wrapper) => ctx.args.raw(wrapper).asInstanceOf[Map[String, Any]].keys.toSet
      case None          => ctx.args.raw.keys.toSet
    }

    val mutationNamePaths: List[List[String]] = pathsToNode.map(mutationName +: _)
    val alias: Option[String]                 = ctx.astFields.find(_.name == mutationName).flatMap(_.alias)

    val aliasPath: List[List[String]] = alias match {
      case Some(a) => pathsToNode.map(a +: _)
      case None    => List(List.empty)
    }

    this.paths = mutationNamePaths ++ aliasPath
  }

  def isMutationQuery = _isMutationQuery

  def isWhitelisted(path: Vector[Any]) = path.reverse.toList match {
    case (field: String) :: pathToNode if paths.contains(pathToNode.reverse) =>
      fields.contains(field) || field == "id"

    case _ =>
      false
  }

}
