package com.prisma.jwt.jna

import com.prisma.jwt.Algorithm.Algorithm
import com.prisma.jwt.{Auth, AuthFailure, JwtGrant}
import com.sun.jna.Native
import org.joda.time.{DateTime, DateTimeZone}

import scala.util.Try

object JnaAuth {
  type GrantType = JnaJwtGrant

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

  // expirationOffset is the offset in seconds to the current timestamp. None is no expiration at all (todo: edge case: -1).
  def createToken(secret: String, expirationOffset: Option[Long], grant: Option[JwtGrant]): Try[String] = Try {
    val jnaGrant = grant.map { g =>
      val jGrant = new JnaJwtGrant.ByReference()
      jGrant.setFrom(g)
      jGrant
    }

    val buffer = library.create_token(
      algorithm.toString,
      secret,
      expirationOffset
        .map { e =>
          DateTime.now(DateTimeZone.UTC).plusSeconds(e.toInt).getMillis / 1000
        }
        .getOrElse(NO_EXP),
      jnaGrant.orNull
    )

    debug(buffer)
    throwOnError(buffer)

    val dataString = buffer.data.getString(0)

    library.destroy_buffer(buffer)
    dataString
  }

  override def verifyToken(token: String, secrets: Vector[String], expectedGrant: Option[JwtGrant]): Try[Unit] = Try {
    val jnaGrant = expectedGrant.map { g =>
      val jGrant = new JnaJwtGrant.ByReference()
      jGrant.setFrom(g)
      jGrant
    }

    val nativeArray = JnaUtils.copyToNativeStringArray(secrets)
    val buffer = library.verify_token(
      token,
      nativeArray,
      secrets.length,
      jnaGrant.orNull
    )

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
