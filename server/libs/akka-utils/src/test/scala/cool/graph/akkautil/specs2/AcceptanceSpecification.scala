package cool.graph.akkautil.specs2

import org.specs2.Specification
import org.specs2.matcher.ThrownExpectations

/**
  * This trait enables the usage of blocks for examples in the acceptance spec style.
  */
trait AcceptanceSpecification extends Specification with ThrownExpectations
