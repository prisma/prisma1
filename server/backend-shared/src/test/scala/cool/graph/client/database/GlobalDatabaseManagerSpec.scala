package cool.graph.client.database

import com.typesafe.config.ConfigFactory
import cool.graph.shared.database.{GlobalDatabaseManager, ProjectDatabaseRef}
import cool.graph.shared.models.Region
import org.scalatest.{FlatSpec, Matchers}

class GlobalDatabaseManagerSpec extends FlatSpec with Matchers {

  it should "initialize correctly for a single region" in {
    val config = ConfigFactory.parseString(s"""
        |awsRegion = "eu-west-1"
        |
        |clientDatabases {
        |  client1 {
        |    master {
        |      connectionInitSql="set names utf8mb4"
        |      dataSourceClass = "slick.jdbc.DriverDataSource"
        |      properties {
        |        url = "jdbc:mysql:aurora://host1:1000/?autoReconnect=true&useSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000"
        |        user = user
        |        password = password
        |      }
        |      numThreads = 1
        |      connectionTimeout = 5000
        |    }
        |    readonly {
        |      connectionInitSql="set names utf8mb4"
        |      dataSourceClass = "slick.jdbc.DriverDataSource"
        |      properties {
        |        url = "jdbc:mysql:aurora://host2:2000/?autoReconnect=true&useSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000"
        |        user = user
        |        password = password
        |      }
        |      readOnly = true
        |      numThreads = 1
        |      connectionTimeout = 5000
        |    }
        |  }
        |
        |  client2 {
        |    master {
        |      connectionInitSql="set names utf8mb4"
        |      dataSourceClass = "slick.jdbc.DriverDataSource"
        |      properties {
        |        url = "jdbc:mysql:aurora://host3:3000/?autoReconnect=true&useSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000"
        |        user = user
        |        password = password
        |      }
        |      numThreads = 1
        |      connectionTimeout = 5000
        |    }
        |  }
        |}
      """.stripMargin)

    val region = Region.EU_WEST_1
    val result = GlobalDatabaseManager.initializeForSingleRegion(config)
    result.currentRegion should equal(region)
    result.databases should have size (2)
    result.databases should contain key (ProjectDatabaseRef(region, name = "client1"))
    result.databases should contain key (ProjectDatabaseRef(region, name = "client2"))
  }

  it should "initialize correctly for a multiple regions" in {
    val config = ConfigFactory.parseString(s"""
                                              |awsRegion = "ap-northeast-1"
                                              |
                                              |allClientDatabases {
                                              |  eu-west-1 {
                                              |    client1 {
                                              |      master {
                                              |        connectionInitSql="set names utf8mb4"
                                              |        dataSourceClass = "slick.jdbc.DriverDataSource"
                                              |        properties {
                                              |          url = "jdbc:mysql:aurora://host1:1000/?autoReconnect=true&useSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000"
                                              |          user = user
                                              |          password = password
                                              |        }
                                              |        numThreads = 1
                                              |        connectionTimeout = 5000
                                              |      }
                                              |      readonly {
                                              |        connectionInitSql="set names utf8mb4"
                                              |        dataSourceClass = "slick.jdbc.DriverDataSource"
                                              |        properties {
                                              |          url = "jdbc:mysql:aurora://host2:2000/?autoReconnect=true&useSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000"
                                              |          user = user
                                              |          password = password
                                              |        }
                                              |        readOnly = true
                                              |        numThreads = 1
                                              |        connectionTimeout = 5000
                                              |      }
                                              |    }
                                              |  }
                                              |  us-west-2 {
                                              |    client1 {
                                              |      master {
                                              |        connectionInitSql="set names utf8mb4"
                                              |        dataSourceClass = "slick.jdbc.DriverDataSource"
                                              |        properties {
                                              |          url = "jdbc:mysql:aurora://host3:3000/?autoReconnect=true&useSSL=false&serverTimeZone=UTC&useUnicode=true&characterEncoding=UTF-8&socketTimeout=60000"
                                              |          user = user
                                              |          password = password
                                              |        }
                                              |        numThreads = 1
                                              |        connectionTimeout = 5000
                                              |      }
                                              |    }
                                              |  }
                                              |}
      """.stripMargin)

    val result = GlobalDatabaseManager.initializeForMultipleRegions(config)
    result.currentRegion should equal(Region.AP_NORTHEAST_1)
    result.databases should have size (2)
    result.databases should contain key (ProjectDatabaseRef(Region.EU_WEST_1, name = "client1"))
    result.databases should contain key (ProjectDatabaseRef(Region.US_WEST_2, name = "client1"))
  }
}
