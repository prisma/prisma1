package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.QueryArguments
import com.prisma.api.helpers.LimitClauseHelper

trait LimitClauseBuilder {
  // Increase by 1 to know if we have a next page / previous page
  def limitClauseForWindowFunction(args: QueryArguments): (Int, Int) = {
    val (firstOpt, lastOpt, skipOpt) = (args.first, args.last, args.skip)
    LimitClauseHelper.validate(args)

    lastOpt.orElse(firstOpt) match {
      case Some(limitedCount) => (skipOpt.getOrElse(0) + 1, limitedCount + skipOpt.getOrElse(0) + 1)
      case None               => (skipOpt.getOrElse(0) + 1, 100000000)
    }
  }
}
