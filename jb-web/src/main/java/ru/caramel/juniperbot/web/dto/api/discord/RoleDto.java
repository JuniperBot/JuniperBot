package ru.caramel.juniperbot.web.dto.api.discord;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class RoleDto implements Serializable {
    private static final long serialVersionUID = -6054216106353163284L;

    private String id;

    private String name;

    private int position;

    private int positionRaw;

    private int colorRaw;

    private boolean interactable;

}
