package com.babyorm.db;

import com.babyorm.annotation.ColumnCasing;
import com.babyorm.annotation.ColumnName;
import com.babyorm.annotation.FK;
import com.babyorm.annotation.PK;
import com.babyorm.util.Case;

@ColumnCasing(Case.SNAKE_CASE)
public class Baby {

    @PK
    private Long pk;
    private String name;
    @Column(name = "hair_color")
    private String hairColor;
    @ColumnName("numberOfToes")
    private int numberOfToes;
    @FK("pk")
    private Parent parent;

    public Baby(){}

    public Baby(Baby baby){
        this.name = baby.name;
        this.hairColor = baby.hairColor;
        this.numberOfToes = baby.numberOfToes;
    }

    public Long getPk() {
        return pk;
    }

    public void setPk(Long pk) {
        this.pk = pk;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHairColor() {
        return hairColor;
    }

    public void setHairColor(String hairColor) {
        this.hairColor = hairColor;
    }

    public int getNumberOfToes() {
        return numberOfToes;
    }

    public void setNumberOfToes(int numberOfToes) {
        this.numberOfToes = numberOfToes;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }

    public Parent getParent() {
        return parent;
    }
}
