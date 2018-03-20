package com.babyorm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * WHICH FIELD IS THE KEY FIELD
 * if you don't have a key, make one
 * if you have a table with a multi column key, make a single key or use hibernate. This is just a baby.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PK {}