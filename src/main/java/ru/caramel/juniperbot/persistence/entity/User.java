package ru.caramel.juniperbot.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class User extends BaseEntity {

    @Column
    @Getter
    @Setter
    private String name;


    @Column
    @Getter
    @Setter
    private String name2;
}
