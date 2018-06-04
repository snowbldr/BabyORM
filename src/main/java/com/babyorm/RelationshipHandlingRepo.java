package com.babyorm;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class for methods related only to handling joins to other entities
 */
public abstract class RelationshipHandlingRepo<T> extends CoreRepo<T> {

    private Map<Class<?>, Map<String, Field>> relationshipJoinKeys = new HashMap<>();
    private Map<String, Class<?>> colOrFieldNameToClass = new HashMap<>();

    protected RelationshipHandlingRepo(Class<T> entityType) {
        super(entityType);
    }
    //provide the necessary query methods that take a connection object to re-use for down stream sql calls

    protected Class<?> getColumnClass(String name){
        return colOrFieldNameToClass.computeIfAbsent(name, s-> {
            try {
                return entityType.getDeclaredField(colNameToFieldName.getOrDefault(s, s)).getType();
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Invalid field or column colName \""+s+"\". Check your @References annotations!");
            }
        });
    }
}
