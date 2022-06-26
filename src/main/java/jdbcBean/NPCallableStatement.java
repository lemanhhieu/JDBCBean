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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import static jdbcBean.BeanUtil.*;



public class NPCallableStatement implements AutoCloseable {

    @Getter
    private final CallableStatement statement;

    @Getter
    private final MappedQuery mappedQuery;

    public NPCallableStatement(String npSqlString, Connection connection) throws SQLException {
        mappedQuery = QueryParser.parseNPSql(npSqlString);
        statement = connection.prepareCall(mappedQuery.getTranslatedQuery());
    }

    public NPCallableStatement(String npSqlString, Connection connection, boolean cacheSqlParsing) throws SQLException {
        mappedQuery = QueryParser.parseNPSql(npSqlString, cacheSqlParsing);
        statement = connection.prepareCall(mappedQuery.getTranslatedQuery());
    }


    public void registerOutParameters(Class<?> clazz) throws SQLException {
        DeepAnnotationInfo deepAnnotationInfo = getAnnotationInfo(clazz);

        for (MappedInfo mappedInfo : deepAnnotationInfo.shallowInfo().mappedInfoList()) {
            statement.registerOutParameter(
                mappedQuery.getParamIndex(mappedInfo.finalizedName()),
                mappedInfo.annotation().type()
            );
        }

        for (EmbeddedInfo embeddedInfo : deepAnnotationInfo.shallowInfo().embeddedInfoList()) {
            registerOutParameters(embeddedInfo.field().getType());
        }
    }

    public <T> T getObject(String paramName) throws SQLException {
        return (T) statement.getObject(mappedQuery.getParamIndex(paramName));
    }

    public <T> T getReturnedOutParameters(Class<T> clazz) throws SQLException {
        DeepAnnotationInfo deepAnnotationInfo = getAnnotationInfo(clazz);
        try {
            T out = (T) deepAnnotationInfo.shallowInfo().noArgsConstructor().newInstance();

            for (MappedInfo mappedInfo : deepAnnotationInfo.shallowInfo().mappedInfoList()) {
                mappedInfo.setter().invoke(out, statement.getObject(mappedInfo.finalizedName()));
            }

            for (EmbeddedInfo embeddedInfo : deepAnnotationInfo.shallowInfo().embeddedInfoList()) {
                embeddedInfo.setter().invoke(out, getReturnedOutParameters(embeddedInfo.field().getType()));
            }
            return out;
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void setParameters(T object) throws SQLException {
        NPPreparedStatement.setStatementParameters(statement, mappedQuery, object);
    }

    @Override
    public void close() throws Exception {
        statement.close();
    }
}
