package com.babyorm.annotation;

import com.babyorm.util.Case;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The casing to use for each database column. For instance, if you choose {@link com.babyorm.util.Case#SNAKE_CASE},
 * a field with the name, someCrazyField, will map to the db column some_crazy_field.
 * This does NOT apply to fields annotated withe the {@link ColumnName} annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ColumnCasing {
    Case value();
}
