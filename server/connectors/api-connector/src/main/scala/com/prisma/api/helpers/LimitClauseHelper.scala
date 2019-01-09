package com.prisma.api.helpers

import com.prisma.api.connector.QueryArguments
import com.prisma.api.schema.APIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}

case class SkipAndLimit(skip: Int, limit: Option[Int])

object LimitClauseHelper {

  def skipAndLimitValues(args: QueryArguments): SkipAndLimit = {
    val (firstOpt, lastOpt, skipOpt) = (args.first, args.last, args.skip)
    validate(args)
    lastOpt.orElse(firstOpt) match {
      case Some(limitedCount) => SkipAndLimit(skipOpt.getOrElse(0), Some(limitedCount + 1))
      case None               => SkipAndLimit(skipOpt.getOrElse(0), None)
    }
  }

  def validate(args: QueryArguments): Unit = {
    throwIfBelowZero(args.first, InvalidFirstArgument())
    throwIfBelowZero(args.last, InvalidLastArgument())
    throwIfBelowZero(args.skip, InvalidSkipArgument())
  }

  private def throwIfBelowZero(opt: Option[Int], exception: Exception): Unit = {
    if (opt.exists(_ < 0)) throw exception
  }
}
