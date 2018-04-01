package com.babyorm.db;

import com.babyorm.BaseDBTest;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;

public class InitDBBeforeEach implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if(context.getRequiredTestInstance() instanceof BaseDBTest){
            int index;
            if(context.getRequiredTestMethod().getAnnotation(ParameterizedTest.class) != null){
                index = Integer.valueOf(context.getDisplayName().replaceAll("^\\[([0-9]+)\\].*", "$1"))-1;
            } else {
                index = 0;
            }
            ((BaseDBTest) context.getRequiredTestInstance()).setUp(BaseDBTest.TEST_DBS.get(index));
        }

    }
}
