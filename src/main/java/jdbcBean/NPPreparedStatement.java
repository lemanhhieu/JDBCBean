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

import lombok.Getter;

import java.sql.*;
import java.util.List;
import java.util.Map;

import static jdbcBean.BeanUtil.*;

/**
 * Allow you to assign SQL parameter by name and Java Bean DTO. Example:
 * <pre>{@code
 * try (NPPreparedStatement statement = new NPPreparedStatement("""
 *
 *      SELECT * FROM inventory WHERE created_at >= :created_at
 *
 *      """, connection)
 * ) {
 *      List<InventoryDTO> inventories = statement
 *          .setParameters(inventoryQueryDTO)
 *          .execute()
 *          .getList(InventoryDTO.class)
 * }</pre>
 */
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

    public NPPreparedStatement setObject(String paramName, Object object) throws SQLException {
        statement.setObject(mappedQuery.getParamIndex(paramName), object);
        return this;
    }

    public NPPreparedStatement setObject(String paramName, Object object, SQLType sqlType) throws SQLException {
        statement.setObject(mappedQuery.getParamIndex(paramName), object, sqlType);
        return this;
    }

    /**
     * A method to wrap this statement's {@link ResultSet} in {@link Result2Bean}
     */
    public Result2Bean getResult2Bean() throws SQLException {
        return new Result2Bean(statement.getResultSet());
    }

    public <T> NPPreparedStatement setParameters(T object) throws SQLException {
        setStatementParameters(statement, mappedQuery, object);
        return this;
    }


    /**
     * This method is useful when you want to build a dynamic query.
     * @param parameters A dictionary of parameter's name and parameter's value
     * @throws SQLException thrown by JDBC
     */
    public NPPreparedStatement setParametersFroMap(Map<String, SqlValue> parameters) throws SQLException {
        for (var entry : parameters.entrySet()) {
            setObject(entry.getKey(), entry.getValue().value(), entry.getValue().sqlType());
        }
        return this;
    }

    static <T> void setStatementParameters(PreparedStatement statement, MappedQuery mappedQuery ,T object)
        throws SQLException {
        try {
            DeepAnnotationInfo annotationInfo = getAnnotationInfo(object.getClass());
            for (MappedInfo mappedInfo : annotationInfo.shallowInfo().mappedInfoList()) {
                statement.setObject(
                    mappedQuery.getParamIndex(mappedInfo.finalizedName()),
                    mappedInfo.getter().invoke(object),
                    mappedInfo.annotation().type().getVendorTypeNumber()
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

    public NPPreparedStatement execute() throws SQLException {
        statement.execute();
        return this;
    }



    public <T> List<T> getList(Class<T> clazz) throws SQLException {
        return new Result2Bean(statement.getResultSet()).getList(clazz);
    }

    public <T> T getFirst(Class<T> clazz) throws SQLException {
        return new Result2Bean(statement.getResultSet()).getFirst(clazz);
    }

    public <T> T getScalar() throws SQLException {
        return new Result2Bean(statement.getResultSet()).getScalar();
    }

    @Override
    public void close() throws Exception {
        statement.close();
    }

}
