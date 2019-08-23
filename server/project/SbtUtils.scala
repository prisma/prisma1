import sbt.Project

object SbtUtils {
  implicit class SyntaxExtensions(proj: Project) {
    def dependsOn(all: List[Project], mode: String = ""): Project = {
      all.foldLeft(proj) { (p, dep) =>
        if (mode.nonEmpty) p.dependsOn(Project.projectToRef(dep) % mode)
        else p.dependsOn(Project.projectToRef(dep))
      }
    }
  }
}
