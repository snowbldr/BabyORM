package com.babyorm;

import com.babyorm.util.SqlGen;

import java.util.*;

/**
 * A repo, baby
 * <p>
 * To make a new repo, use {@link #forType(Class)}
 * <p>
 * You must call {@link #setDefaultConnectionSupplier(ConnectionSupplier)} or provide a ConnectionSupplier via Constructor or setter
 * If you don't, shit's gonna throw errors telling you to do this.
 * <p>
 *
 * @param <T> The type of entity this repo likes the most
 */
public class BabyRepo<T> extends RelationshipHandlingRepo<T> {

    private final HashMap<Class<?>,CoreRepo<?>> children = new HashMap<>();

    /**
     * Pretty straight forward, can't really screw this one up.
     */
    private BabyRepo(Class<T> entityType, ConnectionSupplier connectionSupplier) {
        super(entityType, connectionSupplier);
    }

    /**
     * Factory method to get a new repository
     */
    public static <E> BabyRepo<E> forType(Class<E> type) {
        return getOrInitRepoForType(type, BabyRepo::new, BabyRepo.defaultConnectionSupplier);
    }

    /**
     * Factory method to get a new repository while specifying a non-default connection supplier to use for the repo.
     * Multiple repos for the same class but different connection suppliers are not supported
     */
    public static <E> BabyRepo<E> forType(Class<E> type, ConnectionSupplier connectionSupplier) {
        return getOrInitRepoForType(type, BabyRepo::new, connectionSupplier);
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
     * Insert or update the given record. This has worse performance that insert or update by themselves as we have to
     * determine if we're doing an insert or update by checking to see if the key is non null or if the record already
     * exists in the database. If you are concerned about performance, use insert or update directly for less overhead.
     *
     * @param record The record to save
     * @return The saved record. The record is retrieved from the database after saving to guarantee generated values are retrieved.
     */

    public T save(T record) {
        Objects.requireNonNull(record, "Can't save a null record");
        Map<String, ?> key = fieldValueMap(getKeyFields(), record);
        Object o = key.size() == getKeyFields().size() ? getOneByAll(key) : null;
        if (o == null) {
            return insert(record);
        } else {
            T saved = getOneByAll(key);
            return saved != null ? update(record) : insert(record);
        }
    }

    /**
     * Delete a record if no primary key is defined in the database all columns will be used in the deletes where statement
     *
     * @param entity The record to delete
     * @return whether a record was deleted or not
     */
    public boolean delete(T entity) {
        return (getKeyFields().isEmpty() ? deleteByAll(fieldValueMap(getFields(), entity)) : deleteByAll(fieldValueMap(getKeyFields(), entity))) > 0;
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