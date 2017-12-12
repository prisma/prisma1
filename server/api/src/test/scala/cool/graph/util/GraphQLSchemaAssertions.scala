package cool.graph.util

object GraphQLSchemaAssertions extends GraphQLSchemaAssertions

trait GraphQLSchemaAssertions {
  implicit class SchemaAssertions(schemaString: String) {
    val mutationStart = "type Mutation {"
    val objectEnd     = "}"

    def mustContainMutation(name: String): String = {
      val mutationDef = mutationDefinition()
      val mutationField = mutationDef.lines.map(_.trim).find { line =>
        line.startsWith(name)
      }
      mutationField match {
        case Some(field) => field
        case None        => sys.error(s"Could not find the mutation field $name in this mutation definition: $mutationDef")
      }
    }

    def mustContainInputType(name: String): String = definition(s"input $name {")

    def mutationDefinition(): String = definition(mutationStart)

    private def definition(start: String): String = {
      val startOfDefinition = schemaString.lines.dropWhile(_ != start)
      if (startOfDefinition.isEmpty) {
        sys.error(s"The schema did not contain the definition [$start] in the schema: $schemaString")
      }

      val definitionWithOutClosingBrace = startOfDefinition.takeWhile(_ != objectEnd).mkString(start = "", sep = "\n", end = "\n")
      definitionWithOutClosingBrace + objectEnd
    }
  }
}
