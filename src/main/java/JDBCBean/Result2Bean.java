package JDBCBean;

import lombok.val;

import static JDBCBean.BeanUtil.*;

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
            return (T) resultSet.getObject(1);
        }
    }

    public <T> List<T> getList(Class<T> clazz) throws SQLException, ReflectiveOperationException {
        try (resultSet) {
            LinkedList<T> outputList = new LinkedList<>();
            AnnotationInfo annotationInfo = getAnnotationInfo(clazz);

            while (resultSet.next()) {
                outputList.add(flatRowToObject(resultSet, annotationInfo));
            }
            return outputList;
        }
    }


    public <T> List<T> getListWithNestedToMany(Class<T> clazz) throws SQLException, ReflectiveOperationException {

        List<AnnotationInfo> annotationInfoList = getToManyInfo(clazz);

        List<T> outputList = new ArrayList<>();
        Object[] curFrontObjects = new Object[annotationInfoList.size()];
        boolean[] equalStatuses = new boolean[annotationInfoList.size()];

        while (resultSet.next()) {
            // replace element of curRowObjects with elements from this row
            // update equal statuses
            for (int i = 0; i < curFrontObjects.length; i++) {

                AnnotationInfo curInfo = annotationInfoList.get(i);

                if (curFrontObjects[i] != null) {
                    equalStatuses[i] = Objects.equals(
                        resultSet.getObject(curInfo.distinctField().finalizedName()),
                        curInfo.distinctField().fieldInfo().getter().exec(curFrontObjects[i])
                    );
                }
                else {
                    equalStatuses[i] = false;
                }

                if (!equalStatuses[i]) {
                    curFrontObjects[i] = flatRowToObject(resultSet, curInfo);
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
        resultSet.close();

        return outputList;
    }


    private void addToNestedCollection(AnnotationInfo parentObjectInfo, Object parentObject, Object objectToAdd)
        throws ReflectiveOperationException {

        Object collection = parentObjectInfo.toMany().getter().exec(parentObject);
        if (collection == null) {
            List<Object> newCollection = new ArrayList<>();
            newCollection.add(objectToAdd);
            parentObjectInfo.toMany().setter().exec(
                parentObject,
                parentObjectInfo.toMany().field().getType().cast(newCollection)
            );
        } else {
            ((List<Object>) collection).add(objectToAdd);
        }
    }

    public <T> T getFirst(Class<T> clazz) throws SQLException, ReflectiveOperationException {
        T output = flatRowToObject(resultSet, getAnnotationInfo(clazz));
        resultSet.close();
        return output;
    }

    private static <T> T flatRowToObject(ResultSet resultSet, AnnotationInfo annotationInfo) throws ReflectiveOperationException, SQLException {
        T output = (T) annotationInfo.noArgsConstructor().newInstance();

        for (val mappedField : annotationInfo.mapped()) {
            mappedField.fieldInfo().setter().exec(
                output,
                mappedField.fieldInfo().field().getType().cast(
                    resultSet.getObject(mappedField.finalizedName())
                )
            );

        }

        for (val embeddedField : annotationInfo.embeddedFields()) {
            embeddedField.fieldInfo().setter().exec(output,
                flatRowToObject(resultSet, embeddedField.embeddedAnnotationInfo()
            ));
        }
        return output;
    }
}
