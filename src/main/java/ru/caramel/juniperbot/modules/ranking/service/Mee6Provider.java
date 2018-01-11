package ru.caramel.juniperbot.modules.ranking.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.modules.ranking.model.RankingInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Mee6Provider {

    private static final String LEVELS_PAGE = "https://mee6.xyz/levels/%s";

    private Pattern USER_ID_PATTERN = Pattern.compile("https://cdn.discordapp.com/avatars/(\\d{18})");

    private Pattern NAME_PATTERN = Pattern.compile("\\s*(.*)\\s*#(\\d{4})");

    private Pattern EXP_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)\\s+XP\\s+\\[\\s*(\\d+)\\s+total\\s*\\]");

    private Pattern RANK_PATTERN = Pattern.compile("#(\\d+)");

    private Pattern LEVEL_PATTERN = Pattern.compile("Level\\s+(\\d+)");

    public List<RankingInfo> export(long guildId) throws IOException {
        Document document = Jsoup.connect(String.format(LEVELS_PAGE, guildId)).get();
        if (document != null) {
            Element group = document.selectFirst(".list-group");
            if (group != null) {
                List<RankingInfo> result = new ArrayList<>();
                for (Element entry : group.select(".list-group-item > .row")) {
                    Element imageElement = entry.selectFirst("img.img-circle");
                    if (imageElement != null) {
                        String avatarUrl = imageElement.attr("src");
                        Matcher matcher = USER_ID_PATTERN.matcher(avatarUrl);
                        if (matcher.find()) {
                            String userId = matcher.group(1);
                            if (userId != null) {
                                RankingInfo entryInfo = new RankingInfo();
                                entryInfo.setId(userId);
                                entryInfo.setAvatarUrl(avatarUrl);

                                boolean valid = false;
                                Element expBlock = entry.selectFirst(".col-md-4 > center");
                                if (expBlock != null) {
                                    matcher = EXP_PATTERN.matcher(expBlock.text());
                                    if (matcher.find()) {
                                        entryInfo.setRemainingExp(Long.parseLong(matcher.group(1)));
                                        entryInfo.setLevelExp(Long.parseLong(matcher.group(2)));
                                        entryInfo.setTotalExp(Long.parseLong(matcher.group(3)));
                                        result.add(entryInfo);
                                        valid = true;
                                    }
                                }
                                if (!valid) continue;

                                Element rankBlock = entry.selectFirst("div > h3");
                                if (rankBlock != null) {
                                    matcher = RANK_PATTERN.matcher(rankBlock.text());
                                    if (matcher.find()) {
                                        entryInfo.setRank(Integer.parseInt(matcher.group(1)));
                                    }
                                }
                                Element nameBlock = entry.selectFirst(".col-md-4 > h3");
                                if (nameBlock != null) {
                                    matcher = NAME_PATTERN.matcher(nameBlock.text());
                                    if (matcher.find()) {
                                        entryInfo.setName(matcher.group(1));
                                        entryInfo.setNick(matcher.group(1));
                                        entryInfo.setDiscriminator(matcher.group(2));
                                    }
                                }
                                Element levelBlock = entry.selectFirst(".col-md-2");
                                if (levelBlock != null) {
                                    matcher = LEVEL_PATTERN.matcher(levelBlock.text());
                                    if (matcher.find()) {
                                        entryInfo.setLevel(Integer.parseInt(matcher.group(1)));
                                    }
                                }
                            }
                        }
                    }
                }
                return result;
            }
        }
        return null;
    }
}
