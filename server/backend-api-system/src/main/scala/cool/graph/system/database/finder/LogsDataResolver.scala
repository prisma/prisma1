package cool.graph.system.database.finder

import cool.graph.shared.externalServices.TestableTime
import cool.graph.shared.models.Log
import cool.graph.system.database.finder.HistogramPeriod.HistogramPeriod
import cool.graph.system.database.tables.Tables
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object HistogramPeriod extends Enumeration {
  type HistogramPeriod = Value
  val MONTH     = Value("MONTH")
  val WEEK      = Value("WEEK")
  val DAY       = Value("DAY")
  val HOUR      = Value("HOUR")
  val HALF_HOUR = Value("HALF_HOUR")
}

class LogsDataResolver(implicit inj: Injector) extends Injectable {

  val logsDatabase = inject[DatabaseDef](identified by "logs-db")
  val testableTime = inject[TestableTime]

  def load(functionId: String, count: Int = 1000, before: Option[String] = None): Future[Seq[Log]] = {

    val query = before match {
      case Some(curosr) =>
        for {
          log <- Tables.Logs
          if log.functionId === functionId && log.id < before
        } yield log
      case None =>
        for {
          log <- Tables.Logs
          if log.functionId === functionId
        } yield log
    }

    logsDatabase
      .run(query.sortBy(_.id.desc).take(count).result)
      .map(_.map(l => Log(id = l.id, requestId = l.requestId, status = l.status, duration = l.duration, timestamp = l.timestamp, message = l.message)))
  }

  def calculateHistogram(projectId: String, period: HistogramPeriod, functionId: Option[String] = None): Future[List[Int]] = {
    val currentTimeStamp: Long = getCurrentUnixTimestamp

    val (fullDurationInMinutes, intervalInSeconds, sections) = period match {
      case HistogramPeriod.HALF_HOUR => (30, 10, 180)
      case HistogramPeriod.HOUR      => (60, 20, 180)
      case HistogramPeriod.DAY       => (60 * 24, 20 * 24, 180)
      case HistogramPeriod.WEEK      => (60 * 24 * 7, 20 * 24 * 7, 180)
      case HistogramPeriod.MONTH     => (60 * 24 * 30, 20 * 24 * 30, 180)
    }

    val functionIdCriteria = functionId.map(id => s"AND functionId = '$id'").getOrElse("")

    logsDatabase
      .run(sql"""
      SELECT COUNT(*), FLOOR(unix_timestamp(timestamp)/$intervalInSeconds) * $intervalInSeconds as ts FROM Log
      WHERE timestamp > date_sub(FLOOR(from_unixtime($currentTimeStamp)), INTERVAL $fullDurationInMinutes MINUTE)
      AND projectId = $projectId
      #$functionIdCriteria
      GROUP BY ts""".as[(Int, Int)])
      .map(res => fillInBlankSections(currentTimeStamp, res, fullDurationInMinutes, intervalInSeconds, sections))
  }

  private def fillInBlankSections(currentTimeStamp: Long, data: Seq[(Int, Int)], fullDurationInMinutes: Int, intervalInSeconds: Int, sections: Int) = {
    val firstTimestamp = ((currentTimeStamp - fullDurationInMinutes * 60 + intervalInSeconds) / intervalInSeconds) * intervalInSeconds
    List
      .tabulate(sections)(n => firstTimestamp + n * intervalInSeconds)
      .map(ts => data.find(_._2 == ts).map(_._1).getOrElse(0))
  }

  def countRequests(functionId: String): Future[Int] = {
    logsDatabase
      .run(sql"""
      SELECT COUNT(*) FROM Log
      WHERE timestamp > date_sub(from_unixtime($getCurrentUnixTimestamp), INTERVAL 24 HOUR)
      AND functionId = ${functionId}""".as[Int])
      .map(_.head)
  }

  def countErrors(functionId: String): Future[Int] = {
    logsDatabase
      .run(sql"""
      SELECT COUNT(*) FROM Log
      WHERE timestamp > date_sub(from_unixtime($getCurrentUnixTimestamp), INTERVAL 24 HOUR)
      AND status = 'FAILURE'
      AND functionId = ${functionId}""".as[Int])
      .map(_.head)
  }

  private def getCurrentUnixTimestamp = testableTime.DateTime.getMillis / 1000
}
