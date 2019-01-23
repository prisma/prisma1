package com.prisma.rs

import com.prisma.rs.jna.JnaRustBridge
import com.sun.jna.Native

object NativeBinding {
  val library: JnaRustBridge = {
    System.setProperty("jna.debug_load.jna", "true")
    System.setProperty("jna.debug_load", "true")
    System.setProperty("jna.library.path", s"${sys.env.getOrElse("SERVER_ROOT", sys.error("SERVER_ROOT env var required but not found"))}/prisma-rs/build")
    Native.loadLibrary("prisma", classOf[JnaRustBridge])
  }

  def select_1(): Int = library.select_1()
}
