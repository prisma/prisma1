package util

case class ApiTestDatabase() {
  def setup(project: Project): Unit = {
    // TODO: delegate to migration-engine
    deleteProjectDatabase(project)
    createProjectDatabase(project)
  }

  def truncateProjectTables(project: Project): Unit = {
    // FIXME: implement
  }
  private def deleteProjectDatabase(project: Project): Unit = {
    // FIXME: implement
  }
  private def createProjectDatabase(project: Project) = {
    // FIXME: implement
  }
}
