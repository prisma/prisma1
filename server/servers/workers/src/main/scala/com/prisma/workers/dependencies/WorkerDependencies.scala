package com.prisma.workers.dependencies

import com.prisma.akkautil.http.SimpleHttpClient
import com.prisma.errors.ErrorReporter
import com.prisma.messagebus.QueueConsumer
import com.prisma.workers.payloads.Webhook

trait WorkerDependencies {
  def httpClient: SimpleHttpClient
  def webhooksConsumer: QueueConsumer[Webhook]

  implicit val reporter: ErrorReporter
}
