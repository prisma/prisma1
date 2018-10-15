package com.prisma.jwt.jna

import com.prisma.jwt.{FailedNativeCallException, NativeBinding}
import com.sun.jna.Native

import scala.util.Try

object JnaBinding extends NativeBinding {
  val library = {
    System.setProperty("jna.debug_load.jna", "true")
    System.setProperty("jna.debug_load", "true")
    Native.loadLibrary("jwt_native", classOf[JnaRustBridge])
  }

  private val NO_EXP = -1

  def createToken(secret: String, expiration: Option[Long]): Try[String] = Try {
    val buffer = library.create_token(secret, expiration.getOrElse(NO_EXP))
    debug(buffer)
    throwOnError(buffer)

    val dataString = buffer.data.getString(0)

    library.destroy_buffer(buffer)
    dataString
  }

  override def verifyToken(token: String, secrets: Vector[String]): Try[Boolean] = Try {
    val nativeArray = JnaUtils.copyToNativeStringArray(secrets)
    val buffer      = library.verify_token(token, nativeArray, secrets.length)

    debug(buffer)
    throwOnError(buffer)

    if (buffer.data_len.intValue() > 1) {
      throw new RuntimeException(s"Boolean with size ${buffer.data_len.intValue()} found.")
    }

    val asBoolean = buffer.data.getByteArray(0, 1).head != 0
    library.destroy_buffer(buffer)

    asBoolean
  }

  private def throwOnError(buffer: ProtocolBufferJna.ByReference): Unit = {
    if (buffer.error != null) {
      val errorString = buffer.error.getString(0)
      library.destroy_buffer(buffer)
      throw FailedNativeCallException(errorString)
    }
  }

  private def debug(buffer: ProtocolBufferJna): Unit = {
    println(s"[JNA][Debug] $buffer")
  }
}
