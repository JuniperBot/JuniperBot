package ru.caramel.juniperbot.persistence.entity;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Max;

import lombok.Getter;
import lombok.Setter;

@Entity
public class Reminder extends MessageOwnedEntity {

    @Getter
    @Setter
    @Max(2000)
    @Basic
    private String message;

    @Getter
    @Setter
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

}
