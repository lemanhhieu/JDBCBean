package JDBCBean;

@FunctionalInterface
interface FieldSetter {
    void exec(Object o, Object val) throws ReflectiveOperationException;
}
