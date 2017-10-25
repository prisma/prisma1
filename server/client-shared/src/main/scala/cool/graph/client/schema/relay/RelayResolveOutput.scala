package cool.graph.client.schema.relay

import cool.graph.DataItem
import sangria.schema.Args

case class RelayResolveOutput(clientMutationId: String, item: DataItem, args: Args)
