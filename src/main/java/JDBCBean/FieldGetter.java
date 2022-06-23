package JDBCBean;

@FunctionalInterface
interface FieldGetter {
    Object exec(Object o) throws ReflectiveOperationException;
}
