/*
 * MIT License
 *
 * Copyright (c) 2022 LE MANH HIEU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package jdbcBean;


import jdbcBean.annotation.Mapped;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import java.sql.JDBCType;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
