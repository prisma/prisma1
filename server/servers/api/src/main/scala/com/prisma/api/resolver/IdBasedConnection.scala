package com.prisma.api.resolver

import com.prisma.api.connector.QueryArguments
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models
import sangria.schema._

import scala.annotation.implicitNotFound
import scala.language.higherKinds
import scala.reflect.ClassTag

case class ConnectionParentElement(nodeId: Option[IdGCValue], field: Option[models.RelationField], args: QueryArguments)

trait IdBasedConnection[T] {
  def pageInfo: PageInfo
  def edges: Seq[Edge[T]]
  def parent: ConnectionParentElement
  def toNodes = edges.map(_.node)
}

object IdBasedConnection {
  object Args {
    val Before = Argument("before", OptionInputType(StringType))
    val After  = Argument("after", OptionInputType(StringType))
    val First  = Argument("first", OptionInputType(IntType))
    val Last   = Argument("last", OptionInputType(IntType))

    val All = Before :: After :: First :: Last :: Nil
  }

  def isValidNodeType[Val](nodeType: OutputType[Val]): Boolean =
    nodeType match {
      case _: ScalarType[_] | _: EnumType[_] | _: CompositeType[_] ⇒ true
      case OptionType(ofType)                                      ⇒ isValidNodeType(ofType)
      case _                                                       ⇒ false
    }

  def definition[Ctx, Conn[_], Val](
      name: String,
      nodeType: OutputType[Val],
      edgeFields: ⇒ List[Field[Ctx, Edge[Val]]] = Nil,
      connectionFields: ⇒ List[Field[Ctx, Conn[Val]]] = Nil
  )(implicit connEv: IdBasedConnectionLike[Conn, Val], classEv: ClassTag[Conn[Val]]) = {
    if (!isValidNodeType(nodeType))
      throw new IllegalArgumentException(
        "Node type is invalid. It must be either a Scalar, Enum, Object, Interface, Union, " +
          "or a Non‐Null wrapper around one of those types. Notably, this field cannot return a list.")

    val edgeType = OptionType(
      ObjectType[Ctx, Edge[Val]](
        name + "Edge",
        "An edge in a connection.",
        () ⇒ {
          List[Field[Ctx, Edge[Val]]](
            Field("node", nodeType, Some("The item at the end of the edge."), resolve = _.value.node),
            Field("cursor", StringType, Some("A cursor for use in pagination."), resolve = _.value.cursor.value.toString) // fixme: is this correct for numeric ids?
          ) ++ edgeFields
        }
      ))

    val connectionType = ObjectType[Ctx, Conn[Val]](
      name + "Connection",
      "A connection to a list of items.",
      () ⇒ {
        List[Field[Ctx, Conn[Val]]](
          Field("pageInfo", PageInfoType, Some("Information to aid in pagination."), resolve = ctx ⇒ connEv.pageInfo(ctx.value)),
          Field(
            "edges",
            ListType(edgeType),
            Some("A list of edges."),
            resolve = ctx ⇒ {
              val items = ctx.value
              val edges = connEv.edges(items).map(Some(_))
              edges
            }
          )
        ) ++ connectionFields
      }
    )

    IdBasedConnectionDefinition(edgeType, connectionType)
  }

  /**
    * The common page info type used by all connections.
    */
  val PageInfoType =
    ObjectType(
      "PageInfo",
      "Information about pagination in a connection.",
      fields[Unit, PageInfo](
        Field("hasNextPage", BooleanType, Some("When paginating forwards, are there more items?"), resolve = _.value.hasNextPage),
        Field("hasPreviousPage", BooleanType, Some("When paginating backwards, are there more items?"), resolve = _.value.hasPreviousPage),
        Field(
          "startCursor",
          OptionType(StringType),
          Some("When paginating backwards, the cursor to continue."),
          resolve = _.value.startCursor.map(_.value.toString) // fixme: is this the correct way to handle IntGCValues?
        ),
        Field("endCursor", OptionType(StringType), Some("When paginating forwards, the cursor to continue."), resolve = _.value.endCursor.map(_.value.toString)) // fixme: is this the correct way to handle IntGCValues?
      )
    )

  val CursorPrefix = "arrayconnection:"

  def empty[T] = DefaultIdBasedConnection(PageInfo.empty, Vector.empty[Edge[T]], ConnectionParentElement(None, None, QueryArguments.empty))
}

case class SliceInfo(sliceStart: Int, size: Int)

case class IdBasedConnectionDefinition[Ctx, Conn, Val](edgeType: OutputType[Option[Edge[Val]]], connectionType: ObjectType[Ctx, Conn])

case class DefaultIdBasedConnection[T](pageInfo: PageInfo, edges: Seq[Edge[T]], parent: ConnectionParentElement) extends IdBasedConnection[T]

trait Edge[T] {
  def node: T
  def cursor: IdGCValue
}

object Edge {
  def apply[T](node: T, cursor: IdGCValue) = DefaultEdge(node, cursor)
}

case class DefaultEdge[T](node: T, cursor: IdGCValue) extends Edge[T]

case class PageInfo(hasNextPage: Boolean = false, hasPreviousPage: Boolean = false, startCursor: Option[IdGCValue] = None, endCursor: Option[IdGCValue] = None)

object PageInfo {
  def empty = PageInfo()
}

@implicitNotFound(
  "Type ${T} can't be used as a IdBasedConnection. Please consider defining implicit instance of sangria.relay.IdBasedConnectionLike for type ${T} or extending IdBasedConnection trait.")
trait IdBasedConnectionLike[T[_], E] {
  def pageInfo(conn: T[E]): PageInfo
  def edges(conn: T[E]): Seq[Edge[E]]
}

object IdBasedConnectionLike {
  private object IdBasedConnectionIsIdBasedConnectionLike$ extends IdBasedConnectionLike[IdBasedConnection, Any] {
    override def pageInfo(conn: IdBasedConnection[Any]) = conn.pageInfo
    override def edges(conn: IdBasedConnection[Any])    = conn.edges
  }

  implicit def connectionIsConnectionLike[E, T[_]]: IdBasedConnectionLike[T, E] =
    IdBasedConnectionIsIdBasedConnectionLike$.asInstanceOf[IdBasedConnectionLike[T, E]]
}

case class IdBasedConnectionArgs(before: Option[String] = None, after: Option[String] = None, first: Option[Int] = None, last: Option[Int] = None)

object IdBasedConnectionArgs {
  def apply(args: WithArguments): IdBasedConnectionArgs =
    IdBasedConnectionArgs(args arg IdBasedConnection.Args.Before,
                          args arg IdBasedConnection.Args.After,
                          args arg IdBasedConnection.Args.First,
                          args arg IdBasedConnection.Args.Last)

  val empty = IdBasedConnectionArgs()
}
