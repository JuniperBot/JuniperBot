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
package ru.juniperbot.api.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.api.dto.config.ModerationConfigDto;
import ru.juniperbot.common.persistence.entity.ModerationAction;
import ru.juniperbot.common.persistence.entity.ModerationConfig;
import ru.juniperbot.common.persistence.repository.ModerationActionRepository;
import ru.juniperbot.common.service.ModerationConfigService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModerationDao extends AbstractDao {

    @Autowired
    private ModerationConfigService configService;

    @Autowired
    private ModerationActionRepository actionRepository;

    @Transactional
    public ModerationConfigDto getConfig(long guildId) {
        ModerationConfig config = configService.getOrCreate(guildId);
        return apiMapper.getModerationDto(config);
    }

    @Transactional
    public void saveConfig(ModerationConfigDto dto, long guildId) {
        ModerationConfig modConfig = configService.getOrCreate(guildId);

        List<ModerationAction> result = new ArrayList<>();
        dto.getActions().forEach(e -> {
            ModerationAction action;
            if (e.getId() != null) {
                action = modConfig.getActions().stream().filter(e1 -> Objects.equals(e1.getId(), e.getId()))
                        .findFirst()
                        .orElse(null);
            } else {
                action = new ModerationAction();
                action.setConfig(modConfig);
            }
            if (action != null) {
                apiMapper.updateModerationAction(e, action);
                result.add(action);
            }
        });

        Set<Integer> seenCount = new HashSet<>();
        result.removeIf(e -> !seenCount.add(e.getCount()));

        actionRepository.saveAll(result);
        actionRepository.deleteAll(modConfig.getActions().stream()
                .filter(e -> !result.contains(e))
                .collect(Collectors.toList()));

        apiMapper.updateModerationConfig(dto, modConfig);
        modConfig.setActions(result);
        configService.save(modConfig);
    }
}
