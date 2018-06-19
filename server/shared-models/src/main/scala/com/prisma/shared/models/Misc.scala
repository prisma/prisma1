package com.prisma.shared.models

object IdType {
  type Id = String
}

object ModelMutationType extends Enumeration {
  type ModelMutationType = Value
  val Created = Value("CREATED")
  val Updated = Value("UPDATED")
  val Deleted = Value("DELETED")
}
