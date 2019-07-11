package util

trait StringMatchers {
  def mustBeEqual(actual: String, expected: String): Unit = {
    if (actual != expected) {
      sys.error(s"""The strings were not equal!
                   |actual:   $actual
                   |expected: $expected
        """.stripMargin)
    }
  }
}
