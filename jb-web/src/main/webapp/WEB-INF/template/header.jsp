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

<header class="main-header">
    <a href="<c:url value="/" />" class="logo">
        <span class="logo-mini"><i class="fa fa-paw"></i></span>
        <span class="logo-lg"><i class="fa fa-paw"></i> <b>Juniper</b>BOT</span>
    </a>

    <nav class="navbar navbar-static-top">
        <c:if test="${sidebarVisible}">
            <a href="#" class="sidebar-toggle" data-toggle="push-menu" role="button">
                <span class="sr-only"><spring:message code="global.header.toggle-menu"/></span>
            </a>
        </c:if>

        <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse" aria-expanded="false">
            <i class="fa fa-bars"></i>
        </button>

        <div class="navbar-collapse pull-left collapse" id="navbar-collapse" aria-expanded="false" style="height: 1px;">
            <ul class="nav navbar-nav">
                <c:forEach items="${navigationMenu}" var="item">
                    <c:if test="${not sidebarVisible and item.navbar}">
                        <tiles:insertDefinition name="menu.item">
                            <tiles:putAttribute name="item" value="${item}" />
                        </tiles:insertDefinition>
                    </c:if>
                </c:forEach>
            </ul>
        </div>

        <div class="navbar-custom-menu">
            <ul class="nav navbar-nav">
                <li class="dropdown tasks-menu">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                        <i class="fa fa-globe"></i> ${jb:getDisplayLanguage()} <span class="caret"></span>
                    </a>
                    <ul class="dropdown-menu language-menu">
                        <li>
                            <ul class="menu">
                                <li>
                                    <a href="<%=request.getContextPath()%>?jb_locale=ru">
                                        <div class="pull-left language-title">Русский</div>
                                        <div class="pull-right countryFlag ru"></div>
                                        <div class="clearfix"></div>
                                    </a>
                                </li>
                                <li>
                                    <a href="<%=request.getContextPath()%>?jb_locale=en">
                                        <div class="pull-left language-title">English</div>
                                        <div class="pull-right countryFlag uk"></div>
                                        <div class="pull-right countryFlag us"></div>
                                        <div class="clearfix"></div>
                                    </a>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </li>
                <li title="<spring:message code="global.header.discord"/>" data-toggle="tooltip" data-container="body" data-placement="bottom">
                    <a href="<spring:message code="about.support.server"/>" target="_blank">
                        <div class="discord-header-icon-wrapper">
                            <div class="discord-header-icon"></div>
                        </div>
                    </a>
                </li>
                <li title="<spring:message code="global.header.github"/>" data-toggle="tooltip" data-container="body" data-placement="bottom">
                    <a href="<spring:message code="about.support.github"/>" target="_blank"><i class="fa fa-github"></i></a>
                </li>
                <sec:authorize access="hasAnyRole('ROLE_ANONYMOUS')">
                    <li><a href="<c:url value="/login"/>"><i class="fa fa-sign-in"></i> <spring:message code="global.header.login"/></a></li>
                </sec:authorize>
                <sec:authorize access="isAuthenticated()">
                    <li class="dropdown user user-menu">
                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                            <img src="${userDetails.avatarUrl}" class="user-image" alt="<spring:message code="global.header.avatar"/>">
                            <span class="hidden-xs">${userDetails.userName}</span>
                            <span class="caret"></span>
                        </a>
                        <ul class="dropdown-menu">
                            <li class="user-header">
                                <img src="${userDetails.avatarUrl}" class="img-circle" alt="<spring:message code="global.header.avatar"/>">
                                <p>${userDetails.userName}#${userDetails.discriminator}
                                    <c:if test="${not empty userDetails.email}">
                                        <small>${userDetails.email}</small>
                                    </c:if>
                                </p>
                            </li>
                            <li class="user-footer">
                                <div class="pull-left">
                                    <a href="<c:url value="/servers"/>" class="btn btn-default btn-flat"><spring:message code="global.title.servers"/></a>
                                </div>
                                <div class="pull-right">
                                    <a href="<c:url value="/logout"/>" class="btn btn-default btn-flat"><spring:message code="global.header.logout"/></a>
                                </div>
                            </li>
                        </ul>
                    </li>
                </sec:authorize>
            </ul>
        </div>
    </nav>
</header>