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

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.api.dto.MessageTemplateDto;
import ru.juniperbot.common.persistence.entity.MessageTemplate;
import ru.juniperbot.common.persistence.repository.MessageTemplateFieldRepository;
import ru.juniperbot.common.persistence.repository.MessageTemplateRepository;

import java.util.ArrayList;

@Service
public class MessageTemplateDao extends AbstractDao {

    @Autowired
    private MessageTemplateRepository repository;

    @Autowired
    private MessageTemplateFieldRepository fieldRepository;

    @Transactional
    public MessageTemplate updateOrCreate(MessageTemplateDto source, MessageTemplate target) {
        if (source == null) {
            return target;
        }
        if (target == null) {
            target = new MessageTemplate();
        }
        apiMapper.updateTemplate(source, target);
        if (CollectionUtils.isEmpty(target.getFields())) {
            target.setFields(new ArrayList<>());
        } else {
            fieldRepository.deleteAll(target.getFields());
            target.getFields().clear();
        }

        int index = 0;
        if (CollectionUtils.isNotEmpty(source.getFields())) {
            for (var field : apiMapper.getTemplateFields(source.getFields())) {
                field.setTemplate(target);
                field.setIndex(index++);
                target.getFields().add(field);
            }
        }
        repository.save(target);
        return target;
    }
}
