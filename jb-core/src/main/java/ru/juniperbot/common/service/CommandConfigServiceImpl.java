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
package ru.juniperbot.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.persistence.entity.CommandConfig;
import ru.juniperbot.common.persistence.repository.CommandConfigRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommandConfigServiceImpl implements CommandConfigService {

    @Autowired
    private CommandConfigRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<CommandConfig> findAll(long guildId) {
        return repository.findAllByGuildId(guildId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, CommandConfig> findAllMap(long guildId) {
        return repository.findAllByGuildId(guildId).stream().collect(Collectors.toMap(CommandConfig::getKey, e -> e));
    }

    @Override
    @Transactional(readOnly = true)
    public CommandConfig findByKey(long guildId, String key) {
        return repository.findByKey(guildId, key);
    }

    @Override
    @Transactional
    public CommandConfig save(CommandConfig config) {
        return repository.save(config);
    }

    @Override
    @Transactional
    public Iterable<CommandConfig> save(Iterable<CommandConfig> configs) {
        return repository.saveAll(configs);
    }
}
