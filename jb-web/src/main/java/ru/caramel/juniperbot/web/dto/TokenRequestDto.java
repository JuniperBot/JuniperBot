package ru.caramel.juniperbot.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class TokenRequestDto implements Serializable {

    private String code;

    private String clientId;

    private String redirectUri;

}
