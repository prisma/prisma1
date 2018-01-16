package cool.graph.api.mutations

import cool.graph.shared.models.IdType.Id

import scala.language.reflectiveCalls

object MutationTypes {
  case class ArgumentValue(name: String, value: Any) {
    def unwrappedValue: Any = {
      def unwrapSome(x: Any): Any = {
        x match {
          case Some(x) => x
          case x       => x
        }
      }
      unwrapSome(value)
    }
  }

  object ArgumentValueList {
    def getId(args: List[ArgumentValue]): Option[Id] = args.find(_.name == "id").map(_.value.toString)
    def getId_!(args: List[ArgumentValue]): Id       = getId(args).getOrElse(sys.error("Id is missing")) // throw UserAPIErrors.IdIsMissing())

  }
}
