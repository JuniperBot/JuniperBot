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
                <span class="sr-only">Переключить видимость меню</span>
            </a>
        </c:if>
        <div class="navbar-custom-menu">
            <ul class="nav navbar-nav">
                <li>
                    <a href="https://discord.gg/EdWspu3" target="_blank">
                        <div class="discord-header-icon-wrapper">
                            <div class="discord-header-icon"></div>
                        </div>
                    </a>
                </li>
                <li><a href="https://github.com/GoldRenard/JuniperBotJ" target="_blank"><i class="fa fa-github"></i></a></li>
                <sec:authorize access="hasAnyRole('ROLE_ANONYMOUS')">
                    <li><a href="<c:url value="/login"/>"><i class="fa fa-sign-in"></i> Войти</a></li>
                </sec:authorize>
                <sec:authorize access="isAuthenticated()">
                    <li class="dropdown user user-menu">
                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                            <img src="${userDetails.avatarUrl}" class="user-image" alt="Аватарка">
                            <span class="hidden-xs">${userDetails.userName}</span>
                            <span class="caret"></span>
                        </a>
                        <ul class="dropdown-menu">
                            <li class="user-header">
                                <img src="${userDetails.avatarUrl}" class="img-circle" alt="Аватарка">
                                <p>${userDetails.userName}#${userDetails.discriminator}
                                    <c:if test="${not empty userDetails.email}">
                                        <small>${userDetails.email}</small>
                                    </c:if>
                                </p>
                            </li>
                            <li class="user-footer">
                                <div class="pull-left">
                                    <a href="<c:url value="/servers"/>" class="btn btn-default btn-flat">Серверы</a>
                                </div>
                                <div class="pull-right">
                                    <a href="<c:url value="/logout"/>" class="btn btn-default btn-flat">Выйти</a>
                                </div>
                            </li>
                        </ul>
                    </li>
                </sec:authorize>
            </ul>
        </div>
    </nav>
</header>