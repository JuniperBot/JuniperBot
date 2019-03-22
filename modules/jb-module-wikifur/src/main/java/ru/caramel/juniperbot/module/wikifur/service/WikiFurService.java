package ru.caramel.juniperbot.module.wikifur.service;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.sourceforge.jwbf.core.actions.HttpActionClient;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.core.contentRep.SearchResult;
import net.sourceforge.jwbf.mediawiki.actions.queries.RandomPageTitle;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtPageName;
import org.sweble.wikitext.parser.nodes.WtRedirect;
import org.sweble.wikitext.parser.nodes.WtText;
import org.sweble.wikitext.parser.parser.LinkTargetException;
import ru.caramel.juniperbot.core.message.service.MessageService;
import ru.caramel.juniperbot.module.wikifur.model.WikiFurConfig;
import ru.caramel.juniperbot.module.wikifur.utils.SearchQuery;
import ru.caramel.juniperbot.module.wikifur.utils.TextConverter;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class WikiFurService {

    private static final String SCRIPT_ENDPOINT = "http://ru.wikifur.com/w/";
    private static final String WIKI_URL = "http://ru.wikifur.com/wiki/";


    private MediaWikiBot client;

    private WikiConfig config = WikiFurConfig.generate();

    private WtEngineImpl engine = new WtEngineImpl(config);

    @Value("${spring.application.version}")
    private String version;

    @Autowired
    private MessageService messageService;

    @PostConstruct
    public void init() {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.setUserAgent("JuniperBot DiscordBot (https://github.com/goldrenard/JuniperBot, 1.0)");
        HttpActionClient httpActionClient = HttpActionClient.builder()
                .withUrl(SCRIPT_ENDPOINT)
                .withUserAgent("JuniperBot", version, "goldrenard@gmail.com")
                .withClient(httpClientBuilder.build())
                .build();
        client = new MediaWikiBot(httpActionClient);
    }

    public String getUrl(String article) {
        return WIKI_URL + UriUtils.encode(article, "UTF-8");
    }

    public Article getArticle(String search) {
        if (StringUtils.isEmpty(search)) {
            RandomPageTitle randomPageTitle = new RandomPageTitle(client);
            search = randomPageTitle.getTitle();
        }
        return client.getArticle(search);
    }

    public List<SearchResult> search(String search) {
        SearchQuery searchQuery = new SearchQuery(client, search);
        List<SearchResult> results = new ArrayList<>();
        while (searchQuery.hasNext()) {
            results.add(searchQuery.next());
        }
        return results;
    }

    private EngProcessedPage processedPage(Article article) {
        try {
            PageTitle pageTitle = PageTitle.make(config, article.getTitle());
            PageId pageId = new PageId(pageTitle, Integer.parseInt(article.getRevisionId()));
            return engine.postprocess(pageId, article.getText(), null);
        } catch (LinkTargetException | EngineException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageEmbed renderArticle(String search) {
        return renderArticle(getArticle(search), false);
    }

    private MessageEmbed renderArticle(Article article, boolean redirected) {
        if (article == null || StringUtils.isEmpty(article.getRevisionId())) {
            return null;
        }
        EngProcessedPage processedPage = processedPage(article);
        String redirect = lookupRedirect(processedPage);
        if (redirect != null) {
            if (redirected) {
                return null;
            }
            return renderArticle(getArticle(redirect), true);
        }

        EmbedBuilder embedBuilder = messageService.getBaseEmbed();
        embedBuilder.setTitle(article.getTitle(), WIKI_URL + UriUtils.encode(article.getTitle(), "UTF-8"));
        TextConverter converter = new TextConverter(config, embedBuilder);
        return (MessageEmbed) converter.go(processedPage.getPage());
    }

    public String lookupRedirect(WtNode node) {
        if (node instanceof WtRedirect) {
            WtPageName page = ((WtRedirect) node).getTarget();
            if (!page.isEmpty() && page.get(0) instanceof WtText) {
                return ((WtText) page.get(0)).getContent();
            }
        }
        for (WtNode child : node) {
            String result = lookupRedirect(child);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
