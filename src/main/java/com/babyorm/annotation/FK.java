package com.babyorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A foreign key to another table.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FK {
    /**
     * References from this entity to the target entity.
     *
     * <h1>Examples</h1>:
     *
     * <h2>One to One</h2>:
     *  table foo (
     *      id int,
     *      bar_pk int
     *  )
     *  table bar (
     *      pk int
     *  )
     *
     *  class Foo (
     *      int id;
     *      @FK(@ColumnRef(name="bar_id", references="pk"))
     *      Bar bar;
     *  )
     *
     *  class Bar (
     *      int pk;
     *  )
     *
     *  <h2>One to Many (non-unique key)</h2>:
     *  table foo (
     *      id int,
     *      bar_color char
     *  )
     *  table bar (
     *      pk int,
     *      color char
     *  )
     *
     *  class Foo (
     *      int id;
     *      @FK(@ColumnRef(name="bar_color", references="color"))
     *      List<Bar> bars;
     *  )
     *
     *  class Bar (
     *      int pk;
     *      String color;
     *  )
     *
     *  <h2>Many to One/One to Many (unique key)</h2>:
     *  table foo (
     *      id int
     *  )
     *  table bar (
     *      pk int,
     *      foo_id int
     *  )
     *
     *  class Foo (
     *      int id;
     *      List<Bar> bars;
     *  )
     *
     *  class Bar (
     *      int pk;
     *      @FK(@ColumnRef(name="foo_id", references="id"))
     *      Foo foo;
     *  )
     *
     *  <h2>Many to Many (non-unique key, see {@link JoinTable} for the unique key case)</h2>
     *  table foo (
     *      id int,
     *      name char,
     *      bar_color char
     *  )
     *  table bar (
     *      pk int,
     *      foo_name char,
     *      color char
     *  )
     *
     * class Foo (
     *      int id;
     *      String name;
     *      @FK(@ColumnRef(name="bar_color", references="color"))
     *      List<Bar> bars;
     *  )
     *
     *  class Bar (
     *      int pk;
     *      @FK(@ColumnRef(name="foo_name", references="name"))
     *      List<Foo> foo;
     *  )
     *
     */
    ColumnRef[] value();
}
