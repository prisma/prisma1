package com.prisma.rs
import com.prisma.gc_values.GCValue

class NativeError(reason: String) extends Exception(reason)

case class ConnectionError(reason: String)              extends NativeError(reason)
case class InvalidInputError(reason: String)            extends NativeError(reason)
case class JsonDecodeError(reason: String)              extends NativeError(reason)
case class NoResultError(reason: String)                extends NativeError(reason)
case class ProtobufDecodeError(reason: String)          extends NativeError(reason)
case class QueryError(reason: String)                   extends NativeError(reason)
case class InvalidConnectionArguments(reason: String)   extends NativeError(reason)
case class UniqueConstraintViolation(fieldName: String) extends NativeError(fieldName)
case class FieldCannotBeNull(fieldName: String)         extends NativeError(fieldName)

case class NodeNotFoundForWhere(modelName: String,
                                fieldName: String,
                                value: GCValue) extends NativeError(fieldName)

case class RelationViolation(relationName: String,
                             modelAName: String,
                             modelBName: String) extends NativeError(relationName)
