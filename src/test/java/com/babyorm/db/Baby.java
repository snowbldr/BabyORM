package com.babyorm.db;

import com.babyorm.annotation.ColumnName;
import com.babyorm.annotation.Generated;
import com.babyorm.annotation.JoinTo;
import com.babyorm.annotation.TableName;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@TableName("baby")
public class Baby implements Serializable {

    @Generated(isDatabaseGenerated = true)
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Id
    private Long pk;
    private String name;
    @Column(name = "hair_color")
    private String hairColor;
    @ColumnName("numberOfToes")
    private int numberOfToes;
    @JoinTo("pk")
    @Transient
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
