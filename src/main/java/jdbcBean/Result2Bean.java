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

import jdbcBean.exception.JDBCBeanException;
import org.jetbrains.annotations.Nullable;

import static jdbcBean.BeanUtil.*;
import static jdbcBean.BeanToManyUtil.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import jdbcBean.annotation.*;

public record Result2Bean(ResultSet resultSet) implements AutoCloseable {

    @Override
    public void close() throws Exception {
        resultSet.close();
    }

    /**
     * Get first row, first column of the ResultSet and close that ResultSet.
     * */
    public <T> @Nullable T getScalar() throws SQLException {
        try (resultSet) {
            if (resultSet.next()) {
                return (T) resultSet.getObject(1);
            }
            else return null;
        }
    }

    /**
     * Get data from {@link ResultSet} as a list of Java Bean and close that {@link ResultSet}.
     * <br/>
     * For this method to work as intended, the Java Bean's fields must be appropriately annotated
     * with {@link  Mapped}, {@link Embedded} and {@link ToMany}.
     * <br/>
     * See documentation of each mentioned annotation for more details.
     *
     * @param clazz List element type
     * @return A list of Java Bean
     * @throws SQLException thrown by JDBC
     */
    public <T> List<T> getList(Class<T> clazz) throws SQLException {

        List<DeepAnnotationInfo> annotationInfoList = getDeepToManyInfo(clazz);
        List<T> outputList = new ArrayList<>();

        if (annotationInfoList.size() == 1) {
            try (resultSet) {
                DeepAnnotationInfo annotationInfo = annotationInfoList.get(0);

                while (resultSet.next()) {
                    outputList.add(flatRowToObject(resultSet, annotationInfo.shallowInfo()));
                }
                return outputList;
            }
        }

        try (resultSet) {
            Object[] curFrontObjects = new Object[annotationInfoList.size()];
            boolean[] equalStatuses = new boolean[annotationInfoList.size()];

            while (resultSet.next()) {
                // replace element of curRowObjects with elements from this row
                // update equal statuses
                for (int i = 0; i < curFrontObjects.length; i++) {

                    DeepAnnotationInfo curInfo = annotationInfoList.get(i);
                    Object newDistinctVal = resultSet.getObject(curInfo.distinctInfo().mappedInfo().finalizedName());

                    equalStatuses[i] = Objects.equals(
                        // the distinct field of new object
                        newDistinctVal,
                        // the distinct field of old object
                        curFrontObjects[i] == null ? null : curInfo.distinctInfo().accessor().exec(curFrontObjects[i])
                    );

                    if (!equalStatuses[i]) {
                        curFrontObjects[i] = newDistinctVal != null ?
                            flatRowToObject(resultSet, curInfo.shallowInfo()) : null;
                    }
                }

                for (int i = curFrontObjects.length - 1; i >= 1; i--) {
                    if ((!equalStatuses[i] || !equalStatuses[i - 1])) {
                        addToNestedCollection(annotationInfoList.get(i - 1), curFrontObjects[i - 1], curFrontObjects[i]);
                    }
                }
                if (!equalStatuses[0]) {
                    outputList.add((T) curFrontObjects[0]);
                }
            }
            return outputList;
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

    }


    private void addToNestedCollection(DeepAnnotationInfo parentObjectInfo, Object parentObject, @Nullable Object objectToAdd)
        throws ReflectiveOperationException {

        ToManyInfo toManyInfo = parentObjectInfo.toManyInfo();
        Object collection = toManyInfo.accessor().exec(parentObject);

        if (collection == null) {
            toManyInfo.deepSetter().exec(
                parentObject,
                toManyInfo.field().getType().cast(new ArrayList<>())
            );
        }

        if (objectToAdd != null) {
            ((List<Object>) toManyInfo.accessor().exec(parentObject)).add(objectToAdd);
        }


    }

    /**
     * Get first row of the {@link ResultSet} as a Java Bean, then close {@link ResultSet}.
     * @param clazz Java Bean class
     * @return null result is empty
     * @throws SQLException thrown by JDBC
     */
    public <T> @Nullable T getFirst(Class<T> clazz) throws SQLException {
        try (resultSet) {
            if (resultSet.next()) {
                return flatRowToObject(resultSet, getAnnotationInfo(clazz).shallowInfo());
            }
            else return null;
        }
    }

    private static <T> T flatRowToObject(ResultSet resultSet, ShallowAnnotationInfo annotationInfo) throws SQLException {
        try {
            T output = (T) annotationInfo.noArgsConstructor().newInstance();

            for (MappedInfo mappedInfo : annotationInfo.mappedInfoList()) {
                mappedInfo.setter().invoke(
                    output,
                    mappedInfo.field().getType().cast(
                        resultSet.getObject(mappedInfo.finalizedName())
                    )
                );

            }

            for (EmbeddedInfo embeddedField : annotationInfo.embeddedInfoList()) {
                Object embeddedObject = flatRowToObject(resultSet, embeddedField.annotationInfo());
                embeddedField.setter().invoke(output, embeddedObject);
            }

            return output;
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

    }
}
