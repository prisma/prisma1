package com.prisma.rs

class NativeError(reason: String) extends Exception(reason)

case class ConnectionError(reason: String)     extends NativeError(reason)
case class InvalidInputError(reason: String)   extends NativeError(reason)
case class JsonDecodeError(reason: String)     extends NativeError(reason)
case class NoResultError(reason: String)       extends NativeError(reason)
case class ProtobufDecodeError(reason: String) extends NativeError(reason)
case class QueryError(reason: String)          extends NativeError(reason)
