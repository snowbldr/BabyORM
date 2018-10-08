package com.babyorm;

import com.babyorm.annotation.*;
import com.babyorm.util.Case;
import com.babyorm.util.EntityReflectingUtils;
import com.babyorm.util.SqlGen;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.babyorm.util.EntityReflectingUtils.getSafe;
import static com.babyorm.util.EntityReflectingUtils.setSafe;

/**
 * Where the work happens for the baby repo, this exists just to keep the core logic and all the nice fluff separate
 *
 * @param <T> The type of entity this repo likes the most
 */
public abstract class CoreRepo<T> {

    private static final Logger logger = Logger.getLogger(CoreRepo.class.getCanonicalName());
    protected static ConnectionSupplier defaultConnectionSupplier;

    private EntityMapper<T> entityMapper;
    private ConnectionSupplier connectionSupplier;
    protected Class<T> entityType;
    private Field databaseGeneratedField;
    private List<Field> fields, nonKeyFields;
    private String baseSql, updateSql, insertSqlNoKey, insertSql, deleteSql;
    private List<Field> keyFields;
    private Map<Field, ColumnValueProvider> columnValueProviders;
    private boolean isAutoGen;
    protected Map<String, String> colNameToFieldName, fieldNameToColName;
    private String tableFullName;
    private String catalog;

    private static final ConcurrentHashMap<Class<?>, CoreRepo<?>> REPO_REGISTRY = new ConcurrentHashMap<>();

    protected CoreRepo(Class<T> entityType, ConnectionSupplier connectionSupplier) {
        this.entityType = entityType;
        this.connectionSupplier = connectionSupplier;

        tableFullName = determineTableFullName(entityType);
        String schemaName = tableFullName.contains(".") ? tableFullName.split("\\.")[0] : null;
        String tableName = tableFullName.contains(".") ? tableFullName.split("\\.")[1] : tableFullName;
        fields = Arrays.stream(entityType.getDeclaredFields())
                .filter(f -> !isTransient(f))
                .collect(Collectors.toList());
        fields.forEach(f -> f.setAccessible(true));

        fieldNameToColName = getFieldNameToColNameMapping(tableFullName, fields);
        colNameToFieldName = fieldNameToColName.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        keyFields = determineKeyFields(schemaName, tableName, fields, colNameToFieldName);
        nonKeyFields = fields.stream().filter(f -> !keyFields.contains(f)).collect(Collectors.toList());

        entityMapper = new EntityMapper<>(entityType, fields, fieldNameToColName);

        List<Field> dbGenFields = fields.stream()
                .filter(f ->
                        f.getAnnotation(Generated.class) != null
                                && f.getAnnotation(Generated.class).isDatabaseGenerated())
                .collect(Collectors.toList());
        columnValueProviders = fields.stream()
                .filter(f ->
                        f.getAnnotation(Generated.class) != null
                                && !f.getAnnotation(Generated.class).isDatabaseGenerated())
                .collect(Collectors.toMap(
                        f->f,
                        f -> {
                            try {
                                return f.getAnnotation(Generated.class).columnValueProvider().newInstance();
                            } catch (ReflectiveOperationException e) {
                                throw new BabyDBException("The column value provider: " + f.getAnnotation(Generated.class).columnValueProvider() +
                                        " for field " + f.getDeclaringClass() + "#" + f.getName() + " must have a no arg constructor ", e);
                            }
                        }));
        isAutoGen = dbGenFields.size() > 0;
        if (dbGenFields.size() > 1) {
            throw new BabyDBException("Due to inconsistencies in JDBC \"getGeneratedKeys\" implementations," +
                    " only one database generated value is allowed per table. ");
        } else if (dbGenFields.size() == 1) {
            databaseGeneratedField = dbGenFields.get(0);
        }
        buildCachedSqlStatements();
    }


    protected static <ET, R extends CoreRepo<ET>> R getOrInitRepoForType(Class<ET> entityType, BiFunction<Class<?>, ConnectionSupplier, CoreRepo<?>> repoCreator, ConnectionSupplier connectionSupplier) {
        return (R) REPO_REGISTRY.computeIfAbsent(entityType, clzz -> repoCreator.apply(clzz, connectionSupplier));
    }

    /**
     * Set the global connection supplier to use across all repositories, probably shouldn't change this at run time,
     * but it's your life, do what you want.
     */
    public static void setDefaultConnectionSupplier(ConnectionSupplier defaultConnectionSupplier) {
        CoreRepo.defaultConnectionSupplier = defaultConnectionSupplier;
    }

    /**
     * Set the connection provider to use for this repo
     */
    public void setConnectionSupplier(ConnectionSupplier connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    private void buildCachedSqlStatements() {
        List<String> orderedFields = fields.stream().map(Field::getName).map(fieldNameToColName::get).collect(Collectors.toList());
        List<String> orderedNonKeys = nonKeyFields.stream().map(Field::getName).map(fieldNameToColName::get).collect(Collectors.toList());

        baseSql = SqlGen.all(tableFullName);
        deleteSql = SqlGen.delete(tableFullName);
        updateSql = SqlGen.update(tableFullName, orderedNonKeys);
        insertSqlNoKey = SqlGen.insert(tableFullName, orderedNonKeys);
        insertSql = SqlGen.insert(tableFullName, orderedFields);
    }

    /**
     * Gets the table name from the database to guarantee correct casing and schema
     *
     * @param clazz The entity type
     * @return The full table name with schema
     */
    private String determineTableFullName(Class<T> clazz) {
        String configuredSchema = Optional.ofNullable(clazz.getAnnotation(SchemaName.class))
                .map(SchemaName::value)
                .orElse(null);
        String configuredName = Optional.ofNullable(clazz.getAnnotation(TableName.class))
                .map(TableName::value)
                .orElseGet(clazz::getSimpleName);
        String resolvedTableName, resolvedSchemaName;

        try (Connection conn = getConnection()) {
            ResultSet tables = conn.getMetaData().getTables(null, configuredSchema, "%", null);
            String foundSchema;
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                Optional<String> foundTableName = Arrays.stream(Case.values())
                        .filter(cse -> tableName.equals(Case.convert(configuredName, cse)))
                        .findFirst()
                        .map(cse -> Case.convert(configuredName, cse));
                if (foundTableName.isPresent()) {
                    this.catalog = tables.getString("TABLE_CAT");
                    return Optional.ofNullable(configuredSchema)
                            .map(s -> s + "." + foundTableName.get()).orElse(foundTableName.get());

                }
            }
            throw new BabyDBException("Failed to find table " + configuredName + " in schema " + configuredSchema);

        } catch (SQLException e) {
            throw new BabyDBException("Failed to verify table name!", e);
        }

    }

    private Map<String, String> getFieldNameToColNameMapping(String tableFullName, List<Field> fields) {
        try (Connection conn = getConnection()) {
            Set<String> fieldNames = fields.stream().map(Field::getName).collect(Collectors.toSet());
            Statement st = conn.createStatement();
            st.execute("select * from " + tableFullName + " where 1=0");

            ResultSetMetaData metaData = st.getResultSet().getMetaData();
            Set<String> columnNames = new HashSet<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columnNames.add(metaData.getColumnName(i));
            }

            return fieldNames.stream()
                    .collect(Collectors.toMap(
                            f -> f,
                            fieldName -> columnNames.contains(fieldName) ?
                                    fieldName.toUpperCase()
                                    : Arrays.stream(Case.values())
                                    .filter(cse -> columnNames.contains(Case.convert(fieldName, cse)))
                                    .findFirst()
                                    .map(cse -> Case.convert(fieldName, cse).toUpperCase())
                                    .orElseThrow(() -> new BabyDBException("No matching column found for field " + fieldName + " in database"))

                    ));
        } catch (SQLException e) {
            throw new BabyDBException("Failed to determine column names via select", e);
        }
    }

    private List<Field> determineKeyFields(String schemaName, String tableName, List<Field> fields, Map<String, String> colNameToFieldName) {
        List<Field> configuredFields = fields.stream().filter(f->f.getAnnotation(PK.class)!=null).collect(Collectors.toList());
        if(configuredFields.isEmpty()){
            try (Connection conn = getConnection()) {
                Map<String, Field> fieldsByName = fields.stream().collect(Collectors.toMap(Field::getName, f -> f));
                Map<String, Integer> columnNames = new HashMap<>();

                ResultSet resultSet = conn.getMetaData().getPrimaryKeys(this.catalog, schemaName, tableName);
                while (resultSet.next()) {
                    columnNames.put(
                            resultSet.getString("COLUMN_NAME").toUpperCase(),
                            resultSet.getInt("KEY_SEQ"));
                }
                List<Field> foundKeyFields = columnNames.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue))
                        .map(Map.Entry::getKey)
                        .map(String::toUpperCase)
                        .map(colNameToFieldName::get)
                        .map(fieldsByName::get)
                        .collect(Collectors.toList());
                if(foundKeyFields.isEmpty()){
                    logger.warning("Your entity does not have any primary keys configured." +
                            " Not having this configured means that we cannot deterministically perform updates on entities," +
                            " thus any attempt to perform an update will throw an error. Deletes will use all columns in the where clause.");
                }
                return foundKeyFields;
            } catch (SQLException e) {
                throw new BabyDBException("Failed to getPrimaryKeys from database metadata", e);
            }
        }
        else {
            return configuredFields;
        }

    }


    protected Connection getConnection() {
        if (connectionSupplier == null && defaultConnectionSupplier == null) {
            throw new BabyDBException("You must set a connection supplier. Didn't read the class javadoc eh?");
        }
        return Optional.ofNullable(connectionSupplier)
                .map(ConnectionSupplier::getConnection)
                .orElseGet(() ->
                        Optional.ofNullable(defaultConnectionSupplier)
                                .map(ConnectionSupplier::getConnection)
                                .orElseThrow(() -> new BabyDBException("Failed to get a connection."))
                );
    }

    /**
     * Get one record by it's primary key
     */
    public T get(ColumnValueProvider columnValueProvider) {
        return get(Collections.singletonMap(keyFields.get(0).getName(), columnValueProvider));
    }

    /**
     * Get one record by it's set of primary keys
     */
    public T get(Map<String, ColumnValueProvider> keyProvider) {
        LinkedHashMap<String, ?> key = toKey(keyProvider);
        return getSome(SqlGen.whereAll(key), key.keySet().stream().map(key::get).toArray(), false).get(0);
    }

    protected List<T> getSome(String where, Object[] values, boolean isMany) {
        try (Connection conn = getConnection()) {
            String sql = baseSql + Optional.ofNullable(where).orElse("");
            PreparedStatement st = entityMapper.prepare(conn, sql, values);
            st.execute();
            return entityMapper.mapResultSet(st, isMany);
        } catch (SQLException e) {
            throw new BabyDBException("Failed to execute query", e);
        }
    }

    /**
     * Execute an arbitrary sql statement to retrieve some entities
     *
     * @param sql           The sql to execute
     * @param bindVariables Bind variables, if any
     * @return The found entities
     */
    public List<T> execute(String sql, Object... bindVariables) {
        try (Connection conn = getConnection()) {
            PreparedStatement st = entityMapper.prepare(conn, sql, bindVariables);
            st.execute();
            return entityMapper.mapResultSet(st, true);
        } catch (SQLException e) {
            throw new BabyDBException("Failed to execute sql: " + sql, e);
        }
    }

    /**
     * Update the given record and cascade the updates to each node in the entity graph
     */
    public T update(T record) {
        return update(record, true);
    }

    /**
     * Update the given record and optionally cascade the updates to each node in the entity graph
     */
    public T update(T record, boolean cascade) {
        if(keyFields.isEmpty()){
            throw new BabyDBException("Updates are not allowed because your entity does not have a primary key. " +
                    "Either add the @PK annotation to one or more fields on the entity or add the appropriate constraint" +
                    " to the database.");
        }
        try (Connection conn = getConnection()) {
            LinkedHashMap<String, Object> key = new LinkedHashMap<>(keyFields.size());
            keyFields.forEach(f -> {
                Object val = getSafe(f, record);
                if (val == null) {
                    throw new BabyDBException("Cannot perform an update on an entity when provided a null key. Make sure your keyfield");
                }
                key.put(f.getName(), val);
            });
            String sql = updateSql + SqlGen.whereAll(key);
            PreparedStatement st = entityMapper.prepare(conn, sql, Stream.concat(nonKeyFields.stream().map(f -> getSafe(f, record)), key.values().stream()).toArray());
            st.executeUpdate();
            return st.getUpdateCount() == 0 ? null : get(key.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e::getValue)));
        } catch (SQLException e) {
            throw new BabyDBException("Update failed", e);
        }
    }

    /**
     * perform an arbitrary update on the database.
     *
     * @param fieldsToUpdate The fields to set in the update query and the values to set them to. set key=value...
     * @param whereFields    The fields and values to use in the where clause of the query. where key=value...
     * @return The count of records that were updated
     */
    public int updateMany(Map<String, ?> fieldsToUpdate, Map<String, ?> whereFields) {
        try (Connection conn = getConnection()) {
            LinkedHashMap<String, ?> key = new LinkedHashMap<>(whereFields);
            String updateSql = SqlGen.update(tableFullName, new ArrayList<>(key.keySet())) + SqlGen.whereAll(key);
            PreparedStatement st = entityMapper.prepare(conn, updateSql, key.values().toArray());
            st.executeUpdate();
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            return st.getUpdateCount();
        } catch (SQLException e) {
            throw new BabyDBException("Update failed", e);
        }
    }


    /**
     * This method can be used to perform bulk inserts on this table without dealing with the entity types.
     *
     * @param columnValues The column or field names and the value to insert into the database
     * @return If there is one field that is labeled with the {@link Generated} annotation with the isDatabaseGenerate=true,
     * then this will be
     */
    public Optional<Object> insertByValues(Map<String, Object> columnValues) {
        try (Connection conn = getConnection()) {
            columnValues = columnValues instanceof LinkedHashMap ? columnValues : new LinkedHashMap<>(columnValues);
            List<String> columnNames = columnValues.keySet().stream()
                    .map(k -> colNameToFieldName.containsKey(k.toUpperCase()) ? k.toUpperCase() : fieldNameToColName.get(k))
                    .collect(Collectors.toList());
            String insert = SqlGen.insert(this.tableFullName, columnNames);
            PreparedStatement st = entityMapper.prepare(
                    conn,
                    insert,
                    columnValues.keySet().stream().map(columnValues::get).toArray());
            st.executeUpdate();
            ResultSet keys = st.getGeneratedKeys();
            if (databaseGeneratedField != null) {
                if (keys.next()) {
                    return Optional.of(entityMapper.getResultValueByPosition(databaseGeneratedField, keys, 1));
                } else {
                    throw new BabyDBException("No generated value was returned for field " +
                            entityType.getCanonicalName() + "#" + databaseGeneratedField.getName());
                }
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new BabyDBException("InsertByValues failed!", e);
        }
    }


    /**
     * Insert the given record into the database
     *
     * @param record The record to insert
     */
    public T insert(T record) {
        Objects.requireNonNull(record, "Can't save a null record");
        Map<String, ?> keyValue = fieldValueMap(keyFields, record);
        boolean hasKey = keyValue != null && keyValue.size() > 0;
        final Map<String, ColumnValueProvider> lookupKeyProvider;

        try (Connection conn = getConnection()) {
            final Map<String, Object> generatedValues = new HashMap<>();
            if (!columnValueProviders.isEmpty()) {
                columnValueProviders.forEach((k, v)->{
                    if(getSafe(k, record) == null){
                        Object generatedValue = v.value();
                        generatedValues.put(k.getName(), generatedValue);
                        setSafe(k, record, generatedValue);
                    }
                });
            }

            PreparedStatement st = entityMapper.prepare(
                    conn,
                    hasKey || !isAutoGen ? insertSql : insertSqlNoKey,
                    getColumnValues(record, hasKey || !isAutoGen ? fields : nonKeyFields).toArray());
            st.executeUpdate();

            if (!hasKey && isAutoGen) {
                ResultSet keys = st.getGeneratedKeys();
                if (!keys.next()) {
                    throw new BabyDBException("No key was returned from the db on insert for " + entityType.getCanonicalName());

                }
                lookupKeyProvider = keyFields.stream().collect(
                        Collectors.toMap(
                                Field::getName,
                                f -> {
                                    //this is not inline to guarantee the keys resultset is open
                                    Object r = entityMapper.getResultValueByPosition(f, keys, 1);
                                    return () -> r;
                                })
                );
            } else if (!keyFields.isEmpty()) {
                lookupKeyProvider = keyFields.stream().collect(Collectors.toMap(Field::getName, f -> {
                    Object value = generatedValues.getOrDefault(f.getName(), getSafe(f, record));
                    return ()->value;
                }));
            } else {
                lookupKeyProvider = null;
            }
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        } catch (SQLException e) {
            throw new BabyDBException("Insert failed", e);
        }
        if(lookupKeyProvider == null){
            logger.warning("Because no primary key fields are configured on the class and there is not a primary" +
                    " key constraint on the entity: "+ entityType.getCanonicalName()+" the record that was being inserted" +
                    " is being returned without first re-fetching the record from the database. This means you will miss" +
                    " any data that changed due to triggers firing on insert in the database.");
        }
        return lookupKeyProvider == null ? record : get(lookupKeyProvider);
    }

    private List<Object> getColumnValues(Object entity, List<Field> fields) {
        List<Object> values = new ArrayList<>(fields.size());
        for (Field f : fields) {
            if (EntityMapper.isSupportedSqlType(f.getType())) {
                values.add(EntityReflectingUtils.getSafe(f, entity));
            } else if (!isTransient(f)) {
                JoinTo joinTo = f.getAnnotation(JoinTo.class);
                if (joinTo == null) {
                    throw new BabyDBException("You must specify the column to join to using the JoinTo annotation, " +
                            "or add the transient modify on the field: " + f.getDeclaringClass().getCanonicalName() + "#" + f.getName());
                }
                String ref = joinTo.value();
                Object child = EntityReflectingUtils.getSafe(f, entity);
                if (child == null) {
                    values.add(null);
                } else {
                    BabyRepo<?> childRepo = BabyRepo.forType(f.getType());
                    String fieldName = childRepo.colNameToFieldName.getOrDefault(ref.toUpperCase(), ref);
                    Field field = EntityReflectingUtils.getField(child.getClass(), fieldName)
                            .orElseThrow(() -> new RuntimeException("Unable to find fk field value for " + entityType.getCanonicalName() + "#" + f.getName()));
                    values.add(EntityReflectingUtils.getSafe(field, child));
                }
            }
        }
        return values;
    }

    private boolean isTransient(Field f){
        return Modifier.isTransient(f.getModifiers()) || f.getAnnotation(BabyIgnore.class) != null;
    }

    /**
     * Delete a record by it's primary key
     *
     * @param keyProvider The pk of the record to delete
     * @return whether a record was deleted or not
     */
    public int delete(ColumnValueProvider keyProvider) {
        if (this.keyFields.size() > 1)
            throw new BabyDBException("Cannot delete entity by single value when entity has multi valued primary key");
        Map<String, ?> key = toKey(Collections.singletonMap(this.keyFields.get(0).getName(), keyProvider));
        return delete(key, SqlGen.whereAll(keysToColumnNames(key)));
    }

    protected int delete(Map<String, ?> columnValueMap, String where) {
        if (columnValueMap == null || columnValueMap.size() < 1) {
            return 0;
        }
        try (Connection conn = getConnection()) {
            PreparedStatement st = entityMapper.prepare(conn, deleteSql + where, columnValueMap.values().toArray());
            int count = st.executeUpdate();
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            return count;
        } catch (SQLException e) {
            throw new BabyDBException("Delete failed", e);
        }
    }

    protected LinkedHashMap<String, ?> keysToColumnNames(Map<String, ?> map) {
        LinkedHashMap<String, Object> colNameValueMap = new LinkedHashMap<>(map.size());
        map.forEach((s, o) -> colNameValueMap.put(colNameToFieldName.containsKey(s.toUpperCase()) ? s.toUpperCase() : fieldNameToColName.get(s), o));
        return colNameValueMap;
    }

    protected Map<String, ?> fieldValueMap(List<Field> fields, T record) {
        Map<String, Object> keyValues = new HashMap<>(fields.size());
        fields.forEach(f -> {
            Object v = getSafe(f, record);
            if (v != null) keyValues.put(f.getName(), v);
        });
        return keyValues;
    }

    private LinkedHashMap<String, ?> toKey(Map<String, ColumnValueProvider> valueProviders) {
        LinkedHashMap<String, Object> key = new LinkedHashMap<>(valueProviders.size());
        valueProviders.forEach((k, v) -> key.put(k, v.value()));
        return keysToColumnNames(key);
    }

    protected List<Field> getKeyFields() {
        return keyFields;
    }

    protected List<Field> getFields() {
        return fields;
    }
}
