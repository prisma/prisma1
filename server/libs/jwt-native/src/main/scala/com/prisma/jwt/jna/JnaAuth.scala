package com.prisma.jwt.jna

import com.prisma.jwt.Algorithm.Algorithm
import com.prisma.jwt.{Auth, AuthFailure, Grant, JwtGrant}
import com.sun.jna.Native
import org.joda.time.{DateTime, DateTimeZone}

import scala.util.Try

object JnaAuth {
  val library: JnaRustBridge = {
    System.setProperty("jna.debug_load.jna", "true")
    System.setProperty("jna.debug_load", "true")
    Native.loadLibrary("jwt_native", classOf[JnaRustBridge])
  }

  // No expiration value for the native code
  val NO_EXP: Int = -1

  def apply(algorithm: Algorithm): JnaAuth = new JnaAuth(algorithm)
}

case class JnaAuth(algorithm: Algorithm) extends Auth {
  import JnaAuth._

  // expirationOffset is the offset in seconds to the current timestamp. None is no expiration at all.
  def createToken(secret: String, expirationOffset: Option[Int]): Try[String] = Try {
    val buffer = library.create_token(
      algorithm.toString,
      secret,
      expirationOffset
        .map { e =>
          DateTime.now(DateTimeZone.UTC).plusSeconds(e).getMillis / 1000
        }
        .getOrElse(NO_EXP)
    )

    debug(buffer)
    throwOnError(buffer)

    val dataString = buffer.data.getString(0)

    library.destroy_buffer(buffer)
    dataString
  }

  override def verifyToken(token: String, secrets: Vector[String]): Try[Unit] = Try {
    val nativeArray = JnaUtils.copyToNativeStringArray(secrets)
    val buffer      = library.verify_token(token, nativeArray, secrets.length, null)

    debug(buffer)
    throwOnError(buffer)

    if (buffer.data_len.intValue() > 1) {
      throw AuthFailure(s"Boolean with size ${buffer.data_len.intValue()} found.")
    }

    val failed = buffer.data.getByteArray(0, 1).head == 0
    library.destroy_buffer(buffer)

    if (failed) {
      // Only here as a safeguard, it is not expected to happen. If this ever pops up the rust impl needs to be checked again.
      throw AuthFailure(s"Verification failed.")
    }
  }

  override def verifyTokenGrant(expectedGrant: JwtGrant, token: String, secrets: Vector[String]): Try[Unit] = Try {
    val jnaGrant    = new JnaJwtGrant(expectedGrant.target, expectedGrant.grant)
    val nativeArray = JnaUtils.copyToNativeStringArray(secrets)
    val buffer      = library.verify_token(token, nativeArray, secrets.length, null)

    debug(buffer)
    throwOnError(buffer)

    if (buffer.data_len.intValue() > 1) {
      throw AuthFailure(s"Boolean with size ${buffer.data_len.intValue()} found.")
    }

    val failed = buffer.data.getByteArray(0, 1).head == 0
    library.destroy_buffer(buffer)

    if (failed) {
      // Only here as a safeguard, it is not expected to happen. If this ever pops up the rust impl needs to be checked again.
      throw AuthFailure(s"Verification failed.")
    }
  }

  private def throwOnError(buffer: ProtocolBufferJna.ByReference): Unit = {
    if (buffer.error != null) {
      val errorString = buffer.error.getString(0)
      library.destroy_buffer(buffer)

      throw AuthFailure(errorString.capitalize)
    }
  }

  private def debug(buffer: ProtocolBufferJna): Unit = {
    println(s"[JNA][Debug] $buffer")
  }
}
