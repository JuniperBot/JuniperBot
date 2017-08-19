package ru.caramel.juniperbot.persistence.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;
import ru.caramel.juniperbot.persistence.entity.base.MemberMessageEntity;

@Entity
@Getter
@Setter
public class Reminder extends MemberMessageEntity {

    private static final long serialVersionUID = -3814573681159758727L;

    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

}
