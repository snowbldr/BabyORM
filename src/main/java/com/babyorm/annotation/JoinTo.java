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
public @interface JoinTo {
    /**
     * References from this entity to the target entity.
     *
     * <h1>Examples</h1>:
     *
     * <h2>One to One</h2>:
     *  table foo (
     *      id int,
     *      bar int
     *  )
     *  table bar (
     *      pk int
     *  )
     *
     *  class Foo (
     *      int id;
     *      @JoinTo("pk")
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
     *      bars char
     *  )
     *  table bar (
     *      pk int,
     *      color char
     *  )
     *
     *  class Foo (
     *      int id;
     *      @JoinTo("color")
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
     *      foo int
     *  )
     *
     *  class Foo (
     *      int id;
     *      List<Bar> bars;
     *  )
     *
     *  class Bar (
     *      int pk;
     *      @JoinTo("id")
     *      Foo foo;
     *  )
     *
     *  <h2>Many to Many (non-unique key, see {@link JoinTable} for the unique key case)</h2>
     *  table foo (
     *      id int,
     *      name char,
     *      bars char
     *  )
     *  table bar (
     *      pk int,
     *      foos char,
     *      color char
     *  )
     *
     * class Foo (
     *      int id;
     *      String name;
     *      @JoinTo("color")
     *      List<Bar> bars;
     *  )
     *
     *  class Bar (
     *      int pk;
     *      @JoinTo("colName")
     *      List<Foo> foos;
     *  )
     *
     */
    String value();
}
