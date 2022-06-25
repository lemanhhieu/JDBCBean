package jdbcBean;


import jdbcBean.annotation.Mapped;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.JDBCType;
import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.*;

public class NPreparedStatementTest extends SharedDbContext {

    public NPreparedStatementTest() throws Exception {
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchComment {
        @Mapped(type = JDBCType.INTEGER)
        private Integer id;
        @Mapped(type = JDBCType.VARCHAR)
        private String comment;
    }
    @Test
    void happyDays() throws Exception {
        try (NPPreparedStatement preparedStatement = new NPPreparedStatement("""
            SELECT * FROM comment WHERE id = :id and comment = :comment
            """, commentsDb.getConnection())
        ) {
            preparedStatement.setParameters(new SearchComment(5, "comment 5"));
            preparedStatement.getStatement().execute();

            try (ResultSet resultSet = preparedStatement.getStatement().getResultSet()) {
                resultSet.next();
                assertEquals(5, resultSet.getInt("id"));
                assertEquals("comment 5", resultSet.getString("comment"));
            }
        }
    }

}
