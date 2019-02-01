package com.prisma.rs

import com.prisma.gc_values._
import com.prisma.rs.jna.{JnaRustBridge, ProtobufEnvelope}
import com.sun.jna.{Memory, Native, Pointer}
import org.joda.time.DateTime
import play.api.libs.json.Json
import prisma.protocol.GraphqlId.IdValue
import prisma.protocol.ValueContainer.PrismaValue
import prisma.protocol._
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

case class NodeResult(id: IdGCValue, data: RootGCValue)

object NativeBinding {
  val library: JnaRustBridge = {
    System.setProperty("jna.debug_load.jna", "true")
    System.setProperty("jna.debug_load", "true")
    System.setProperty("jna.library.path", s"${sys.env.getOrElse("SERVER_ROOT", sys.error("SERVER_ROOT env var required but not found"))}/prisma-rs/build")
    Native.loadLibrary("prisma", classOf[JnaRustBridge])
  }

  def select_1(): Int = library.select_1()

  def get_node_by_where(getNodeByWhere: GetNodeByWhereInput): Option[Node] = {
    val (pointer, length) = writeBuffer(getNodeByWhere)

    handleProtoResult(library.get_node_by_where(pointer, length)) { nodes: Seq[Node] =>
      nodes.headOption
    }
  }

  def handleProtoResult[U, T](envelope: ProtobufEnvelope.ByReference)(processMessage: T => U) = {
    val messageContent = envelope.data.getByteArray(0, envelope.len.intValue())
    library.destroy(envelope)

    val decodedMessage = RpcResponse.parseFrom(messageContent)
    decodedMessage.response match {
      // Success cases
      case RpcResponse.Response.Result(Result(value: Result.Value)) =>
        value match {
          case Result.Value.NodesResult(NodesResult(nodes: Seq[Node])) => processMessage(nodes)
          case Result.Value.Empty                                      => sys.error("Empty RPC response result value")
        }
      // Error cases
      case RpcResponse.Response.Error(error: Error) =>
        error.value match {
          case Error.Value.ConnectionError(str)     => ???
          case Error.Value.InvalidInputError(str)   => ???
          case Error.Value.JsonDecodeError(str)     => ???
          case Error.Value.NoResultsError(str)      => ???
          case Error.Value.ProtobufDecodeError(str) => ???
          case Error.Value.QueryError(str)          => ???
          case Error.Value.Empty                    => sys.error("Empty RPC response error value")

        }
      case RpcResponse.Response.Empty => sys.error("Empty RPC response value")
    }
  }

  def toNodeResult(values: Seq[ValueContainer.PrismaValue]): NodeResult = {
    val idValue   = values.find(_.isGraphqlId).get
    val dataValue = values.find(!_.isGraphqlId).get

    NodeResult(toGcValue(idValue).asInstanceOf[StringIdGCValue], toGcValue(dataValue).asRoot)
  }

  def toGcValue(value: ValueContainer.PrismaValue): GCValue = {
    value match {
      case PrismaValue.Empty                => NullGCValue
      case PrismaValue.Boolean(b: Boolean)  => BooleanGCValue(b)
      case PrismaValue.DateTime(dt: String) => DateTimeGCValue(DateTime.parse(dt))
      case PrismaValue.Enum(e: String)      => EnumGCValue(e)
      case PrismaValue.Float(f: Float)      => FloatGCValue(f)
      case PrismaValue.GraphqlId(id: GraphqlId) =>
        id.idValue match {
          case IdValue.String(s) => StringIdGCValue(s)
          case IdValue.Int(i)    => IntGCValue(i.toInt)
          case _                 => sys.error("empty protobuf")
        }
      case PrismaValue.Int(i: Int)        => IntGCValue(i)
      case PrismaValue.Json(j: String)    => JsonGCValue(Json.parse(j))
      case PrismaValue.Null(_)            => NullGCValue
      case PrismaValue.Relation(r: Long)  => ??? // What are we supposed to do here?
      case PrismaValue.String(s: String)  => StringGCValue(s)
      case PrismaValue.Uuid(uuid: String) => UuidGCValue.parse(uuid).get
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
