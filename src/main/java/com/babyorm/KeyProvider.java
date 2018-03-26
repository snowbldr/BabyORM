package com.babyorm;

@FunctionalInterface
public interface KeyProvider<T> {
    T nextKey();
}
