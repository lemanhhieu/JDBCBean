package JDBCBean;

import java.lang.reflect.Field;

record FieldInfo(
    Field field,
    FieldGetter getter,
    FieldSetter setter
) {
}
