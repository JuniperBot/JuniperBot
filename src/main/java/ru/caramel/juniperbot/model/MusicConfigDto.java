package ru.caramel.juniperbot.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class MusicConfigDto {

    private Long channelId;

    @NotNull
    private Boolean playlistEnabled;

    private boolean userJoinEnabled;

    private boolean streamsEnabled;

    @Min(0)
    private Long queueLimit;

    @Min(0)
    private Long durationLimit;

    @Min(0)
    private Long duplicateLimit;

}
