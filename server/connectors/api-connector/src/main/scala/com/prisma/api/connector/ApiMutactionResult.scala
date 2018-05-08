package com.prisma.api.connector

import com.prisma.gc_values.GCValue

sealed trait ApiMutactionResult
sealed trait DatabaseMutactionResult
case class CreateDataItemResult(id: GCValue) extends DatabaseMutactionResult
object UnitDatabaseMutactionResult           extends DatabaseMutactionResult
