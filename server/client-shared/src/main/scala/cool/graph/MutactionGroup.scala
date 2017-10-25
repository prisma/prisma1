package cool.graph

case class MutactionGroup(mutactions: List[Mutaction], async: Boolean) {

  // just for debugging!
  def unpackTransactions: List[Mutaction] = {
    mutactions.flatMap {
      case t: Transaction => t.clientSqlMutactions
      case x              => Seq(x)
    }
  }
}
