package com.babyorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use a join table to join records together
 *  table foo (
 *      id int
 *  )
 *  table bar (
 *      pk int
 *  )
 *
 *  table foo_bar {
 *      id int,
 *      foo_id int,
 *      bar_pk int
 *  }
 *
 * class Foo (
 *      int id;
 *      String name;
 *      @JoinTable(tableName="foo_bar",
 *                 thisEntityRef=@ColumnRef(name="id", references="foo_id"),
 *                 propEntityRef=@ColumnRef(name="pk", references="bar_pk"))
 *      Bar bar;
 *  )
 *
 *  class Bar (
 *      int pk;
 *      @JoinTable(tableName="foo_bar",
 *                 thisEntityRef=@ColumnRef(name="pk", references="bar_pk"),
 *                 propEntityRef=@ColumnRef(name="id", references="foo_id"))
 *      List<Foo> foo;
 *  )
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinTable {
    String tableName();
    /**
     * name: The column name on this entity; references: The column name on the join table to join to
     */
    ColumnRef thisEntityRef();
    /**
     * name: The column name on the entity type of this property; references: The column name on the join table to join to
     */
    ColumnRef propEntityRef();
}
