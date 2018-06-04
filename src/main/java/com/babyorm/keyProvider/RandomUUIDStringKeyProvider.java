package com.babyorm.keyProvider;

import com.babyorm.KeyProvider;

import java.util.UUID;

public class RandomUUIDStringKeyProvider implements KeyProvider<String> {

    @Override
    public String nextKey() {
        return UUID.randomUUID().toString();
    }
}
