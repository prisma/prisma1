package cool.graph.system.mutations

trait MutationInput { this: Product =>
  import shapeless._
  import syntax.typeable._

  def clientMutationId: Option[String]

  def isAnyArgumentSet(exclude: List[String] = List()): Boolean = {
    getCaseClassParams(this)
      .filter(x => !(exclude :+ "clientMutationId").contains(x._1))
      .map(_._2)
      .map(_.cast[Option[Any]])
      .collect {
        case Some(x: Option[Any]) => x.isDefined
      } exists identity
  }

  private def getCaseClassParams(cc: AnyRef): Seq[(String, Any)] =
    (Seq[(String, Any)]() /: cc.getClass.getDeclaredFields) { (a, f) =>
      f.setAccessible(true)
      a :+ (f.getName, f.get(cc))
    }
}
