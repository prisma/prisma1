package com.prisma.jwt.jna

import com.prisma.jwt.Algorithm.Algorithm
import com.prisma.jwt.{Auth, AuthFailure, JwtGrant}
import com.sun.jna.Native
import org.joda.time.{DateTime, DateTimeZone}

import scala.util.Try

object JnaAuth {
  val library: JnaRustBridge = {
    System.setProperty("jna.debug_load.jna", "true")
    System.setProperty("jna.debug_load", "true")
    Native.loadLibrary("jwt_native", classOf[JnaRustBridge])
  }

  lazy val init = library.jwt_initialize()

  def apply(algorithm: Algorithm): JnaAuth = {
    init
    new JnaAuth(algorithm)
  }
}

case class JnaAuth(algorithm: Algorithm) extends Auth {
  import JnaAuth._

  // expirationOffset is the offset in seconds to the current timestamp. None is no expiration at all (todo: edge case: -1).
  def createToken(secret: String, expirationOffset: Option[Long], grant: Option[JwtGrant]): Try[String] = Try {
    val buffer = library.create_token(
      algorithm.toString,
      secret,
      expirationOffset
        .map(e => DateTime.now(DateTimeZone.UTC).plusSeconds(e.toInt).getMillis / 1000)
        .getOrElse(NO_EXP),
      grant.map(_.target).orNull,
      grant.map(_.action).orNull
    )

    throwOnError(buffer)

    val dataString = buffer.data.getString(0)

    library.destroy_buffer(buffer)
    dataString
  }

  override def verifyToken(token: String, secrets: Vector[String], expectedGrant: Option[JwtGrant]): Try[Unit] = Try {
    if (secrets.nonEmpty) {
      val buffer = library.verify_token(
        token,
        secrets.toArray,
        secrets.length,
        expectedGrant.map(_.target).orNull,
        expectedGrant.map(_.action).orNull
      )

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
  }

  private def throwOnError(buffer: ProtocolBufferJna.ByReference): Unit = {
    if (buffer.error != null) {
      val errorString = buffer.error.getString(0)
      library.destroy_buffer(buffer)

      throw AuthFailure(errorString.capitalize)
    }
  }
}
