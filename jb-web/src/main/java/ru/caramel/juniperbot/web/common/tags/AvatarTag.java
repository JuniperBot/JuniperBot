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
package ru.caramel.juniperbot.web.common.tags;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.servlet.tags.RequestContextAwareTag;
import ru.caramel.juniperbot.core.model.enums.AvatarType;
import ru.caramel.juniperbot.web.security.model.DiscordUserDetails;
import ru.caramel.juniperbot.web.security.utils.SecurityUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

public class AvatarTag extends RequestContextAwareTag {

    private static final long serialVersionUID = -5188535954386072251L;

    @Getter
    @Setter
    private String avatar;

    @Getter
    @Setter
    private String userId;

    @Getter
    @Setter
    private boolean current;

    @Override
    protected int doStartTagInternal() throws Exception {
        try {
            String avatarUrl;
            String id = userId;
            String avatar = this.avatar;

            if (current || id == null) {
                DiscordUserDetails details = SecurityUtils.getCurrentUser();
                if (details != null) {
                    id = details.getId();
                    avatar = details.getAvatar();
                }
            }
            avatarUrl = AvatarType.USER.getUrl(id, avatar);
            JspWriter out = pageContext.getOut();
            out.write(avatarUrl);
        } catch (Exception ex) {
            throw new JspException(ex);
        }
        return SKIP_BODY;
    }
}
