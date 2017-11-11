package cool.graph.deprecated.packageMocks

import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.{FunctionBinding, TypeIdentifier}

object FacebookAuthProvider extends Package {

  val name    = "TheAmazingFacebookAuthProvider"
  val version = ("0." * 20) + "1-SNAPSHOT"

  /*
  interface FacebookUser {
    facebookUserId: String
    isVerified: Boolean! @default(value="true")
  }
   */

  lazy val interfaces = List(facebookUserInterface)

  lazy val facebookUserInterface = {
    Interface("FacebookUser", List(facebookUserIdField, isVerifiedField))
  }

  lazy val facebookUserIdField =
    InterfaceField("facebookUserId", TypeIdentifier.String, "The id Facebook uses to identify the user", isUnique = true, isRequired = false)

  lazy val isVerifiedField = InterfaceField("isVerified",
                                            TypeIdentifier.Boolean,
                                            "Is true if the users identity has been verified",
                                            isUnique = false,
                                            isRequired = true,
                                            defaultValue = Some("true"))

  val authLambda = ServerlessFunction(
    name = "authenticateFacebookUser",
    input = List(InterfaceField("fbToken", TypeIdentifier.String, "", isUnique = false, isRequired = true)),
    output = List(InterfaceField("token", TypeIdentifier.String, "", isUnique = false, isRequired = true)),
    binding = FunctionBinding.CUSTOM_MUTATION
  )

  def functions = List(authLambda)
}
