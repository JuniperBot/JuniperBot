package ru.caramel.juniperbot.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class WebHookDto implements Serializable {

    private static final long serialVersionUID = -6115298074325733341L;

    private boolean enabled;

    private boolean available;

    private Long channelId;
}
