import sbt.Keys._
import sbt._

import scala.sys.process.ProcessLogger

trait PrismaGraalPluginKeys {
  val nativeImageOptions = SettingKey[Seq[String]]("graalvm-options", "GraalVM native-image options")
  val excludeJars        = SettingKey[Seq[String]](label = "excludeJars", description = "Explicitly exclude dependencies from classpath, checked with .contains()")
}

object PrismaGraalPlugin extends AutoPlugin {
  object autoImport extends PrismaGraalPluginKeys {
    val PrismaNativeImage: Configuration = config("prisma-native-image")
  }

  private val GraalVMNativeImageCommand = "native-image"
  val logger = ProcessLogger(
    (out: String) => println("[StdOut] " + out),
    (err: String) => println("[StdErr] " + err)
  )

  import autoImport._

  override def projectConfigurations: Seq[Configuration] = Seq(PrismaNativeImage)

  override lazy val projectSettings = Seq(
    target in PrismaNativeImage := target.value / "prisma-native-image",
    nativeImageOptions := Seq.empty,
    excludeJars := Seq.empty[String],
    packageBin in PrismaNativeImage := {
      val targetDirectory = (target in PrismaNativeImage).value
      targetDirectory.mkdirs()
      val binaryName = name.value
      val command = {
        val nativeImageArguments = {
          val className     = (mainClass in Compile).value.getOrElse(sys.error("Could not find a main class."))
          val classpathJars = Seq((packageBin in Compile).value) ++ (dependencyClasspath in Compile).value.map(_.data)
          val exclude       = excludeJars.value
          val classpath = classpathJars
            .filter((jar: File) => {
              val jarPath  = jar.getAbsolutePath
              val excluded = exclude.exists(e => jarPath.contains(e))
              if (excluded) {
                println(s"Excluded: $jarPath")
              }

              !excluded
            })
            .mkString(":")
          val extraOptions = nativeImageOptions.value
          Seq("--class-path", classpath, s"-H:Name=$binaryName") ++ extraOptions ++ Seq(className)
        }

        val fullCommand = Seq(GraalVMNativeImageCommand) ++ nativeImageArguments
        println(s"Invoking: ${fullCommand.mkString(" ")}")
        fullCommand
      }

      sys.process.Process(command, targetDirectory) ! logger match {
        case 0 => targetDirectory / binaryName
        case x => sys.error(s"Failed to run $GraalVMNativeImageCommand, exit status: " + x)
      }
    }
  )
}
