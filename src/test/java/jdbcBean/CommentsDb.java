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

import jdbcBean.annotation.Embedded;
import jdbcBean.annotation.Mapped;
import jdbcBean.annotation.ToMany;
import lombok.*;
import org.junit.jupiter.api.Assertions;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


public class CommentsDb {
    private static final String CONNECTION_STRING = "jdbc:hsqldb:mem:testdb;allowmultiqueries=true";
    private static final String USERNAME = "SA";
    private static final String PASSWORD = "";
    public static final int LEVEL_COMMENT_COUNT = 10;
    @Getter
    private final Connection connection;
    public static final List<Comment1> seededData;
    public static final int totalCommentCount;

    static {
        seededData = new ArrayList<>(LEVEL_COMMENT_COUNT);

        int curId = 1;

        // 1st level
        for (int i = 0; i < LEVEL_COMMENT_COUNT; i++) {

            List<Comment1.Comment2> comment2List = new ArrayList<>(LEVEL_COMMENT_COUNT);
            // 2nd level
            for (int j = 0; j < LEVEL_COMMENT_COUNT; j++) {

                List<Comment1.Comment2.Comment3> comment3List = new ArrayList<>(LEVEL_COMMENT_COUNT);
                // 3rd level
                for (int k = 0; k < LEVEL_COMMENT_COUNT; k++) {
                    comment3List.add(new Comment1.Comment2.Comment3(
                        curId, "comment " + curId, OffsetDateTime.now()
                    ));
                    curId++;
                }
                comment2List.add(new Comment1.Comment2(
                    curId, "comment " + curId, OffsetDateTime.now(), comment3List
                ));
                curId++;
            }
            seededData.add(new Comment1(
                new EmbeddedCommentData(curId, "comment " + curId, OffsetDateTime.now(), comment2List)
            ));
            curId++;
        }
        System.out.printf("Added %s comments to seed data%n", curId - 1);
        totalCommentCount = curId - 1;
    }
    private static CommentsDb instance;
    public static CommentsDb getInstance() throws SQLException {
        if (instance == null) {
            instance = new CommentsDb();
        }
        return instance;
    }

    private CommentsDb() throws SQLException {
        connection = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSWORD);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE comment (
                    id INTEGER PRIMARY KEY,
                    comment VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
                    parent_comment_id INTEGER,
                    FOREIGN KEY (parent_comment_id) REFERENCES comment(id)
                )
                """);

        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("""
            INSERT INTO comment VALUES (?,?,?,?)
            """)
        ) {
            for (val comment1 : seededData) {
                preparedStatement.setObject(1, comment1.getCommentData().getId());
                preparedStatement.setObject(2, comment1.getCommentData().getComment());
                preparedStatement.setObject(3, comment1.getCommentData().getCreatedAt());
                preparedStatement.setObject(4, null);
                preparedStatement.addBatch();
                preparedStatement.clearParameters();

                for (val comment2 : comment1.getCommentData().getChildComments()) {

                    preparedStatement.setObject(1, comment2.getId());
                    preparedStatement.setObject(2, comment2.getComment());
                    preparedStatement.setObject(3, comment2.getCreatedAt());
                    preparedStatement.setObject(4, comment1.getCommentData().getId());
                    preparedStatement.addBatch();
                    preparedStatement.clearParameters();

                    for (val comment3 : comment2.getChildComments()) {
                        preparedStatement.setObject(1, comment3.getId());
                        preparedStatement.setObject(2, comment3.getComment());
                        preparedStatement.setObject(3, comment3.getCreatedAt());
                        preparedStatement.setObject(4, comment2.getId());
                        preparedStatement.addBatch();
                        preparedStatement.clearParameters();
                    }
                }
            }

            preparedStatement.executeBatch();
        }

        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("""
                SELECT COUNT(*) FROM comment
                """);
            try (resultSet) {
                resultSet.next();
                Assertions.assertEquals(totalCommentCount, resultSet.getInt(1));
            }
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddedCommentData {
        @Mapped(type = JDBCType.INTEGER, isDistinct = true, name = "id_1")
        private Integer id;
        @Mapped(type = JDBCType.VARCHAR, name = "comment_1")
        private String comment;
        @Mapped(type = JDBCType.TIMESTAMP_WITH_TIMEZONE, name = "created_at_1")
        private OffsetDateTime createdAt;
        @ToMany
        private List<Comment1.Comment2> childComments;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Comment1 {

        @Embedded
        private EmbeddedCommentData commentData;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Comment2 {
            @Mapped(type = JDBCType.INTEGER, isDistinct = true, name = "id_2")
            private Integer id;
            @Mapped(type = JDBCType.VARCHAR, name = "comment_2")
            private String comment;
            @Mapped(type = JDBCType.TIMESTAMP_WITH_TIMEZONE, name = "created_at_2")
            private OffsetDateTime createdAt;
            @ToMany
            private List<Comment3> childComments;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            public static class Comment3 {
                @Mapped(type = JDBCType.INTEGER, isDistinct = true, name = "id_3")
                private Integer id;
                @Mapped(type = JDBCType.VARCHAR, name = "comment_3")
                private String comment;
                @Mapped(type = JDBCType.TIMESTAMP_WITH_TIMEZONE, name = "created_at_3")
                private OffsetDateTime createdAt;
            }
        }
    }
    @Data
    @ToString
    public static class RecursiveComment {
        @Mapped(type = JDBCType.INTEGER, isDistinct = true)
        private Integer id;
        @Mapped(type = JDBCType.VARCHAR)
        private String comment;
        @Mapped(type = JDBCType.TIMESTAMP_WITH_TIMEZONE)
        private OffsetDateTime createdAt;
        @ToMany(recursiveDepth = 3)
        private List<RecursiveComment> comments;
    }

    @Data
    public static class Comment {
        @Mapped(type = JDBCType.INTEGER)
        private Integer id;
        @Mapped(type = JDBCType.VARCHAR)
        private String comment;
        @Mapped(type = JDBCType.TIMESTAMP_WITH_TIMEZONE)
        private OffsetDateTime createdAt;
    }
}
