package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.models._
import cool.graph.system.database.SystemFields
import scaldi.Injectable

object SignupCustomerMutation extends Injectable {
  def generateUserModel = {
    Model(
      id = Cuid.createCuid(),
      name = "User",
      isSystem = true,
      fields = List()
    )
  }

  def generateUserFields = {
    SystemFields.generateAll
  }

  def generateFileModel = {
    Model(
      id = Cuid.createCuid(),
      name = "File",
      isSystem = true,
      fields = List()
    )
  }

  def generateFileFields = {
    SystemFields.generateAll ++
      List(
        Field(
          id = Cuid.createCuid(),
          name = "secret",
          typeIdentifier = TypeIdentifier.String,
          isRequired = true,
          isList = false,
          isUnique = true,
          isSystem = true,
          isReadonly = true
        ),
        Field(
          id = Cuid.createCuid(),
          name = "url",
          typeIdentifier = TypeIdentifier.String,
          isRequired = true,
          isList = false,
          isUnique = true,
          isSystem = true,
          isReadonly = true
        ),
        Field(
          id = Cuid.createCuid(),
          name = "name",
          typeIdentifier = TypeIdentifier.String,
          isRequired = true,
          isList = false,
          isUnique = false,
          isSystem = true,
          isReadonly = false
        ),
        Field(
          id = Cuid.createCuid(),
          name = "contentType",
          typeIdentifier = TypeIdentifier.String,
          isRequired = true,
          isList = false,
          isUnique = false,
          isSystem = true,
          isReadonly = true
        ),
        Field(
          id = Cuid.createCuid(),
          name = "size",
          typeIdentifier = TypeIdentifier.Int,
          isRequired = true,
          isList = false,
          isUnique = false,
          isSystem = true,
          isReadonly = true
        )
      )
  }

  def generateExampleProject(projectDatabase: ProjectDatabase) = {
    Project(
      id = Cuid.createCuid(),
      name = "Example Project",
      ownerId = "just-a-temporary-dummy-gets-set-to-real-client-id-later",
      models = List.empty,
      projectDatabase = projectDatabase
    )
  }
}
