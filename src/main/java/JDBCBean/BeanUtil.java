package JDBCBean;

import JDBCBean.annotation.Embedded;
import JDBCBean.annotation.Mapped;
import JDBCBean.annotation.ToMany;
import JDBCBean.exception.JDBCBeanException;
import lombok.val;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static JDBCBean.JDBCUtil.*;
class BeanUtil {

    private static final Map<Class<?>, AnnotationInfo> cachedAnnotationInfo = new ConcurrentHashMap<>();
    public static AnnotationInfo getAnnotationInfo(Class<?> clazz) {
        AnnotationInfo cachedResult = cachedAnnotationInfo.get(clazz);
        if (cachedResult != null) {
            return cachedResult;
        }

        Constructor<?> noArgsConstructor;
        try {
            noArgsConstructor = clazz.getConstructor();
        }
        catch (NoSuchMethodException e) {
            throw new JDBCBeanException(String.format("Can't find no args constructor for %s", clazz.getName()));
        }
        Collection<AnnotationInfo.MappedInfo> mappedFields = new LinkedList<>();
        Collection<AnnotationInfo.EmbeddedInfo> embeddedFields = new LinkedList<>();
        FieldInfo toMany = null;
        AnnotationInfo.MappedInfo distinctField = null;

        for (Class<?> curClass = clazz; curClass != null; curClass = curClass.getSuperclass()) {

            for (Field field : curClass.getDeclaredFields()) {

                Mapped mapped = field.getAnnotation(Mapped.class);
                if (mapped != null) {

                    FieldInfo fieldInfo = createFieldInfo(field);
                    String finalizedName = mapped.name().isEmpty() ?
                        convertCamelCaseToSnakeCase(field.getName())
                        : mapped.name();
                    val mappedInfo = new AnnotationInfo.MappedInfo(finalizedName, fieldInfo, mapped);

                    mappedFields.add(mappedInfo);
                    if (mapped.isDistinct()) {
                        distinctField = mappedInfo;
                    }
                }
                else if (field.isAnnotationPresent(Embedded.class)) {
                    embeddedFields.add(new AnnotationInfo.EmbeddedInfo(createFieldInfo(field), getAnnotationInfo(field.getType())));
                }
                else if (field.isAnnotationPresent(ToMany.class)) {
                    if (!field.getType().equals(List.class)) {
                        throw new JDBCBeanException(String.format("Field \"%s\" is not of type %s", field.getName(), List.class.getName()));
                    }
                    toMany = createFieldInfo(field);
                }
            }
        }

        AnnotationInfo annotationInfo = new AnnotationInfo(
            clazz,
            noArgsConstructor,
            mappedFields,
            embeddedFields,
            toMany,
            distinctField
        );
        cachedAnnotationInfo.put(clazz, annotationInfo);
        return annotationInfo;
    }

    private static FieldInfo createFieldInfo(Field field) {
        return new FieldInfo(
            field,
            (o)-> getGetter(field).invoke(o),
            (o, val)-> getSetter(field).invoke(o, val)
        );
    }

    private static final Map<Class<?>, List<AnnotationInfo>> cachedToManyInfo = new ConcurrentHashMap<>();

    public static List<AnnotationInfo> getToManyInfo(Class<?> clazz) {
        val cachedResult = cachedToManyInfo.get(clazz);
        if (cachedResult != null) return cachedResult;

        ArrayList<AnnotationInfo> annotationInfoList = new ArrayList<>();
        AnnotationInfo curAnnotationInfo = getAnnotationInfo(clazz);

        // recursive mode
        if (
            curAnnotationInfo.toMany() != null
            && getCollectionElementType(curAnnotationInfo.toMany().field()).equals(clazz)
        ) {
            getToManyInfoRecursiveMode(annotationInfoList, curAnnotationInfo);
        }
        else {
            getToManyInfoNormalMode(annotationInfoList, curAnnotationInfo);
        }

        cachedToManyInfo.put(clazz, annotationInfoList);
        return annotationInfoList;
    }

    private static void getToManyInfoRecursiveMode(
        ArrayList<AnnotationInfo> annotationInfoList,
        AnnotationInfo curAnnotationInfo
    ) {
        int recursiveDepth = curAnnotationInfo
            .toMany().field()
            .getAnnotation(ToMany.class).recursiveDepth();

        for (int i = 1; i <= recursiveDepth; i++) {
            annotationInfoList.add(appendNumberToFieldName(i, curAnnotationInfo));
        }
    }

    private static AnnotationInfo appendNumberToFieldName(int number, AnnotationInfo annotationInfo) {
        val newMappedInfoList = annotationInfo.mapped()
            .stream()
            .map(mappedInfo -> new AnnotationInfo.MappedInfo(
                "%s_%s".formatted(mappedInfo.finalizedName(), number),
                mappedInfo.fieldInfo(),
                mappedInfo.mappedAnnotation()
            ))
            .toList();

        val newEmbeddedFields = annotationInfo.embeddedFields().stream()
            .map(embeddedInfo -> new AnnotationInfo.EmbeddedInfo(
                embeddedInfo.fieldInfo(),
                appendNumberToFieldName(number, embeddedInfo.embeddedAnnotationInfo())
            ))
            .toList();

        return new AnnotationInfo(
            annotationInfo.clazz(),
            annotationInfo.noArgsConstructor(),
            newMappedInfoList,
            newEmbeddedFields,
            annotationInfo.toMany(),
            new AnnotationInfo.MappedInfo(
                "%s_%s".formatted(annotationInfo.distinctField().finalizedName(), number),
                annotationInfo.distinctField().fieldInfo(),
                annotationInfo.distinctField().mappedAnnotation()
            )
        );
    }

    private static void getToManyInfoNormalMode(
        ArrayList<AnnotationInfo> annotationInfoList,
        AnnotationInfo curAnnotationInfo
    ) {
        while (true) {

            assertTrue(
                curAnnotationInfo.distinctField() != null,
                String.format("No distinct field found for \"%s\"", curAnnotationInfo.clazz().getName())
            );
            annotationInfoList.add(curAnnotationInfo);
            if (curAnnotationInfo.toMany() == null) {
                break;
            }
            else {
                Class<?> listElementType = getCollectionElementType(curAnnotationInfo.toMany().field());
                curAnnotationInfo = getAnnotationInfo(listElementType);
            }
        }
    }
    private static Class<?> getCollectionElementType(Field field) {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }

    public static Method getGetter(Field field) {
        if (field.getDeclaringClass().isRecord()) {
            try {
                return field.getDeclaringClass().getMethod(field.getName());
            }
            catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        boolean isBool = field.getType().isPrimitive() && field.getType().equals(boolean.class);
        String name = (isBool ? "is" : "get") +
            Character.toUpperCase(field.getName().charAt(0)) +
            field.getName().substring(1);
        try {
            return field.getDeclaringClass().getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new JDBCBeanException("Cannot find public getter for " + field.getName());
        }
    }

    public static Method getSetter(Field field) {
        if (field.getDeclaringClass().isRecord()) {
            throw new JDBCBeanException("Record %s doesn't have setters".formatted(field.getDeclaringClass()));
        }

        String name = "set" +
            Character.toUpperCase(field.getName().charAt(0)) +
            field.getName().substring(1);
        try {
            return field.getDeclaringClass().getMethod(name, field.getType());
        } catch (NoSuchMethodException e) {
            throw new JDBCBeanException("Cannot find public setter with argument type " + field.getType() + " for " + field.getName());
        }
    }
}
