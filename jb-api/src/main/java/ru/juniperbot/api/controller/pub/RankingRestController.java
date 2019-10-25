/*
 * This file is part of JuniperBot.
 *
 * JuniperBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBot. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.api.controller.pub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.juniperbot.api.common.aspect.GuildId;
import ru.juniperbot.api.controller.base.BasePublicRestController;
import ru.juniperbot.api.dto.PageDto;
import ru.juniperbot.api.dto.RankingInfoDto;
import ru.juniperbot.api.dto.request.RankingInfoRequest;
import ru.juniperbot.common.model.exception.NotFoundException;
import ru.juniperbot.common.service.RankingConfigService;

@RestController
public class RankingRestController extends BasePublicRestController {

    @Autowired
    private RankingConfigService rankingConfigService;

    /**
     * Kept for back compatibility
     */
    @GetMapping("/ranking/list/{guildId}")
    @ResponseBody
    public PageDto<RankingInfoDto> list(@GuildId(validate = false) @PathVariable long guildId,
                                        @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                        @RequestParam(value = "size", defaultValue = "100", required = false) int size,
                                        @RequestParam(value = "search", required = false) String search) {
        if (size > RankingInfoRequest.MAX_PAGE || size < 1) {
            size = RankingInfoRequest.MAX_PAGE;
        }
        if (page < 0) {
            page = 0;
        }
        return list(guildId, new RankingInfoRequest(page, size, search));
    }

    @PostMapping("/ranking/list/{guildId}")
    @ResponseBody
    public PageDto<RankingInfoDto> list(@GuildId(validate = false) @PathVariable long guildId,
                                        @RequestBody @Validated RankingInfoRequest request) {
        if (!rankingConfigService.isEnabled(guildId)) {
            throw new NotFoundException();
        }
        Pageable pageRequest = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "exp"));
        return new PageDto<>(rankingConfigService.getRankingInfos(guildId, request.getSearch(), pageRequest)
                .map(apiMapperService::getRankingInfoDto));
    }
}
