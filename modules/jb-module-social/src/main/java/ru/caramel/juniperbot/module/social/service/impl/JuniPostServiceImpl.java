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
package ru.caramel.juniperbot.module.social.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.subscription.persistence.WebHook;
import ru.caramel.juniperbot.core.common.service.AbstractDomainServiceImpl;
import ru.caramel.juniperbot.module.social.persistence.entity.JuniPost;
import ru.caramel.juniperbot.module.social.persistence.repository.JuniPostRepository;
import ru.caramel.juniperbot.module.social.service.JuniPostService;

@Service
public class JuniPostServiceImpl extends AbstractDomainServiceImpl<JuniPost, JuniPostRepository> implements JuniPostService {

    public JuniPostServiceImpl(@Autowired JuniPostRepository repository) {
        super(repository);
    }

    @Override
    protected JuniPost createNew(long guildId) {
        JuniPost juniPost = new JuniPost();
        juniPost.setGuildId(guildId);
        juniPost.setWebHook(new WebHook());
        return juniPost;
    }

    @Override
    protected Class<JuniPost> getDomainClass() {
        return JuniPost.class;
    }
}
