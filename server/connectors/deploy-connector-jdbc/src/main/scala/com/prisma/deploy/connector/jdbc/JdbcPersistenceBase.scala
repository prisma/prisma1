package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.{SharedJdbcExtensions, SharedSlickExtensions}

trait JdbcPersistenceBase extends SharedSlickExtensions with SharedJdbcExtensions
