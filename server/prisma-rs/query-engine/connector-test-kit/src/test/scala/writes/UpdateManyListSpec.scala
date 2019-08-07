package writes

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.ScalarListsCapability
import util._

class UpdateManyListSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(ScalarListsCapability)

  val testDataModels = {
    def dm(scalarList: String) = s"""
      model MyObject {
        id        String     @id @default(cuid())
        name      String     @unique
        strings   String[]   $scalarList
        ints      Int[]      $scalarList
        floats    Float[]    $scalarList
        booleans  Boolean[]  $scalarList
        datetimes DateTime[] $scalarList
        enums     Tag[]      $scalarList
      }
      
      enum Tag{
       A
       B
      }"""

    TestDataModels(mongo = dm(""), sql = dm("//@scalarList(strategy: RELATION)"))
  }

  "The updateMany Mutation" should "update Lists of Strings" in {
    test { project =>
      server
        .query("""mutation a {updateManyMyObjects(data:{strings: { set: ["Alpha","Beta"] }}){count}}""", project)
        .pathAsLong("data.updateManyMyObjects.count") should be(1)
      server.query("""{myObjects{strings}}""", project).toString should be("""{"data":{"myObjects":[{"strings":["Alpha","Beta"]}]}}""")
      server.query("""mutation a {updateManyMyObjects(data:{strings: { set: [] }}){count}}""", project).pathAsLong("data.updateManyMyObjects.count") should be(
        1)
      server.query("""{myObjects{strings}}""", project).toString should be("""{"data":{"myObjects":[{"strings":[]}]}}""")

    }
  }

  "The updateMany Mutation" should "update Lists of Ints" in {
    test { project =>
      server
        .query("""mutation a {updateManyMyObjects(data:{ints: { set: [1,2,3] }}){count}}""", project)
        .pathAsLong("data.updateManyMyObjects.count") should be(1)
      server.query("""{myObjects{ints}}""", project).toString should be("""{"data":{"myObjects":[{"ints":[1,2,3]}]}}""")
      server.query("""mutation a {updateManyMyObjects(data:{ints: { set: [] }}){count}}""", project).pathAsLong("data.updateManyMyObjects.count") should be(1)
      server.query("""{myObjects{ints}}""", project).toString should be("""{"data":{"myObjects":[{"ints":[]}]}}""")
    }
  }

  "The updateMany Mutation" should "update Lists of Floats" in {
    test { project =>
      server
        .query("""mutation a {updateManyMyObjects(data:{floats: { set: [1.231312312312, 12.23432234] }}){count}}""", project)
        .pathAsLong("data.updateManyMyObjects.count") should be(1)
      server.query("""{myObjects{floats}}""", project).toString should be("""{"data":{"myObjects":[{"floats":[1.231312312312,12.23432234]}]}}""")
      server.query("""mutation a {updateManyMyObjects(data:{floats: { set: [] }}){count}}""", project).pathAsLong("data.updateManyMyObjects.count") should be(1)
      server.query("""{myObjects{floats}}""", project).toString should be("""{"data":{"myObjects":[{"floats":[]}]}}""")
    }
  }

  "The updateMany Mutation" should "update Lists of Booleans" in {
    test { project =>
      server
        .query("""mutation a {updateManyMyObjects(data:{booleans: { set: [true,false] }}){count}}""", project)
        .pathAsLong("data.updateManyMyObjects.count") should be(1)
      server.query("""{myObjects{booleans}}""", project).toString should be("""{"data":{"myObjects":[{"booleans":[true,false]}]}}""")
      server.query("""mutation a {updateManyMyObjects(data:{booleans: { set: [] }}){count}}""", project).pathAsLong("data.updateManyMyObjects.count") should be(
        1)
      server.query("""{myObjects{booleans}}""", project).toString should be("""{"data":{"myObjects":[{"booleans":[]}]}}""")
    }
  }

  "The updateMany Mutation" should "update Lists of DateTimes" in {
    test { project =>
      server
        .query("""mutation a {updateManyMyObjects(data:{datetimes: { set: ["2018-12-05T12:34:23.000Z"] }}){count}}""", project)
        .pathAsLong("data.updateManyMyObjects.count") should be(1)
      server.query("""{myObjects{datetimes}}""", project).toString should be("""{"data":{"myObjects":[{"datetimes":["2018-12-05T12:34:23.000Z"]}]}}""")
      server
        .query("""mutation a {updateManyMyObjects(data:{datetimes: { set: [] }}){count}}""", project)
        .pathAsLong("data.updateManyMyObjects.count") should be(1)
      server.query("""{myObjects{datetimes}}""", project).toString should be("""{"data":{"myObjects":[{"datetimes":[]}]}}""")
    }
  }

  "The updateMany Mutation" should "update Lists of Enums" in {
    test { project =>
      server.query("""mutation a {updateManyMyObjects(data:{enums: { set: [A,B] }}){count}}""", project).pathAsLong("data.updateManyMyObjects.count") should be(
        1)
      server.query("""{myObjects{enums}}""", project).toString should be("""{"data":{"myObjects":[{"enums":["A","B"]}]}}""")
      server.query("""mutation a {updateManyMyObjects(data:{enums: { set: [] }}){count}}""", project).pathAsLong("data.updateManyMyObjects.count") should be(1)
      server.query("""{myObjects{enums}}""", project).toString should be("""{"data":{"myObjects":[{"enums":[]}]}}""")
    }
  }

  def test(fn: Project => Unit) = {
    testDataModels.testV11 { project =>
      server.query("""mutation{createMyObject(data:{name: "Test"}){name}}""", project)
      fn(project)
    }
  }
}
