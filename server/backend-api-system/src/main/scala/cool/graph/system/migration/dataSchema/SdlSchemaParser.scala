package cool.graph.system.migration.dataSchema

import sangria.ast.Document
import sangria.parser.{QueryParser, SyntaxError}

import scala.util.Try

/**
  * Parses SDL schema files.
  * Accepts empty schemas
  */
object SdlSchemaParser {
  def parse(schema: String): Try[Document] = {
    QueryParser.parse(schema) recover {
      case e: SyntaxError if e.getMessage().contains("Unexpected end of input") => Document(Vector.empty)
    }
  }
}
