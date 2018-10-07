package com.babyorm;

import com.babyorm.db.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ExtendWith({InitDBBeforeEach.class, TotalTestTime.class})
public class BaseDBTest {

    public static final List<TestDB> TEST_DBS = Arrays.asList(new DerbyTestDB(), new SQLiteTestDB(), new PostgresTestDB(), new MysqlTestDB());
//    switch to this for quick tests
//    public static final List<TestDB> TEST_DBS = Collections.singletonList(new DerbyTestDB());

    protected BabyRepo<Baby> repo;

    public void setUp(TestDB testDB) {
        CoreRepo.setDefaultConnectionSupplier(testDB::connectionSupplier);
        this.repo = BabyRepo.forType(Baby.class);
        this.repo.setConnectionSupplier(testDB::connectionSupplier);
        BabyRepo.forType(Parent.class).setConnectionSupplier(testDB::connectionSupplier);
    }

    @AfterAll
    public static void tearDownClass(){
        TEST_DBS.forEach(TestDB::tearDown);
    }
}
