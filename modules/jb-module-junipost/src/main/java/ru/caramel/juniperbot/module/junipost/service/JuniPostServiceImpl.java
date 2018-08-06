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
package ru.caramel.juniperbot.module.junipost.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.caramel.juniperbot.core.service.impl.AbstractDomainServiceImpl;
import ru.caramel.juniperbot.module.junipost.persistence.entity.JuniPost;
import ru.caramel.juniperbot.module.junipost.persistence.repository.JuniPostRepository;

@Service
public class JuniPostServiceImpl extends AbstractDomainServiceImpl<JuniPost, JuniPostRepository> implements JuniPostService {

    public JuniPostServiceImpl(@Autowired JuniPostRepository repository) {
        super(repository);
    }

    @Override
    protected JuniPost createNew(long guildId) {
        return null;
    }

    @Override
    protected Class<JuniPost> getDomainClass() {
        return JuniPost.class;
    }
}
