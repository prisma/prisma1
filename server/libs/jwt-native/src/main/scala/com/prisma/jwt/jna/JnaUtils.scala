package com.prisma.jwt.jna

import com.sun.jna.{Memory, Native, Pointer}

object JnaUtils {
  def copyToNativeStringArray(elements: Vector[String]): Pointer = {
    var offset        = 0
    val memorySize    = elements.foldLeft(0)((prev, next) => prev + next.getBytes().length + 1) // +1 for \0 termination
    val memoryPointer = new Memory(memorySize * Native.getNativeSize(Character.TYPE))

    for (s <- elements) {
      memoryPointer.setString(offset, s)

      // null-terminator
      memoryPointer.setMemory(offset + s.length, 1, 0.toByte)
      offset += s.length + 1
    }

    memoryPointer // todo understand the memory implications of this object + GC
  }
}
