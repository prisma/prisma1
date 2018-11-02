package com.prisma.deploy.migration.validation

import sangria.ast.Document
import sangria.parser.{QueryParser, SyntaxError}

import scala.util.Try

/**
  * Parses SDL schema files.
  * Accepts empty schemas
  */
object GraphQlSdlParser {
  def parse(schema: String): Try[Document] = {
    QueryParser.parse(schema) recover {
      case e: SyntaxError if e.getMessage().contains("Unexpected end of input") => Document(Vector.empty)
    }
  }
}
