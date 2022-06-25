package jdbcBean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static jdbcBean.JDBCUtil.*;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JDBCUtilTest {

    @Test
    void testCamelCaseToSnakeCase() {
        assertEquals("jdbc_driver", convertCamelCaseToSnakeCase("JDBCDriver"));
        assertEquals("column_1", convertCamelCaseToSnakeCase("column1"));
        assertEquals("special_jdbc_field_of_the_day", convertCamelCaseToSnakeCase("specialJDBCFieldOfTheDay"));
        assertEquals("13_sheeps_with_1_shepherd_jdbc", convertCamelCaseToSnakeCase("13SheepsWith1ShepherdJDBC"));
    }
}
