package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector.QueryArguments
import com.prisma.api.schema.APIErrors.{InvalidFirstArgument, InvalidLastArgument, InvalidSkipArgument}

trait LimitClauseBuilder {

  // Increase by 1 to know if we have a next page / previous page
  def limitClauseForWindowFunction(args: Option[QueryArguments]): (Int, Int) = {
    val (firstOpt, lastOpt, skipOpt) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.skip))
    validate(args)

    lastOpt.orElse(firstOpt) match {
      case Some(limitedCount) => (skipOpt.getOrElse(0), limitedCount + skipOpt.getOrElse(0) + 1)
      case None               => (0, 100000000)
    }
  }

  def limitClause(args: Option[QueryArguments]): Option[(Int, Int)] = {
    val (firstOpt, lastOpt, skipOpt) = (args.flatMap(_.first), args.flatMap(_.last), args.flatMap(_.skip))
    validate(args)
    lastOpt.orElse(firstOpt) match {
      case Some(limitedCount) => Some(limitedCount + 1, skipOpt.getOrElse(0))
      case None               => None
    }
  }

  private def validate(args: Option[QueryArguments]): Unit = {
    throwIfBelowZero(args.flatMap(_.first), InvalidFirstArgument())
    throwIfBelowZero(args.flatMap(_.last), InvalidLastArgument())
    throwIfBelowZero(args.flatMap(_.skip), InvalidSkipArgument())
  }

  private def throwIfBelowZero(opt: Option[Int], exception: Exception): Unit = {
    if (opt.exists(_ < 0)) throw exception
  }
}
