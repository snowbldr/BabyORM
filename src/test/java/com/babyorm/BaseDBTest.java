package com.babyorm;

import com.babyorm.db.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ExtendWith(InitDBBeforeEach.class)
public class BaseDBTest {

    public static final List<TestDB> TEST_DBS = Arrays.asList(new DerbyTestDB(), new SQLiteTestDB(), new PostgresTestDB(), new MysqlTestDB());

    protected BabyRepo<Baby> repo;

    protected Set<Long> myBabies;


    public void setUp(TestDB testDB) {
        this.repo = new BabyRepo<Baby>(){};
        this.repo.setLocalConnectionSupplier(testDB::connectionSupplier);
        this.myBabies = new HashSet<>();
    }

    @AfterAll
    public static void tearDownClass(){
        TEST_DBS.forEach(TestDB::tearDown);
    }

    @AfterEach
    void tearDown() {
        if(myBabies.size()> 0) repo.deleteBy("pk", myBabies);
    }

    protected Baby saveBaby(Baby baby){
        Baby saved = this.repo.save(baby);
        this.myBabies.add(saved.getPk());
        return saved;
    }

    protected Baby insertBaby(Baby baby){
        Baby saved = this.repo.save(baby);
        this.myBabies.add(saved.getPk());
        return saved;
    }
}
