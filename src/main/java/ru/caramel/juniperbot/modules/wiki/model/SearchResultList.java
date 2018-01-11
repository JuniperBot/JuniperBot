package ru.caramel.juniperbot.modules.wiki.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import net.sourceforge.jwbf.core.contentRep.SearchResult;
import net.sourceforge.jwbf.core.internal.Checked;

import java.util.List;

@JsonNaming(PropertyNamingStrategy.LowerCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class SearchResultList {

    static class Query {
        private final List<SearchResult> results;

        @JsonCreator
        public Query(@JsonProperty("search") List<SearchResult> results) {
            this.results = Checked.nonNull(results, "search results");
        }
    }

    private final Query query;

    @JsonCreator
    public SearchResultList(@JsonProperty("query") Query query) {
        this.query = Checked.nonNull(query, "search query");
    }

    public List<SearchResult> getResults() {
        return query.results;
    }
}
