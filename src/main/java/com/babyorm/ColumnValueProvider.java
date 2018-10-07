package com.babyorm;

@FunctionalInterface
public interface ColumnValueProvider<T> {
    T value();
}
