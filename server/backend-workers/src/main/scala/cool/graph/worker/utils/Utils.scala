package cool.graph.worker.utils

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object Utils {
  val msqlDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS") // mysql datetime(3) format

  /**
    * Generates a mysql datetime(3) timestamp (now)
    */
  def msqlDateTime3Timestamp(): String = Utils.msqlDateFormatter.print(DateTime.now())
}
