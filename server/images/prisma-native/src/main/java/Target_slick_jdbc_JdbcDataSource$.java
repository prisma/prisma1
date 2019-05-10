import java.sql.Driver;
import com.typesafe.config.Config;
import slick.jdbc.hikaricp.HikariCPJdbcDataSource$;
import slick.jdbc.JdbcDataSource$;
import slick.jdbc.JdbcDataSource;
import com.oracle.svm.core.annotate.*;
import com.oracle.svm.core.annotate.RecomputeFieldValue.*;

@TargetClass(value = JdbcDataSource$.class)
final class Target_slick_jdbc_JdbcDataSource$ {
    @Substitute
    public JdbcDataSource forConfig(Config c, Driver driver, String name, ClassLoader classLoader) {
        return HikariCPJdbcDataSource$.MODULE$.forConfig(c, driver, name, classLoader);
    }
}
