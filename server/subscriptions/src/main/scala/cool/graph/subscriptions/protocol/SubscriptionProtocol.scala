package cool.graph.subscriptions.protocol

import cool.graph.subscriptions.protocol.SubscriptionProtocolV05.Responses.{InitConnectionFail, SubscriptionErrorPayload, SubscriptionFail}
import play.api.libs.json._

case class StringOrInt(string: Option[String], int: Option[Int]) {
  def asString = string.orElse(int.map(_.toString)).get
}

object StringOrInt {
  implicit val writer = new Writes[StringOrInt] {
    def writes(stringOrInt: StringOrInt): JsValue = {
      stringOrInt match {
        case StringOrInt(Some(id), _) => JsString(id)
        case StringOrInt(_, Some(id)) => JsNumber(id)
        case _                        => sys.error("writes: this StringOrInt is neither")
      }
    }
  }
}

object SubscriptionProtocolV07 {
  val protocolName = "graphql-ws"

  object MessageTypes {
    val GQL_CONNECTION_INIT       = "connection_init"      // Client -> Server
    val GQL_CONNECTION_TERMINATE  = "connection_terminate" // Client -> Server
    val GQL_CONNECTION_ACK        = "connection_ack"       // Server -> Client
    val GQL_CONNECTION_ERROR      = "connection_error"     // Server -> Client
    val GQL_CONNECTION_KEEP_ALIVE = "ka"                   // Server -> Client

    val GQL_START    = "start"    // Client -> Server
    val GQL_STOP     = "stop"     // Client -> Server
    val GQL_DATA     = "data"     // Server -> Client
    val GQL_ERROR    = "error"    // Server -> Client
    val GQL_COMPLETE = "complete" // Server -> Client
  }

  /**
    * REQUESTS
    */
  object Requests {
    sealed trait SubscriptionSessionRequest {
      def `type`: String
    }

    case class GqlConnectionInit(payload: Option[JsObject]) extends SubscriptionSessionRequest {
      val `type` = MessageTypes.GQL_CONNECTION_INIT
    }

    object GqlConnectionTerminate extends SubscriptionSessionRequest {
      val `type` = MessageTypes.GQL_CONNECTION_TERMINATE
    }

    case class GqlStart(id: StringOrInt, payload: GqlStartPayload) extends SubscriptionSessionRequest {
      val `type` = MessageTypes.GQL_START
    }

    case class GqlStartPayload(query: String, variables: Option[JsObject], operationName: Option[String])

    case class GqlStop(id: StringOrInt) extends SubscriptionSessionRequest {
      val `type` = MessageTypes.GQL_STOP
    }
  }

  /**
    * RESPONSES
    */
  object Responses {
    sealed trait SubscriptionSessionResponse {
      def `type`: String
    }

    object GqlConnectionAck extends SubscriptionSessionResponse {
      val `type` = MessageTypes.GQL_CONNECTION_ACK
    }

    case class GqlConnectionError(payload: ErrorMessage) extends SubscriptionSessionResponse {
      val `type` = MessageTypes.GQL_CONNECTION_ERROR
    }

    object GqlConnectionKeepAlive extends SubscriptionSessionResponse {
      val `type` = MessageTypes.GQL_CONNECTION_KEEP_ALIVE
    }

    case class GqlData(id: StringOrInt, payload: JsValue) extends SubscriptionSessionResponse {
      val `type` = MessageTypes.GQL_DATA
    }
    case class GqlDataPayload(data: JsValue, errors: Option[Seq[ErrorMessage]] = None)

    case class GqlError(id: StringOrInt, payload: ErrorMessage) extends SubscriptionSessionResponse {
      val `type` = MessageTypes.GQL_ERROR
    }

    case class GqlComplete(id: StringOrInt) extends SubscriptionSessionResponse {
      val `type` = MessageTypes.GQL_COMPLETE
    }

    /**
      * Companions for the Responses
      */
    object GqlConnectionError {
      def apply(errorMessage: String): GqlConnectionError = GqlConnectionError(ErrorMessage(errorMessage))
    }
    object GqlError {
      def apply(id: StringOrInt, errorMessage: String): GqlError = GqlError(id, ErrorMessage(errorMessage))
    }
  }
}

object SubscriptionProtocolV05 {
  val protocolName = "graphql-subscriptions"

  object MessageTypes {
    val INIT         = "init"         // Client -> Server
    val INIT_FAIL    = "init_fail"    // Server -> Client
    val INIT_SUCCESS = "init_success" // Server -> Client
    val KEEPALIVE    = "keepalive"    // Server -> Client

    val SUBSCRIPTION_START   = "subscription_start"   // Client -> Server
    val SUBSCRIPTION_END     = "subscription_end"     // Client -> Server
    val SUBSCRIPTION_SUCCESS = "subscription_success" // Server -> Client
    val SUBSCRIPTION_FAIL    = "subscription_fail"    // Server -> Client
    val SUBSCRIPTION_DATA    = "subscription_data"    // Server -> Client
  }

  /**
    * REQUESTS
    */
  object Requests {
    sealed trait SubscriptionSessionRequestV05 {
      def `type`: String
    }

    case class InitConnection(payload: Option[JsObject]) extends SubscriptionSessionRequestV05 {
      val `type` = MessageTypes.INIT
    }

    case class SubscriptionStart(id: StringOrInt, query: String, variables: Option[JsObject], operationName: Option[String])
        extends SubscriptionSessionRequestV05 {

      val `type` = MessageTypes.SUBSCRIPTION_START
    }

    case class SubscriptionEnd(id: Option[StringOrInt]) extends SubscriptionSessionRequestV05 {
      val `type` = MessageTypes.SUBSCRIPTION_END
    }
  }

  /**
    * RESPONSES
    */
  object Responses {
    sealed trait SubscriptionSessionResponseV05 {
      def `type`: String
    }

    object InitConnectionSuccess extends SubscriptionSessionResponseV05 {
      val `type` = MessageTypes.INIT_SUCCESS
    }

    case class InitConnectionFail(payload: ErrorMessage) extends SubscriptionSessionResponseV05 {
      val `type` = MessageTypes.INIT_FAIL
    }

    case class SubscriptionSuccess(id: StringOrInt) extends SubscriptionSessionResponseV05 {
      val `type` = MessageTypes.SUBSCRIPTION_SUCCESS
    }

    case class SubscriptionFail(id: StringOrInt, payload: SubscriptionErrorPayload) extends SubscriptionSessionResponseV05 {
      val `type` = MessageTypes.SUBSCRIPTION_FAIL
    }

    case class SubscriptionData(id: StringOrInt, payload: JsValue) extends SubscriptionSessionResponseV05 {
      val `type` = MessageTypes.SUBSCRIPTION_DATA
    }

    object SubscriptionKeepAlive extends SubscriptionSessionResponseV05 {
      val `type` = MessageTypes.KEEPALIVE
    }

    case class SubscriptionErrorPayload(errors: Seq[ErrorMessage])

    /**
      * Companions for the Responses
      */
    object SubscriptionFail {
      def apply(id: StringOrInt, errorMessage: String): SubscriptionFail = {
        SubscriptionFail(id, SubscriptionErrorPayload(Seq(ErrorMessage(errorMessage))))
      }
    }
    object InitConnectionFail {
      def apply(errorMessage: String): InitConnectionFail = InitConnectionFail(ErrorMessage(errorMessage))
    }
  }
}

case class ErrorMessage(message: String)
