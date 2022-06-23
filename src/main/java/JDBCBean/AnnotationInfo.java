package JDBCBean;

import JDBCBean.annotation.Mapped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;

record AnnotationInfo (
    @NotNull Class<?> clazz,
    @NotNull Constructor<?> noArgsConstructor,
    @NotNull Collection<MappedInfo> mapped,
    @Nullable Collection<EmbeddedInfo> embeddedFields,
    @Nullable FieldInfo toMany,
    @Nullable MappedInfo distinctField
) {
    public record MappedInfo(
        String finalizedName,
        FieldInfo fieldInfo,
        Mapped mappedAnnotation
    ) {
    }

    public record EmbeddedInfo(
        FieldInfo fieldInfo,
        AnnotationInfo embeddedAnnotationInfo
    ){}
}
