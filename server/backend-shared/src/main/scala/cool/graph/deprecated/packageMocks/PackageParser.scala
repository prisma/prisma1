package cool.graph.deprecated.packageMocks

import cool.graph.shared.TypeInfo
import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.{FunctionBinding, Project, TypeIdentifier}
import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._
import sangria.ast.{Argument, InterfaceTypeDefinition, ObjectTypeDefinition, Value}

object PackageParser {
  case class PackageDefinition(name: String,
                               functions: Map[String, FunctionDefinition],
                               interfaces: Map[String, InterfaceDefinition],
                               install: List[InstallDefinition])
  case class FunctionDefinition(schema: String, `type`: String, url: Option[String])
  case class InterfaceDefinition(schema: String)
  case class InstallDefinition(`type`: String, binding: String, name: Option[String], onType: Option[String])

  object PackageYamlProtocol extends DefaultYamlProtocol {
    implicit val installFormat   = yamlFormat4(InstallDefinition)
    implicit val interfaceFormat = yamlFormat1(InterfaceDefinition)
    implicit val functionFormat  = yamlFormat3(FunctionDefinition)
    implicit val PackageFormat   = yamlFormat4(PackageDefinition)
  }

  def parse(packageDefinition: String): PackageDefinition = {
    import PackageYamlProtocol._

    packageDefinition.parseYaml.convertTo[PackageDefinition]
  }

  def install(packageDefinition: PackageDefinition, project: Project): InstalledPackage = {
    val pat = get(project.rootTokens.find(_.name == packageDefinition.name).map(_.token), s"No PAT called '${packageDefinition.name}'")

    val installedFunctions = packageDefinition.install
      .filter(_.`type` == "mutation")
      .map(f => {
        val boundName                         = f.binding.split('.')(1)
        val boundFunction: FunctionDefinition = packageDefinition.functions(boundName)
        AppliedServerlessFunction(
          url = boundFunction.url.get,
          name = f.name.getOrElse(boundName),
          binding = FunctionBinding.CUSTOM_MUTATION,
          input = fieldsFromInterface(boundFunction.schema, "input"),
          output = fieldsFromInterface(boundFunction.schema, "output"),
          pat = pat,
          context = Map(("onType" -> f.onType.getOrElse("")))
        )
      })

    val installedInterfaces = packageDefinition.install
      .filter(_.`type` == "interface")
      .map(i => {
        val boundName      = i.binding.split('.')(1)
        val name           = i.name.getOrElse(boundName)
        val boundInterface = packageDefinition.interfaces(boundName)
        val onType =
          get(i.onType, s"You have to specify the 'onType' argument to define on what type the interface should be added")
        val model = get(project.models.find(_.name == onType), s"Could not add interface '$name' to type '$onType' as it doesn't exist in your project")

        AppliedInterface(name = name, model = model, originalInterface = None, fieldsFromInterface(boundInterface.schema, boundName))
      })

    InstalledPackage(originalPackage = None, functions = installedFunctions, interfaces = installedInterfaces)
  }

  private def fieldsFromInterface(schema: String, interfaceName: String): List[AppliedInterfaceField] = {

    val ast =
      sangria.parser.QueryParser.parse(schema)
    val definitions = ast.get.definitions
    def interfaceTypeDefinitions = definitions collect {
      case x: InterfaceTypeDefinition => x
    }

    val fields = get(interfaceTypeDefinitions.find(_.name == interfaceName).map(_.fields), s"no interface called '$interfaceName' in schema '$schema'")

    fields
      .map(f => {
        val defaultValue: Option[String] = f.directives
          .find(_.name == "defaultValue")
          .flatMap(_.arguments.find(_.name == "value").map(_.value.renderCompact))
        val typeInfo = TypeInfo.extract(f, None, Seq(), true)
        AppliedInterfaceField(
          name = f.name,
          originalInterfaceField = InterfaceField(
            defaultName = f.name,
            typeIdentifier = typeInfo.typeIdentifier,
            description = "",
            isUnique = typeInfo.isUnique,
            isRequired = typeInfo.isRequired,
            isList = typeInfo.isList,
            defaultValue = defaultValue
          )
        )
      })
      .toList
  }

  private def get[T](option: Option[T], error: String): T = option match {
    case Some(model) => model
    case None =>
      sys.error(error)
  }
}
