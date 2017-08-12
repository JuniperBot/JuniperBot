package ru.caramel.juniperbot.persistence.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.persistence.entity.base.MemberMessageEntity;

@Entity
public class Reminder extends MemberMessageEntity {

    @Getter
    @Setter
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

}
