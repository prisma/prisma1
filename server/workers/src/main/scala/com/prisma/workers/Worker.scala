package com.prisma.workers

import scala.concurrent.Future

trait Worker {
  def start: Future[_]
  def stop: Future[_]
}
