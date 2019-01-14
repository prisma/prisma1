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
  def toJavaString(str: CCharPointer)                            = CTypeConversion.toJavaString(str)
  def toCString(str: String): CTypeConversion.CCharPointerHolder = CTypeConversion.toCString(str)

//  def strOptToPointer(opt: Option[String]) = {
//    // The following code has to be this verbose to fight scala type inference / erasure and native image compiler code analysis.
//    if (opt.isDefined) {
//      val v: String = opt.get
//      toCString(v)
//    } else {
//      WordFactory.nullPointer[CCharPointer]()
//    }
//  }

  case class DeferredCCharPointerHolderClosable(holderOpt: Option[CTypeConversion.CCharPointerHolder]) {
    def get(): CCharPointer = {
      // The following code has to be this verbose to fight scala type inference / erasure not fully working with the native image compiler code analysis.
      if (holderOpt.isDefined) {
        holderOpt.get.get()
      } else {
        WordFactory.nullPointer[CCharPointer]()
      }
    }

    def close(): Unit = holderOpt.foreach(_.close())
  }

  // expirationOffset is the offset in seconds to the current timestamp. None is no expiration at all (todo: edge case: -1).
  def createToken(secret: String, expirationOffset: Option[Long], grant: Option[JwtGrant]): Try[String] = Try {
    val target = DeferredCCharPointerHolderClosable(grant.map(x => toCString(x.target)))
    val action = DeferredCCharPointerHolderClosable(grant.map(x => toCString(x.action)))

    val alg = toCString(algorithm.toString)
    val s   = toCString(secret)
    val buffer: CIntegration.ProtocolBuffer = GraalRustBridge.create_token(
      alg.get(),
      s.get(),
      expirationOffset
        .map(e => DateTime.now(DateTimeZone.UTC).plusSeconds(e.toInt).getMillis / 1000)
        .getOrElse(NO_EXP),
      target.get(),
      action.get()
    )

    target.close()
    action.close()
    alg.close()
    s.close()
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
      val target = DeferredCCharPointerHolderClosable(expectedGrant.map(x => toCString(x.target)))
      val action = DeferredCCharPointerHolderClosable(expectedGrant.map(x => toCString(x.action)))
      val tkn    = toCString(token)
      val buffer = GraalRustBridge.verify_token(
        tkn.get(),
        holder.get(),
        secrets.length,
        target.get(),
        action.get()
      )

      target.close()
      action.close()
      tkn.close()
      holder.close()
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
