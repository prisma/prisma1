package util

import java.io.ByteArrayInputStream

object PrismaRsBuild {
  def apply(): Unit = {
    if (!EnvVars.isBuildkite) {
      val workingDirectory = new java.io.File(EnvVars.serverRoot + "/prisma-rs")
      val command          = Seq("cargo", "build", "--release")
      val env              = ("RUST_LOG", "error")
      sys.process.Process(command, workingDirectory, env).!!
    }
  }
}
