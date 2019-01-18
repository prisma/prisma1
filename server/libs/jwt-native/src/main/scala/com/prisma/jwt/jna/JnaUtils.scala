package com.prisma.jwt.jna

import java.nio.charset.Charset

import com.sun.jna.{Memory, Pointer}

object JnaUtils {
  val charset = Charset.forName("UTF-8")

  // todo This impl is inefficient, as it uses getBytes to get the actual byte sizes, which copies the data.
  // todo We then copy the data AGAIN after determining the size.
  def copyToNativeStringArray(elements: Vector[String]): Pointer = {
    var offset        = 0
    val memorySizes   = elements.map(e => e.getBytes(charset).length)
    val memoryPointer = new Memory(memorySizes.sum + elements.length) // +1's for \0 termination

    for (i <- elements.indices) {
      memoryPointer.setString(offset, elements(i), "UTF-8")

      // null-terminator
      memoryPointer.setMemory(offset + memorySizes(i), 1, 0.toByte)
      offset += memorySizes(i) + 1
    }

    memoryPointer
  }
}
