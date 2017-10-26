package cool.graph.system.migration

object Diff {

  def diff[T](current: T, updated: T): Option[T] = {
    diffOpt(Some(current), Some(updated))
  }

  def diffOpt[T](current: Option[T], updated: Option[T]): Option[T] = {
    if (current == updated) {
      None
    } else {
      updated
    }
  }
}
