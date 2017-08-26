package ru.caramel.juniperbot.commands.audio;

import com.google.api.services.youtube.model.Video;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import ru.caramel.juniperbot.commands.model.BotContext;
import ru.caramel.juniperbot.commands.model.CommandGroup;
import ru.caramel.juniperbot.commands.model.CommandSource;
import ru.caramel.juniperbot.commands.model.DiscordCommand;
import ru.caramel.juniperbot.integration.discord.model.DiscordException;
import ru.caramel.juniperbot.integration.youtube.YouTubeClient;
import ru.caramel.juniperbot.service.MessageService;
import ru.caramel.juniperbot.utils.CommonUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@DiscordCommand(
        key = "ютуб",
        description = "Произвести поиск по указанному запросу по YouTube для выбора воспроизведения",
        source = CommandSource.GUILD,
        group = CommandGroup.MUSIC,
        priority = 103)
public class YouTubeCommand extends AudioCommand {

    @Autowired
    private YouTubeClient youTubeClient;

    @Autowired
    private MessageService messageService;

    @Override
    public boolean doInternal(MessageReceivedEvent message, BotContext context, String content) throws DiscordException {
        List<Video> results = youTubeClient.searchDetailed(content, 10L);
        if (results.isEmpty()) {
            messageManager.onMessage(message.getChannel(), "Ничего не найдено по указанному запросу :flag_white:");
            return false;
        }

        EmbedBuilder builder = messageService.getBaseEmbed();
        builder.setTitle("Результаты поиска:");

        List<String> urls = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Video result = results.get(i);
            long duration = Duration.parse(result.getContentDetails().getDuration()).toMillis();
            String url = youTubeClient.getUrl(result);
            String title = String.format("%d. [%s](%s) [%s]", i + 1, result.getSnippet().getTitle(),
                    url, CommonUtils.formatDuration(duration));
            builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, title, false);
            urls.add(url);
        }

        builder.addField(EmbedBuilder.ZERO_WIDTH_SPACE, String.format("Для воспроизведения введите команду `%sплей N`, где N — номер трека из списка выше",
                context.getPrefix()), false);
        context.setSearchResults(urls);
        context.setSearchMessage(message.getChannel().sendMessage(builder.build()).complete());
        return true;
    }

    @Override
    protected boolean isChannelRestricted() {
        return false;
    }
}
