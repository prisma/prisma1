package com.prisma.rs

import com.prisma.gc_values._
import com.prisma.rs.jna.{JnaRustBridge, ProtobufEnvelope}
import com.sun.jna.{Memory, Native, Pointer}
import prisma.protocol._
import scalapb.GeneratedMessage

case class NodeResult(id: IdGCValue, data: RootGCValue)

object NativeBinding {
  val library: JnaRustBridge = {
    System.setProperty("jna.debug_load.jna", "true")
    System.setProperty("jna.debug_load", "true")
    System.setProperty("jna.library.path", s"${sys.env.getOrElse("SERVER_ROOT", sys.error("SERVER_ROOT env var required but not found"))}/prisma-rs/build")
    Native.loadLibrary("prisma", classOf[JnaRustBridge])
  }

  def get_node_by_where(getNodeByWhere: GetNodeByWhereInput): Option[(Node, Vector[String])] = {
    val (pointer, length) = writeBuffer(getNodeByWhere)

    handleProtoResult(library.get_node_by_where(pointer, length)) { nodesAndFields: (Seq[Node], Seq[String]) =>
      nodesAndFields._1.headOption.map((_, nodesAndFields._2.toVector))
    }
  }

  def get_nodes(getNodes: GetNodesInput): (Vector[Node], Vector[String]) = {
    val (pointer, length) = writeBuffer(getNodes)

    handleProtoResult(library.get_nodes(pointer, length)) { nodesAndFields: (Vector[Node], Vector[String]) =>
      nodesAndFields
    }
  }

  def handleProtoResult[T, U](envelope: ProtobufEnvelope.ByReference)(processMessage: T => U): U = {
    val messageContent = envelope.data.getByteArray(0, envelope.len.intValue())
    library.destroy(envelope)

    val decodedMessage = RpcResponse.parseFrom(messageContent)
    decodedMessage.response match {
      // Success cases
      case RpcResponse.Response.Result(Result(value: Result.Value)) =>
        value match {
          case Result.Value.NodesResult(NodesResult(nodes: Seq[Node], fields: Seq[String])) =>
            processMessage((nodes, fields).asInstanceOf[T])

          case Result.Value.Empty =>
            processMessage((Seq.empty[Node], Seq.empty[String]).asInstanceOf[T])
        }

      // Error cases
      case RpcResponse.Response.Error(error: Error) =>
        error.value match {
          case Error.Value.ConnectionError(str)            => throw ConnectionError(str)
          case Error.Value.InvalidInputError(str)          => throw InvalidInputError(str)
          case Error.Value.JsonDecodeError(str)            => throw JsonDecodeError(str)
          case Error.Value.NoResultsError(str)             => throw NoResultError(str)
          case Error.Value.ProtobufDecodeError(str)        => throw ProtobufDecodeError(str)
          case Error.Value.QueryError(str)                 => throw QueryError(str)
          case Error.Value.InvalidConnectionArguments(str) => throw InvalidConnectionArguments(str)
          case Error.Value.Empty                           => sys.error("Empty RPC response error value")
        }

      case RpcResponse.Response.Empty => sys.error("Empty RPC response value")
    }
  }

  def writeBuffer[T](msg: GeneratedMessage): (Pointer, Int) = {
    val length       = msg.serializedSize
    val serialized   = msg.toByteArray
    val nativeMemory = new Memory(length)

    nativeMemory.write(0, serialized, 0, length)
    (nativeMemory, length)
  }
}
