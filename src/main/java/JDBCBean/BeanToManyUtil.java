package JDBCBean;

import lombok.val;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static JDBCBean.JDBCUtil.assertTrue;
import static JDBCBean.BeanUtil.*;
class BeanToManyUtil {
    public static List<DeepAnnotationInfo> getDeepToManyInfo(Class<?> clazz) {

        List<DeepAnnotationInfo> annotationInfoList = new ArrayList<>();
        DeepAnnotationInfo curAnnotationInfo = getAnnotationInfo(clazz);

        if (curAnnotationInfo.toManyInfo() == null) {
            return Collections.singletonList(curAnnotationInfo);
        }

        if (getCollectionElementType(curAnnotationInfo.toManyInfo().field()).equals(clazz)) {
            toManyRecursiveMode(curAnnotationInfo, annotationInfoList);
        }
        else {
            toManyNormalMode(curAnnotationInfo, annotationInfoList);
        }

        return annotationInfoList;
    }

    private static void toManyRecursiveMode(
        DeepAnnotationInfo start,
        List<DeepAnnotationInfo> annotationInfoList
    ) {
        assertTrue(
            start.toManyInfo() != null && start.distinctInfo() != null,
            "Class %s doesn't have both ToMany annotated and distinct field"
                .formatted(start.shallowInfo().noArgsConstructor().getDeclaringClass().getName())
        );

        int recursiveDepth = start.toManyInfo().annotation().recursiveDepth();

        for (int i = 1; i <= recursiveDepth; i++) {
            DistinctInfo newDistinctInfo = new DistinctInfo(
                start.distinctInfo().accessor(),
                new MappedInfo(
                    "%s_%s".formatted(start.distinctInfo().mappedInfo().finalizedName(), i),
                    start.distinctInfo().mappedInfo().getter(),
                    start.distinctInfo().mappedInfo().setter(),
                    start.distinctInfo().mappedInfo().field(),
                    start.distinctInfo().mappedInfo().annotation()
                )
            );

            annotationInfoList.add(new DeepAnnotationInfo(
                appendNumberToFieldName(i, start.shallowInfo()),
                newDistinctInfo,
                start.toManyInfo()
            ));
        }
    }

    private static ShallowAnnotationInfo appendNumberToFieldName(
        int number,
        BeanUtil.ShallowAnnotationInfo annotationInfo
    ) {
        val newMappedInfoList = annotationInfo
            .mappedInfoList()
            .stream().map(mappedInfo -> new MappedInfo(
                "%s_%s".formatted(mappedInfo.finalizedName(), number),
                mappedInfo.getter(),
                mappedInfo.setter(),
                mappedInfo.field(),
                mappedInfo.annotation()
            ))
            .toList();

        val newEmbeddedInfoList = annotationInfo
            .embeddedInfoList()
            .stream().map(embeddedInfo -> new EmbeddedInfo(
                embeddedInfo.getter(),
                embeddedInfo.setter(),
                embeddedInfo.field(),
                appendNumberToFieldName(number, embeddedInfo.annotationInfo()),
                embeddedInfo.annotation()
            ))
            .toList();

        return new ShallowAnnotationInfo(
            annotationInfo.noArgsConstructor(),
            newMappedInfoList,
            newEmbeddedInfoList
        );
    }

    private static void toManyNormalMode(
        DeepAnnotationInfo start,
        List<DeepAnnotationInfo> annotationInfoList
    ) {
        DeepAnnotationInfo curAnnotationInfo = start;

        while (true) {

            assertTrue(
                curAnnotationInfo.distinctInfo() != null,
                "Class %s doesn't have distinct field"
                    .formatted(curAnnotationInfo.shallowInfo().noArgsConstructor().getDeclaringClass().getName())
            );

            annotationInfoList.add(curAnnotationInfo);

            if (curAnnotationInfo.toManyInfo() == null) {
                break;
            }
            else {
                Class<?> listElementType = getCollectionElementType(
                    curAnnotationInfo.toManyInfo().field()
                );
                curAnnotationInfo = getAnnotationInfo(listElementType);
            }
        }
    }

    private static Class<?> getCollectionElementType(Field field) {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }
}
