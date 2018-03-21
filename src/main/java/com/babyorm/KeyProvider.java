package com.babyorm;

@FunctionalInterface
public interface KeyProvider {
    Object nextKey();
}
