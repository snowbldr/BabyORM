package com.babyorm;

import com.babyorm.db.Baby;
import com.babyorm.db.TestDB;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class SingleKeyAutoGenEntityTest extends BaseDBTest{

    public static List<TestDB> testDBs(){
        return TEST_DBS;
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void setLocalConnectionSupplier(TestDB testDB) {
        BabyRepo.setGlobalConnectionSupplier(()->{throw new RuntimeException("This shouldn't have been used");});
        repo.setLocalConnectionSupplier(()->{throw new IllegalStateException("localConnection was used");});

        Baby baby = new Baby();
        baby.setPk(1L);
        try{
            assertThrows(IllegalStateException.class, ()->repo.save(baby));
            assertThrows(IllegalStateException.class, ()->repo.get(()->1));
            assertThrows(IllegalStateException.class, ()->repo.update(baby));
            assertThrows(IllegalStateException.class, ()->repo.delete(baby));
        } finally {
            BabyRepo.setGlobalConnectionSupplier(testDB::connectionSupplier);
        }
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void get(TestDB testDB) {
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");
        Baby saved = saveBaby(baby);

        Baby gotten = repo.get(saved::getPk);
        assertNotSame(gotten, saved);
        assertEquals(saved.getPk(), gotten.getPk());
        assertEquals(saved.getName(), gotten.getName());
        assertEquals(saved.getNumberOfToes(), gotten.getNumberOfToes());
        assertEquals(saved.getHairColor(), gotten.getHairColor());
    }


    @ParameterizedTest
    @MethodSource("testDBs")
    void getAll() {
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");
        IntStream.range(0,20).forEach(i->saveBaby(baby));
        List<Baby> all = repo.getAll();
        assertEquals(20, all.size());
        assertTrue(all.stream().allMatch(b->"Charlie".equals(b.getName())));
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void getOneBy() {
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");
        baby = saveBaby(baby);

        Baby got = repo.getOneBy("name", "Charlie");

        assertEquals(baby.getPk(), got.getPk());
        assertEquals(baby.getNumberOfToes(), got.getNumberOfToes());
        assertEquals(baby.getName(), got.getName());
        assertEquals(baby.getHairColor(), got.getHairColor());
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void getManyBy_fieldName() {
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");
        saveBaby(baby);
        baby.setName("bob");
        saveBaby(baby);

        List<Baby> gotten = repo.getManyBy("hairColor", "brown");
        assertTrue(gotten.size()>1);
        assertTrue(gotten.stream().anyMatch(b->"bob".equals(b.getName())));
        assertTrue(gotten.stream().allMatch(b->"brown".equals(b.getHairColor())));
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void getManyBy_collectionValue() {
        Baby baby = new Baby();
        baby.setHairColor("red");
        baby.setNumberOfToes(9);
        baby.setName("Betty");
        saveBaby(baby);
        baby.setHairColor("brown");
        saveBaby(baby);

        List<Baby> gotten = repo.getManyBy("hairColor", Arrays.asList("brown", "red"));
        assertTrue(gotten.size()>1);
        assertTrue(gotten.stream().allMatch(b->"brown".equals(b.getHairColor()) || "red".equals(b.getHairColor())));
        assertTrue(gotten.stream().anyMatch(b->"brown".equals(b.getHairColor())));
        assertTrue(gotten.stream().anyMatch(b->"red".equals(b.getHairColor())));
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void getManyBy_colName() {
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");
        saveBaby(baby);
        baby.setName("bob");
        saveBaby(baby);

        List<Baby> gotten = repo.getManyBy("hair_color", "brown");
        assertTrue(gotten.size()>1);
        assertTrue(gotten.stream().anyMatch(b->"bob".equals(b.getName())));
        assertTrue(gotten.stream().allMatch(b->"brown".equals(b.getHairColor())));
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void save_insert() {
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");

        Baby saved = saveBaby(baby);
        assertNotNull(saved.getPk());
        assertEquals(baby.getHairColor(), saved.getHairColor());
        assertEquals(baby.getName(), saved.getName());
        assertEquals(baby.getNumberOfToes(), saved.getNumberOfToes());

        Baby loaded = repo.get(saved::getPk);
        assertEquals(loaded.getPk(), saved.getPk());
        assertEquals(loaded.getHairColor(), saved.getHairColor());
        assertEquals(loaded.getName(), saved.getName());
        assertEquals(loaded.getNumberOfToes(), saved.getNumberOfToes());
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void save_update() {
        Baby saved = new Baby();
        saved.setHairColor("brown");
        saved.setNumberOfToes(9);
        saved.setName("Charlie");
        saved = saveBaby(saved);

        Baby updated = new Baby(saved);
        updated.setPk(saved.getPk());
        updated.setName("Bob");
        updated.setHairColor("purple");
        updated = saveBaby(updated);

        assertEquals(saved.getPk(), updated.getPk());
        assertNotEquals(saved.getHairColor(), updated.getHairColor());
        assertNotEquals(saved.getName(), updated.getName());
        assertEquals(saved.getNumberOfToes(), updated.getNumberOfToes());

        Baby loaded = repo.get(updated::getPk);
        assertEquals(loaded.getPk(), updated.getPk());
        assertEquals(loaded.getHairColor(), updated.getHairColor());
        assertEquals(loaded.getName(), updated.getName());
        assertEquals(loaded.getNumberOfToes(), updated.getNumberOfToes());
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void save_null() {
        assertThrows(NullPointerException.class, ()->repo.save(null));
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void update() {
        Baby saved = new Baby();
        saved.setHairColor("brown");
        saved.setNumberOfToes(9);
        saved.setName("Charlie");
        saved = saveBaby(saved);
        Baby updated = new Baby(saved);
        updated.setPk(saved.getPk());
        updated.setName("Bob");
        updated.setHairColor("purple");
        updated = repo.update(updated);

        assertEquals(saved.getPk(), updated.getPk());
        assertNotEquals(saved.getHairColor(), updated.getHairColor());
        assertNotEquals(saved.getName(), updated.getName());
        assertEquals(saved.getNumberOfToes(), updated.getNumberOfToes());

        Baby loaded = repo.get(updated::getPk);
        assertEquals(loaded.getPk(), updated.getPk());
        assertEquals(loaded.getHairColor(), updated.getHairColor());
        assertEquals(loaded.getName(), updated.getName());
        assertEquals(loaded.getNumberOfToes(), updated.getNumberOfToes());
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void update_nullKey(){
        assertThrows(BabyDBException.class, ()->repo.update(new Baby()));
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void insert() {
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby.setNumberOfToes(9);
        baby.setName("Charlie");
        Baby saved = insertBaby(baby);
        assertNotNull(saved);
        assertNotNull(saved.getPk());
        assertNotSame(baby, saved);
        assertEquals(baby.getHairColor(), saved.getHairColor());
        assertEquals(baby.getName(), saved.getName());
        assertEquals(baby.getNumberOfToes(), saved.getNumberOfToes());
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void insert_nulls() {
        Baby baby = new Baby();
        Baby saved = insertBaby(baby);
        assertNotNull(saved);
        assertNotNull(saved.getPk());
        assertNotSame(baby, saved);
        assertNull(saved.getHairColor());
        assertNull(saved.getName());
        assertEquals(saved.getNumberOfToes(), 0);
        assertEquals(baby.getNumberOfToes(), saved.getNumberOfToes());
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void deleteByPK() {
        Baby baby = saveBaby(new Baby());
        boolean deleted = repo.delete(baby::getPk);
        assertTrue(deleted);
        Baby dbBaby = repo.get(baby::getPk);
        assertNull(dbBaby);

    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void delete_notdeleted() {
        boolean deleted = repo.delete(()->null);
        assertFalse(deleted);
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void delete() {
        Baby baby = saveBaby(new Baby());
        boolean deleted = repo.delete(baby);
        assertTrue(deleted);
        Baby dbBaby = repo.get(baby::getPk);
        assertNull(dbBaby);
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void delete_notDeleted() {
        boolean deleted = repo.delete(new Baby());
        assertFalse(deleted);
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void deleteBy() {
        Baby baby = new Baby();
        baby.setHairColor("brown");
        baby = saveBaby(baby);

        int deleted = repo.deleteBy("hair_color", "brown");
        assertEquals(1, deleted);
        Baby dbBaby = repo.get(baby::getPk);
        assertNull(dbBaby);
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void deleteByAll() {
        Baby baby = new Baby();
        baby.setName("one");
        baby.setHairColor("red");
        baby = saveBaby(baby);

        Baby otherBaby = new Baby(baby);
        otherBaby.setName("two");
        otherBaby.setHairColor("red");
        otherBaby = saveBaby(otherBaby);

        Map<String,Object> vals = new HashMap<>();
        vals.put("name", "one");
        vals.put("hairColor", "red");
        int deleted = repo.deleteByAll(vals);
        assertEquals(1, deleted);
        assertNull(repo.get(baby::getPk));
        assertNotNull(repo.get(otherBaby::getPk));
    }

    @ParameterizedTest
    @MethodSource("testDBs")
    void deleteByAny() {
        Baby baby = new Baby();
        baby.setName("one");
        baby.setHairColor("red");
        baby = saveBaby(baby);

        Baby otherBaby = new Baby(baby);
        otherBaby.setName("two");
        otherBaby.setHairColor("blue");
        otherBaby = saveBaby(otherBaby);

        Map<String,Object> vals = new HashMap<>();
        vals.put("name", "one");
        vals.put("hairColor", "blue");
        int deleted = repo.deleteByAny(vals);
        assertEquals(2, deleted);
        assertNull(repo.get(baby::getPk));
        assertNull(repo.get(otherBaby::getPk));
    }

}