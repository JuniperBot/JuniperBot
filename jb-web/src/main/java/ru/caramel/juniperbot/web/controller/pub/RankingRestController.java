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
package ru.caramel.juniperbot.web.controller.pub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import ru.caramel.juniperbot.web.common.aspect.GuildId;
import ru.caramel.juniperbot.web.controller.base.BasePublicRestController;
import ru.caramel.juniperbot.web.dto.PageDto;
import ru.caramel.juniperbot.web.dto.RankingInfoDto;
import ru.juniperbot.common.model.exception.NotFoundException;
import ru.juniperbot.common.service.RankingConfigService;

@RestController
public class RankingRestController extends BasePublicRestController {

    private static final int MAX_PAGE = 100;

    @Autowired
    private RankingConfigService rankingConfigService;

    @RequestMapping("/ranking/list/{guildId}")
    @ResponseBody
    public PageDto<RankingInfoDto> list(@GuildId(validate = false) @PathVariable long guildId,
                                        @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                        @RequestParam(value = "size", defaultValue = "100", required = false) int size,
                                        @RequestParam(value = "search", required = false) String search) {
        if (!rankingConfigService.isEnabled(guildId)) {
            throw new NotFoundException();
        }
        if (size > MAX_PAGE || size < 0) {
            size = MAX_PAGE;
        }
        if (page < 0) {
            page = 0;
        }
        Pageable pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "exp"));
        return new PageDto<>(rankingConfigService.getRankingInfos(guildId, search, pageRequest)
                .map(apiMapperService::getRankingInfoDto));
    }
}
