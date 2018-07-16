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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.core.model.exception.AccessDeniedException;
import ru.caramel.juniperbot.web.common.aspect.GuildId;
import ru.caramel.juniperbot.web.controller.base.BaseRestController;
import ru.caramel.juniperbot.web.dao.SubscriptionDao;
import ru.caramel.juniperbot.web.dto.config.SubscriptionDto;
import ru.caramel.juniperbot.web.dto.request.SubscriptionCreateRequest;
import ru.caramel.juniperbot.web.model.SubscriptionType;

import java.util.List;

@RestController
public class SubscriptionsController extends BaseRestController {

    @Autowired
    private SubscriptionDao subscriptionDao;

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
    public SubscriptionDto create(@GuildId @PathVariable long guildId,
                               @RequestBody @Validated SubscriptionCreateRequest request) {
        SubscriptionDto result = subscriptionDao.create(guildId, request);
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
}
