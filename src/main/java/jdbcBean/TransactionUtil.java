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

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionUtil {

    /**
     *  A convenient method to wrap a transaction inside a lambda.
     * @param isolationLevel
     *      one of the isolation level declared in {@link Connection} (example: {@link Connection#TRANSACTION_READ_COMMITTED})
     * @param connection The database connection
     * @param runnableTransaction
     *      The lambda where you execute statements insides it.
     *      Any non {@link SQLException} exception thrown is wrapped in {@link RuntimeException}.
     */
    public static void runTransaction(
        int isolationLevel,
        Connection connection,
        RunnableTransaction runnableTransaction
    ) throws SQLException {

        int oldIsolationLevel = connection.getTransactionIsolation();
        boolean oldAutoCommitSetting = connection.getAutoCommit();


        connection.setTransactionIsolation(isolationLevel);
        connection.setAutoCommit(false);
        try {
            runnableTransaction.exec(connection);
        }
        catch (SQLException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        connection.commit();

        connection.setAutoCommit(oldAutoCommitSetting);
        connection.setTransactionIsolation(oldIsolationLevel);
    }

    public interface RunnableTransaction {
        void exec(Connection connection) throws Exception;
    }
}
