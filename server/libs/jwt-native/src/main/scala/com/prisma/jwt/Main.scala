package com.prisma.jwt

import com.sun.jna.{Memory, Native, Pointer}
import org.joda.time.DateTime

object Main extends App {
  def copyToNativeStringArray(elements: Vector[String]): Pointer = {
    var offset        = 0
    val memorySize    = elements.foldLeft(0)((prev, next) => prev + next.getBytes().size + 1) // +1 for \0 termination
    val memoryPointer = new Memory(memorySize * Native.getNativeSize(Character.TYPE))

    for (s <- elements) {
      memoryPointer.setString(offset, s)

      // null-terminator
      memoryPointer.setMemory(offset + s.length, 1, 0.toByte)
      offset += s.length + 1
    }

    memoryPointer // todo when is this released / GC'ed?
  }

  val currentDir = System.getProperty("user.dir")

  System.setProperty("jna.debug_load.jna", "true")
  System.setProperty("jna.debug_load", "true")

  println("---------------------- Creation ----------------------")
  val secrets = Vector("some_secret", "some_other_secret", "another_one")
  val library = Native.loadLibrary("jwt_native", classOf[JnaRustBridge])
  val withExp = library.create_token("some_secret", DateTime.now().plusHours(1).getMillis / 1000)
  val noExp   = library.create_token("some_secret", -1)

  println(withExp)
  println(noExp)

  // Possibility #1
  println("1: " + withExp.data.getString(0))
  println("2: " + noExp.data.getString(0))

  // Possibility #2
//  val bytes = withExp.data.getByteArray(0, withExp.len.intValue())

  val tokenWithExp    = withExp.data.getString(0)
  val tokenWithoutExp = noExp.data.getString(0)

  library.destroy_buffer(withExp)
  library.destroy_buffer(noExp)

  println("---------------------- Validation ----------------------")

  val mem           = copyToNativeStringArray(secrets)
  val verify1Result = library.verify_token(tokenWithExp, mem, secrets.length)

  println(verify1Result)
  assert(verify1Result.success)

  library.destroy_buffer(verify1Result)

  val verify2Result = library.verify_token(tokenWithoutExp, mem, secrets.length)

  println(verify2Result)
  if (!verify2Result.success) {
    println(verify2Result.data.getString(0))
  }
  assert(verify2Result.success)

  library.destroy_buffer(verify2Result)
}
