import com.oracle.svm.core.annotate.*;
import com.prisma.api.connector.postgres.PostgresDatabasesFactory$;
import java.sql.Driver;
import com.typesafe.config.ConfigFactory;
import com.prisma.config.DatabaseConfig;
import com.prisma.connector.shared.jdbc.Databases;
import com.prisma.connector.shared.jdbc.SlickDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import slick.jdbc.DataSourceJdbcDataSource;
import slick.jdbc.DriverDataSource;
import slick.jdbc.PostgresProfile;
import slick.jdbc.hikaricp.HikariCPJdbcDataSource;

@TargetClass(value = PostgresDatabasesFactory$.class)
final class Target_com_prisma_api_connector_postgres_PostgresDatabasesFactory$ {
    @Substitute
    private boolean isPooled(DatabaseConfig dbConfig) {
        return false;
    }
}
