package com.babyorm;

import com.babyorm.annotation.PK;
import com.babyorm.util.SqlGen;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * A repo, baby
 * <p>
 * To make a new repo, use {@link #forType(Class)} or {@link #BabyRepo()};
 * <p>
 * You must call {@link #setGlobalConnectionSupplier(ConnectionSupplier)} or provide a ConnectionSupplier via Constructor or setter
 * If you don't, shit's gonna throw errors telling you to do this.
 * <p>
 * You may also want to provide a {@link KeyProvider} if your {@link PK} is not autogenerated.
 * we'll remind you if we need to.
 *
 * @param <T> The type of entity this repo likes the most
 */
public class BabyRepo<T> extends RelationshipHandlingRepo<T> {

    private final HashMap<Class<?>,CoreRepo<?>> children = new HashMap<>();

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
            throw new BabyDBException("You must extend BabyRepo to use the no-arg constructor.");
        }
        init((Class<T>) ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0]);
    }

    /**
     * Use a local connection supplier instead of the global connection supplier
     */
    public BabyRepo(ConnectionSupplier connectionSupplier) {
        this();
        setLocalConnectionSupplier(connectionSupplier);
    }

    /**
     * Use a local connection supplier instead of the global connection supplier
     */
    public BabyRepo(Class<T> entityType, ConnectionSupplier connectionSupplier) {
        this(entityType);
        setLocalConnectionSupplier(connectionSupplier);
    }

    /**
     * Factory method to get a new repository
     */
    public static <E> BabyRepo<E> forType(Class<E> type) {
        return new BabyRepo<>(type);
    }

    /**
     * Find a single record that matches ALL of the columns
     *
     * @param columnValueMap A map of column names and the values to look up by.
     *                       If the columnNameOnThisEntity is a collection, an in list will be created.
     * @return The found record, if any
     */
    public T getOneByAll(Map<String, ?> columnValueMap) {
        LinkedHashMap<String, ?> map = keysToColumnNames(columnValueMap);
        return getSome(SqlGen.whereAll(map), map.keySet().stream().map(map::get).toArray(), false).get(0);
    }
    /**
     * select * from
     */
    public List<T> getAll() {
        return getSome(null, null, true);
    }

    /**
     * Get one record by a single column columnNameOnThisEntity. this is either the database column name or the field name.
     * We'll figure it out.
     *
     * @param field The field name/column name you want to look up the record by.
     * @param value The columnNameOnThisEntity you're searching for.
     *              If the columnNameOnThisEntity is a collection, an in list will be created.
     * @return The found record if any
     */
    public T getOneBy(String field, Object value) {
        return getOneByAll(Collections.singletonMap(field, value));
    }

    /**
     * Find a single record that matches ANY of the columns.
     *
     * @param columnValueMap A map of column names and the values to look up by.
     *                       If the columnNameOnThisEntity is a collection, an in list will be created.
     * @return The found record, if any
     */
    public T getOneByAny(Map<String, ?> columnValueMap) {
        LinkedHashMap<String, ?> map = keysToColumnNames(columnValueMap);
        return getSome(SqlGen.whereAny(map), map.keySet().stream().map(map::get).toArray(), false).get(0);
    }

    /**
     * Find a many records that match a single column.
     *
     * @param field The field name/column name you want to look up the record by.
     * @param value The columnNameOnThisEntity you're searching for.
     *              If the columnNameOnThisEntity is a collection, an in list will be created.
     */
    public List<T> getManyBy(String field, Object value) {
        return getManyByAll(Collections.singletonMap(field, value));
    }

    /**
     * Find a many records that match ALL of the columns.
     *
     * @param columnValueMap A map of column/field names and the values to look up by.
     *                       If the columnNameOnThisEntity is a collection, an in list will be created.
     * @return The found records, if any
     */
    public List<T> getManyByAll(Map<String, ?> columnValueMap) {
        LinkedHashMap<String, ?> map = keysToColumnNames(columnValueMap);
        return getSome(SqlGen.whereAll(map), map.keySet().stream().map(map::get).toArray(), true);
    }

    /**
     * Find a many records that match ANY of the columns.
     *
     * @param columnValueMap A map of column names and the values to look up by.
     *                       If the columnNameOnThisEntity is a collection, an in list will be created.
     * @return The found records, if any
     */
    public List<T> getManyByAny(Map<String, ?> columnValueMap) {
        LinkedHashMap<String, ?> map = keysToColumnNames(columnValueMap);
        return getSome(SqlGen.whereAny(map), map.keySet().stream().map(map::get).toArray(), true);
    }


    /**
     * Insert or update the given record
     *
     * @param record The record to save
     * @return The saved record. The record is retrieved from the database after saving to guarantee generated values are retrieved.
     */

    public T save(T record) {
        Objects.requireNonNull(record, "Can't save a null record");
        Map<String, ?> key = keyValueFromRecord(record);
        Object o = key.size() == getKeyFields().size() ? getOneByAll(key) : null;
        if (o == null) {
            return insert(record);
        } else {
            T saved = getOneByAll(key);
            return saved != null ? update(record) : insert(record);
        }
    }

    /**
     * Delete a record by it's {@link PK}
     *
     * @param entity The record to delete, the PK will be retrieved and used in the delete statement
     * @return whether a record was deleted or not
     */
    public boolean delete(T entity) {
        return deleteByAll(keyValueFromRecord(entity)) > 0;
    }

    /**
     * Delete by a specific column
     *
     * @param field The field/column name to delete by
     * @param value The columnNameOnThisEntity to search by
     * @return Whether any records were deleted or not
     */
    public int deleteBy(String field, Object value) {
        return deleteByAll(Collections.singletonMap(field, value));
    }

    /**
     * delete records that match ALL of the columns.
     *
     * @param columnValueMap A map of column names and the values to look up by.
     *                       If the columnNameOnThisEntity is a collection, an in list will be created.
     * @return whether any records were deleted
     */
    public int deleteByAll(Map<String, ?> columnValueMap) {
        return delete(columnValueMap, SqlGen.whereAll(keysToColumnNames(columnValueMap)));
    }

    /**
     * delete records that match ANY of the columns.
     *
     * @param columnValueMap A map of column names and the values to look up by.
     *                       If the columnNameOnThisEntity is a collection, an in list will be created.
     * @return whether any records were deleted
     */
    public int deleteByAny(Map<String, ?> columnValueMap) {
        return delete(columnValueMap, SqlGen.whereAny(keysToColumnNames(columnValueMap)));
    }

}