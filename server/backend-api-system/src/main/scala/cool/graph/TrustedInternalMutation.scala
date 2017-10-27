package cool.graph

import com.typesafe.config.Config
import cool.graph.shared.database.InternalDatabase
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.mutactions.InvalidInput
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

case class TrustedInternalMutation[+T <: Mutation](
    mutation: InternalMutation[T],
    args: TrustedInternalMutationInput[Product],
    internalDatabase: InternalDatabase
)(implicit inj: Injector)
    extends InternalMutation[T]()
    with Injectable {

  val config: Config = inject[Config](identified by "config")

  override def prepareActions(): List[Mutaction] = {
    if (args.secret == config.getString("systemApiSecret")) {
      actions = mutation.prepareActions()
    } else {
      actions = List(InvalidInput(SystemErrors.InvalidSecret()))
    }
    actions
  }

  override def getReturnValue: Option[T] = mutation.getReturnValue
}

case class TrustedInternalMutationInput[+T](secret: String, mutationInput: T)
