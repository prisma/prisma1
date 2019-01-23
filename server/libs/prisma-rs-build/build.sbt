import sbt._

val buildNativeLib = TaskKey[Unit]("buildNativeLib", "builds the Prisma native lib")
buildNativeLib := {
  import sys.process._
  println("Building Prisma native lib.")

  val logger = ProcessLogger(println, println)
  val nativePath = new java.io.File("prisma-rs")
  val cargoFlags = sys.env.get("RUST_BACKTRACE").map(_ => "").getOrElse("--release")


  if ((Process(Seq("bash", "build.sh"), nativePath, "CARGO_FLAGS"-> cargoFlags) ! logger) != 0) {
    sys.error("Prisma library build failed.")
  }
}

compile in Compile := {
  buildNativeLib.value
  (compile in Compile).value
}

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
  classpathStr
}