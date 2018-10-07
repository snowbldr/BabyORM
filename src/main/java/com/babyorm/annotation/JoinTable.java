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
 *                 thisColumn"foo_id",
 *                 propColumn="bar_pk")
 *      Bar bar;
 *  )
 *
 *  class Bar (
 *      int pk;
 *      @JoinTable(tableName="foo_bar",
 *                 thisColumn="bar_pk",
 *                 propColumn="foo_id")
 *      List<Foo> foo;
 *  )
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinTable {
    String tableName();
    /**
     * The column name on this entity to join with. The join table must have the same exact column name.
     */
    String thisColumn();
    /**
     *  The column name on the prop entity to join with. The join table must have the same exact column name.
     */
    String propColumn();
}
