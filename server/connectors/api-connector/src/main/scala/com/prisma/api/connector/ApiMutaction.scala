package com.prisma.api.connector

sealed trait ApiMutaction
sealed trait DatabaseMutaction   extends ApiMutaction // by default transactionally?
sealed trait SideEffectMutaction extends ApiMutaction
