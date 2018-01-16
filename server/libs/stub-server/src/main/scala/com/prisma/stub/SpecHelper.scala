package cool.graph.stub

import scala.util.Random

object Import {
  val Request = StubDsl.Default.Request

  def withStubServer[T](stubs: List[Stub], port: Int = 8000 + Random.nextInt(1000), stubNotFoundStatusCode: Int = 999): WithStubServer = {
    WithStubServer(stubs, port, stubNotFoundStatusCode)
  }

  case class WithStubServer(stubs: List[Stub], port: Int, stubNotFoundStatusCode: Int) {
    def apply[T](block: => T): T = {
      withArg { stubServer =>
        block
      }
    }

    def withArg[T](block: StubServer => T): T = {
      val stubServer = StubServer(stubs, port, stubNotFoundStatusCode)
      // We need to synchronize as the following block contains mutating global state - the Java System Properties
      // In case two tests run in parallel - one test might end up talking to the wrong port.
      "stub-server".intern.synchronized {
        try {
          // These sys props expose required information to the tests
          sys.props += "STUB_SERVER_RUNNING" -> "true"
          sys.props += "STUB_SERVER_PORT"    -> port.toString
          stubServer.start
          block(stubServer)
        } finally {
          sys.props -= "STUB_SERVER_RUNNING"
          sys.props -= "STUB_SERVER_PORT"
          stubServer.stop
        }
      }
    }
  }
}
