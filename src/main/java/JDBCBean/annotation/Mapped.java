package JDBCBean.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.JDBCType;


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mapped {

    /**
     * Name of the column / sql parameter.
     * If empty, snake-case converted field name will be used instead.
     */
    String name() default "";

    /**
     * Name of one of the type specified in {@link java.sql.JDBCType}.
     * Ignored if empty.
     */
    JDBCType type();
    boolean isDistinct() default false;
}
