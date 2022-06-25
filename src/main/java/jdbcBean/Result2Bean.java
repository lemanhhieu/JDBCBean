package jdbcBean;

import static jdbcBean.BeanUtil.*;
import static jdbcBean.BeanToManyUtil.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public record Result2Bean(ResultSet resultSet) implements AutoCloseable {

    @Override
    public void close() throws Exception {
        resultSet.close();
    }

    public <T> T getScalar() throws SQLException {
        try (resultSet) {
            resultSet.next();
            return (T) resultSet.getObject(1);
        }
    }

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

                    if (curFrontObjects[i] != null) {
                        equalStatuses[i] = Objects.equals(
                            // the distinct field of new object
                            resultSet.getObject(curInfo.distinctInfo().mappedInfo().finalizedName()),
                            // the distinct field of old object
                            curInfo.distinctInfo().accessor().exec(curFrontObjects[i])
                        );
                    }
                    else {
                        equalStatuses[i] = false;
                    }

                    if (!equalStatuses[i]) {
                        curFrontObjects[i] = flatRowToObject(resultSet, curInfo.shallowInfo());
                    }
                }

                for (int i = curFrontObjects.length - 1; i >= 1; i--) {
                    if (!equalStatuses[i] || !equalStatuses[i - 1]) {
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


    private void addToNestedCollection(DeepAnnotationInfo parentObjectInfo, Object parentObject, Object objectToAdd)
        throws ReflectiveOperationException {

        ToManyInfo toManyInfo = parentObjectInfo.toManyInfo();
        Object collection = toManyInfo.accessor().exec(parentObject);
        if (collection == null) {
            List<Object> newCollection = new ArrayList<>();
            newCollection.add(objectToAdd);
           toManyInfo.deepSetter().exec(
                parentObject,
                toManyInfo.field().getType().cast(newCollection)
            );
        } else {
            ((List<Object>) collection).add(objectToAdd);
        }
    }

    public <T> T getFirst(Class<T> clazz) throws SQLException {
        resultSet.next();
        T output = flatRowToObject(resultSet, getAnnotationInfo(clazz).shallowInfo());
        resultSet.close();
        return output;
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
