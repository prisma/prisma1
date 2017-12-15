package cool.graph.util

object GraphQLSchemaAssertions extends GraphQLSchemaAssertions

trait GraphQLSchemaAssertions {
  implicit class SchemaAssertions(schemaString: String) {
    val mutationStart = "type Mutation {"
    val queryStart    = "type Query {"
    val objectEnd     = "}"

    def mustContainMutation(name: String): String = mustContainField(definition(mutationStart), name)

    def mustContainQuery(name: String): String = mustContainField(definition(queryStart), name)

    private def mustContainField(typeDef: String, field: String): String = {
      val theField = typeDef.lines.map(_.trim).find { line =>
        line.startsWith(field + "(")
      }
      theField match {
        case Some(field) => field
        case None        => sys.error(s"Could not find the field $field in this definition: $typeDef")
      }
    }

    def mustContainInputType(name: String): String = definition(s"input $name {")

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
