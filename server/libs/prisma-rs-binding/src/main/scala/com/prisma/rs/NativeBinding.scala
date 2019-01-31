package com.prisma.rs

import com.prisma.rs.jna.JnaRustBridge
import com.sun.jna.{Memory, Native, Pointer}
import prisma.getNodeByWhere.GetNodeByWhere
import scalapb.GeneratedMessage

object NativeBinding {
  val library: JnaRustBridge = {
    System.setProperty("jna.debug_load.jna", "true")
    System.setProperty("jna.debug_load", "true")
    System.setProperty("jna.library.path", s"${sys.env.getOrElse("SERVER_ROOT", sys.error("SERVER_ROOT env var required but not found"))}/prisma-rs/build")
    Native.loadLibrary("prisma", classOf[JnaRustBridge])
  }

  def select_1(): Int = library.select_1()

  def get_node_by_where(getNodeByWhere: GetNodeByWhere): Unit = {
    val (pointer, length) = writeBuffer(getNodeByWhere)
    val callResult        = library.get_node_by_where(pointer, length)

  }

  def writeBuffer[T](msg: GeneratedMessage): (Pointer, Int) = {
    val length       = msg.serializedSize
    val serialized   = msg.toByteArray
    val nativeMemory = new Memory(length)

    nativeMemory.write(0, serialized, 0, length)
    (nativeMemory, length)
  }

//  def destroy()
}
