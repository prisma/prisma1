import com.oracle.svm.core.annotate.*;

import java.sql.Driver;
import java.sql.DriverManager;

@TargetClass(value = DriverManager.class)
final class Target_java_sql_DriverManager {
    @Substitute
    private static boolean isDriverAllowed(Driver driver, Class<?> caller) {
        return true;
    }
}
