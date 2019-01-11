package com.prisma.jwt.graal

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import com.prisma.jwt.Algorithm.Algorithm
import com.prisma.jwt.{Auth, AuthFailure, JwtGrant}
import org.graalvm.nativeimage.c.`type`.CTypeConversion.CCharPointerPointerHolder
import org.graalvm.nativeimage.c.`type`.{CCharPointer, CTypeConversion}
import org.graalvm.word.WordFactory
import org.joda.time.{DateTime, DateTimeZone}

import scala.util.Try

object GraalAuth {
  def initialize = GraalRustBridge.jwt_initialize()
}

case class GraalAuth(algorithm: Algorithm) extends Auth {
  def toJavaString(str: CCharPointer) = CTypeConversion.toJavaString(str)
  def toCString(str: String)          = CTypeConversion.toCString(str).get()

  def strOptToPointer(opt: Option[String]): CCharPointer = {
    // The following code has to be this verbose to fight scala type inference / erasure and native image compiler code analysis.
    if (opt.isDefined) {
      val v: String = opt.get
      toCString(v)
    } else {
      WordFactory.nullPointer[CCharPointer]()
    }
  }

  // expirationOffset is the offset in seconds to the current timestamp. None is no expiration at all (todo: edge case: -1).
  def createToken(secret: String, expirationOffset: Option[Long], grant: Option[JwtGrant]): Try[String] = Try {
    val target: CCharPointer = strOptToPointer(grant.map(_.target))
    val action: CCharPointer = strOptToPointer(grant.map(_.action))
    val buffer: CIntegration.ProtocolBuffer = GraalRustBridge.create_token(
      toCString(algorithm.toString),
      toCString(secret),
      expirationOffset
        .map(e => DateTime.now(DateTimeZone.UTC).plusSeconds(e.toInt).getMillis / 1000)
        .getOrElse(NO_EXP),
      target,
      action
    )

    throwOnError(buffer)

    if (buffer.getDataLen == 0) {
      throw AuthFailure("Native call returned no token")
    }

    val buf: ByteBuffer = CTypeConversion.asByteBuffer(buffer.getData, buffer.getDataLen.toInt - 1) // Cut null terminator
    val dataString      = StandardCharsets.UTF_8.decode(buf).toString

    GraalRustBridge.destroy_buffer(buffer)
    dataString
  }

  override def verifyToken(token: String, secrets: Vector[String], expectedGrant: Option[JwtGrant]): Try[Unit] = Try {
    if (secrets.nonEmpty) {
      val holder = iterableToNativeArray(secrets)
      val target = strOptToPointer(expectedGrant.map(_.target))
      val action = strOptToPointer(expectedGrant.map(_.action))
      val buffer = GraalRustBridge.verify_token(
        toCString(token),
        holder.get(),
        secrets.length,
        target,
        action
      )

      throwOnError(buffer)

      if (buffer.getDataLen > 1) {
        throw AuthFailure(s"Boolean with size ${buffer.getDataLen} found.")
      }

      val failed = buffer.getData.readByte(0) == 0
      GraalRustBridge.destroy_buffer(buffer)

      if (failed) {
        // Only here as a safeguard, it is not expected to happen. If this ever pops up the rust impl needs to be checked again.
        // Why? Because throwOnError should throw whatever error is contained in the result payload. If there's an error, this
        // code shouldn't be reached. todo: Makes the boolean... pointless? Clean up the interface!
        throw AuthFailure(s"Verification failed.")
      }
    }
  }

  private def iterableToNativeArray(iterable: Iterable[String]): CCharPointerPointerHolder = {
    CTypeConversion.toCStrings(iterable.toArray)
  }

  private def throwOnError(buffer: CIntegration.ProtocolBuffer): Unit = {
    if (buffer.getError.isNonNull) {
      val errorString = toJavaString(buffer.getError)
      GraalRustBridge.destroy_buffer(buffer)

      throw AuthFailure(errorString.capitalize)
    }
  }
}
