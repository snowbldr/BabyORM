package com.babyorm;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


public class TotalTestTime implements BeforeEachCallback, AfterEachCallback {

    long startTime;

    @Override
    public void beforeEach(ExtensionContext context) {
        startTime = System.nanoTime();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        long endTime = System.nanoTime();
        System.out.println(context.getRequiredTestMethod().getDeclaringClass().getSimpleName()+"#"+context.getRequiredTestMethod().getName()
                +" Total Duration: "+((endTime - startTime)/1000000D)+"ms.");
    }
}
