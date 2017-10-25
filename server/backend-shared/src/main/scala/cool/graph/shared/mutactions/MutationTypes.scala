package cool.graph.shared.mutactions

import cool.graph.Types.Id
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.Field

import scala.language.reflectiveCalls

object MutationTypes {
  case class ArgumentValue(name: String, value: Any, field: Option[Field] = None) {
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
  object ArgumentValue {
    def apply(name: String, value: Any, field: Field): ArgumentValue = ArgumentValue(name, value, Some(field))
  }

  object ArgumentValueList {
    def getId(args: List[ArgumentValue]): Option[Id] = args.find(_.name == "id").map(_.value.toString)
    def getId_!(args: List[ArgumentValue]): Id       = getId(args).getOrElse(throw UserAPIErrors.IdIsMissing())

  }
}
