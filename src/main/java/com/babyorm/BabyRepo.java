package com.babyorm;

import com.sun.beans.finder.PrimitiveWrapperMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BabyRepo<T> {

    private Class<T> clazz;
    private List<Field> fields, nonKeyFields;
    private String byKeySql, baseSql, updateSql, insertSqlNoKey, insertSql;
    private static Supplier<Connection> connectionSupplier;

    public static void setConnectionSupplier(Supplier<Connection> connectionSupplier) {
        BabyRepo.connectionSupplier = connectionSupplier;
    }

    private static final Map<String, Method> GETTERS =
        addSuperTypes(
            findMethods(
                ResultSet.class.getMethods(),
                Method::getReturnType,
                m -> m.getName().startsWith("get"),
                m -> !m.getName().startsWith("getN"),
                m -> m.getParameterCount() == 1,
                m -> m.getParameterTypes()[0].equals(String.class)));
    private static final Map<String, Method> SETTERS =
        addSuperTypes(
            findMethods(
                PreparedStatement.class.getMethods(),
                m -> m.getParameterTypes()[1],
                m -> m.getName().startsWith("set"),
                m -> !m.getName().startsWith("setN"),
                m -> m.getParameterCount() == 2,
                m -> m.getParameterTypes()[0].equals(Integer.TYPE)));
    private Field keyField;

    @SafeVarargs
    private static Map<String, Method> findMethods(Method[] methods, Function<Method, Class<?>> mapKey, Predicate<Method>... predicates) {
        Map<String, Method> map = Arrays.stream(methods)
                                        .filter(m -> {
                                            boolean r = true;
                                            for (Predicate<Method> p : predicates) {
                                                r = r && p.test(m);
                                            }
                                            return r;
                                        })
                                        .collect(Collectors.groupingBy(m -> mapKey.apply(m).getCanonicalName(), Collectors.toList()))
                                        .entrySet().stream()
                                        .filter(e -> e.getValue().size() == 1)
                                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        //make sure all the primitive and wrapper types are added
        map = map.entrySet().stream()
                 .flatMap(e -> {
                     Class<?> c = PrimitiveWrapperMap.getType(e.getKey());
                     if (c == null) {
                         try {
                             c = Class.forName(e.getKey());
                             Field type = c.getField("TYPE");
                             c = PrimitiveWrapperMap.getType(((Class<?>) type.get(c)).getCanonicalName());
                         } catch (ReflectiveOperationException ignored) {
                         }
                     }
                     if (c != null) {
                         return Stream.of(e, new HashMap.SimpleEntry<>(c.getCanonicalName(), e.getValue()));
                     } else {
                         return Stream.of(e);
                     }
                 })
                 .distinct()
                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        ;
        return map;
    }


    private static Map<String, Method> addSuperTypes(Map<String, Method> map) {
        Map<String, Method> newMap = new HashMap<>();
        map.forEach((c, v) -> {
            Class<?> d = null;
            try {
                try {
                    d = Class.forName(c);
                } catch (ClassNotFoundException e) {
                    Class<?> wrapper = PrimitiveWrapperMap.getType(c);
                    if (wrapper == null) {
                        return;
                    }
                    d = ((Class<?>) wrapper.getField("TYPE").get(wrapper));
                }
            } catch (ReflectiveOperationException ignored) {
            }

            while (d != null && !Object.class.equals(d)) {
                newMap.put(d.getCanonicalName(), map.get(c));
                d = d.getSuperclass();
            }
        });
        return newMap;
    }

    public BabyRepo() {
        Class<? extends BabyRepo> repoClass = this.getClass();
        this.clazz = (Class<T>) ((ParameterizedType) repoClass.getGenericSuperclass()).getActualTypeArguments()[0];
        this.fields = Arrays.asList(this.clazz.getDeclaredFields());
        this.fields.forEach(f -> f.setAccessible(true));
        String tableName = camelCase(clazz.getSimpleName());
        findKeyField(tableName);
        builCachedSqlStatements(tableName);
    }

    private void findKeyField(String tableName) {
        List<Field> idFIelds = Arrays.stream(this.clazz.getDeclaredFields())
                                     .filter(f -> f.getAnnotation(Id.class) != null)
                                     .collect(Collectors.toList());
        if (idFIelds == null || idFIelds.size() < 1) {
            try {
                this.keyField = clazz.getDeclaredField(tableName + "Id");
            } catch (NoSuchFieldException e) {
                throw new BabyDBException("Unable to determine the Id field of " + this.clazz.getCanonicalName());
            }
        } else if (idFIelds.size() > 1) {
            throw new BabyDBException("Multiple Id fields found on " + this.clazz.getCanonicalName());
        } else {
            this.keyField = idFIelds.get(0);
        }
        this.keyField.setAccessible(true);
        this.nonKeyFields = fields.stream()
                                  .filter(f -> !keyField.getName().equals(f.getName()))
                                  .collect(Collectors.toList());
    }

    private void builCachedSqlStatements(String tableName) {
        this.baseSql = "select * from " + tableName;
        this.byKeySql = baseSql + " where %s=?";
        this.updateSql = "update " + tableName
                         + " set " + nonKeyFields.stream().map(f -> f.getName() + "=?").collect(Collectors.joining(","))
                         + " where " + keyField.getName() + "=?";
        this.insertSqlNoKey = "insert into " + tableName
                              + "(" + nonKeyFields.stream()
                                                  .map(Field::getName)
                                                  .collect(Collectors.joining(",")) + ")"
                              + " values (" + nonKeyFields.stream().map(f -> "?").collect(Collectors.joining(",")) + ")";
        this.insertSql = "insert into " + tableName
                         + "(" + fields.stream()
                                       .map(Field::getName)
                                       .collect(Collectors.joining(",")) + ")"
                         + " values (" + fields.stream().map(f -> "?").collect(Collectors.joining(",")) + ")";
    }

    private String camelCase(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    public T get(Object id) {
        return getOneBy(keyField.getName(), id);
    }

    public List<T> getAll() {
        return executeForMany(baseSql);
    }

    public T getOneBy(String field, Object value) {
        return execute(String.format(byKeySql, field), value);
    }

    public List<T> getManyBy(String field, Object value) {
        return executeForMany(String.format(byKeySql, field), value);
    }

    protected T execute(String sql, Object... args) {
        return (T) mapResultSet(toResultSet(runSql(sql, false, args)), false);
    }

    private PreparedStatement runSql(String sql, boolean update, Object... args) {
        try {
            Connection conn = connectionSupplier.get();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                try {
                    if (SETTERS.containsKey(args[i].getClass().getCanonicalName())) {
                        SETTERS.get(args[i].getClass().getCanonicalName()).invoke(ps, i + 1, args[i]);
                    } else {
                        throw new RuntimeException("No Setter found for class: " + args[i].getClass().getCanonicalName());
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Unsupported parameter type: " + args[i].getClass().getCanonicalName());
                }
            }
            if (update) {
                ps.executeUpdate();
            } else {
                ps.execute();
            }
            return ps;
        } catch (SQLException e) {
            throw new BabyDBException("Failed to execute sql: " + sql, e);
        }
    }

    protected List<T> executeForMany(String sql, Object... args) {
        return (List<T>) mapResultSet(toResultSet(runSql(sql, false, args)), true);
    }

    private ResultSet toResultSet(PreparedStatement preparedStatement) {
        try {
            return preparedStatement.getResultSet();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get the resultset", e);
        }
    }

    private Object mapResultSet(ResultSet rs, boolean isMany) {
        List<T> many = isMany ? new ArrayList<>() : null;
        try {
            boolean hasOne = false;
            T model = null;
            while (rs.next()) {
                if (hasOne && !isMany) {
                    throw new BabyDBException("Multiple rows found for single row query");
                }
                hasOne = true;
                model = clazz.getConstructor().newInstance();
                for (Field f : fields) {
                    f.set(model, getValue(f, rs));
                }
                if (isMany) {
                    many.add(model);
                }
            }
            return isMany ? many : model;
        } catch (ReflectiveOperationException | SQLException e) {
            throw new RuntimeException("Something's messed up yo", e);
        }
    }

    private Object getValue(Field field, ResultSet resultSet) {
        String name = field.getName();
        Class<?> type = field.getType();
        if (!GETTERS.containsKey(type.getCanonicalName())) {
            throw new RuntimeException("Unsupported model property type:" + type.getCanonicalName());
        }
        return Optional.ofNullable(GETTERS.get(type.getCanonicalName()))
                       .map(m -> {
                           try {
                               return m.invoke(resultSet, name);
                           } catch (ReflectiveOperationException e) {
                               throw new RuntimeException("Invocation failed for: " + type.getCanonicalName(), e);
                           }
                       }).orElse(null);
    }

    public T save(T val) {
        try {
            Object o = keyField.get(Objects.requireNonNull(val, "can't save a null record"));
            if (o == null) {
                return insert(val, false);
            } else {
                T saved = get(o);
                return saved != null ? update(val) : insert(val, true);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get key value for type " + this.clazz.getCanonicalName(), e);
        }
    }

    private T update(T val) {
        PreparedStatement st;
        List<Object> values = getFieldValues(val, nonKeyFields);
        try {
            values.add(keyField.get(val));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get key value for class " + keyField.getDeclaringClass().getCanonicalName());
        }
        st = runSql(updateSql, true, values.toArray());

        try {
            if (st.getUpdateCount() != 1) {
                throw new RuntimeException("Update failed");
            }
        } catch (SQLException e) {
            throw new RuntimeException("omg", e);
        }
        return val;
    }

    private List<Object> getFieldValues(T val, List<Field> fields) {
        return fields.stream().map(f -> {
            try {
                return f.get(val);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to get property of field " + f.getDeclaringClass().getCanonicalName() + "#" + f.getName());
            }
        }).collect(Collectors.toList());
    }

    private T insert(T val, boolean hasKey) {
        PreparedStatement st = runSql(hasKey ? insertSql : insertSqlNoKey, true, getFieldValues(val, hasKey ? fields : nonKeyFields).toArray());
        if (hasKey) {
            return get(getFieldValues(val, Collections.singletonList(keyField)).get(0));
        } else {
            try {
                ResultSet keys;
                keys = st.getGeneratedKeys();
                if (keys.next()) {
                    return get(getValue(keyField, keys));
                } else {
                    throw new RuntimeException("No key was returned from the db on insert for " + this.clazz.getCanonicalName());
                }
            } catch (SQLException e) {
                throw new RuntimeException(":|", e);
            }
        }
    }
}