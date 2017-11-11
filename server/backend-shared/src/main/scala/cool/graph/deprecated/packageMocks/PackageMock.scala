package cool.graph.deprecated.packageMocks

import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.RequestPipelineOperation.RequestPipelineOperation
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models.{Model, Project}

import scala.util.Try

case class InstallConfiguration(
    namesForFields: Map[InterfaceField, String],
    modelsForInterfaces: Map[Interface, String],
    namesForInterfaces: Map[Interface, String],
    urlsForFunctions: Map[ServerlessFunction, String],
    project: Project,
    pat: String
) {

  def fieldNameFor(field: InterfaceField): Option[String] = namesForFields.get(field)

  def modelForInterface(interface: Interface): Model = project.getModelByName_!(modelsForInterfaces(interface))

  def nameForInterface(interface: Interface): Option[String] = namesForInterfaces.get(interface)

  def urlForFunction(fn: ServerlessFunction): String = urlsForFunctions(fn)
}

/**
  * PACKAGE
  */
trait Package {
  def name: String
  def version: String
  def interfaces: List[Interface]
  def functions: List[Function]

  def install(config: InstallConfiguration): InstalledPackage = {
    InstalledPackage(
      originalPackage = Some(this),
      interfaces = interfaces.map { interface =>
        interface.install(config)
      },
      functions = functions.map { function =>
        function.install(config)
      }
    )
  }
}

case class Interface(defaultName: String, fields: List[InterfaceField]) {
  def install(config: InstallConfiguration): AppliedInterface = {
    val installedFields = fields.map { field =>
      val fieldName: Option[String] = config.fieldNameFor(field)
      field.install(name = fieldName)
    }
    AppliedInterface(
      name = config.namesForInterfaces.getOrElse(this, defaultName),
      model = config.modelForInterface(this),
      originalInterface = Some(this),
      fields = installedFields
    )
  }
}

case class InterfaceField(defaultName: String,
                          typeIdentifier: TypeIdentifier,
                          description: String,
                          isList: Boolean = false,
                          isUnique: Boolean = false,
                          isRequired: Boolean = false,
                          defaultValue: Option[String] = None) {

  def install(name: Option[String] = None): AppliedInterfaceField = {
    AppliedInterfaceField(name.getOrElse(defaultName), this)
  }
}

sealed trait Function {
  def name: String
  def binding: FunctionBinding
  def input: List[InterfaceField]
  def output: List[InterfaceField]

  def install(config: InstallConfiguration): AppliedFunction
}
case class InlineFunction(script: String, name: String, binding: FunctionBinding, input: List[InterfaceField], output: List[InterfaceField]) extends Function {

  def install(config: InstallConfiguration): AppliedInlineFunction = {
    AppliedInlineFunction(
      script = script,
      binding = binding,
      name = name,
      input = input.map(field => field.install(config.fieldNameFor(field))),
      output = output.map(field => field.install(config.fieldNameFor(field))),
      pat = config.pat
    )
  }
}

case class ServerlessFunction(name: String, binding: FunctionBinding, input: List[InterfaceField], output: List[InterfaceField]) extends Function {
  def install(config: InstallConfiguration): AppliedServerlessFunction = {
    AppliedServerlessFunction(
      url = config.urlsForFunctions(this),
      binding = binding,
      name = name,
      input = input.map(field => field.install(config.fieldNameFor(field))),
      output = output.map(field => field.install(config.fieldNameFor(field))),
      pat = config.pat
    )
  }
}

/**
  * INSTALLED PACKAGE
  */
case class InstalledPackage(originalPackage: Option[Package], interfaces: List[AppliedInterface], functions: List[AppliedFunction]) {

  def function(binding: FunctionBinding): List[AppliedFunction] = functions.filter(_.binding == binding)

  def interfacesFor(model: Model): List[AppliedInterface] = interfaces.filter(_.model.name == model.name)
}

case class AppliedInterface(name: String, model: Model, originalInterface: Option[Interface], fields: List[AppliedInterfaceField])

case class AppliedInterfaceField(name: String, originalInterfaceField: InterfaceField) {

  def typeIdentifier: TypeIdentifier = originalInterfaceField.typeIdentifier
  def description: String            = originalInterfaceField.description
  def isUnique: Boolean              = originalInterfaceField.isUnique
  def isRequired: Boolean            = originalInterfaceField.isRequired
  def defaultValue: Option[String]   = originalInterfaceField.defaultValue
  def isList: Boolean                = originalInterfaceField.isList
}

sealed trait AppliedFunction {
  def name: String
  def binding: FunctionBinding
  def input: List[AppliedInterfaceField]
  def output: List[AppliedInterfaceField]
  def pat: String
  def context: Map[String, Any]
}

case class AppliedInlineFunction(script: String,
                                 name: String,
                                 binding: FunctionBinding,
                                 input: List[AppliedInterfaceField],
                                 output: List[AppliedInterfaceField],
                                 pat: String,
                                 context: Map[String, Any] = Map())
    extends AppliedFunction

case class AppliedServerlessFunction(url: String,
                                     name: String,
                                     binding: FunctionBinding,
                                     input: List[AppliedInterfaceField],
                                     output: List[AppliedInterfaceField],
                                     pat: String,
                                     context: Map[String, Any] = Map(),
                                     requestPipelineModelId: Option[String] = None,
                                     requestPipelineOperation: Option[RequestPipelineOperation] = None,
                                     headers: Seq[(String, String)] = Seq.empty,
                                     id: Option[String] = None)
    extends AppliedFunction

object PackageMock {
  def getInstalledPackagesForProject(project: Project): List[InstalledPackage] = {

    def facebookMockConfig(pat: String) = InstallConfiguration(
      namesForFields = Map(
        FacebookAuthProvider.facebookUserIdField -> "facebookUserId"
      ),
      modelsForInterfaces = Map(
        FacebookAuthProvider.facebookUserInterface -> "User"
      ),
      namesForInterfaces = Map(
        FacebookAuthProvider.facebookUserInterface -> "FacebookUser"
      ),
      urlsForFunctions = Map(
        FacebookAuthProvider.authLambda -> "https://cmwww7ara1.execute-api.eu-west-1.amazonaws.com/dev/facebook-auth-provider/authenticateFacebookUser"
      ),
      project,
      pat
    )

    Try(
      project.id match {
        // soren - test project
        case "cj09q7rok00hmxt00j4gteslw" =>
          List(FacebookAuthProvider.install(facebookMockConfig(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0ODk2NzUwODYsImNsaWVudElkIjoiY2lubThhOHJuMDAwMmZpcWNvMDJkMWNlOSIsInByb2plY3RJZCI6ImNqMDlxN3JvazAwaG14dDAwajRndGVzbHciLCJwZXJtYW5lbnRBdXRoVG9rZW5JZCI6ImNqMGNpMzE5NTA0YXdwaTAwNGRpZThmdzYifQ.gNSw0X43JrQaDFSx9lCZ4L6ppIt8JYxtMRqnT7FviF0")))
        // Mvp Space - LingoBites
        case "ciyx06u900lk8016093sfx201" =>
          List(FacebookAuthProvider.install(facebookMockConfig(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0ODk3ODA1MjAsImNsaWVudElkIjoiY2l6anh5ZG5tdnVibzAxNzJpOWxiM3ozaSIsInByb2plY3RJZCI6ImNpeXgwNnU5MDBsazgwMTYwOTNzZngyMDEiLCJwZXJtYW5lbnRBdXRoVG9rZW5JZCI6ImNqMGU4dXVyMTAzcW8wMTE2cGsybGQ0MnEifQ.VSBfHSQvtO8ttR9hN6J99BmOzx3ENS4jKwy91v4GCgc")))
        // Martin Adams - LifePurposeApp
        case "ciy0lc7u302ov0119p56aari0" =>
          List(FacebookAuthProvider.install(facebookMockConfig(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0ODk3OTQ4ODIsImNsaWVudElkIjoiY2l4dXAzZzJwMG5lMzAxMThvdTI0d2s1ZyIsInByb2plY3RJZCI6ImNpeTBsYzd1MzAyb3YwMTE5cDU2YWFyaTAiLCJwZXJtYW5lbnRBdXRoVG9rZW5JZCI6ImNqMGVoZW9jNDAxd2QwMTQycnp6NzkzMGkifQ.z4Ba5hm5rgpnGqu1SNAiDSeOJ_YkTDE-6aMe4ioRPWs")))
        // Jimmy Chan - Wallo
        case "cizpelivr0y2u0175qqj4cxth" =>
          List(FacebookAuthProvider.install(facebookMockConfig(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0OTIyNzc2OTIsImNsaWVudElkIjoiY2l6cGQ5eTNlMGE0YzAxNzVsejgyd3hveCIsInByb2plY3RJZCI6ImNpenBlbGl2cjB5MnUwMTc1cXFqNGN4dGgiLCJwZXJtYW5lbnRBdXRoVG9rZW5JZCI6ImNqMWpqbHdxZTJjMHkwMTY5eHMwdzY2N2IifQ.pRyPDNOn3TBy_8XClIbodASmgf2H2dcOfuH2zkz6k1w")))
        case "cizpel9if0xqa0175hyme165a" =>
          List(FacebookAuthProvider.install(facebookMockConfig(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0OTIyNzc3NDksImNsaWVudElkIjoiY2l6cGQ5eTNlMGE0YzAxNzVsejgyd3hveCIsInByb2plY3RJZCI6ImNpenBlbDlpZjB4cWEwMTc1aHltZTE2NWEiLCJwZXJtYW5lbnRBdXRoVG9rZW5JZCI6ImNqMWpqbjRsbDJkYWQwMTY5dGx5NnB5MzYifQ.3Xh-ouEMxLxOv8gFYQY9wu0sqWxoUXrXDZnaVokgfhk")))
        case "cizpekk9o0x9x01734fs3zv90" =>
          List(FacebookAuthProvider.install(facebookMockConfig(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0OTIyNzc4MzYsImNsaWVudElkIjoiY2l6cGQ5eTNlMGE0YzAxNzVsejgyd3hveCIsInByb2plY3RJZCI6ImNpenBla2s5bzB4OXgwMTczNGZzM3p2OTAiLCJwZXJtYW5lbnRBdXRoVG9rZW5JZCI6ImNqMWpqcDAxNzJnaWowMTY5b2VvMmlmZjIifQ.fIGXRAL8LaAecolVsbdIAwqWg1gYCkUe9mHPVCkTmKM")))
        case "cj1nbfd430mgb0153mxosooo7" =>
          List(FacebookAuthProvider.install(facebookMockConfig(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0OTI3NjMwNDMsImNsaWVudElkIjoiY2l6cGQ5eTNlMGE0YzAxNzVsejgyd3hveCIsInByb2plY3RJZCI6ImNqMW5iZmQ0MzBtZ2IwMTUzbXhvc29vbzciLCJwZXJtYW5lbnRBdXRoVG9rZW5JZCI6ImNqMXJra254dGFlb2swMTM0MnBkaDc1MGYifQ.ZuHAMWPgmWTRzk9Gd_c9P90SCc9YR1RgBZWAFlm3sEc")))
        case "project-with-facebook" => List(FacebookAuthProvider.install(facebookMockConfig("")))
        case _                       => List()
      }
    ).getOrElse(List.empty)
  }

}
