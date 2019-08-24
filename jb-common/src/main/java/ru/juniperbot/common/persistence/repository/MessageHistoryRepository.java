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
package ru.juniperbot.common.persistence.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import ru.juniperbot.common.persistence.entity.MessageHistory;
import ru.juniperbot.common.persistence.repository.base.MemberMessageRepository;

import java.util.Date;

@Repository
public interface MessageHistoryRepository extends MemberMessageRepository<MessageHistory> {

    MessageHistory findByChannelIdAndMessageId(String channelId, String messageId);

    @Modifying
    void deleteByCreateDateBefore(Date expiryDate);
}
