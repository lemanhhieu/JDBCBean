package JDBCBean.exception;

public class JDBCBeanException extends RuntimeException {

    public JDBCBeanException() {
    }

    public JDBCBeanException(String message) {
        super(message);
    }

    public JDBCBeanException(String message, Throwable cause) {
        super(message, cause);
    }

    public JDBCBeanException(Throwable cause) {
        super(cause);
    }

    public JDBCBeanException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
