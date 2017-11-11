package cool.graph.system.migration.dataSchema

import cool.graph.system.database.SystemFields

object SystemUtil {
  def isNotSystemField(field: String) = !generalSystemFields.contains(field)

  private val generalSystemFields = SystemFields.generateAll.map(_.name)
}
