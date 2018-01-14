<%--
This file is part of JuniperBotJ.

JuniperBotJ is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

JuniperBotJ is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<div class="container">
    <div class="row">
        <div class="col-md-4">
            <div class="box box-solid">
                <div class="box-header with-border">
                    <i class="fa fa-list-ul"></i>
                    <h3 class="box-title"><spring:message code="page.apidocs.toc.title" /></h3>
                </div>
                <div class="box-body">
                    <ul>
                        <li><a href="#introduction"><spring:message code="page.apidocs.introduction" /></a></li>
                        <li><a href="#rating-group"><spring:message code="page.apidocs.ratingGroup" /></a>
                            <ul>
                                <li><a href="#ranking-list"><spring:message code="page.apidocs.memberList" /></a></li>
                            </ul>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <div id="introduction" class="box box-warning">
        <div class="box-header with-border">
            <h3 class="box-title hyperlink"><spring:message code="page.apidocs.introduction" /> <a href="#introduction"></a></h3>
            <div class="box-tools pull-right">
                <button type="button" class="btn btn-box-tool" data-widget="collapse" data-toggle="tooltip" title="<spring:message code="global.title.collapse" />">
                    <i class="fa fa-minus"></i>
                </button>
            </div>
        </div>
        <div class="box-body"><spring:message code="page.apidocs.introduction.content" /></div>
    </div>

    <h1 id="rating-group" class="hyperlink"><spring:message code="page.apidocs.ratingGroup" /> <a href="#rating-group"></a></h1>

    <div id="ranking-list" class="box box-warning">
        <div class="box-header with-border">
            <h3 class="box-title hyperlink"><spring:message code="page.apidocs.memberList" /> <a href="#ranking-list"></a></h3>
            <div class="box-tools pull-right">
                <button type="button" class="btn btn-box-tool" data-widget="collapse" data-toggle="tooltip" title="<spring:message code="global.title.collapse" />">
                    <i class="fa fa-minus"></i>
                </button>
            </div>
        </div>
        <div class="box-body">
            <h4 class="rest-resource-url"><span class="rest-resource-method">GET</span> <code>/ranking/list/:guildId</code></h4>
            <p><spring:message code="page.apidocs.memberList.description" /></p>

            <h4 class="text-bold"><spring:message code="page.apidocs.details.parameters" /></h4>
            <ul>
                <li>
                    <p><spring:message code="page.apidocs.details.parameters.required" />:</p>
                    <ul>
                        <li><code>:guildId</code> - <spring:message code="page.apidocs.memberList.param.guildId" /></li>
                    </ul>
                </li>
            </ul>

            <h4 class="text-bold"><spring:message code="page.apidocs.details.successResponse" /></h4>
            <spring:message code="page.apidocs.memberList.successResponse.description" />
            <table class="table table-striped table-hover table-responsive">
                <thead>
                <tr>
                    <th><spring:message code="page.apidosc.responseTable.header.field" /></th>
                    <th><spring:message code="page.apidosc.responseTable.header.type" /></th>
                    <th><spring:message code="page.apidosc.responseTable.header.description" /></th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>id</td>
                    <td>snowflake</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.id" /></td>
                </tr>
                <tr>
                    <td>name</td>
                    <td>string</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.name" /></td>
                </tr>
                <tr>
                    <td>discriminator</td>
                    <td>string</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.discriminator" /></td>
                </tr>
                <tr>
                    <td>nick</td>
                    <td>string</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.nick" /></td>
                </tr>
                <tr>
                    <td>avatarUrl</td>
                    <td>string</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.avatarUrl" /></td>
                </tr>
                <tr>
                    <td>level</td>
                    <td>integer</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.level" /></td>
                </tr>
                <tr>
                    <td>remainingExp</td>
                    <td>integer</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.remainingExp" /></td>
                </tr>
                <tr>
                    <td>levelExp</td>
                    <td>integer</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.levelExp" /></td>
                </tr>
                <tr>
                    <td>totalExp</td>
                    <td>integer</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.totalExp" /></td>
                </tr>
                <tr>
                    <td>rank</td>
                    <td>integer</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.rank" /></td>
                </tr>
                <tr>
                    <td>pct</td>
                    <td>integer</td>
                    <td><spring:message code="page.apidocs.memberList.response.field.pct" /></td>
                </tr>
                </tbody>
            </table>

            <h4 class="text-bold"><spring:message code="page.apidocs.details.exampleRqRs" /></h4>
            <ul>
                <li>
                    <p class="text-bold"><spring:message code="page.apidocs.details.request" /></p>
                    <p><spring:message code="page.apidocs.details.thisRequest" />: <code>GET https://juniperbot.ru/api/ranking/list/310850506107125760</code></p>
                </li>
                <li>
                    <p class="text-bold"><spring:message code="page.apidocs.details.response" /></p>
                    <pre>[
  {
    "id":"247734710682255361",
    "name":"Карамелька",
    "discriminator":"1453",
    "nick":"Карамелька",
    "avatarUrl":"https://cdn.discordapp.com/avatars/247734710682255361/0a3618a74a3914aaadb18955f3d2cdd2.png",
    "level":0,
    "remainingExp":76,
    "levelExp":100,
    "totalExp":76,
    "rank":1,
    "pct":76
  }
]</pre>
                </li>
            </ul>
        </div>
    </div>
</div>
