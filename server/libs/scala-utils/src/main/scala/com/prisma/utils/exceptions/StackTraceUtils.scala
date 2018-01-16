package com.prisma.utils.exceptions

import java.io.StringWriter
import java.io.PrintWriter

object StackTraceUtils {
  def print(err: Throwable): String = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)

    err.printStackTrace(pw)
    sw.toString
  }
}
