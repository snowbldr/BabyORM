package com.babyorm;

import java.sql.Connection;

public class RelationshipHandlingRepo<T> extends CoreRepo<T> {



    @Override
    protected Connection getConnection() {
            /*
            ensure all queries to get any given object graph all use the same connection
            keep a stack of entity class names
            keep using the same inheritable trhead local until we empty the stack
            pop on each downstream, downstream call, pop off
            if we find a circular dependency, make sure to set it but avoid getting stuck in a loop
    */
        return super.getConnection();
    }
}
