package ru.juniperbot.common.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class RankingReward implements Serializable {
    private static final long serialVersionUID = 8095233311225257044L;

    protected String roleId;

    protected boolean reset;

    protected Integer level;
}
