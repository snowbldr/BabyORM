package com.babyorm.db;

import com.babyorm.annotation.Generated;
import com.babyorm.annotation.PK;
import com.babyorm.annotation.TableName;
import com.babyorm.keyProvider.RandomUUIDStringColumnValueProvider;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@TableName("parent")
public class Parent {
    @Id
    @Generated(isDatabaseGenerated = false, columnValueProvider = RandomUUIDStringColumnValueProvider.class)
    private String pk;

    private String name;

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
