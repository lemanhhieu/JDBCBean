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


import lombok.val;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;

import static jdbcBean.CommentsDb.*;

public class Result2BeanTest extends SharedDbContext {

    static final String testQuery1 = """
        SELECT
            c1.id AS id_1, c1.comment AS comment_1, c1.created_at AS created_at_1,
            c2.id AS id_2, c2.comment AS comment_2, c2.created_at AS created_at_2,
            c3.id AS id_3, c3.comment AS comment_3, c3.created_at AS created_at_3
        FROM comment c1
        LEFT JOIN comment c2 ON c2.parent_comment_id = c1.id
        LEFT JOIN comment c3 ON c3.parent_comment_id = c2.id
        WHERE c1.parent_comment_id IS NULL
    """;

    public Result2BeanTest() throws Exception {
    }


    @RepeatedTest(2)
    void testNormalToMany() throws Exception {
        try (Statement statement = commentsDb.getConnection().createStatement()) {
            Result2Bean resultSet = new Result2Bean(statement.executeQuery(testQuery1));
            List<Comment1> comment1List = resultSet.getList(Comment1.class);
            assertEquals(LEVEL_COMMENT_COUNT, comment1List.size());

            for (int i = 0; i < LEVEL_COMMENT_COUNT; i++) {
                val curComment1 = comment1List.get(i);
                val expectedComment1 = seededData.get(i);

                assertEquals(expectedComment1.getCommentData().getId(), curComment1.getCommentData().getId());
                assertEquals(expectedComment1.getCommentData().getComment(), curComment1.getCommentData().getComment());
                assertEquals(expectedComment1.getCommentData().getCreatedAt(), curComment1.getCommentData().getCreatedAt());

                assertEquals(LEVEL_COMMENT_COUNT, curComment1.getCommentData().getChildComments().size());

                for (int j = 0; j < LEVEL_COMMENT_COUNT; j++) {
                    val curComment2 = curComment1.getCommentData().getChildComments().get(j);
                    val expectedComment2 = expectedComment1.getCommentData().getChildComments().get(j);

                    assertEquals(expectedComment2.getId(), curComment2.getId());
                    assertEquals(expectedComment2.getComment(), curComment2.getComment());
                    assertEquals(expectedComment2.getCreatedAt(), curComment2.getCreatedAt());

                    assertEquals(LEVEL_COMMENT_COUNT, curComment2.getChildComments().size());

                    for (int k = 0; k < LEVEL_COMMENT_COUNT; k++) {
                        val curComment3 = curComment2.getChildComments().get(k);
                        val expectedComment3 = expectedComment2.getChildComments().get(k);

                        assertEquals(expectedComment3.getId(), curComment3.getId());
                        assertEquals(expectedComment3.getComment(), curComment3.getComment());
                        assertEquals(expectedComment3.getCreatedAt(), curComment3.getCreatedAt());
                    }
                }
            }
        }
    }

    @RepeatedTest(2)
    void testRecursiveToMany() throws Exception {
        try (Statement statement = commentsDb.getConnection().createStatement()) {
            Result2Bean resultSet = new Result2Bean(statement.executeQuery(testQuery1));
            List<RecursiveComment> commentsList = resultSet.getList(RecursiveComment.class);
            assertEquals(LEVEL_COMMENT_COUNT, commentsList.size());

            for (int i = 0; i < LEVEL_COMMENT_COUNT; i++) {
                val curComment1 = commentsList.get(i);
                val expectedComment1 = seededData.get(i);
                assertEquals(expectedComment1.getCommentData().getId(), curComment1.getId());
                assertEquals(expectedComment1.getCommentData().getComment(), curComment1.getComment());
                assertEquals(expectedComment1.getCommentData().getCreatedAt(), curComment1.getCreatedAt());

                assertEquals(LEVEL_COMMENT_COUNT, curComment1.getComments().size());

                for (int j = 0; j < LEVEL_COMMENT_COUNT; j++) {
                    val curComment2 = curComment1.getComments().get(j);
                    val expectedComment2 = expectedComment1.getCommentData().getChildComments().get(j);
                    assertEquals(expectedComment2.getId(), curComment2.getId());
                    assertEquals(expectedComment2.getComment(), curComment2.getComment());
                    assertEquals(expectedComment2.getCreatedAt(), curComment2.getCreatedAt());

                    assertEquals(LEVEL_COMMENT_COUNT, curComment2.getComments().size());

                    for (int k = 0; k < LEVEL_COMMENT_COUNT; k++) {
                        val curComment3 = curComment2.getComments().get(k);
                        val expectedComment3 = expectedComment2.getChildComments().get(k);
                        assertEquals(expectedComment3.getId(), curComment3.getId());
                        assertEquals(expectedComment3.getComment(), curComment3.getComment());
                        assertEquals(expectedComment3.getCreatedAt(), curComment3.getCreatedAt());

                    }
                }
            }
        }
    }

    @Test
    void testScalar() throws Exception {
        try (Statement statement = commentsDb.getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery("""
                SELECT COUNT(*) FROM comment
                """);
            assertEquals(totalCommentCount, new Result2Bean(resultSet).<Long>getScalar());
        }
    }

    @Test
    void testGetFirst() throws Exception {
        try (Statement statement = commentsDb.getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery("""
                SELECT * from comment WHERE id = 1
                """);
            Comment comment = new Result2Bean(resultSet).getFirst(Comment.class);
            assertEquals(1, comment.getId());
            assertEquals("comment 1", comment.getComment());
        }
    }

    @Test
    void testGetListWithoutToMany() throws Exception {
        try (Statement statement = commentsDb.getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery("""
                SELECT * FROM comment WHERE parent_comment_id = 11
                """);
            List<Comment> comments = new Result2Bean(resultSet).getList(Comment.class);
            assertEquals(LEVEL_COMMENT_COUNT, comments.size());

            comments.sort(Comparator.comparing(Comment::getId));
            for (int i = 0; i < 10; i++) {
                assertEquals(i+1, comments.get(i).getId());
                assertEquals("comment %s".formatted(i+1), comments.get(i).getComment());
            }
        }
    }



    @Test
    void testGetListToManyWithEmptyToMany() throws Exception {
        ProductDb productDb = ProductDb.getInstance();

        try (Statement statement = productDb.getConnection().createStatement()) {
            List<ProductDb.Order> orders = new Result2Bean(statement.executeQuery("""
                SELECT o.id order_id, o.name order_name, p.id product_id, p.name product_name
                FROM product_order o
                LEFT JOIN product p ON o.id = p.order_id
                ORDER BY o.id, p.id
                """
            )).getList(ProductDb.Order.class);

            assertEquals(2, orders.get(0).getProducts().size());
            assertEquals(0, orders.get(1).getProducts().size());
            assertEquals(3, orders.get(2).getProducts().size());
        }
    }
}
