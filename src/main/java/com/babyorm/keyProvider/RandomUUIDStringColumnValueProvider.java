package com.babyorm.keyProvider;

import com.babyorm.ColumnValueProvider;

import java.util.UUID;

public class RandomUUIDStringColumnValueProvider implements ColumnValueProvider<String> {

    @Override
    public String value() {
        return UUID.randomUUID().toString();
    }
}
