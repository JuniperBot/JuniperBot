package ru.juniperbot.module.wikifur.utils;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import net.sourceforge.jwbf.core.contentRep.SearchResult;
import net.sourceforge.jwbf.mapper.JsonMapper;
import net.sourceforge.jwbf.mediawiki.actions.queries.Search;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import ru.juniperbot.module.wikifur.model.SearchResultList;

public class SearchQuery extends Search {

    private final JsonMapper mapper = new JsonMapper();

    public SearchQuery(MediaWikiBot bot, String query) {
        super(bot, query, SearchWhat.DEFAULT, SearchInfo.DEFAULT, SearchProps.DEFAULT, DEFAULT_NS);
    }

    @Override
    protected ImmutableList<SearchResult> parseElements(String json) {
        SearchResultList resultList = mapper.get(json, SearchResultList.class);
        return ImmutableList.copyOf(resultList.getResults());
    }

    @Override
    protected Optional<String> parseHasMore(String s) {
        return Optional.absent();
    }

    @Override
    public int getTotalHits() {
        return 1;
    }

    @Override
    public String getSuggestion() {
        return null;
    }
}
