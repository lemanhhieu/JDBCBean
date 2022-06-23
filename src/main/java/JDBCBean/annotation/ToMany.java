package JDBCBean.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Use for left-join/inner-join select statement to store to-many rows as a collection.
 * Only {@link java.util.List} is supported
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToMany {

    /**
     * This attribute is used only when the collection element type of the annotated field
     * is exactly the same as the class
     * (no superclass allowed and will result in unintended behaviour).
     */
    int recursiveDepth() default 0;
}
