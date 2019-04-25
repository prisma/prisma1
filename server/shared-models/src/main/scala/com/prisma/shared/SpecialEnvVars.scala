package com.prisma.shared

object SpecialEnvVars {
  val lowerCasedTableNames: Boolean = sys.env.get("LOWERCASED_TABLE_NAMES").contains("1")
}
