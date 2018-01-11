package ru.caramel.juniperbot.modules.ranking.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Reward implements Serializable {
    private static final long serialVersionUID = 8095233311225257044L;

    protected String roleId;

    protected Integer level;
}
