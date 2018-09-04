package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.QueryArguments
import com.prisma.api.schema.APIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}

trait LimitClauseBuilder {

  case class SkipAndLimit(skip: Int, limit: Option[Int])

  // Increase by 1 to know if we have a next page / previous page
  def limitClauseForWindowFunction(args: Option[QueryArguments]): (Int, Int) = {
    val (firstOpt, lastOpt, skipOpt) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.skip))
    validate(args)

    lastOpt.orElse(firstOpt) match {
      case Some(limitedCount) => (skipOpt.getOrElse(0) + 1, limitedCount + skipOpt.getOrElse(0) + 1)
      case None               => (skipOpt.getOrElse(0) + 1, 100000000)
    }
  }

  def skipAndLimitValues(args: Option[QueryArguments]): SkipAndLimit = skipAndLimitValues(args.getOrElse(QueryArguments.empty))

  def skipAndLimitValues(args: QueryArguments): SkipAndLimit = {
    val (firstOpt, lastOpt, skipOpt) = (args.first, args.last, args.skip)
    validate(args)
    lastOpt.orElse(firstOpt) match {
      case Some(limitedCount) => SkipAndLimit(skipOpt.getOrElse(0), Some(limitedCount + 1))
      case None               => SkipAndLimit(skipOpt.getOrElse(0), None)
    }
  }

  private def validate(args: Option[QueryArguments]): Unit = args.foreach(validate)

  private def validate(args: QueryArguments): Unit = {
    throwIfBelowZero(args.first, InvalidFirstArgument())
    throwIfBelowZero(args.last, InvalidLastArgument())
    throwIfBelowZero(args.skip, InvalidSkipArgument())
  }

  private def throwIfBelowZero(opt: Option[Int], exception: Exception): Unit = {
    if (opt.exists(_ < 0)) throw exception
  }
}
