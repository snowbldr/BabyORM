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
 *                 thisEntity"foo_id",
 *                 propEntity="bar_pk")
 *      Bar bar;
 *  )
 *
 *  class Bar (
 *      int pk;
 *      @JoinTable(tableName="foo_bar",
 *                 thisEntity="bar_pk",
 *                 propEntity="foo_id")
 *      List<Foo> foo;
 *  )
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinTable {
    String tableName();
    /**
     * The column name on the join table to join to using the PK fields of this entity
     */
    String thisEntity();
    /**
     *  The column name to get the key to look up the child entity with
     */
    String propEntity();
}
