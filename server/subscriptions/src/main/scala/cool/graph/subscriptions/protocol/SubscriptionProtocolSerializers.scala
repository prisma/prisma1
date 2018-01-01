package cool.graph.subscriptions.protocol

import play.api.libs.json._

object ProtocolV07 {

  object SubscriptionResponseWriters {
    import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Responses._
    val emptyJson = Json.obj()

    implicit lazy val subscriptionResponseWrites = new Writes[SubscriptionSessionResponse] {
      implicit lazy val stringOrIntWrites        = StringOrInt.writer
      implicit lazy val errorWrites              = Json.writes[ErrorMessage]
      implicit lazy val gqlConnectionErrorWrites = Json.writes[GqlConnectionError]
      implicit lazy val gqlDataPayloadWrites     = Json.writes[GqlDataPayload]
      implicit lazy val gqlDataWrites            = Json.writes[GqlData]
      implicit lazy val gqlErrorWrites           = Json.writes[GqlError]
      implicit lazy val gqlCompleteWrites        = Json.writes[GqlComplete]

      override def writes(resp: SubscriptionSessionResponse): JsValue = {
        val json = resp match {
          case GqlConnectionAck       => emptyJson
          case x: GqlConnectionError  => gqlConnectionErrorWrites.writes(x)
          case GqlConnectionKeepAlive => emptyJson
          case x: GqlData             => gqlDataWrites.writes(x)
          case x: GqlError            => gqlErrorWrites.writes(x)
          case x: GqlComplete         => gqlCompleteWrites.writes(x)
        }
        json + ("type", JsString(resp.`type`))
      }
    }
  }

  object SubscriptionRequestReaders {
    import cool.graph.subscriptions.protocol.SubscriptionProtocolV07.Requests._

    implicit lazy val stringOrIntReads     = CommonReaders.stringOrIntReads
    implicit lazy val initReads            = Json.reads[GqlConnectionInit]
    implicit lazy val gqlStartPayloadReads = Json.reads[GqlStartPayload]
    implicit lazy val gqlStartReads        = Json.reads[GqlStart]
    implicit lazy val gqlStopReads         = Json.reads[GqlStop]

    implicit lazy val subscriptionRequestReadsV07 = new Reads[SubscriptionSessionRequest] {
      import SubscriptionProtocolV07.MessageTypes

      override def reads(json: JsValue): JsResult[SubscriptionSessionRequest] = {
        (json \ "type").validate[String] match {
          case x: JsError =>
            x
          case JsSuccess(value, _) =>
            value match {
              case MessageTypes.GQL_CONNECTION_INIT =>
                initReads.reads(json)
              case MessageTypes.GQL_CONNECTION_TERMINATE =>
                JsSuccess(GqlConnectionTerminate)
              case MessageTypes.GQL_START =>
                gqlStartReads.reads(json)
              case MessageTypes.GQL_STOP =>
                gqlStopReads.reads(json)
              case _ =>
                JsError(error = s"Message could not be parsed. Message Type '$value' is not defined.")
            }
        }
      }
    }
  }
}

object ProtocolV05 {
  object SubscriptionResponseWriters {
    import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses._
    val emptyJson = Json.obj()

    implicit lazy val subscriptionResponseWrites = new Writes[SubscriptionSessionResponseV05] {
      implicit val stringOrIntWrites                   = StringOrInt.writer
      implicit lazy val errorWrites                    = Json.writes[ErrorMessage]
      implicit lazy val subscriptionErrorPayloadWrites = Json.writes[SubscriptionErrorPayload]
      implicit lazy val subscriptionFailWrites         = Json.writes[SubscriptionFail]
      implicit lazy val subscriptionSuccessWrites      = Json.writes[SubscriptionSuccess]
      implicit lazy val subscriptionDataWrites         = Json.writes[SubscriptionData]
      implicit lazy val initConnectionFailWrites       = Json.writes[InitConnectionFail]

      override def writes(resp: SubscriptionSessionResponseV05): JsValue = {
        val json = resp match {
          case InitConnectionSuccess  => emptyJson
          case x: InitConnectionFail  => initConnectionFailWrites.writes(x)
          case x: SubscriptionSuccess => subscriptionSuccessWrites.writes(x)
          case x: SubscriptionFail    => subscriptionFailWrites.writes(x)
          case x: SubscriptionData    => subscriptionDataWrites.writes(x)
          case SubscriptionKeepAlive  => emptyJson
        }
        json + ("type", JsString(resp.`type`))
      }
    }
  }

  object SubscriptionRequestReaders {
    import CommonReaders._
    import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Requests._
    import play.api.libs.functional.syntax._

    implicit lazy val subscriptionStartReads = (
      (JsPath \ "id").read(stringOrIntReads) and
        (JsPath \ "query").read[String] and
        (JsPath \ "variables").readNullable[JsObject] and
        (JsPath \ "operationName").readNullable[String]
    )(SubscriptionStart.apply _)

    implicit lazy val subscriptionEndReads =
      (JsPath \ "id").readNullable(stringOrIntReads).map(id => SubscriptionEnd(id))

    implicit lazy val subscriptionInitReads = Json.reads[InitConnection]

    implicit lazy val subscriptionRequestReadsV05 = new Reads[SubscriptionSessionRequestV05] {
      import SubscriptionProtocolV05.MessageTypes

      override def reads(json: JsValue): JsResult[SubscriptionSessionRequestV05] = {
        (json \ "type").validate[String] match {
          case x: JsError =>
            x
          case JsSuccess(value, _) =>
            value match {
              case MessageTypes.INIT =>
                subscriptionInitReads.reads(json)
              case MessageTypes.SUBSCRIPTION_START =>
                subscriptionStartReads.reads(json)
              case MessageTypes.SUBSCRIPTION_END =>
                subscriptionEndReads.reads(json)
              case _ =>
                JsError(error = s"Message could not be parsed. Message Type '$value' is not defined.")
            }
        }
      }
    }
  }
}

object CommonReaders {
  lazy val stringOrIntReads: Reads[StringOrInt] = Reads {
    case JsNumber(x) =>
      JsSuccess(StringOrInt(string = None, int = Some(x.toInt)))
    case JsString(x) =>
      JsSuccess(StringOrInt(string = Some(x), int = None))
    case _ =>
      JsError("Couldn't parse request id. Supply a number or a string.")
  }
}
