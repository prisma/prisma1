package cool.graph.util.collection

object ToImmutable {
  implicit class ToImmutableSeq[T](seq: Seq[T]) {
    def toImmutable: collection.immutable.Seq[T] = {
      collection.immutable.Seq(seq: _*)
    }
  }
}
