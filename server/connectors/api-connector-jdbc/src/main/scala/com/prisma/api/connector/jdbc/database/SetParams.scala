package com.prisma.api.connector.jdbc.database

import java.sql.PreparedStatement

import com.prisma.api.connector.{TrueFilter, _}
import com.prisma.api.connector.jdbc.extensions.SlickExtensions
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.gc_values.{NullGCValue, StringGCValue}
import slick.jdbc.PositionedParameters

object SetParams extends SlickExtensions with LimitClauseBuilder {
  def setQueryArgs(preparedStatement: PreparedStatement, queryArguments: QueryArguments): Unit = {
    val pp = new PositionedParameters(preparedStatement)
    setQueryArgs(pp, queryArguments)
  }

  def setQueryArgs(pp: PositionedParameters, queryArguments: QueryArguments): Unit = {
    setFilter(pp, queryArguments.filter)
    setCursor(pp, queryArguments)
    setLimit(pp, queryArguments)

  }

  def setCursor(pp: PositionedParameters, queryArguments: QueryArguments): Unit = {
    queryArguments.after.foreach { value =>
      pp.setGcValue(value)
      pp.setGcValue(value)
    }
    queryArguments.before.foreach { value =>
      pp.setGcValue(value)
      pp.setGcValue(value)
    }
  }

  def setLimit(pp: PositionedParameters, queryArguments: QueryArguments): Unit = {
    val skipAndLimit = LimitClauseHelper.skipAndLimitValues(queryArguments)
    skipAndLimit.limit.foreach(pp.setInt)
    pp.setInt(skipAndLimit.skip)
  }

  def setFilter(pp: PositionedParameters, filter: Option[Filter]): Unit = {
    filter.foreach { filter =>
      setFilter(pp, filter)
    }
  }

  def setFilter(pp: PositionedParameters, filter: Filter): Unit = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter             => // NOOP
      case AndFilter(filters)                 => filters.foreach(setFilter(pp, _))
      case OrFilter(filters)                  => filters.foreach(setFilter(pp, _))
      case NotFilter(filters)                 => filters.foreach(setFilter(pp, _))
      case RelationFilter(_, nestedFilter, _) => setFilter(pp, nestedFilter)
      //--------------------------------ANCHORS------------------------------------
      case TrueFilter                                  => // NOOP
      case FalseFilter                                 => // NOOP
      case ScalarFilter(_, Contains(value))            => pp.setGcValue(value)
      case ScalarFilter(_, NotContains(value))         => pp.setGcValue(value)
      case ScalarFilter(_, StartsWith(value))          => pp.setGcValue(value)
      case ScalarFilter(_, NotStartsWith(value))       => pp.setGcValue(value)
      case ScalarFilter(_, EndsWith(value))            => pp.setGcValue(value)
      case ScalarFilter(_, NotEndsWith(value))         => pp.setGcValue(value)
      case ScalarFilter(_, LessThan(value))            => pp.setGcValue(value)
      case ScalarFilter(_, GreaterThan(value))         => pp.setGcValue(value)
      case ScalarFilter(_, LessThanOrEquals(value))    => pp.setGcValue(value)
      case ScalarFilter(_, GreaterThanOrEquals(value)) => pp.setGcValue(value)
      case ScalarFilter(_, NotEquals(NullGCValue))     => // NOOP
      case ScalarFilter(_, NotEquals(value))           => pp.setGcValue(value)
      case ScalarFilter(_, Equals(NullGCValue))        => // NOOP
      case ScalarFilter(_, Equals(value))              => pp.setGcValue(value)
      case ScalarFilter(_, In(Vector(NullGCValue)))    => // NOOP
      case ScalarFilter(_, NotIn(Vector(NullGCValue))) => // NOOP
      case ScalarFilter(_, In(values))                 => values.foreach(pp.setGcValue)
      case ScalarFilter(_, NotIn(values))              => values.foreach(pp.setGcValue)
      case OneRelationIsNullFilter(_)                  => // NOOP
      case x                                           => sys.error(s"Not supported: $x")
    }
  }

  // todo find solution / factor out of shared trait
  override lazy val slickDatabase: SlickDatabase = null
}
