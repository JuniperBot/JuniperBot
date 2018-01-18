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

<div class="jumbotron jumbotron-fluid">
    <div class="container">
        <div class="row">
            <div class="col-md-7">
                <h1 class="jumbo-header"><spring:message code="page.index.title"/></h1>
                <p class="lead"><spring:message code="page.index.subTitle"/></p>
            </div>
            <div class="col-md-5">
                <a href="#"
                   onclick="window.open('https://discordapp.com/api/oauth2/authorize?client_id=${clientId}&scope=bot&permissions=${permissions}', 'newwindow', 'width=500,height=700'); return false;"
                   class="btn btn-default btn-lg btn-add">
                    <spring:message code="page.index.add"/> <img src="<c:url value="/resources/img/discord-logo.svg"/>" class="discord-logo">
                </a>
            </div>
        </div>
        <div class="row">
            <div class="col-md-6 col-md-offset-3">
                <div class="feature">— <spring:message code="page.features.music"/></div>
                <div class="feature">— <spring:message code="page.features.ranking"/></div>
                <div class="feature">— <spring:message code="page.features.custom"/></div>
                <div class="feature">— <spring:message code="page.features.vk"/></div>
            </div>
        </div>
    </div>
</div>