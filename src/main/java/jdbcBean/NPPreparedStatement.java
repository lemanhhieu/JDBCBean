package jdbcBean;

import lombok.Getter;

import java.sql.*;
import static jdbcBean.BeanUtil.*;

public class NPPreparedStatement implements AutoCloseable {

    @Getter
    private final PreparedStatement statement;

    @Getter
    protected final MappedQuery mappedQuery;



    public NPPreparedStatement(String npSqlString, Connection connection) throws SQLException {

        mappedQuery = QueryParser.parseNPSql(npSqlString);
        statement = connection.prepareStatement(mappedQuery.getTranslatedQuery());
    }

    public NPPreparedStatement(String npSqlString, Connection connection, boolean cacheSqlParsing) throws SQLException {

        mappedQuery = QueryParser.parseNPSql(npSqlString, cacheSqlParsing);
        statement = connection.prepareStatement(mappedQuery.getTranslatedQuery());
    }

    public void setObject(String paramName, Object object) throws SQLException {
        statement.setObject(mappedQuery.getParamIndex(paramName), object);
    }

    public void setObject(String paramName, Object object, SQLType sqlType) throws SQLException {
        statement.setObject(mappedQuery.getParamIndex(paramName), object, sqlType);
    }

    public Result2Bean getResult2Bean() throws SQLException {
        return new Result2Bean(statement.getResultSet());
    }

    public <T> void setParameters(T object) throws SQLException {
        setStatementParameters(statement, mappedQuery, object);
    }

    static <T> void setStatementParameters(PreparedStatement statement, MappedQuery mappedQuery ,T object)
        throws SQLException {
        try {
            DeepAnnotationInfo annotationInfo = getAnnotationInfo(object.getClass());
            for (MappedInfo mappedInfo : annotationInfo.shallowInfo().mappedInfoList()) {
                statement.setObject(
                    mappedQuery.getParamIndex(mappedInfo.finalizedName()),
                    mappedInfo.getter().invoke(object),
                    mappedInfo.annotation().type()
                );
            }

            for (EmbeddedInfo embeddedField : annotationInfo.shallowInfo().embeddedInfoList()) {
                setStatementParameters(statement, mappedQuery, embeddedField.getter().invoke(object));
            }
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        statement.close();
    }

}
