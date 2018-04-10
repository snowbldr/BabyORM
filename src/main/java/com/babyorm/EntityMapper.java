package com.babyorm;

import com.babyorm.util.ReflectiveUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;

import static com.babyorm.util.ReflectiveUtils.*;

public class EntityMapper<T> {

    private Class<T> entityType;
    private List<Field> fields;
    private Map<String, String> fieldNameToColName;

    /**
     * the column get methods on the ResultSet
     */
    protected static final Map<Class<?>, Method> RESULTSET_COLUMN_NAME_GETTERS =
            addKeySuperTypes(
                    addPrimitivesToMap(
                            findMethods(
                                    ResultSet.class.getMethods(),
                                    Method::getReturnType,
                                    m -> m.getName().startsWith("get"),
                                    m -> !m.getName().startsWith("getN"),
                                    m -> m.getParameterCount() == 1,
                                    m -> m.getParameterTypes()[0].equals(String.class))));

    private static final Map<Class<?>, Method> RESULTSET_POSITION_GETTERS =
            addKeySuperTypes(
                    addPrimitivesToMap(
                            findMethods(
                                    ResultSet.class.getMethods(),
                                    Method::getReturnType,
                                    m -> m.getName().startsWith("get"),
                                    m -> !m.getName().startsWith("getN"),
                                    m -> m.getParameterCount() == 1,
                                    m -> m.getParameterTypes()[0].equals(Integer.TYPE))));

    /**
     * the sql bind set methods on the PreparedStatement
     */
    private static final Map<Class<?>, Method> STATEMENT_SETTERS =
            addKeySuperTypes(
                    addPrimitivesToMap(
                            findMethods(
                                    PreparedStatement.class.getMethods(),
                                    m -> m.getParameterTypes()[1],
                                    m -> m.getName().startsWith("set"),
                                    m -> !m.getName().startsWith("setN"),
                                    m -> m.getParameterCount() == 2,
                                    m -> m.getParameterTypes()[0].equals(Integer.TYPE))));


    public EntityMapper(Class<T> entityType, List<Field> fields, Map<String,String> fieldNameToColName) {
        this.fields = fields;
        this.fieldNameToColName = fieldNameToColName;
        this.entityType = entityType;
    }

    /**
     * Create a prepared statement
     * @param conn The connection to prepare the statement with
     * @param sql The sql to execute
     * @param args bind variables for the prepared statement
     * @return The prepared statement
     */
    public PreparedStatement prepare(Connection conn, String sql, Object... args) {
        try {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (args != null && args.length > 0) {
                int[] pos = new int[]{1};
                Arrays.stream(args)
                        .flatMap(o -> o instanceof Collection ? ((Collection) o).stream() : Stream.of(o))
                        .forEach(o -> invokeSafe(
                                Optional.ofNullable(o)
                                        .map(Object::getClass)
                                        .map(STATEMENT_SETTERS::get)
                                    .orElse(STATEMENT_SETTERS.get(Object.class)),
                                ps, pos[0]++, o));
            }
            return ps;
        } catch (SQLException e) {
            throw new BabyDBException("Failed to prepare statement", e);
        }
    }

    /**
     * Map a single field to a result set
     * @param field The field to map to (This is needed to determine the proper return type)
     * @param resultSet The resultSet to get data from
     * @param position The position
     * @return The value
     */
    public Object getResultValueByPosition(Field field, ResultSet resultSet, int position) {
        return getResultValue(field, resultSet, RESULTSET_POSITION_GETTERS, position);
    }

    public Object getResultValueByName(Field field, ResultSet resultSet){
        return getResultValue(field, resultSet, RESULTSET_COLUMN_NAME_GETTERS, fieldNameToColName.get(field.getName()));
    }

    private Object getResultValue(Field field, ResultSet resultSet, Map<Class<?>, Method> getters, Object getterArg) {
        Class<?> type = field.getType();
        Method getter = Optional.ofNullable(getters.get(type)).orElse(getters.get(Object.class));

        Object result = ReflectiveUtils.invokeSafe(getter, resultSet, getterArg);
        if (result == null) {
            return null;
        } else if (PRIMITIVE_INVERSE.containsKey(type)) {
            if (!result.getClass().isAssignableFrom(type) && !result.getClass().isAssignableFrom(PRIMITIVE_INVERSE.get(type))) {
                throw new BabyDBException("Incompatible types for field: " + field.getDeclaringClass().getCanonicalName() + "." + field.getName() + ".  " +
                        "Wanted a " + type.getCanonicalName() + " but got a " + result.getClass().getCanonicalName());
            }
        } else if (!result.getClass().isAssignableFrom(type)) {
            throw new BabyDBException("Incompatible types for field: " + field.getDeclaringClass().getCanonicalName() + "." + field.getName() + ".  " +
                    "Wanted a " + type.getCanonicalName() + " but got a " + result.getClass().getCanonicalName());
        }
        return result;
    }

    public List<T> mapResultSet(PreparedStatement st, boolean isMany) {
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
                    f.set(model, getResultValueByName(f, rs));
                }
                if (isMany) {
                    many.add(model);
                }
            }
            return isMany ? many : Collections.singletonList(model);
        } catch (ReflectiveOperationException | SQLException e) {
            throw new BabyDBException("Failed to map resultSet to object", e);
        }
    }
}
