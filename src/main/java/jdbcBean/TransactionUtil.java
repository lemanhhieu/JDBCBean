package jdbcBean;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionUtil {


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
