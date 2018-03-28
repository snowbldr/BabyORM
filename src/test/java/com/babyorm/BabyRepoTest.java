package com.babyorm;

import com.babyorm.db.Baby;
import com.babyorm.db.TestDB;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BabyRepoTest {

    @BeforeAll
    public static void init(){
        TestDB.init();
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void setGlobalConnectionSupplier() {
    }

    @Test
    void setLocalConnectionSupplier() {
    }

    @Test
    void get() {
        BabyRepo<Baby> repo = new BabyRepo<Baby>(){};
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");
        Baby saved = repo.insert(baby);

        Baby gotten = repo.get(saved::getPk);
        assertNotSame(gotten, saved);
        assertEquals(saved.getPk(), gotten.getPk());
        assertEquals(saved.getName(), gotten.getName());
        assertEquals(saved.getNumberOfToes(), gotten.getNumberOfToes());
        assertEquals(saved.getHairColor(), gotten.getHairColor());
    }


    @Test
    void getAll() {
    }

    @Test
    void getOneBy() {
    }

    @Test
    void getManyBy_fieldName() {
        BabyRepo<Baby> repo = new BabyRepo<Baby>(){};
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");
        repo.insert(baby);
        baby.setName("bob");
        repo.insert(baby);

        List<Baby> gotten = repo.getManyBy("hairColor", "brown");
        assertTrue(gotten.size()>1);
        assertTrue(gotten.stream().anyMatch(b->"bob".equals(b.getName())));
        assertTrue(gotten.stream().allMatch(b->"brown".equals(b.getHairColor())));
    }

    @Test
    void getManyBy_collectionValue() {
        BabyRepo<Baby> repo = new BabyRepo<Baby>(){};
        Baby baby = new Baby();
        baby.setHairColor("red");
        baby.setNumberOfToes(9);
        baby.setName("Betty");
        repo.insert(baby);
        baby.setHairColor("red");
        repo.insert(baby);

        List<Baby> gotten = repo.getManyBy("hairColor", Arrays.asList("brown", "red"));
        assertTrue(gotten.size()>1);
        assertTrue(gotten.stream().allMatch(b->"brown".equals(b.getHairColor()) || "red".equals(b.getHairColor())));
    }

    @Test
    void getManyBy_colName() {
        BabyRepo<Baby> repo = new BabyRepo<Baby>(){};
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");
        repo.insert(baby);
        baby.setName("bob");
        repo.insert(baby);

        List<Baby> gotten = repo.getManyBy("hair_color", "brown");
        assertTrue(gotten.size()>1);
        assertTrue(gotten.stream().anyMatch(b->"bob".equals(b.getName())));
        assertTrue(gotten.stream().allMatch(b->"brown".equals(b.getHairColor())));
    }

    @Test
    void save() {
    }

    @Test
    void update() {
    }

    @Test
    void insert() {
        BabyRepo<Baby> repo = new BabyRepo<Baby>(){};
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");
        Baby saved = repo.insert(baby);
        assertNotNull(saved);
        assertNotNull(saved.getPk());
        assertNotSame(baby, saved);
        assertEquals(baby.getHairColor(), saved.getHairColor());
        assertEquals(baby.getName(), saved.getName());
        assertEquals(baby.getNumberOfToes(), saved.getNumberOfToes());
    }

    @Test
    void insert_nulls() {
        BabyRepo<Baby> repo = new BabyRepo<Baby>(){};
        Baby baby = new Baby();
        Baby saved = repo.insert(baby);
        assertNotNull(saved);
        assertNotNull(saved.getPk());
        assertNotSame(baby, saved);
        assertNull(saved.getHairColor());
        assertNull(saved.getName());
        assertEquals(saved.getNumberOfToes(), 0);
        assertEquals(baby.getNumberOfToes(), saved.getNumberOfToes());
    }

    @Test
    void deleteByPK() {
    }

    @Test
    void delete() {
    }

    @Test
    void deleteBy() {
    }

}