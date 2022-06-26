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

import jdbcBean.annotation.Embedded;
import jdbcBean.annotation.Mapped;
import jdbcBean.annotation.ToMany;
import jdbcBean.exception.JDBCBeanException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static jdbcBean.JDBCUtil.*;

class BeanUtil {

    public static void fieldsConsumer(Class<?> clazz, FieldConsumer fieldConsumer) {
        for (Class<?> curClass = clazz; curClass != null; curClass = curClass.getSuperclass()) {
            for (Field field : curClass.getDeclaredFields()) {
                try {
                    fieldConsumer.accept(field);
                }
                catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static final Map<Class<?>, DeepAnnotationInfo> cachedDeepAnnotationInfo = new ConcurrentHashMap<>();

    public static DeepAnnotationInfo getAnnotationInfo(Class<?> clazz) {
        DeepAnnotationInfo cachedResult = cachedDeepAnnotationInfo.get(clazz);
        if (cachedResult != null) return cachedResult;

        AtomicReference<DistinctInfo> distinctInfo = new AtomicReference<>(null);
        AtomicReference<ToManyInfo> toManyInfo = new AtomicReference<>(null);

        ShallowAnnotationInfo shallowAnnotationInfo = getAnnotationInfo(distinctInfo, toManyInfo, o -> o, clazz);
        DeepAnnotationInfo deepAnnotationInfo = new DeepAnnotationInfo(
            shallowAnnotationInfo,
            distinctInfo.get(),
            toManyInfo.get()
        );
        cachedDeepAnnotationInfo.put(clazz, deepAnnotationInfo);
        return deepAnnotationInfo;
    }

    private static ShallowAnnotationInfo getAnnotationInfo(
        AtomicReference<@Nullable DistinctInfo> distinctInfo,
        AtomicReference<@Nullable ToManyInfo> toManyInfo,
        Accessor objectAccessor,
        Class<?> clazz
    ) {
        Constructor<?> noArgsConstructor;
        try {
            noArgsConstructor = clazz.getConstructor();
        }
        catch (NoSuchMethodException e) {
            throw new JDBCBeanException(String.format("Can't find no args constructor for %s", clazz.getName()));
        }

        List<MappedInfo> mappedInfoList = new ArrayList<>();
        List<EmbeddedInfo> embeddedInfoList = new ArrayList<>();


        fieldsConsumer(clazz, field -> {
            @Nullable Mapped mapped = field.getAnnotation(Mapped.class);
            @Nullable Embedded embedded = field.getAnnotation(Embedded.class);
            @Nullable ToMany toMany = field.getAnnotation(ToMany.class);

            if (mapped != null) {
                Method getter = getGetter(field);
                MappedInfo mappedInfo = new MappedInfo(
                    mapped.name().isEmpty() ? convertCamelCaseToSnakeCase(field.getName()) : mapped.name(),
                    getGetter(field),
                    getSetter(field),
                    field,
                    mapped
                );
                mappedInfoList.add(mappedInfo);

                if (mapped.isDistinct() && distinctInfo.get() == null) {
                    distinctInfo.set(new DistinctInfo(
                        o -> getter.invoke(objectAccessor.exec(o)),
                        mappedInfo
                    ));
                }
            }
            else if (embedded != null) {
                Method getter = getGetter(field);
                embeddedInfoList.add(new EmbeddedInfo(
                    getter,
                    getSetter(field),
                    field,
                    getAnnotationInfo(
                        distinctInfo,
                        toManyInfo,
                        o -> getter.invoke(objectAccessor.exec(o)),
                        field.getType()
                    ),
                    embedded
                ));
            }
            else if (toMany != null) {
                if (toManyInfo.get() != null) {
                    throw new JDBCBeanException(
                        "Cannot use annotation ToMany on field %s of %s because field %s of %s is already annotated with ToMany."
                            .formatted(
                                field.getName(),
                                field.getDeclaringClass().getName(),
                                toManyInfo.get().field().getName(),
                                toManyInfo.get().field().getDeclaringClass()
                            )
                    );
                }
                if (!field.getType().equals(List.class)) {
                    throw new JDBCBeanException("Collection type of field %s must be %s"
                        .formatted(field.getName(), List.class.getName())
                    );
                }
                toManyInfo.set(new ToManyInfo(
                    o -> getGetter(field).invoke(objectAccessor.exec(o)),
                    (o, val) -> getSetter(field).invoke(objectAccessor.exec(o), val),
                    field,
                    toMany
                ));


            }
        });

        return new ShallowAnnotationInfo(
            noArgsConstructor,
            mappedInfoList,
            embeddedInfoList
        );
    }

    public record DeepAnnotationInfo(
        @NotNull ShallowAnnotationInfo shallowInfo,
        @Nullable DistinctInfo distinctInfo,
        @Nullable ToManyInfo toManyInfo
    ) {}

    public record ShallowAnnotationInfo(
        @NotNull Constructor<?> noArgsConstructor,
        @NotNull List<MappedInfo> mappedInfoList,
        @NotNull List<EmbeddedInfo> embeddedInfoList
    ) {
    }


    public record MappedInfo(
        @NotNull String finalizedName,
        @NotNull Method getter,
        @NotNull Method setter,
        @NotNull Field field,
        @NotNull Mapped annotation
    ) {
    }

    public record EmbeddedInfo(
        @NotNull Method getter,
        @NotNull Method setter,
        @NotNull Field field,
        @NotNull ShallowAnnotationInfo annotationInfo,
        @NotNull Embedded annotation
    ) {
    }

    public record DistinctInfo(
        @NotNull Accessor accessor,
        @NotNull MappedInfo mappedInfo
    ) {
    }

    public record ToManyInfo(
        @NotNull Accessor accessor,
        @NotNull DeepSetter deepSetter,
        @NotNull Field field,
        @NotNull ToMany annotation
    ) {
    }

    @FunctionalInterface
    public interface FieldConsumer {
        void accept(Field field) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    public interface Accessor {
        Object exec(Object o) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    public interface DeepSetter {
        void exec(Object o, Object val) throws ReflectiveOperationException;
    }

    public static Method getGetter(Field field) {
        if (field.getDeclaringClass().isRecord()) {
            try {
                return field.getDeclaringClass().getMethod(field.getName());
            } catch (ReflectiveOperationException e) {
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
