package com.babyorm;

import com.babyorm.db.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

@ExtendWith(InitDBBeforeEach.class)
public class BaseDBTest {

    public static final List<TestDB> TEST_DBS;
    static {
        DerbyTestDB derbyTestDB = new DerbyTestDB();
        SQLiteDB sqLiteDB = new SQLiteDB();
        PostgresDB postgresDB = new PostgresDB();
        TEST_DBS = Arrays.asList(derbyTestDB, sqLiteDB, postgresDB);
    }

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
