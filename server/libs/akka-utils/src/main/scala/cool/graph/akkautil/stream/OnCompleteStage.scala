package cool.graph.akkautil.stream

import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream.impl.fusing.GraphStages.SimpleLinearGraphStage
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, Supervision}

case class OnCompleteStage[T](op: () ⇒ Unit) extends SimpleLinearGraphStage[T] {
  override def toString: String = "OnComplete"

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler with InHandler {
      def decider =
        inheritedAttributes
          .get[SupervisionStrategy]
          .map(_.decider)
          .getOrElse(Supervision.stoppingDecider)

      override def onPush(): Unit = {
        push(out, grab(in))
      }

      override def onPull(): Unit = pull(in)

      override def onDownstreamFinish() = {
        op()
        super.onDownstreamFinish()
      }

      override def onUpstreamFinish() = {
        op()
        super.onUpstreamFinish()
      }

      setHandlers(in, out, this)
    }
}
