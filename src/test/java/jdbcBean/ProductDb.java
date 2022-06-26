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
import jdbcBean.annotation.ToMany;
import lombok.Data;
import lombok.Getter;

import java.sql.*;
import java.util.List;

public class ProductDb implements AutoCloseable {
    private static final String CONNECTION_STRING = "jdbc:hsqldb:mem:productdb;allowmultiqueries=true";
    private static final String USERNAME = "SA";
    private static final String PASSWORD = "";
    @Getter
    private final Connection connection;

    private ProductDb() throws SQLException {
        connection = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSWORD);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE product_order (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(50) NOT NULL
                )
                """);

            statement.execute("""
                CREATE TABLE product (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(50) NOT NULL,
                    order_id INTEGER NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES product_order(id)
                )
                """);

            statement.execute("INSERT INTO product_order VALUES (1, 'order 1')");
            statement.execute("INSERT INTO product_order VALUES (2, 'order 2')");
            statement.execute("INSERT INTO product_order VALUES (3, 'order 3')");
            statement.execute("INSERT INTO product VALUES (1, 'product 1', 1)");
            statement.execute("INSERT INTO product VALUES (2, 'product 2', 1)");
            statement.execute("INSERT INTO product VALUES (3, 'product 3', 3)");
            statement.execute("INSERT INTO product VALUES (4, 'product 4', 3)");
            statement.execute("INSERT INTO product VALUES (5, 'product 5', 3)");
        }
    }

    private static ProductDb instance;
    public static ProductDb getInstance() throws SQLException {
        if (instance == null) {
            instance = new ProductDb();
        }
        return instance;
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    @Data
    public static class Order {
        @Mapped(type = JDBCType.INTEGER, isDistinct = true)
        private Integer orderId;
        @Mapped(type = JDBCType.VARCHAR)
        private String orderName;
        @ToMany
        private List<Product> products;
    }

    @Data
    public static class Product {
        @Mapped(type = JDBCType.INTEGER, isDistinct = true)
        private Integer productId;
        @Mapped(type = JDBCType.VARCHAR)
        private String productName;
    }
}
