package com.babyorm.db;

import com.babyorm.annotation.TableName;

@TableName("multi_key")
public class EntityWithMultiValuedKey {
    private String name;
    private String type;
    private long taco;
    private int banana;
}
