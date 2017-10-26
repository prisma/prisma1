package cool.graph.util.exceptions

import java.io.{PrintWriter, StringWriter}

object ExceptionStacktraceToString {

  implicit class ThrowableStacktraceExtension(t: Throwable) {
    def stackTraceAsString: String = ExceptionStacktraceToString(t)
  }

  def apply(t: Throwable): String = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    t.printStackTrace(pw)
    sw.toString()
  }
}
