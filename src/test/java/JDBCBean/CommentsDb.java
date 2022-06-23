package JDBCBean;

import JDBCBean.annotation.Embedded;
import JDBCBean.annotation.Mapped;
import JDBCBean.annotation.ToMany;
import lombok.*;
import org.junit.jupiter.api.Assertions;

import java.sql.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

public class CommentsDb implements AutoCloseable {
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
                        UUID.randomUUID(), "comment " + curId, OffsetDateTime.now()
                    ));
                    curId++;
                }
                comment2List.add(new Comment1.Comment2(
                    UUID.randomUUID(), "comment " + curId, OffsetDateTime.now(), comment3List
                ));
                curId++;
            }
            seededData.add(new Comment1(
                UUID.randomUUID(),
                new EmbeddedCommentData("comment " + curId, OffsetDateTime.now()),
                comment2List
            ));
            curId++;
        }
        System.out.printf("Added %s comments to seed data%n", curId - 1);
        totalCommentCount = curId - 1;
    }

    public CommentsDb() throws SQLException {
        connection = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSWORD);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE comment (
                    id UUID PRIMARY KEY,
                    comment VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP(9) WITH TIME ZONE NOT NULL,
                    parent_comment_id UUID,
                    FOREIGN KEY (parent_comment_id) REFERENCES comment(id)
                )
                """);

        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("""
            INSERT INTO comment VALUES (?,?,?,?)
            """)
        ) {
            for (val comment1 : seededData) {
                preparedStatement.setObject(1, comment1.getId());
                preparedStatement.setObject(2, comment1.getCommentData().getComment());
                preparedStatement.setObject(3, comment1.getCommentData().getCreatedAt());
                preparedStatement.setObject(4, null);
                preparedStatement.addBatch();
                preparedStatement.clearParameters();

                for (val comment2 : comment1.getChildComments()) {

                    preparedStatement.setObject(1, comment2.getId());
                    preparedStatement.setObject(2, comment2.getComment());
                    preparedStatement.setObject(3, comment2.getCreatedAt());
                    preparedStatement.setObject(4, comment1.getId());
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

    @Override
    public void close() throws Exception {
        connection.close();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddedCommentData {
        @Mapped(type = JDBCType.VARCHAR, name = "comment_1")
        private String comment;
        @Mapped(type = JDBCType.TIMESTAMP_WITH_TIMEZONE, name = "created_at_1")
        private OffsetDateTime createdAt;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Comment1 {
        @Mapped(type = JDBCType.INTEGER, isDistinct = true, name = "id_1")
        private UUID id;
        @Embedded
        private EmbeddedCommentData commentData;
        @ToMany
        private List<Comment2> childComments;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Comment2 {
            @Mapped(type = JDBCType.INTEGER, isDistinct = true, name = "id_2")
            private UUID id;
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
                private UUID id;
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
        @Mapped(type = JDBCType.INTEGER, isDistinct = true, name = "id")
        private UUID id;
        @Mapped(type = JDBCType.VARCHAR, name = "comment")
        private String comment;
        @Mapped(type = JDBCType.TIMESTAMP_WITH_TIMEZONE, name = "created_at")
        private OffsetDateTime createdAt;
        @ToMany(recursiveDepth = 3)
        private List<RecursiveComment> comments;
    }
}
