package com.babyorm.db;

import com.babyorm.BaseDBTest;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.Optional;

public class InitDBBeforeEach implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        Optional<Method> testMethod = context.getTestMethod();
        Optional<Boolean> isBaseDBTest = testMethod.map(Method::getDeclaringClass).map(BaseDBTest.class::isAssignableFrom);
        if(isBaseDBTest.isPresent() && isBaseDBTest.get()){
            int index = 0;
            if(testMethod.get().getAnnotation(ParameterizedTest.class) != null ){
                Optional<Boolean> isTestDBsSource = testMethod.map(m -> m.getAnnotation(MethodSource.class)).map(ms -> ms.value().length == 1 && "testDBs".equals(ms.value()[0]));
                if(isTestDBsSource.isPresent()&&isTestDBsSource.get())
                    index = Integer.valueOf(context.getDisplayName().replaceAll("^\\[([0-9]+)].*", "$1"))-1;
            }
            ((BaseDBTest) context.getRequiredTestInstance()).setUp(BaseDBTest.TEST_DBS.get(index));
        }

    }
}
