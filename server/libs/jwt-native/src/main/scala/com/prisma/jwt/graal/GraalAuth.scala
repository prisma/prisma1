package com.prisma.jwt.graal

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.prisma.jwt.Algorithm.Algorithm
import com.prisma.jwt.{Auth, AuthFailure, JwtGrant}
import org.graalvm.nativeimage.c.`type`.CTypeConversion.CCharPointerPointerHolder
import org.graalvm.nativeimage.c.`type`.{CCharPointer, CTypeConversion}
import org.graalvm.word.WordFactory
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.LoggerFactory

import scala.util.Try
//import scala.util.control.NonFatal

object GraalAuth {
  def initialize = GraalRustBridge.jwt_initialize()
}

case class GraalAuth(algorithm: Algorithm) extends Auth {
  val logger = LoggerFactory.getLogger("prisma")

  def toJavaString(str: CCharPointer)                            = CTypeConversion.toJavaString(str)
  def toCString(str: String): CTypeConversion.CCharPointerHolder = CTypeConversion.toCString(str)

// todo This doesn't work (yet) with the native image compiler (erasure and lambdas are a problem)
// todo However, this is the best code to handle AutoClosable from Scala, so we definitely need it down the road.
//  def withAutoclose[T <: AutoCloseable, V](resources: T*)(f: Seq[T] => V): V = {
//    var exception: Throwable = null // We need to save the (nonfatal) exception if thrown for clean resource closing.
//    try {
//      f(resources)
//    } catch {
//      // Fatal errors will still propagate (eg. out-of-memory errors)
//      case NonFatal(e) =>
//        exception = e
//        throw e
//    } finally {
//      resources.foreach(closeSilently(exception, _))
//    }
//  }
//
//  private def closeSilently(e: Throwable, resource: AutoCloseable): Unit = {
//    if (e != null) {
//      try {
//        resource.close()
//      } catch {
//        // todo trace here
//        case NonFatal(suppressed) => e.addSuppressed(suppressed)
//      }
//    } else {
//      resource.close()
//    }
//  }

  case class NullableCCharPointerHolder(holderOpt: Option[CTypeConversion.CCharPointerHolder]) extends AutoCloseable {
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
    // (See todo's above) On error we're leaking memory here.
    val target = NullableCCharPointerHolder(grant.map(x => toCString(x.target)))
    val action = NullableCCharPointerHolder(grant.map(x => toCString(x.action)))
    val alg    = toCString(algorithm.toString)
    val s      = toCString(secret)

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
      val target = NullableCCharPointerHolder(expectedGrant.map(x => toCString(x.target)))
      val action = NullableCCharPointerHolder(expectedGrant.map(x => toCString(x.action)))
      val tkn    = toCString(token)

      val buffer: CIntegration.ProtocolBuffer = GraalRustBridge.verify_token(
        tkn.get(),
        holder.get(),
        secrets.length,
        target.get(),
        action.get()
      )

      holder.close()
      target.close()
      action.close()
      tkn.close()

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
