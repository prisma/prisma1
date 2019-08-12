package util

import java.io.ByteArrayInputStream

object PrismaRsBuild {
  def apply(): Unit = {
    val workingDirectory = new java.io.File(EnvVars.serverRoot + "/prisma-rs")
    val command          = Seq("cargo", "build", "--release")
    val env              = ("RUST_LOG", "error")
//    val begin = System.currentTimeMillis()
    sys.process.Process(command, workingDirectory, env).!!
//    val end = System.currentTimeMillis()
//    println(s"took ${end - begin}ms")
  }
}
