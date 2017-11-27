package cool.graph.util.coolSangria

import sangria.schema.Args

import scala.collection.concurrent.TrieMap

object Sangria {

  def rawArgs(raw: Map[String, Any]): Args = {
    new Args(raw, Set.empty, Set.empty, Set.empty, TrieMap.empty)
  }
}
