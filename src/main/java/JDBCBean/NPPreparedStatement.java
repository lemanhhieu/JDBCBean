package JDBCBean;

import lombok.Getter;

import java.sql.*;
import static JDBCBean.BeanUtil.*;

public class NPPreparedStatement implements AutoCloseable {

    @Getter
    private final PreparedStatement preparedStatement;

    @Getter
    private final MappedQuery mappedQuery;

    public NPPreparedStatement(String npSqlString, Connection connection) throws SQLException {

        mappedQuery = QueryParser.parseNPSql(npSqlString);
        preparedStatement = connection.prepareStatement(mappedQuery.getTranslatedQuery());
    }

    public void setObject(String paramName, Object object) throws SQLException {
        preparedStatement.setObject(mappedQuery.getParamIndex(paramName), object);
    }

    public void setObject(String paramName, Object object, SQLType sqlType) throws SQLException {
        preparedStatement.setObject(mappedQuery.getParamIndex(paramName), object, sqlType);
    }

    public Result2Bean getResultSet() throws SQLException {
        return new Result2Bean(preparedStatement.getResultSet());
    }

    public <T> void setParameters(T object) throws ReflectiveOperationException, SQLException {
        DeepAnnotationInfo annotationInfo = getAnnotationInfo(object.getClass());
        for (MappedInfo mappedInfo : annotationInfo.shallowInfo().mappedInfoList()) {
            preparedStatement.setObject(
                mappedQuery.getParamIndex(mappedInfo.finalizedName()),
                mappedInfo.getter().invoke(object),
                mappedInfo.annotation().type()
            );
        }

        for (EmbeddedInfo embeddedField : annotationInfo.shallowInfo().embeddedInfoList()) {
            setParameters(embeddedField.getter().invoke(object));
        }
    }

    @Override
    public void close() throws Exception {
        preparedStatement.close();
    }
}
