package cool.graph.deploy.schema.fields

import sangria.schema.{InputField, StringType}

object ManualMarshallerHelpers {
  val projectIdFields = List(InputField("name", StringType), InputField("stage", StringType))

  implicit class ManualMarshallerHelper(args: Any) {
    val asMap: Map[String, Any] = args.asInstanceOf[Map[String, Any]]

    def clientMutationId: Option[String] = optionalArgAsString("clientMutationId")

    def projectId: String = {
      val name  = requiredArgAsString("name")
      val stage = requiredArgAsString("stage")
      s"$name-$stage"
    }

    def requiredArgAsString(name: String): String         = requiredArgAs[String](name)
    def optionalArgAsString(name: String): Option[String] = optionalArgAs[String](name)

    def requiredArgAsBoolean(name: String): Boolean         = requiredArgAs[Boolean](name)
    def optionalArgAsBoolean(name: String): Option[Boolean] = optionalArgAs[Boolean](name)

    def requiredArgAs[T](name: String): T         = asMap(name).asInstanceOf[T]
    def optionalArgAs[T](name: String): Option[T] = asMap.get(name).flatMap(x => x.asInstanceOf[Option[T]])

    def optionalOptionalArgAsString(name: String): Option[Option[String]] = {

      asMap.get(name) match {
        case None                  => None
        case Some(None)            => Some(None)
        case Some(x: String)       => Some(Some(x))
        case Some(Some(x: String)) => Some(Some(x))
        case x                     => sys.error("OptionalOptionalArgsAsStringFailed" + x.toString)
      }
    }
  }
}
