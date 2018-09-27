import Dependencies._
import sbt._

import scala.sys.process.ProcessLogger

val nativeClasspath = taskKey[String]("The classpath.")
nativeClasspath := {
  val baseDir = baseDirectory.value
  val managedDir = managedDirectory.value
  val packagedFile = Keys.`package`.in(Compile).value.relativeTo(baseDir) // make sure that package is built
  val deps = dependencyClasspath.in(Compile).value.toVector.map { dep ⇒
    dep.data.relativeTo(baseDir).orElse {
      // For some reason sbt sometimes decides to use the scala-library from `~/.sbt/boot` (which is outside of the project dir)
      // As a workaround we copy the file in lib_managed and use the copy instead (shouldn't cause name collisions)
      val inManaged = managedDir / dep.data.name
      IO.copy(Seq(dep.data → inManaged))
      inManaged.relativeTo(baseDir)
    }
  }
  val classpath = (deps :+ packagedFile).flatten

  def relativePath(path: File): String = path.toString.replaceAll("\\\\", "/")

  val classpathStr = classpath.map(relativePath).mkString(":")
  println(classpathStr)
  classpathStr
}