package com.babyorm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.babyorm.ReflectiveUtils.*;

/**
 * A repo, baby
 * <p>
 * To use this, try one of: new BabyRepo<Foo>(){}; or new BabyRepo<Foo>(Foo.class); or BabyRepo.forType(Foo.class);
 *
 * @param <T> The type of entity this repo likes the most
 */
public class BabyRepo<T> {

    /**
     * the column getter methods on the ResultSet class
     */
    private static final Map<Class<?>, Method> GETTERS =
            addKeySuperTypes(
                    addPrimitivesToMap(
                            findMethods(
                                    ResultSet.class.getMethods(),
                                    Method::getReturnType,
                                    m -> m.getName().startsWith("get"),
                                    m -> !m.getName().startsWith("getN"),
                                    m -> m.getParameterCount() == 1,
                                    m -> m.getParameterTypes()[0].equals(String.class))));

    /**
     * the parameter setter methods on the PreparedStatement class
     */
    private static final Map<Class<?>, Method> SETTERS =
            addKeySuperTypes(
                    addPrimitivesToMap(
                            findMethods(
                                    PreparedStatement.class.getMethods(),
                                    m -> m.getParameterTypes()[1],
                                    m -> m.getName().startsWith("set"),
                                    m -> !m.getName().startsWith("setN"),
                                    m -> m.getParameterCount() == 2,
                                    m -> m.getParameterTypes()[0].equals(Integer.TYPE))));

    private Class<T> entityType;
    private List<Field> fields, nonKeyFields;
    private String byKeySql, baseSql, updateSql, insertSqlNoKey, insertSql, deleteSql;
    private static Supplier<Connection> globalConnectionSupplier;
    private Supplier<Connection> localConnectionSupplier;
    private Field keyField;

    /**
     * Pretty straight forward, can't really screw this one up.
     */
    public BabyRepo(Class<T> entityType) {
        init(entityType);
    }

    /**
     * You MUST extend this class and specify your entity type on the class that directly extends
     * this class. try: new BabyRepo<Foo>(){};
     */
    public BabyRepo() {
        Type genericSuperclass = this.getClass().getGenericSuperclass();
        if (genericSuperclass == null || !(genericSuperclass instanceof ParameterizedType)) {
            throw new BabyDBException("You must extend BabyRepo to use the no-arg constructor. See class java doc for help");
        }
        init((Class<T>) ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0]);
    }

    /**
     * Set a local connection supplier to use instead of the global connection supplier
     */
    public BabyRepo(Supplier<Connection> connectionSupplier) {
        this();
        this.localConnectionSupplier = connectionSupplier;
    }

    /**
     * Set a local connection supplier to use instead of the global connection supplier
     */
    public BabyRepo(Class<T> entityType, Supplier<Connection> connectionSupplier) {
        this(entityType);
        this.localConnectionSupplier = connectionSupplier;
    }

    /**
     * Factory method to get a new repository
     */
    public static <E> BabyRepo<E> forType(Class<E> type) {
        return new BabyRepo<>(type);
    }

    /**
     * Set the global connection supplier to use across all repositories
     */
    public static void setGlobalConnectionSupplier(Supplier<Connection> globalConnectionSupplier) {
        BabyRepo.globalConnectionSupplier = globalConnectionSupplier;
    }

    public void setLocalConnectionSupplier(Supplier<Connection> localConnectionSupplier) {
        this.localConnectionSupplier = localConnectionSupplier;
    }

    private void init(Class<T> entityType) {
        this.entityType = entityType;
        this.fields = Arrays.asList(this.entityType.getDeclaredFields());
        this.fields.forEach(f -> f.setAccessible(true));
        this.keyField = findKeyField();
        this.keyField.setAccessible(true);
        this.nonKeyFields = fields.stream()
                .filter(f -> !keyField.equals(f))
                .collect(Collectors.toList());
        buildCachedSqlStatements();
    }

    private Field findKeyField() {
        List<Field> idFIelds = findFields(this.entityType, f -> f.getAnnotation(PK.class) != null);
        if (idFIelds == null || idFIelds.size() < 1) {
            throw new BabyDBException("No field labeled as PK for " + this.entityType.getCanonicalName());
        } else if (idFIelds.size() > 1) {
            throw new BabyDBException("Multiple PK fields found on " + this.entityType.getCanonicalName());
        } else {
            return idFIelds.get(0);
        }
    }

    private void buildCachedSqlStatements() {
        String tableName = determineTableName();

        this.baseSql = "select * from " + tableName;
        this.byKeySql = baseSql + " where %s=?";
        this.deleteSql = "delete from " + tableName + " where %s=?";
        this.updateSql = "update " + tableName
                + " set " + nonKeyFields.stream().map(f -> colName(f) + "=?").collect(Collectors.joining(","))
                + " where " + colName(keyField) + "=?";
        this.insertSqlNoKey = "insert into " + tableName
                + "(" + nonKeyFields.stream()
                .map(this::colName)
                .collect(Collectors.joining(",")) + ")"
                + " values (" + nonKeyFields.stream().map(f -> "?").collect(Collectors.joining(",")) + ")";
        this.insertSql = "insert into " + tableName
                + "(" + fields.stream()
                .map(this::colName)
                .collect(Collectors.joining(",")) + ")"
                + " values (" + fields.stream().map(f -> "?").collect(Collectors.joining(",")) + ")";
    }

    private String colName(Field f) {
        return Optional.ofNullable(f.getAnnotation(ColumnName.class)).map(ColumnName::value).orElseGet(f::getName);
    }

    private String determineTableName() {
        String tableName = Optional.ofNullable(this.entityType.getAnnotation(SchemaName.class))
                .map(s -> s.value() + ".")
                .orElse("");
        tableName += Optional.ofNullable(this.entityType.getAnnotation(TableName.class))
                .map(TableName::value)
                .orElseGet(() -> camelCase(entityType.getSimpleName()));
        return tableName;
    }

    private String camelCase(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    public T get(Object id) {
        return getOneBy(colName(keyField), id);
    }

    public List<T> getAll() {
        try (Connection conn = getConnection()) {
            PreparedStatement st = prepare(conn, baseSql);
            st.execute();
            return (List<T>) mapResultSet(st, true);
        } catch (SQLException e) {
            throw new BabyDBException("Failed to execute sql: " + baseSql, e);
        }
    }

    public T getOneBy(String field, Object value) {
        PreparedStatement st;
        String sql = String.format(byKeySql, field);
        try (Connection conn = getConnection()) {
            st = prepare(conn, sql, value);
            st.execute();
            return (T) mapResultSet(st, false);
        } catch (SQLException e) {
            throw new BabyDBException("Failed to execute sql: " + sql, e);
        }
    }

    public List<T> getManyBy(String field, Object value) {
        PreparedStatement st;
        String sql = String.format(byKeySql, field);
        try (Connection conn = getConnection()) {
            st = prepare(conn, sql, value);
            st.execute();
            return (List<T>) mapResultSet(st, true);
        } catch (SQLException e) {
            throw new BabyDBException("Failed to execute sql: " + sql, e);
        }
    }

    private PreparedStatement prepare(Connection conn, String sql, Object... args) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < args.length; i++) {
            if (SETTERS.containsKey(args[i].getClass())) {
                invokeSafe(SETTERS.get(args[i].getClass()), ps, i + 1, args[i]);
            } else {
                throw new BabyDBException("Unsupported property type: " + args[i].getClass().getCanonicalName());
            }
        }
        return ps;
    }

    private Connection getConnection() {
        return Optional.ofNullable(localConnectionSupplier).map(Supplier::get).orElseGet(globalConnectionSupplier);
    }

    private Object mapResultSet(PreparedStatement st, boolean isMany) {
        try {
            ResultSet rs = st.getResultSet();
            List<T> many = isMany ? new ArrayList<>() : null;
            boolean hasOne = false;
            T model = null;
            while (rs.next()) {
                if (hasOne && !isMany) {
                    throw new BabyDBException("Multiple rows found for single row query");
                }
                hasOne = true;
                model = entityType.getConstructor().newInstance();
                for (Field f : fields) {
                    f.set(model, getResultValue(f, rs));
                }
                if (isMany) {
                    many.add(model);
                }
            }
            return isMany ? many : model;
        } catch (ReflectiveOperationException | SQLException e) {
            throw new BabyDBException("Failed to map resultSet to object", e);
        }
    }

    private Object getResultValue(Field field, ResultSet resultSet) {
        Class<?> type = field.getType();
        if (!GETTERS.containsKey(type)) {
            throw new BabyDBException("Unsupported model property type:" + type.getCanonicalName());
        }
        return ReflectiveUtils.invokeSafe(GETTERS.get(type), resultSet, colName(field));
    }

    public T save(T val) {
        Object o = getSafe(keyField, Objects.requireNonNull(val, "Can't save a null record"));
        if (o == null) {
            return insert(val, false);
        } else {
            T saved = get(o);
            return saved != null ? update(val) : insert(val, true);
        }
    }

    public T update(T val) {
        try (Connection conn = getConnection()) {
            List<Object> values = getFieldValues(val, nonKeyFields);
            //always add the key last to match the sql
            values.add(getSafe(keyField, val));
            PreparedStatement st = prepare(conn, updateSql, values.toArray());
            st.executeUpdate();
            return st.getUpdateCount() == 0 ? null : val;
        } catch (SQLException e) {
            throw new BabyDBException("Update failed", e);
        }

    }

    public T insert(T val, boolean hasKey) {
        try (Connection conn = getConnection()) {
            PreparedStatement st = prepare(
                    conn,
                    hasKey ? insertSql : insertSqlNoKey,
                    getFieldValues(val, hasKey ? fields : nonKeyFields).toArray());
            st.executeUpdate();
            if (hasKey) {
                return get(getFieldValues(val, Collections.singletonList(keyField)).get(0));
            } else {

                ResultSet keys;
                keys = st.getGeneratedKeys();
                if (keys.next()) {
                    return get(getResultValue(keyField, keys));
                } else {
                    throw new BabyDBException("No key was returned from the db on insert for " + this.entityType.getCanonicalName());
                }
            }
        } catch (SQLException e) {
            throw new BabyDBException("Insert failed", e);
        }
    }

    public boolean deleteByPK(Object key) {
        return deleteBy(colName(keyField), key);
    }

    public boolean delete(T entity){
        return deleteBy(colName(keyField), getSafe(keyField, entity));
    }

    public boolean deleteBy(String field, Object value) {
        try(Connection conn = getConnection()){
            PreparedStatement st = prepare(conn, String.format(this.deleteSql, field), value);
            return st.executeUpdate() > 0;
        } catch (SQLException e){
            throw new BabyDBException("Delete failed", e);
        }
    }


}