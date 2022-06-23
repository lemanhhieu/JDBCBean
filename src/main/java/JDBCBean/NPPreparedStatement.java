package JDBCBean;

import lombok.Getter;
import lombok.val;

import java.sql.*;

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
        AnnotationInfo annotationInfo = BeanUtil.getAnnotationInfo(object.getClass());

        for (AnnotationInfo.MappedInfo mappedInfo : annotationInfo.mapped()) {
            preparedStatement.setObject(
                mappedQuery.getParamIndex(mappedInfo.finalizedName()),
                mappedInfo.fieldInfo().getter().exec(object),
                mappedInfo.mappedAnnotation().type()
            );
        }

        for (val embeddedField : annotationInfo.embeddedFields()) {
            setParameters(embeddedField.fieldInfo().getter().exec(object));
        }
    }

    @Override
    public void close() throws Exception {
        preparedStatement.close();
    }
}
