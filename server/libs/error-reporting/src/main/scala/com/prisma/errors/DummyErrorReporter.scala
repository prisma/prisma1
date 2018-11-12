package com.prisma.errors

object DummyErrorReporter extends ErrorReporter {
  override def report(t: Throwable, meta: ErrorMetadata*): Unit = {}
}
