package cool.graph.shared.logging

import cool.graph.cuid.Cuid.createCuid

class RequestLogger(requestIdPrefix: String, log: Function[String, Unit]) {
  val requestId: String                  = requestIdPrefix + ":" + createCuid()
  var requestBeginningTime: Option[Long] = None

  def query(query: String, args: String): Unit = {
    log(
      LogData(
        key = LogKey.RequestQuery,
        requestId = requestId,
        payload = Some(Map("query" -> query, "arguments" -> args))
      ).json
    )
  }

  def begin: String = {
    requestBeginningTime = Some(System.currentTimeMillis())
    log(LogData(LogKey.RequestNew, requestId).json)

    requestId
  }

  def end(projectId: Option[String] = None, clientId: Option[String] = None): Unit =
    requestBeginningTime match {
      case None =>
        sys.error("you must call begin before end")

      case Some(beginTime) =>
        log(
          LogData(
            key = LogKey.RequestComplete,
            requestId = requestId,
            projectId = projectId,
            clientId = clientId,
            payload = Some(Map("request_duration" -> (System.currentTimeMillis() - beginTime)))
          ).json
        )
    }
}
