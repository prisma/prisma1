package com.prisma.deploy.connector.postgres.database

import com.prisma.deploy.connector.jdbc.database.TypeMapper
import com.prisma.gc_values.GCValue
import com.prisma.shared.models.TypeIdentifier
import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import org.jooq.DSLContext

case class PostgresTypeMapper() extends TypeMapper {
  override def rawSQLFromParts(
      name: String,
      isRequired: Boolean,
      typeIdentifier: TypeIdentifier,
      isAutoGeneratedByDb: Boolean = false
  )(implicit dsl: DSLContext): String = {
    val n        = esc(name)
    val nullable = if (isRequired) "NOT NULL" else "NULL"
    val ty       = rawSqlTypeForScalarTypeIdentifier(typeIdentifier)

    s"$n $ty $nullable"
  }

  override def rawSqlTypeForScalarTypeIdentifier(t: TypeIdentifier.TypeIdentifier): String = t match {
    case TypeIdentifier.String   => "text"
    case TypeIdentifier.Boolean  => "boolean"
    case TypeIdentifier.Int      => "int"
    case TypeIdentifier.BigInt   => "b
    case TypeIdentifier.Float    => "Decimal(65,30)"
    case TypeIdentifier.Cuid     => "varchar (25)"
    case TypeIdentifier.Enum     => "text"
    case TypeIdentifier.Json     => "text"
    case TypeIdentifier.DateTime => "timestamp (3)"
    case TypeIdentifier.UUID     => "uuid"
    case _                       => ???
  }

}
