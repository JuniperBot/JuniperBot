package ru.caramel.juniperbot.web.common.tags;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.servlet.tags.RequestContextAwareTag;
import ru.caramel.juniperbot.integration.discord.model.AvatarType;
import ru.caramel.juniperbot.security.model.DiscordUserDetails;
import ru.caramel.juniperbot.security.utils.SecurityUtils;

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
