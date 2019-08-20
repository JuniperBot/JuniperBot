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
package ru.juniperbot.common.worker.patreon;

import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.Link;
import com.github.jasminb.jsonapi.Links;
import lombok.NonNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.juniperbot.common.model.patreon.Member;
import ru.juniperbot.common.model.patreon.User;
import ru.juniperbot.common.model.patreon.shared.BaseResource;
import ru.juniperbot.common.model.patreon.shared.Field;
import ru.juniperbot.common.worker.shared.support.JsonApiHttpMessageConverter;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static ru.juniperbot.common.utils.CommonUtils.HTTP_TIMEOUT_DURATION;

public class PatreonAPI {

    private static final String BASE_URI = "https://www.patreon.com/api/oauth2/v2/";

    private final RestTemplate restTemplate;

    public PatreonAPI(@NonNull String accessToken) {

        this.restTemplate = new RestTemplateBuilder()
                .rootUri(BASE_URI)
                .setConnectTimeout(HTTP_TIMEOUT_DURATION)
                .setReadTimeout(HTTP_TIMEOUT_DURATION)
                .uriTemplateHandler(new DefaultUriBuilderFactory(BASE_URI))
                .additionalInterceptors((request, body, execution) -> {
                    HttpHeaders headers = request.getHeaders();
                    headers.add("Authorization", "Bearer " + accessToken);
                    headers.add("User-Agent", "JuniperBot");
                    return execution.execute(request, body);
                })
                .additionalMessageConverters(new JsonApiHttpMessageConverter(
                        Member.class,
                        User.class
                ))
                .build();
    }

    public List<Member> fetchAllMembers(String campaignId) {
        return fetchAllMembers(campaignId, null);
    }

    public List<Member> fetchAllMembers(String campaignId, Collection<Member.MemberField> optionalFields) {
        Set<Member> members = new HashSet<>();
        String cursor = null;
        while (true) {
            JSONAPIDocument<List<Member>> membersPage = fetchPageOfMember(campaignId, 15, cursor, optionalFields);
            members.addAll(membersPage.get());
            cursor = getNextCursorFromDocument(membersPage);
            if (cursor == null) {
                break;
            }
        }
        return new ArrayList<>(members);
    }

    public JSONAPIDocument<List<Member>> fetchPageOfMember(String campaignId,
                                                           int pageSize,
                                                           String pageCursor) {
        return fetchPageOfMember(campaignId, pageSize, pageCursor, null);
    }

    public JSONAPIDocument<List<Member>> fetchPageOfMember(String campaignId,
                                                           int pageSize,
                                                           String pageCursor,
                                                           Collection<Member.MemberField> optionalFields) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(String.format("campaigns/%s/members", campaignId))
                .queryParam("include", "currently_entitled_tiers,user")
                .queryParam("page[count]", pageSize);
        if (pageCursor != null) {
            builder.queryParam("page[cursor]", pageCursor);
        }
        Set<Member.MemberField> optionalMemberAndDefaultFields = new HashSet<>();
        if (optionalFields != null) {
            optionalMemberAndDefaultFields.addAll(optionalFields);
        }
        optionalMemberAndDefaultFields.addAll(Member.MemberField.getDefaultFields());
        addFieldsParam(builder, Member.class, optionalMemberAndDefaultFields);
        addFieldsParam(builder, User.class, User.UserField.getDefaultFields());

        return execute(builder, new ParameterizedTypeReference<JSONAPIDocument<List<Member>>>() {});
    }

    private <T> T execute(UriComponentsBuilder builder, ParameterizedTypeReference<T> type) {
        return restTemplate.exchange(
                URLDecoder.decode(builder.toUriString(), StandardCharsets.UTF_8),
                HttpMethod.GET,
                null,
                type).getBody();
    }

    private String getNextCursorFromDocument(JSONAPIDocument document) {
        Links links = document.getLinks();
        if (links == null) {
            return null;
        }
        Link nextLink = links.getNext();
        if (nextLink == null) {
            return null;
        }
        String nextLinkString = URLDecoder.decode(nextLink.toString(), StandardCharsets.UTF_8);
        MultiValueMap<String, String> queryParameters =
                UriComponentsBuilder.fromUriString(nextLinkString).build().getQueryParams();
        return queryParameters.getFirst("page[cursor]");
    }

    private void addFieldsParam(UriComponentsBuilder builder,
                                Class<? extends BaseResource> type,
                                Collection<? extends Field> fields) {
        String typeName = BaseResource.getType(type);
        builder.queryParam("fields[" + typeName + "]",
                fields.stream().map(Field::getPropertyName).collect(Collectors.joining(",")));
    }
}
