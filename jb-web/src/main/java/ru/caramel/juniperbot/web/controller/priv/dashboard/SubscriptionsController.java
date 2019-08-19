/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.caramel.juniperbot.web.controller.priv.dashboard;

import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.juniperbot.common.model.exception.AccessDeniedException;
import ru.caramel.juniperbot.module.social.service.YouTubeService;
import ru.caramel.juniperbot.web.common.aspect.GuildId;
import ru.caramel.juniperbot.web.controller.base.BaseRestController;
import ru.caramel.juniperbot.web.dao.SubscriptionDao;
import ru.caramel.juniperbot.web.dto.config.SubscriptionDto;
import ru.caramel.juniperbot.web.dto.config.SuggestionDto;
import ru.caramel.juniperbot.web.dto.request.SubscriptionCreateRequest;
import ru.caramel.juniperbot.web.dto.request.SubscriptionCreateResponse;
import ru.caramel.juniperbot.web.model.SubscriptionType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
public class SubscriptionsController extends BaseRestController {

    @Autowired
    private SubscriptionDao subscriptionDao;

    @Autowired
    private YouTubeService youTubeService;

    @RequestMapping(value = "/subscriptions/{guildId}", method = RequestMethod.GET)
    @ResponseBody
    public List<SubscriptionDto> list(@GuildId @PathVariable long guildId) {
        return subscriptionDao.getSubscriptions(guildId);
    }

    @RequestMapping(value = "/subscriptions/{guildId}", method = RequestMethod.POST)
    public ResponseEntity save(@GuildId @PathVariable long guildId,
                               @RequestBody @Validated SubscriptionDto dto) {
        if (subscriptionDao.update(dto)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @RequestMapping(value = "/subscriptions/{guildId}", method = RequestMethod.PUT)
    public SubscriptionCreateResponse create(@GuildId @PathVariable long guildId,
                                             @RequestBody @Validated SubscriptionCreateRequest request) {
        SubscriptionCreateResponse result = subscriptionDao.create(guildId, request);
        if (result == null) {
            throw new AccessDeniedException();
        }
        return result;
    }

    @RequestMapping(value = "/subscriptions/{guildId}/{type}/{id}", method = RequestMethod.DELETE)
    public ResponseEntity delete(@GuildId @PathVariable long guildId,
                                 @PathVariable SubscriptionType type,
                                 @PathVariable long id) {
        if (subscriptionDao.delete(type, id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @RequestMapping(value = "/subscriptions/suggestions/{type}", method = RequestMethod.GET)
    @ResponseBody
    public List<SuggestionDto> list(@PathVariable("type") SubscriptionType type, @RequestParam("q") String search) {
        switch (type) {
            case YOUTUBE:
                List<SearchResult> results = youTubeService.searchChannel(search, 25);
                return results.stream()
                        .map(e -> {
                            SuggestionDto dto = new SuggestionDto();
                            if (e.getId() == null) {
                                return null;
                            }
                            dto.setId(e.getId().getChannelId());

                            SearchResultSnippet snippet = e.getSnippet();
                            if (snippet == null) {
                                return null;
                            }
                            dto.setName(snippet.getChannelTitle());
                            if (snippet.getThumbnails() != null && snippet.getThumbnails().getDefault() != null) {
                                dto.setIconUrl(snippet.getThumbnails().getDefault().getUrl());
                            }
                            return dto;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

            default:
                return Collections.emptyList();
        }
    }
}
