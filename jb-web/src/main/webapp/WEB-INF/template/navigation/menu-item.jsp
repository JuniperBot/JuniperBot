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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<tilesx:useAttribute id="item" name="item" classname="ru.caramel.juniperbot.web.common.navigation.NavigationItem" />

<c:if test="${item.getClass().name == 'ru.caramel.juniperbot.web.common.navigation.MenuItem'}">
    <li class="${empty item.childs ? '' : 'treeview'} ${item.active ? 'active' : ''}">
        <a href="<c:url value="${item.url}"/>" ${item.blank ? 'target="_blank"' : ''}>
            <i class="${item.icon}"></i> <span>${item.name}</span>
            <c:if test="${not empty item.childs}">
            <span class="pull-right-container">
                  <i class="fa fa-angle-left pull-right"></i>
            </span>
            </c:if>
        </a>
        <c:if test="${not empty item.childs}">
            <ul class="treeview-menu">
                <c:forEach items="${item.childs}" var="child">
                    <tiles:insertDefinition name="menu.item">
                        <tiles:putAttribute name="item" value="${child}" />
                    </tiles:insertDefinition>
                </c:forEach>
            </ul>
        </c:if>
    </li>
</c:if>

<c:if test="${item.getClass().name == 'ru.caramel.juniperbot.web.common.navigation.MenuSeparator'}">
    <li class="header">
        <spring:message code="${item.code}"/>
    </li>
</c:if>
