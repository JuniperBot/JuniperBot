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
package ru.caramel.juniperbot.module.ranking.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import ru.caramel.juniperbot.module.ranking.model.RankingInfo;
import ru.caramel.juniperbot.module.ranking.utils.RankingUtils;

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

    private Pattern EXP_JS_PATTERN = Pattern.compile("getXpInfo[(](\\d+)[)]"); // Ha! JavaScript? Really? Why are you so evil?

    private Pattern RANK_PATTERN = Pattern.compile("#(\\d+)");

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

                                matcher = EXP_JS_PATTERN.matcher(entry.toString());
                                if (matcher.find()) {
                                    Long totalExp = Long.parseLong(matcher.group(1));
                                    entryInfo.setTotalExp(totalExp);
                                    entryInfo.setLevel(RankingUtils.getLevelFromExp(totalExp));
                                    entryInfo.setRemainingExp(RankingUtils.getRemainingExp(totalExp));
                                    entryInfo.setLevelExp(RankingUtils.getLevelExp(entryInfo.getLevel()));
                                    result.add(entryInfo);

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
