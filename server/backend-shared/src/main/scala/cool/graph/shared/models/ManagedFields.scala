package cool.graph.shared.models

import cool.graph.shared.models.IntegrationName.IntegrationName
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier

object ManagedFields {
  case class ManagedField(
      defaultName: String,
      typeIdentifier: TypeIdentifier,
      description: Option[String] = None,
      isUnique: Boolean = false,
      isReadonly: Boolean = true
  )

  def apply(authProviderName: IntegrationName): List[ManagedField] = {
    authProviderName match {
      case IntegrationName.AuthProviderEmail  => emailAuthProviderManagedFields
      case IntegrationName.AuthProviderDigits => digisAuthProviderManagedFields
      case IntegrationName.AuthProviderAuth0  => auth0AuthProviderManagedFields
      case _                                  => throw new Exception(s"$authProviderName is not an AuthProvider")
    }
  }

  private lazy val emailAuthProviderManagedFields =
    List(
      ManagedField(defaultName = "email", typeIdentifier = TypeIdentifier.String, isUnique = true, isReadonly = true),
      ManagedField(defaultName = "password", typeIdentifier = TypeIdentifier.String, isReadonly = true)
    )

  private lazy val digisAuthProviderManagedFields = List(
    ManagedField(defaultName = "digitsId", typeIdentifier = TypeIdentifier.String, isUnique = true)
  )

  private lazy val auth0AuthProviderManagedFields = List(auth0UserId)

  lazy val auth0UserId = ManagedField(defaultName = "auth0UserId", typeIdentifier = TypeIdentifier.String, isUnique = true)
}
