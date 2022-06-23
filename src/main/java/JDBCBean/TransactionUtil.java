package JDBCBean;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionUtil {


    public static void runTransaction(
        int isolationLevel,
        Connection connection,
        RunnableTransaction runnableTransaction
    ) throws Exception {

        connection.setTransactionIsolation(isolationLevel);
        connection.setAutoCommit(false);

        runnableTransaction.exec(connection);
        connection.commit();

        connection.setAutoCommit(true);
    }

    public interface RunnableTransaction {
        void exec(Connection connection) throws Exception;
    }
}
